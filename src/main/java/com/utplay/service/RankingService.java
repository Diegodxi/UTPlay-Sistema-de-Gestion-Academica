
package com.utplay.service;

import com.utplay.model.Usuario;
import com.utplay.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RankingService {

    private final UsuarioRepository usuarioRepository;

    public RankingService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<RankingItem> obtenerRanking() {

        List<Usuario> usuarios = usuarioRepository
                .findTop10ByOrderByPuntosTotalesDescIdUsuarioAsc();

        List<RankingItem> ranking = new ArrayList<>();

        int posicion = 1;

        for (Usuario usuario : usuarios) {

            RankingItem item = new RankingItem(
                    posicion,
                    usuario.getIdUsuario(),
                    usuario.getNombre(),
                    usuario.getNivel(),
                    usuario.getPuntosTotales()
            );

            ranking.add(item);
            posicion++;
        }

        return ranking;
    }

    public record RankingItem(
            int posicion,
            Long idUsuario,
            String nombre,
            Integer nivel,
            Integer puntosTotales
    ) {
    }
}
