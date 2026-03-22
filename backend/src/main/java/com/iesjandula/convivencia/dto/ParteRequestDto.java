package com.iesjandula.convivencia.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ParteRequestDto {
    private String profesorEmail;
    private LocalDate fecha;
    private String curso;
    private Integer alumnoId;
    private String descripcion;
    private String gravedad; // "LEVE" o "GRAVE"
    private String medidaTomada; // "AULA_CONVIVENCIA" o "SE_QUEDA_EN_CLASE"
    private String tareas;
    private String archivoUrl;
    private Integer conductaId;
}
