package com.utplay.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "codigo_curso", unique = true, length = 20)
    private String codigoCurso;

    @Column(
            name = "nombre_curso",
            nullable = false,
            unique = true,
            length = 120
    )
    private String nombreCurso;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer ciclo;

    @Column(precision = 5, scale = 2)
    private BigDecimal creditos;

    @Column(name = "horas_semanales", precision = 5, scale = 2)
    private BigDecimal horasSemanales;

    @Column(name = "tipo_curso", nullable = false, length = 1)
    private String tipoCurso = "O";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "curso_prerrequisito",
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(
                    name = "id_prerrequisito"
            )
    )
    @OrderBy("ciclo ASC, nombreCurso ASC")
    private List<Curso> prerrequisitos = new ArrayList<>();

    // Compatibilidad temporal con el servicio actual.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prerrequisito")
    private Curso prerrequisito;

    @Column(nullable = false)
    private Boolean activo = true;
}