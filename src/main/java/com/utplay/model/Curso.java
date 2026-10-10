package com.utplay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Long idCurso;

    @Column(name = "nombre_curso", nullable = false, unique = true, length = 120)
    private String nombreCurso;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer ciclo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prerrequisito")
    private Curso prerrequisito;

    @Column(nullable = false)
    private Boolean activo = true;
}
