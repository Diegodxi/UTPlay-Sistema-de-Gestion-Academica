package com.utplay.dto;

import java.time.LocalDateTime;

public record UsuarioResponse(
        Long idUsuario,
        String nombre,
        String correo,
        Integer cicloActual,
        Integer puntosTotales,
        Integer nivel,
        LocalDateTime fechaRegistro
) {
}
