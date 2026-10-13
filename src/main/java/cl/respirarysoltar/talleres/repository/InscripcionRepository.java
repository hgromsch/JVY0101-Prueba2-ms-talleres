package cl.respirarysoltar.talleres.repository;

import cl.respirarysoltar.talleres.domain.Inscripcion;
import cl.respirarysoltar.talleres.domain.EstadoInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InscripcionRepository extends JpaRepository<Inscripcion, UUID> {

    List<Inscripcion> findByTallerId(UUID tallerId);

    boolean existsByTallerIdAndPacienteIdAndEstadoIn(UUID tallerId, UUID pacienteId, List<EstadoInscripcion> estados);
}
