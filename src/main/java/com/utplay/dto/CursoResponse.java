package com.utplay.dto;

import java.util.List;

public record CursoResponse(
        Long idCurso,
        String nombreCurso,
        String descripcion,
        Integer ciclo,
        Long idPrerrequisito,
        String nombrePrerrequisito,
        boolean disponible,
        String motivoBloqueo,
        List<QuizResumenResponse> quizzes
) {
}
