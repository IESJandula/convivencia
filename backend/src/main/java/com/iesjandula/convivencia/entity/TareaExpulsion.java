package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tareas_expulsion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TareaExpulsion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "expulsion_id", nullable = false)
    private Expulsion expulsion;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profesor_email", nullable = false, columnDefinition = "varchar(100)")
    private Profesor profesor;

    @Column(nullable = false, length = 100)
    private String asignatura;

    @Column(name = "descripcion_tarea", columnDefinition = "TEXT")
    private String descripcionTarea;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    public enum Estado {
        PENDIENTE,
        COMPLETADA
    }
}
