package cl.respirarysoltar.talleres.api;

import cl.respirarysoltar.talleres.api.dto.InscripcionRequest;
import cl.respirarysoltar.talleres.api.dto.TallerRequest;
import cl.respirarysoltar.talleres.domain.Inscripcion;
import cl.respirarysoltar.talleres.domain.Taller;
import cl.respirarysoltar.talleres.service.TallerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API REST de talleres (RF-06) e inscripciones con control atómico de cupo y
 * lista de espera (RF-07).
 */
@RestController
@RequestMapping("/api/talleres")
public class TallerController {

    private final TallerService service;

    public TallerController(TallerService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Taller> crear(@Valid @RequestBody TallerRequest request) {
        Taller creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.getId()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @GetMapping
    public List<Taller> listar(@RequestParam(required = false, defaultValue = "false") boolean proximos) {
        return service.listar(proximos);
    }

    @GetMapping("/{id}")
    public Taller obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public Taller actualizar(@PathVariable UUID id, @Valid @RequestBody TallerRequest request) {
        return service.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/inscripciones")
    public ResponseEntity<Inscripcion> inscribir(@PathVariable UUID id,
                                                 @Valid @RequestBody InscripcionRequest request) {
        Inscripcion inscripcion = service.inscribir(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscripcion);
    }

    @GetMapping("/{id}/inscripciones")
    public List<Inscripcion> listarInscripciones(@PathVariable UUID id) {
        return service.listarInscripciones(id);
    }

    @DeleteMapping("/{id}/inscripciones/{inscripcionId}")
    public ResponseEntity<Void> cancelarInscripcion(@PathVariable UUID id, @PathVariable UUID inscripcionId) {
        service.cancelarInscripcion(id, inscripcionId);
        return ResponseEntity.noContent().build();
    }
}
