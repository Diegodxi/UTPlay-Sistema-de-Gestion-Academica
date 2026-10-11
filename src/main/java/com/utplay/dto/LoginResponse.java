package com.utplay.dto;

public record LoginResponse(
        String token,
        String tipo,
        Long expiraEnMilisegundos,
        UsuarioResponse usuario
) {
}
