package cl.respirarysoltar.talleres.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de entrada para crear o actualizar un taller (RF-06). */
public record TallerRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        String nombre,

        @Size(max = 500)
        String descripcion,

        @NotNull(message = "La fecha de inicio es obligatoria")
        @Future(message = "La fecha de inicio debe ser futura")
        java.time.LocalDateTime fechaInicio,

        @NotNull(message = "El cupo total es obligatorio")
        @Min(value = 1, message = "El taller debe tener al menos 1 cupo")
        Integer cupoTotal
) {
}
