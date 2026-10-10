package com.utplay.repository;

import com.utplay.model.Resultado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResultadoRepository extends JpaRepository<Resultado, Long> {
    List<Resultado> findByUsuarioIdUsuarioOrderByFechaDesc(Long idUsuario);

    Optional<Resultado> findTopByUsuarioIdUsuarioAndQuizIdQuizOrderByPuntajeDesc(
            Long idUsuario,
            Long idQuiz
    );

    @Query("""
            select count(r) from Resultado r
            where r.usuario.idUsuario = :idUsuario
              and r.quiz.curso.idCurso = :idCurso
              and r.puntaje >= r.quiz.puntajeMinimo
            """)
    long contarAprobacionesDeCurso(
            @Param("idUsuario") Long idUsuario,
            @Param("idCurso") Long idCurso
    );
}
