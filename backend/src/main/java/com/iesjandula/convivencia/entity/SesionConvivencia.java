package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_convivencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionConvivencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parte_id", nullable = false)
    private ParteDisciplinario parte;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profesor_guardia_email", nullable = false)
    private Profesor profesorGuardia;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "tramo_horario", nullable = false)
    private String tramoHorario;

    @Enumerated(EnumType.STRING)
    private Comportamiento comportamiento;

    private Boolean trabaja;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Comportamiento {
        BIEN,
        REGULAR,
        MAL
    }
}
