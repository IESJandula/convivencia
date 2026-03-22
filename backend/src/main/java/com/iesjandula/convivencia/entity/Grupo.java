package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grupos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String curso;

    @Column(nullable = false, length = 10)
    private String letra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tutor_email", unique = true)
    private Profesor tutor;

    @Column(nullable = false)
    private Boolean activo = true;
}
