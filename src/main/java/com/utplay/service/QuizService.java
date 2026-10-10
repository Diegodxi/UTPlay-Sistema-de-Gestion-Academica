package com.utplay.service;

import com.utplay.dto.DetalleRespuestaResponse;
import com.utplay.dto.LogroResponse;
import com.utplay.dto.PreguntaResponse;
import com.utplay.dto.ResolverQuizRequest;
import com.utplay.dto.ResolverQuizResponse;
import com.utplay.dto.RespuestaPreguntaRequest;
import com.utplay.dto.QuizResponse;
import com.utplay.exception.BusinessException;
import com.utplay.exception.ResourceNotFoundException;
import com.utplay.model.Pregunta;
import com.utplay.model.Quiz;
import com.utplay.model.Resultado;
import com.utplay.model.Usuario;
import com.utplay.repository.PreguntaRepository;
import com.utplay.repository.QuizRepository;
import com.utplay.repository.ResultadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuizRepository quizRepository;
    private final PreguntaRepository preguntaRepository;
    private final ResultadoRepository resultadoRepository;
    private final UsuarioService usuarioService;
    private final CursoService cursoService;
    private final GamificacionService gamificacionService;

    public QuizService(
            QuizRepository quizRepository,
            PreguntaRepository preguntaRepository,
            ResultadoRepository resultadoRepository,
            UsuarioService usuarioService,
            CursoService cursoService,
            GamificacionService gamificacionService
    ) {
        this.quizRepository = quizRepository;
        this.preguntaRepository = preguntaRepository;
        this.resultadoRepository = resultadoRepository;
        this.usuarioService = usuarioService;
        this.cursoService = cursoService;
        this.gamificacionService = gamificacionService;
    }

    @Transactional(readOnly = true)
    public QuizResponse obtener(Long idQuiz, String correoAutenticado) {
        Usuario usuario = usuarioService.obtenerPorCorreo(correoAutenticado);
        Quiz quiz = obtenerQuizActivo(idQuiz);
        cursoService.validarDisponible(quiz.getCurso(), usuario);

        List<PreguntaResponse> preguntas = preguntaRepository
                .findByQuizIdQuizOrderByIdPreguntaAsc(idQuiz).stream()
                .map(this::toPreguntaResponse)
                .toList();

        if (preguntas.isEmpty()) {
            throw new BusinessException("Este quiz todavía no tiene preguntas configuradas");
        }

        return new QuizResponse(
                quiz.getIdQuiz(),
                quiz.getTitulo(),
                quiz.getDescripcion(),
                quiz.getCurso().getIdCurso(),
                quiz.getCurso().getNombreCurso(),
                quiz.getPuntajeMinimo(),
                preguntas
        );
    }

    @Transactional
    public ResolverQuizResponse resolver(ResolverQuizRequest request, String correoAutenticado) {
        Usuario usuario = usuarioService.obtenerPorCorreo(correoAutenticado);
        Quiz quiz = obtenerQuizActivo(request.idQuiz());
        cursoService.validarDisponible(quiz.getCurso(), usuario);

        List<Pregunta> preguntas = preguntaRepository
                .findByQuizIdQuizOrderByIdPreguntaAsc(quiz.getIdQuiz());
        if (preguntas.isEmpty()) {
            throw new BusinessException("Este quiz todavía no tiene preguntas configuradas");
        }

        Map<Long, String> respuestas = normalizarYValidarRespuestas(request.respuestas(), preguntas);
        int correctas = 0;
        List<DetalleRespuestaResponse> detalle = new ArrayList<>();

        for (Pregunta pregunta : preguntas) {
            String elegida = respuestas.get(pregunta.getIdPregunta());
            boolean correcta = pregunta.getRespuestaCorrecta().equalsIgnoreCase(elegida);
            if (correcta) {
                correctas++;
            }
            detalle.add(new DetalleRespuestaResponse(
                    pregunta.getIdPregunta(),
                    pregunta.getEnunciado(),
                    elegida,
                    pregunta.getRespuestaCorrecta(),
                    correcta
            ));
        }

        int puntaje = (int) Math.round(correctas * 100.0 / preguntas.size());
        int puntosPotenciales = calcularPuntosPotenciales(puntaje);
        int mejorPuntajeAnterior = resultadoRepository
                .findTopByUsuarioIdUsuarioAndQuizIdQuizOrderByPuntajeDesc(
                        usuario.getIdUsuario(), quiz.getIdQuiz())
                .map(Resultado::getPuntaje)
                .orElse(0);
        int puntosAnteriores = calcularPuntosPotenciales(mejorPuntajeAnterior);
        int puntosGanados = Math.max(0, puntosPotenciales - puntosAnteriores);

        Resultado resultado = new Resultado();
        resultado.setUsuario(usuario);
        resultado.setQuiz(quiz);
        resultado.setPuntaje(puntaje);
        resultado.setRespuestasCorrectas(correctas);
        resultado.setTotalPreguntas(preguntas.size());
        resultado.setPuntosGanados(puntosGanados);
        resultado = resultadoRepository.save(resultado);

        List<LogroResponse> nuevosLogros = gamificacionService.aplicarPuntos(usuario, puntosGanados);

        return new ResolverQuizResponse(
                resultado.getIdResultado(),
                quiz.getIdQuiz(),
                quiz.getTitulo(),
                correctas,
                preguntas.size(),
                puntaje,
                puntaje >= quiz.getPuntajeMinimo(),
                puntosGanados,
                usuario.getPuntosTotales(),
                usuario.getNivel(),
                resultado.getFecha(),
                detalle,
                nuevosLogros
        );
    }

    private Quiz obtenerQuizActivo(Long idQuiz) {
        return quizRepository.findByIdQuizAndActivoTrue(idQuiz)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz no encontrado"));
    }

    private PreguntaResponse toPreguntaResponse(Pregunta pregunta) {
        Map<String, String> alternativas = new LinkedHashMap<>();
        alternativas.put("A", pregunta.getAlternativaA());
        alternativas.put("B", pregunta.getAlternativaB());
        alternativas.put("C", pregunta.getAlternativaC());
        alternativas.put("D", pregunta.getAlternativaD());
        return new PreguntaResponse(pregunta.getIdPregunta(), pregunta.getEnunciado(), alternativas);
    }

    private Map<Long, String> normalizarYValidarRespuestas(
            List<RespuestaPreguntaRequest> enviadas,
            List<Pregunta> preguntas
    ) {
        Map<Long, String> respuestas = new HashMap<>();
        for (RespuestaPreguntaRequest respuesta : enviadas) {
            String anterior = respuestas.put(
                    respuesta.idPregunta(),
                    respuesta.alternativa().toUpperCase(Locale.ROOT)
            );
            if (anterior != null) {
                throw new BusinessException("No puede responder dos veces la misma pregunta");
            }
        }

        Set<Long> idsEsperados = preguntas.stream()
                .map(Pregunta::getIdPregunta)
                .collect(Collectors.toSet());
        if (respuestas.size() != preguntas.size() || !respuestas.keySet().equals(idsEsperados)) {
            throw new BusinessException("Debe responder todas y solamente las preguntas de este quiz");
        }
        return respuestas;
    }

    private int calcularPuntosPotenciales(int puntaje) {
        return puntaje == 100 ? 120 : puntaje;
    }
}
