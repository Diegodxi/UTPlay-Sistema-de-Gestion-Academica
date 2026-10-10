package com.utplay;

import com.utplay.dto.ResolverQuizRequest;
import com.utplay.dto.RespuestaPreguntaRequest;
import com.utplay.model.Curso;
import com.utplay.model.Pregunta;
import com.utplay.model.Quiz;
import com.utplay.model.Usuario;
import com.utplay.repository.CursoRepository;
import com.utplay.repository.PreguntaRepository;
import com.utplay.repository.QuizRepository;
import com.utplay.repository.ResultadoRepository;
import com.utplay.repository.UsuarioRepository;
import com.utplay.service.QuizService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizServiceIntegrationTest {

    @Autowired private QuizService quizService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CursoRepository cursoRepository;
    @Autowired private QuizRepository quizRepository;
    @Autowired private PreguntaRepository preguntaRepository;
    @Autowired private ResultadoRepository resultadoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void calculaPuntajeGuardaResultadoYNoPremiaUnIntentoPeor() {
        Usuario usuario = nuevoUsuario();
        Curso curso = nuevoCurso();
        Quiz quiz = nuevoQuiz(curso);
        List<Pregunta> preguntas = List.of(
                nuevaPregunta(quiz, "Pregunta 1", "A"),
                nuevaPregunta(quiz, "Pregunta 2", "B"),
                nuevaPregunta(quiz, "Pregunta 3", "C")
        );

        var primerResultado = quizService.resolver(new ResolverQuizRequest(
                quiz.getIdQuiz(),
                List.of(
                        new RespuestaPreguntaRequest(preguntas.get(0).getIdPregunta(), "A"),
                        new RespuestaPreguntaRequest(preguntas.get(1).getIdPregunta(), "B"),
                        new RespuestaPreguntaRequest(preguntas.get(2).getIdPregunta(), "D")
                )
        ), usuario.getCorreo());

        assertThat(primerResultado.puntaje()).isEqualTo(67);
        assertThat(primerResultado.respuestasCorrectas()).isEqualTo(2);
        assertThat(primerResultado.puntosGanados()).isEqualTo(67);
        assertThat(primerResultado.aprobado()).isTrue();

        var segundoResultado = quizService.resolver(new ResolverQuizRequest(
                quiz.getIdQuiz(),
                List.of(
                        new RespuestaPreguntaRequest(preguntas.get(0).getIdPregunta(), "A"),
                        new RespuestaPreguntaRequest(preguntas.get(1).getIdPregunta(), "D"),
                        new RespuestaPreguntaRequest(preguntas.get(2).getIdPregunta(), "D")
                )
        ), usuario.getCorreo());

        assertThat(segundoResultado.puntaje()).isEqualTo(33);
        assertThat(segundoResultado.puntosGanados()).isZero();
        assertThat(resultadoRepository.findByUsuarioIdUsuarioOrderByFechaDesc(usuario.getIdUsuario()))
                .hasSize(2);
        assertThat(usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow().getPuntosTotales())
                .isEqualTo(67);
    }

    private Usuario nuevoUsuario() {
        Usuario usuario = new Usuario();
        usuario.setNombre("Estudiante de prueba");
        usuario.setCorreo("quiz.test@utp.edu.pe");
        usuario.setPassword(passwordEncoder.encode("ClaveSegura123"));
        usuario.setCicloActual(2);
        return usuarioRepository.save(usuario);
    }

    private Curso nuevoCurso() {
        Curso curso = new Curso();
        curso.setNombreCurso("Curso de prueba");
        curso.setDescripcion("Curso creado para la prueba de integracion");
        curso.setCiclo(1);
        curso.setActivo(true);
        return cursoRepository.save(curso);
    }

    private Quiz nuevoQuiz(Curso curso) {
        Quiz quiz = new Quiz();
        quiz.setTitulo("Quiz de integracion");
        quiz.setDescripcion("Quiz temporal");
        quiz.setCurso(curso);
        quiz.setPuntajeMinimo(60);
        quiz.setActivo(true);
        return quizRepository.save(quiz);
    }

    private Pregunta nuevaPregunta(Quiz quiz, String enunciado, String correcta) {
        Pregunta pregunta = new Pregunta();
        pregunta.setQuiz(quiz);
        pregunta.setEnunciado(enunciado);
        pregunta.setAlternativaA("Alternativa A");
        pregunta.setAlternativaB("Alternativa B");
        pregunta.setAlternativaC("Alternativa C");
        pregunta.setAlternativaD("Alternativa D");
        pregunta.setRespuestaCorrecta(correcta);
        return preguntaRepository.save(pregunta);
    }
}
