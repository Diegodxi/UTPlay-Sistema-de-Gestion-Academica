package com.utplay.dto;

public record QuizResumenResponse(
        Long idQuiz,
        String titulo,
        String descripcion,
        Integer puntajeMinimo
) {
}
