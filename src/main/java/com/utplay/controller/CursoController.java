package com.utplay.controller;

import com.utplay.dto.CursoResponse;
import com.utplay.service.CursoService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/cursos")
public class CursoController {

    private final CursoService cursoService;

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoResponse> listar(Authentication authentication) {
        return cursoService.listar(authentication.getName());
    }

    @GetMapping("/ciclo/{numero}")
    public List<CursoResponse> listarPorCiclo(
            @PathVariable Integer numero,
            Authentication authentication
    ) {
        return cursoService.listarPorCiclo(numero, authentication.getName());
    }
}
