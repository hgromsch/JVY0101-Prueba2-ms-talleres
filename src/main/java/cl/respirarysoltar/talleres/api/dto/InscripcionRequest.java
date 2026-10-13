package cl.respirarysoltar.talleres.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Datos de entrada para inscribir un paciente a un taller (RF-07). */
public record InscripcionRequest(
        @NotNull(message = "El paciente es obligatorio")
        UUID pacienteId
) {
}
