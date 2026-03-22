package com.iesjandula.convivencia.dto;

import lombok.Data;

@Data
public class ActualizarTareaExpulsionRequestDto {
    private String profesorEmail;
    private String descripcionTarea;
    private Boolean completar;
}
