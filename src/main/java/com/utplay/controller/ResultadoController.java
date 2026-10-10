package com.utplay.controller;

import com.utplay.dto.ProgresoResponse;
import com.utplay.service.ResultadoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/resultados")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    @GetMapping("/{usuario}")
    public ProgresoResponse obtener(
            @PathVariable Long usuario,
            Authentication authentication
    ) {
        return resultadoService.obtenerProgreso(usuario, authentication.getName());
    }
}
