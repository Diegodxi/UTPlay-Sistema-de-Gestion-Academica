package com.utplay.dto;

import java.util.Map;

public record PreguntaResponse(
        Long idPregunta,
        String enunciado,
        Map<String, String> alternativas
) {
}
