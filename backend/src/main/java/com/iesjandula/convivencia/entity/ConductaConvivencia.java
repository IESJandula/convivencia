package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "conductas_convivencia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConductaConvivencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gravedad gravedad = Gravedad.LEVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (gravedad == null) {
            gravedad = Gravedad.LEVE;
        }
        createdAt = LocalDateTime.now();
    }

    public enum Gravedad {
        LEVE,
        GRAVE
    }
}

