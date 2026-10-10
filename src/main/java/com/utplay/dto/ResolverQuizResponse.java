package com.utplay.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResolverQuizResponse(
        Long idResultado,
        Long idQuiz,
        String tituloQuiz,
        Integer respuestasCorrectas,
        Integer totalPreguntas,
        Integer puntaje,
        boolean aprobado,
        Integer puntosGanados,
        Integer puntosTotales,
        Integer nivel,
        LocalDateTime fecha,
        List<DetalleRespuestaResponse> detalle,
        List<LogroResponse> nuevosLogros
) {
}
