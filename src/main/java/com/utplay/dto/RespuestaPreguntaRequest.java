package com.utplay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RespuestaPreguntaRequest(
        @NotNull(message = "El identificador de pregunta es obligatorio")
        Long idPregunta,

        @NotBlank(message = "Seleccione una alternativa")
        @Pattern(regexp = "(?i)[ABCD]", message = "La alternativa debe ser A, B, C o D")
        String alternativa
) {
}
