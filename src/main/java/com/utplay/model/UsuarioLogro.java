package com.utplay.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_logro")
@Getter
@Setter
@NoArgsConstructor
public class UsuarioLogro {

    @EmbeddedId
    private UsuarioLogroId id;

    @MapsId("idUsuario")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @MapsId("idLogro")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_logro", nullable = false)
    private Logro logro;

    @Column(name = "fecha_obtenido", nullable = false, updatable = false)
    private LocalDateTime fechaObtenido;

    public UsuarioLogro(Usuario usuario, Logro logro) {
        this.usuario = usuario;
        this.logro = logro;
        this.id = new UsuarioLogroId(usuario.getIdUsuario(), logro.getIdLogro());
        this.fechaObtenido = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (fechaObtenido == null) {
            fechaObtenido = LocalDateTime.now();
        }
    }
}
