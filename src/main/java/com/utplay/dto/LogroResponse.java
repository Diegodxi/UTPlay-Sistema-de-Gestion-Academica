package com.utplay.dto;

import java.time.LocalDateTime;

public record LogroResponse(
        Long idLogro,
        String nombre,
        String descripcion,
        Integer puntosRequeridos,
        LocalDateTime fechaObtenido
) {
}
