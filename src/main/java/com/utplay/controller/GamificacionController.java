package com.utplay.controller;

import com.utplay.dto.GamificacionResponse;
import com.utplay.service.GamificacionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gamificacion")
public class GamificacionController {

    private final GamificacionService gamificacionService;

    public GamificacionController(GamificacionService gamificacionService) {
        this.gamificacionService = gamificacionService;
    }

    @GetMapping("/{usuario}")
    public GamificacionResponse obtener(
            @PathVariable Long usuario,
            Authentication authentication
    ) {
        return gamificacionService.obtener(usuario, authentication.getName());
    }
}
