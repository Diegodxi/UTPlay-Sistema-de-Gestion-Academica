package com.utplay.dto;

import java.util.List;

public record GamificacionResponse(
        Integer puntosTotales,
        Integer nivel,
        Integer puntosParaSiguienteNivel,
        List<LogroResponse> logros
) {
}
