package com.utplay.service;

import com.utplay.dto.LogroResponse;
import com.utplay.dto.ProgresoResponse;
import com.utplay.dto.ResultadoItemResponse;
import com.utplay.model.Resultado;
import com.utplay.model.Usuario;
import com.utplay.repository.QuizRepository;
import com.utplay.repository.ResultadoRepository;
import com.utplay.repository.UsuarioLogroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResultadoService {

    private final ResultadoRepository resultadoRepository;
    private final QuizRepository quizRepository;
    private final UsuarioLogroRepository usuarioLogroRepository;
    private final UsuarioService usuarioService;

    public ResultadoService(
            ResultadoRepository resultadoRepository,
            QuizRepository quizRepository,
            UsuarioLogroRepository usuarioLogroRepository,
            UsuarioService usuarioService
    ) {
        this.resultadoRepository = resultadoRepository;
        this.quizRepository = quizRepository;
        this.usuarioLogroRepository = usuarioLogroRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public ProgresoResponse obtenerProgreso(Long idUsuario, String correoAutenticado) {
        Usuario usuario = usuarioService.obtenerYVerificar(idUsuario, correoAutenticado);
        List<Resultado> resultados = resultadoRepository
                .findByUsuarioIdUsuarioOrderByFechaDesc(idUsuario);

        Map<Long, Resultado> mejorPorQuiz = new LinkedHashMap<>();
        for (Resultado resultado : resultados) {
            mejorPorQuiz.merge(
                    resultado.getQuiz().getIdQuiz(),
                    resultado,
                    (actual, candidato) -> candidato.getPuntaje() > actual.getPuntaje()
                            ? candidato
                            : actual
            );
        }

        int promedio = mejorPorQuiz.isEmpty()
                ? 0
                : (int) Math.round(mejorPorQuiz.values().stream()
                        .mapToInt(Resultado::getPuntaje)
                        .average()
                        .orElse(0));
        long disponibles = quizRepository.contarDisponiblesHastaCiclo(usuario.getCicloActual());
        int completados = mejorPorQuiz.size();
        int avance = disponibles == 0
                ? 0
                : (int) Math.min(100, Math.round(completados * 100.0 / disponibles));

        List<ResultadoItemResponse> historial = resultados.stream()
                .map(this::toItemResponse)
                .toList();
        List<LogroResponse> logros = usuarioLogroRepository
                .findByUsuarioIdUsuarioOrderByFechaObtenidoDesc(idUsuario).stream()
                .map(GamificacionService::toResponse)
                .toList();

        return new ProgresoResponse(
                UsuarioService.toResponse(usuario),
                promedio,
                avance,
                completados,
                disponibles,
                historial,
                logros
        );
    }

    private ResultadoItemResponse toItemResponse(Resultado resultado) {
        return new ResultadoItemResponse(
                resultado.getIdResultado(),
                resultado.getQuiz().getIdQuiz(),
                resultado.getQuiz().getTitulo(),
                resultado.getQuiz().getCurso().getNombreCurso(),
                resultado.getPuntaje(),
                resultado.getRespuestasCorrectas(),
                resultado.getTotalPreguntas(),
                resultado.getPuntosGanados(),
                resultado.getPuntaje() >= resultado.getQuiz().getPuntajeMinimo(),
                resultado.getFecha()
        );
    }
}
