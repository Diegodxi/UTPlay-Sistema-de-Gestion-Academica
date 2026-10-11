package com.utplay.repository;

import com.utplay.model.Logro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogroRepository extends JpaRepository<Logro, Long> {
    List<Logro> findByPuntosRequeridosLessThanEqualOrderByPuntosRequeridosAsc(Integer puntos);
}
