package com.utplay.repository;

import com.utplay.model.Pregunta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PreguntaRepository extends JpaRepository<Pregunta, Long> {
    List<Pregunta> findByQuizIdQuizOrderByIdPreguntaAsc(Long idQuiz);
}
