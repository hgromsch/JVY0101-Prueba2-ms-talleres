package cl.respirarysoltar.talleres.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Inscripción de un paciente a un taller (lado "muchos" de ManyToOne).
 * Si el taller está lleno, la inscripción queda EN_ESPERA (lista de espera, RF-07).
 */
@Entity
@Table(name = "inscripciones")
public class Inscripcion {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoInscripcion estado;

    @Column(nullable = false)
    private LocalDateTime fechaInscripcion = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taller_id", nullable = false)
    private Taller taller;

    protected Inscripcion() {
        // Constructor requerido por JPA
    }

    public Inscripcion(UUID pacienteId, EstadoInscripcion estado) {
        this.pacienteId = pacienteId;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPacienteId() {
        return pacienteId;
    }

    public EstadoInscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInscripcion estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaInscripcion() {
        return fechaInscripcion;
    }

    public Taller getTaller() {
        return taller;
    }

    public void setTaller(Taller taller) {
        this.taller = taller;
    }
}
