package com.utplay.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ResolverQuizRequest(
        @NotNull(message = "El quiz es obligatorio")
        Long idQuiz,

        @NotEmpty(message = "Debe enviar al menos una respuesta")
        List<@Valid RespuestaPreguntaRequest> respuestas
) {
}
