package com.iesjandula.convivencia.dto;

import lombok.Data;

@Data
public class LoginResponseDto {
    private Boolean success;
    private String email;
    private String nombre;
    private String rol;
    private Boolean esGuardia;
    private Boolean activo;
    private String message;
}
