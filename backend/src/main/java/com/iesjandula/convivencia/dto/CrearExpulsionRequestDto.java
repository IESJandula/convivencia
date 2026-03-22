package com.iesjandula.convivencia.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CrearExpulsionRequestDto {
    private Integer alumnoId;
    private String jefaturaEmail;
    private List<Integer> parteIds;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
}
