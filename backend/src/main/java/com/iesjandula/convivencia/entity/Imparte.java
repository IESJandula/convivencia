package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "imparte")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Imparte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profesor_email", nullable = false, columnDefinition = "varchar(100)")
    private Profesor profesor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @Column(nullable = false, length = 100)
    private String asignatura;

    @Column(nullable = false)
    private Boolean activo = true;
}
