package cl.respirarysoltar.talleres.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Taller grupal ofrecido por la plataforma (RF-06). El control del cupo disponible
 * es el invariante crítico: se decrementa atómicamente en Redis en cada inscripción
 * (mismo patrón que la reserva de horario de ms-agenda) y, al agotarse, las nuevas
 * inscripciones pasan a lista de espera (RF-07).
 */
@Entity
@Table(name = "talleres")
public class Taller {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private Integer cupoTotal;

    @Column(nullable = false)
    private Integer cupoDisponible;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    /** Relación OneToMany: inscripciones de pacientes al taller. */
    @OneToMany(mappedBy = "taller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();

    protected Taller() {
        // Constructor requerido por JPA
    }

    public Taller(String nombre, String descripcion, LocalDateTime fechaInicio, Integer cupoTotal) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.cupoTotal = cupoTotal;
        this.cupoDisponible = cupoTotal;
    }

    @PrePersist
    void alCrear() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public Integer getCupoTotal() {
        return cupoTotal;
    }

    public void setCupoTotal(Integer cupoTotal) {
        this.cupoTotal = cupoTotal;
    }

    public Integer getCupoDisponible() {
        return cupoDisponible;
    }

    public void setCupoDisponible(Integer cupoDisponible) {
        this.cupoDisponible = cupoDisponible;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public List<Inscripcion> getInscripciones() {
        return inscripciones;
    }
}
