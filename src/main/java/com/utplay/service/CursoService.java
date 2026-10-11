
package com.utplay.service;

import com.utplay.dto.CursoResponse;
import com.utplay.dto.QuizResumenResponse;
import com.utplay.exception.BusinessException;
import com.utplay.exception.ResourceNotFoundException;
import com.utplay.model.Curso;
import com.utplay.model.Usuario;
import com.utplay.repository.CursoRepository;
import com.utplay.repository.QuizRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CursoService {

    private final CursoRepository cursoRepository;
    private final QuizRepository quizRepository;
    private final UsuarioService usuarioService;

    public CursoService(
            CursoRepository cursoRepository,
            QuizRepository quizRepository,
            UsuarioService usuarioService
    ) {
        this.cursoRepository = cursoRepository;
        this.quizRepository = quizRepository;
        this.usuarioService = usuarioService;
    }

    // ==========================================
    // LISTAR TODOS LOS CURSOS
    // ==========================================

    @Transactional(readOnly = true)
    public List<CursoResponse> listar(
            String correoAutenticado
    ) {

        Usuario usuario = usuarioService.obtenerPorCorreo(
                correoAutenticado
        );

        return cursoRepository
                .findByActivoTrueOrderByCicloAscNombreCursoAsc()
                .stream()
                .map(curso -> toResponse(curso, usuario))
                .toList();
    }

    // ==========================================
    // LISTAR CURSOS POR CICLO
    // ==========================================

    @Transactional(readOnly = true)
    public List<CursoResponse> listarPorCiclo(
            Integer ciclo,
            String correoAutenticado
    ) {

        if (ciclo == null || ciclo < 0 || ciclo > 10) {
            throw new BusinessException(
                    "El ciclo debe estar entre 0 y 10; "
                    + "0 corresponde a nivelación"
            );
        }

        Usuario usuario = usuarioService.obtenerPorCorreo(
                correoAutenticado
        );

        return cursoRepository
                .findByCicloAndActivoTrueOrderByNombreCursoAsc(ciclo)
                .stream()
                .map(curso -> toResponse(curso, usuario))
                .toList();
    }

    // ==========================================
    // OBTENER UN CURSO
    // ==========================================

    @Transactional(readOnly = true)
    public Curso obtenerCurso(Long idCurso) {

        Curso curso = cursoRepository.findById(idCurso)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Curso no encontrado"
                        )
                );

        if (!Boolean.TRUE.equals(curso.getActivo())) {
            throw new ResourceNotFoundException(
                    "Curso no disponible"
            );
        }

        return curso;
    }

    // ==========================================
    // VALIDAR DISPONIBILIDAD
    // ==========================================

    public void validarDisponible(
            Curso curso,
            Usuario usuario
    ) {

        String motivo = motivoBloqueo(curso, usuario);

        if (motivo != null) {
            throw new BusinessException(motivo);
        }
    }

    // ==========================================
    // COMPROBAR SI EL CURSO ESTÁ DISPONIBLE
    // ==========================================

    public boolean estaDisponible(
            Curso curso,
            Usuario usuario
    ) {

        return motivoBloqueo(curso, usuario) == null;
    }

    // ==========================================
    // CONVERTIR CURSO A RESPUESTA
    // ==========================================

    private CursoResponse toResponse(
            Curso curso,
            Usuario usuario
    ) {

        // Los prerrequisitos continúan visibles.
        // Solo se elimina su efecto de bloqueo.

        List<CursoResponse.PrerrequisitoResponse> prerrequisitos =
                curso.getPrerrequisitos()
                        .stream()
                        .map(requisito ->
                                new CursoResponse.PrerrequisitoResponse(
                                        requisito.getIdCurso(),
                                        requisito.getCodigoCurso(),
                                        requisito.getNombreCurso()
                                )
                        )
                        .toList();

        // Compatibilidad con el frontend anterior.

        var primero = prerrequisitos.isEmpty()
                ? null
                : prerrequisitos.get(0);

        // Determinar disponibilidad según el ciclo.

        String motivo = motivoBloqueo(curso, usuario);

        // Obtener únicamente quizzes activos.

        List<QuizResumenResponse> quizzes = quizRepository
                .findByCursoIdCursoAndActivoTrueOrderByIdQuizAsc(
                        curso.getIdCurso()
                )
                .stream()
                .map(quiz -> new QuizResumenResponse(
                        quiz.getIdQuiz(),
                        quiz.getTitulo(),
                        quiz.getDescripcion(),
                        quiz.getPuntajeMinimo()
                ))
                .toList();

        // Respuesta enviada al frontend.

        return new CursoResponse(
                curso.getIdCurso(),
                curso.getNombreCurso(),
                curso.getDescripcion(),
                curso.getCiclo(),

                primero == null
                        ? null
                        : primero.idCurso(),

                primero == null
                        ? null
                        : primero.nombreCurso(),

                motivo == null,
                motivo,
                quizzes,

                curso.getCodigoCurso(),
                curso.getCreditos(),
                curso.getHorasSemanales(),
                curso.getTipoCurso(),

                prerrequisitos
        );
    }

    // ==========================================
    // NUEVA LÓGICA DE ACCESO A LOS CURSOS
    // ==========================================

    private String motivoBloqueo(
            Curso curso,
            Usuario usuario
    ) {

        // REGLA 1:
        // Un curso inactivo no puede utilizarse.

        if (!Boolean.TRUE.equals(curso.getActivo())) {
            return "Curso no disponible";
        }

        // REGLA 2:
        // Solo se bloquean los cursos de ciclos
        // superiores al ciclo actual del usuario.

        if (curso.getCiclo() > usuario.getCicloActual()) {
            return "Disponible cuando el estudiante llegue al ciclo "
                    + curso.getCiclo();
        }

        // REGLA 3:
        // Los prerrequisitos son informativos.
        // No se necesita aprobarlos para practicar.

        // Si el curso pertenece al ciclo actual
        // o a uno anterior, se permite el acceso.

        return null;
    }
}
