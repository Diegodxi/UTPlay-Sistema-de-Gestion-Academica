package com.utplay.service;

import com.utplay.dto.CursoResponse;
import com.utplay.dto.QuizResumenResponse;
import com.utplay.exception.BusinessException;
import com.utplay.exception.ResourceNotFoundException;
import com.utplay.model.Curso;
import com.utplay.model.Usuario;
import com.utplay.repository.CursoRepository;
import com.utplay.repository.QuizRepository;
import com.utplay.repository.ResultadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final QuizRepository quizRepository;
    private final ResultadoRepository resultadoRepository;
    private final UsuarioService usuarioService;

    public CursoService(
            CursoRepository cursoRepository,
            QuizRepository quizRepository,
            ResultadoRepository resultadoRepository,
            UsuarioService usuarioService
    ) {
        this.cursoRepository = cursoRepository;
        this.quizRepository = quizRepository;
        this.resultadoRepository = resultadoRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listar(String correoAutenticado) {
        Usuario usuario = usuarioService.obtenerPorCorreo(correoAutenticado);
        return cursoRepository.findByActivoTrueOrderByCicloAscNombreCursoAsc().stream()
                .map(curso -> toResponse(curso, usuario))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> listarPorCiclo(Integer ciclo, String correoAutenticado) {
        if (ciclo == null || ciclo < 1 || ciclo > 10) {
            throw new BusinessException("El ciclo debe estar entre 1 y 10");
        }
        Usuario usuario = usuarioService.obtenerPorCorreo(correoAutenticado);
        return cursoRepository.findByCicloAndActivoTrueOrderByNombreCursoAsc(ciclo).stream()
                .map(curso -> toResponse(curso, usuario))
                .toList();
    }

    @Transactional(readOnly = true)
    public Curso obtenerCurso(Long idCurso) {
        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        if (!Boolean.TRUE.equals(curso.getActivo())) {
            throw new ResourceNotFoundException("Curso no disponible");
        }
        return curso;
    }

    public void validarDisponible(Curso curso, Usuario usuario) {
        String motivo = motivoBloqueo(curso, usuario);
        if (motivo != null) {
            throw new BusinessException(motivo);
        }
    }

    public boolean estaDisponible(Curso curso, Usuario usuario) {
        return motivoBloqueo(curso, usuario) == null;
    }

    private CursoResponse toResponse(Curso curso, Usuario usuario) {
        Curso prerrequisito = curso.getPrerrequisito();
        String motivoBloqueo = motivoBloqueo(curso, usuario);
        boolean disponible = motivoBloqueo == null;
        List<QuizResumenResponse> quizzes = quizRepository
                .findByCursoIdCursoAndActivoTrueOrderByIdQuizAsc(curso.getIdCurso()).stream()
                .map(quiz -> new QuizResumenResponse(
                        quiz.getIdQuiz(),
                        quiz.getTitulo(),
                        quiz.getDescripcion(),
                        quiz.getPuntajeMinimo()))
                .toList();

        return new CursoResponse(
                curso.getIdCurso(),
                curso.getNombreCurso(),
                curso.getDescripcion(),
                curso.getCiclo(),
                prerrequisito == null ? null : prerrequisito.getIdCurso(),
                prerrequisito == null ? null : prerrequisito.getNombreCurso(),
                disponible,
                motivoBloqueo,
                quizzes
        );
    }

    private String motivoBloqueo(Curso curso, Usuario usuario) {
        if (curso.getCiclo() > usuario.getCicloActual()) {
            return "Disponible cuando el estudiante llegue al ciclo " + curso.getCiclo();
        }
        Curso prerrequisito = curso.getPrerrequisito();
        if (prerrequisito != null) {
            long aprobaciones = resultadoRepository.contarAprobacionesDeCurso(
                    usuario.getIdUsuario(),
                    prerrequisito.getIdCurso()
            );
            if (aprobaciones == 0) {
                return "Primero debe aprobar " + prerrequisito.getNombreCurso();
            }
        }
        return null;
    }
}
