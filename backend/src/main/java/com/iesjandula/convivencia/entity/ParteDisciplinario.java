package com.iesjandula.convivencia.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "partes_disciplinarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParteDisciplinario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "profesor_email", nullable = false)
    private Profesor profesor;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 50)
    private String curso;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "alumno_id", nullable = false)
    private Alumno alumno;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gravedad gravedad = Gravedad.LEVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "medida_tomada", nullable = false)
    private MedidaTomada medidaTomada;

    @Column(columnDefinition = "TEXT")
    private String tareas;

    @Column(name = "archivo_url", length = 500)
    private String archivoUrl;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "conducta_id", nullable = false)
    private ConductaConvivencia conducta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_computo", nullable = false, length = 12)
    private EstadoComputo estadoComputo = EstadoComputo.ACTIVO;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (gravedad == null) {
            gravedad = Gravedad.LEVE;
        }
        if (estado == null) {
            estado = Estado.PENDIENTE;
        }
        if (estadoComputo == null) {
            estadoComputo = EstadoComputo.ACTIVO;
        }
        if (activo == null) {
            activo = true;
        }
        createdAt = LocalDateTime.now();
    }

    public enum Gravedad {
        LEVE,
        GRAVE
    }

    public enum MedidaTomada {
        AULA_CONVIVENCIA,
        SE_QUEDA_EN_CLASE
    }

    public enum Estado {
        PENDIENTE,
        EVALUADO
    }

    public enum EstadoComputo {
        ACTIVO,
        COMPUTADO
    }
}
