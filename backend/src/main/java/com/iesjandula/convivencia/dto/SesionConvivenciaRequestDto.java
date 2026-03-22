package com.iesjandula.convivencia.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SesionConvivenciaRequestDto {
    private Integer parteId;
    private String profesorGuardiaEmail;
    private LocalDate fecha;
    private String tramoHorario; // "1", "2", "3", "4", "5", "6"
    private String comportamiento; // "BIEN", "REGULAR", "MAL"
    private Boolean trabaja;
    private String observaciones;
}
