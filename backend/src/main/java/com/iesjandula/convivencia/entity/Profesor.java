package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "profesores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Profesor {

    @Id
    @Column(length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.PROFESOR;

    @Column(name = "es_guardia", nullable = false)
    private Boolean esGuardia = false;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (rol == null) {
            rol = Rol.PROFESOR;
        }
        if (esGuardia == null) {
            esGuardia = false;
        }
        if (activo == null) {
            activo = true;
        }
        createdAt = LocalDateTime.now();
    }

    public enum Rol {
        PROFESOR,
        TUTOR,
        JEFATURA,
        ADMIN
    }
}