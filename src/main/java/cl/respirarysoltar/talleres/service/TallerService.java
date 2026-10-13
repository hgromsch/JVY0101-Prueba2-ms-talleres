package cl.respirarysoltar.talleres.service;

import cl.respirarysoltar.talleres.api.dto.InscripcionRequest;
import cl.respirarysoltar.talleres.api.dto.TallerRequest;
import cl.respirarysoltar.talleres.domain.EstadoInscripcion;
import cl.respirarysoltar.talleres.domain.Inscripcion;
import cl.respirarysoltar.talleres.domain.Taller;
import cl.respirarysoltar.talleres.exception.RecursoNoEncontradoException;
import cl.respirarysoltar.talleres.exception.ReglaNegocioException;
import cl.respirarysoltar.talleres.repository.InscripcionRepository;
import cl.respirarysoltar.talleres.repository.TallerRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lógica de negocio de talleres (RF-06) e inscripciones con control de cupo (RF-07).
 *
 * El cupo se decrementa de forma atómica en Redis (DECR) para evitar sobreventa en
 * concurrencia; la base de datos queda como respaldo/reporte (cupoDisponible).
 * Si el cupo se agota, la inscripción queda EN_ESPERA (lista de espera).
 */
@Service
@Transactional
public class TallerService {

    private final TallerRepository tallerRepository;
    private final InscripcionRepository inscripcionRepository;
    private final StringRedisTemplate redisTemplate;

    public TallerService(TallerRepository tallerRepository,
                         InscripcionRepository inscripcionRepository,
                         StringRedisTemplate redisTemplate) {
        this.tallerRepository = tallerRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.redisTemplate = redisTemplate;
    }

    public Taller crear(TallerRequest request) {
        Taller taller = new Taller(request.nombre(), request.descripcion(), request.fechaInicio(), request.cupoTotal());
        Taller guardado = tallerRepository.save(taller);
        inicializarCupo(guardado);
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<Taller> listar(boolean soloProximos) {
        return soloProximos
                ? tallerRepository.findByFechaInicioAfterOrderByFechaInicioAsc(LocalDateTime.now())
                : tallerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Taller obtener(UUID id) {
        return tallerRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Taller no encontrado: " + id));
    }

    public Taller actualizar(UUID id, TallerRequest request) {
        Taller taller = obtener(id);
        if (request.cupoTotal() < taller.getCupoTotal() - taller.getCupoDisponible()) {
            throw new ReglaNegocioException("El cupo total no puede ser menor que los cupos ya ocupados");
        }
        int ocupados = taller.getCupoTotal() - taller.getCupoDisponible();
        taller.setNombre(request.nombre());
        taller.setDescripcion(request.descripcion());
        taller.setFechaInicio(request.fechaInicio());
        taller.setCupoTotal(request.cupoTotal());
        taller.setCupoDisponible(request.cupoTotal() - ocupados);
        Taller actualizado = tallerRepository.save(taller);
        redisTemplate.opsForValue().set(claveCupo(id), String.valueOf(actualizado.getCupoDisponible()));
        return actualizado;
    }

    public void eliminar(UUID id) {
        Taller taller = obtener(id);
        tallerRepository.delete(taller);
        redisTemplate.delete(claveCupo(id));
    }

    /**
     * RF-07: inscribe al paciente descontando el cupo de forma atómica en Redis.
     * Sin cupo, la inscripción queda EN_ESPERA en vez de perderse.
     */
    public Inscripcion inscribir(UUID tallerId, InscripcionRequest request) {
        Taller taller = obtener(tallerId);
        if (inscripcionRepository.existsByTallerIdAndPacienteIdAndEstadoIn(tallerId, request.pacienteId(),
                List.of(EstadoInscripcion.CONFIRMADA, EstadoInscripcion.EN_ESPERA))) {
            throw new ReglaNegocioException("El paciente ya tiene una inscripción activa en este taller");
        }

        inicializarCupo(taller);
        Long restante = redisTemplate.opsForValue().decrement(claveCupo(tallerId));

        if (restante != null && restante >= 0) {
            taller.setCupoDisponible(restante.intValue());
            tallerRepository.save(taller);
            return guardarInscripcion(taller, request.pacienteId(), EstadoInscripcion.CONFIRMADA);
        }

        // Cupo agotado: se revierte el decremento y el paciente queda en lista de espera.
        redisTemplate.opsForValue().increment(claveCupo(tallerId));
        return guardarInscripcion(taller, request.pacienteId(), EstadoInscripcion.EN_ESPERA);
    }

    @Transactional(readOnly = true)
    public List<Inscripcion> listarInscripciones(UUID tallerId) {
        obtener(tallerId);
        return inscripcionRepository.findByTallerId(tallerId);
    }

    /** Cancela la inscripción; si estaba confirmada, devuelve el cupo al taller. */
    public void cancelarInscripcion(UUID tallerId, UUID inscripcionId) {
        Taller taller = obtener(tallerId);
        Inscripcion inscripcion = inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Inscripción no encontrada: " + inscripcionId));
        if (!inscripcion.getTaller().getId().equals(tallerId)) {
            throw new ReglaNegocioException("La inscripción no pertenece al taller indicado");
        }
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new ReglaNegocioException("La inscripción ya está cancelada");
        }

        if (inscripcion.getEstado() == EstadoInscripcion.CONFIRMADA) {
            taller.setCupoDisponible(taller.getCupoDisponible() + 1);
            tallerRepository.save(taller);
            redisTemplate.opsForValue().increment(claveCupo(tallerId));
        }
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        inscripcionRepository.save(inscripcion);
    }

    /** Crea la clave de cupo en Redis la primera vez, tomando el valor desde la base de datos. */
    private void inicializarCupo(Taller taller) {
        redisTemplate.opsForValue().setIfAbsent(claveCupo(taller.getId()), String.valueOf(taller.getCupoDisponible()));
    }

    private Inscripcion guardarInscripcion(Taller taller, UUID pacienteId, EstadoInscripcion estado) {
        Inscripcion inscripcion = new Inscripcion(pacienteId, estado);
        inscripcion.setTaller(taller);
        return inscripcionRepository.save(inscripcion);
    }

    private String claveCupo(UUID tallerId) {
        return "cupo:" + tallerId;
    }
}
