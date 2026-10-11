package com.utplay.dto;

import java.math.BigDecimal;
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
        List<QuizResumenResponse> quizzes,
        String codigoCurso,
        BigDecimal creditos,
        BigDecimal horasSemanales,
        String tipoCurso,
        List<PrerrequisitoResponse> prerrequisitos
) {
    public record PrerrequisitoResponse(
            Long idCurso,
            String codigoCurso,
            String nombreCurso
    ) {
    }
}