package com.iesjandula.convivencia.dto;

import lombok.Data;

@Data
public class ParteAulaConvivenciaDto {
    private Integer id;
    private Integer alumnoId;
    private String alumnoNombre;
    private String alumnoApellidos;
    private String curso;
    private String grupo;
    private String tareas;
    private String archivoUrl;
    private String descripcion;
}
