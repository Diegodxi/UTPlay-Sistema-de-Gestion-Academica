package com.utplay.dto;

import java.util.List;

public record ProgresoResponse(
        UsuarioResponse usuario,
        Integer promedioMejoresPuntajes,
        Integer porcentajeAvance,
        Integer quizzesCompletados,
        Long quizzesDisponibles,
        List<ResultadoItemResponse> historial,
        List<LogroResponse> logros
) {
}
