package com.iesjandula.convivencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TareaExpulsionDto {
    private Integer id;
    private Integer expulsionId;
    private String profesorEmail;
    private String profesorNombre;
    private String alumnoNombreCompleto;
    private LocalDate fechaInicioExpulsion;
    private LocalDate fechaFinExpulsion;
    private String asignatura;
    private String descripcionTarea;
    private String estado;
}
