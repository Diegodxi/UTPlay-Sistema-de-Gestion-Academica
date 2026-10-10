package com.utplay.repository;

import com.utplay.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Long> {
    List<Curso> findByActivoTrueOrderByCicloAscNombreCursoAsc();

    List<Curso> findByCicloAndActivoTrueOrderByNombreCursoAsc(Integer ciclo);
}
