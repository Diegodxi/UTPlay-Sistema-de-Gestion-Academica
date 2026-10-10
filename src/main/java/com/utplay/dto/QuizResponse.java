package com.utplay.dto;

import java.util.List;

public record QuizResponse(
        Long idQuiz,
        String titulo,
        String descripcion,
        Long idCurso,
        String nombreCurso,
        Integer puntajeMinimo,
        List<PreguntaResponse> preguntas
) {
}
