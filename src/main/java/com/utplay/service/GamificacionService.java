package com.utplay.service;

import com.utplay.dto.GamificacionResponse;
import com.utplay.dto.LogroResponse;
import com.utplay.model.Logro;
import com.utplay.model.Usuario;
import com.utplay.model.UsuarioLogro;
import com.utplay.repository.LogroRepository;
import com.utplay.repository.UsuarioLogroRepository;
import com.utplay.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class GamificacionService {

    public static final int PUNTOS_POR_NIVEL = 500;

    private final UsuarioRepository usuarioRepository;
    private final LogroRepository logroRepository;
    private final UsuarioLogroRepository usuarioLogroRepository;
    private final UsuarioService usuarioService;

    public GamificacionService(
            UsuarioRepository usuarioRepository,
            LogroRepository logroRepository,
            UsuarioLogroRepository usuarioLogroRepository,
            UsuarioService usuarioService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.logroRepository = logroRepository;
        this.usuarioLogroRepository = usuarioLogroRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public List<LogroResponse> aplicarPuntos(Usuario usuario, int puntosGanados) {
        int total = Math.max(0, usuario.getPuntosTotales() + puntosGanados);
        usuario.setPuntosTotales(total);
        usuario.setNivel(calcularNivel(total));
        usuarioRepository.save(usuario);

        List<LogroResponse> nuevos = new ArrayList<>();
        for (Logro logro : logroRepository
                .findByPuntosRequeridosLessThanEqualOrderByPuntosRequeridosAsc(total)) {
            if (!usuarioLogroRepository.existsByUsuarioIdUsuarioAndLogroIdLogro(
                    usuario.getIdUsuario(), logro.getIdLogro())) {
                UsuarioLogro asignacion = usuarioLogroRepository.save(new UsuarioLogro(usuario, logro));
                nuevos.add(toResponse(asignacion));
            }
        }
        return nuevos;
    }

    @Transactional(readOnly = true)
    public GamificacionResponse obtener(Long idUsuario, String correoAutenticado) {
        Usuario usuario = usuarioService.obtenerYVerificar(idUsuario, correoAutenticado);
        List<LogroResponse> logros = usuarioLogroRepository
                .findByUsuarioIdUsuarioOrderByFechaObtenidoDesc(idUsuario).stream()
                .map(GamificacionService::toResponse)
                .toList();
        int siguienteUmbral = usuario.getNivel() * PUNTOS_POR_NIVEL;
        int faltantes = Math.max(0, siguienteUmbral - usuario.getPuntosTotales());
        return new GamificacionResponse(
                usuario.getPuntosTotales(),
                usuario.getNivel(),
                faltantes,
                logros
        );
    }

    public static LogroResponse toResponse(UsuarioLogro usuarioLogro) {
        Logro logro = usuarioLogro.getLogro();
        return new LogroResponse(
                logro.getIdLogro(),
                logro.getNombre(),
                logro.getDescripcion(),
                logro.getPuntosRequeridos(),
                usuarioLogro.getFechaObtenido()
        );
    }

    private int calcularNivel(int puntosTotales) {
        return (puntosTotales / PUNTOS_POR_NIVEL) + 1;
    }
}
