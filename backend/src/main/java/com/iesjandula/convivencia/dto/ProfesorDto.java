package com.iesjandula.convivencia.dto;

import lombok.Data;

@Data
public class ProfesorDto {
    private String email;
    private String nombre;
    private String rol;
    private Boolean esGuardia;
    private Boolean activo;
}
