package com.utplay.repository;

import com.utplay.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    Optional<Quiz> findByIdQuizAndActivoTrue(Long idQuiz);

    List<Quiz> findByCursoIdCursoAndActivoTrueOrderByIdQuizAsc(Long idCurso);

    @Query("select count(q) from Quiz q where q.activo = true and q.curso.ciclo <= :ciclo")
    long contarDisponiblesHastaCiclo(@Param("ciclo") Integer ciclo);
}
