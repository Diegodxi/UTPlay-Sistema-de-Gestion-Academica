package com.utplay.dto;

import java.time.LocalDateTime;

public record ResultadoItemResponse(
        Long idResultado,
        Long idQuiz,
        String tituloQuiz,
        String nombreCurso,
        Integer puntaje,
        Integer respuestasCorrectas,
        Integer totalPreguntas,
        Integer puntosGanados,
        boolean aprobado,
        LocalDateTime fecha
) {
}
