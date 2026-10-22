package com.utplay.dto;

public record DetalleRespuestaResponse(
        Long idPregunta,
        String enunciado,
        String alternativaElegida,
        String respuestaCorrecta,
        boolean correcta,
        String textoElegida,
        String textoCorrecta
) {
}
