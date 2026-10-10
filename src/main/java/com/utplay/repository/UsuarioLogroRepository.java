package com.utplay.repository;

import com.utplay.model.UsuarioLogro;
import com.utplay.model.UsuarioLogroId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioLogroRepository extends JpaRepository<UsuarioLogro, UsuarioLogroId> {
    List<UsuarioLogro> findByUsuarioIdUsuarioOrderByFechaObtenidoDesc(Long idUsuario);

    boolean existsByUsuarioIdUsuarioAndLogroIdLogro(Long idUsuario, Long idLogro);
}
