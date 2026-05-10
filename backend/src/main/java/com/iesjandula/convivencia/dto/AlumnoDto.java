package com.iesjandula.convivencia.dto;

import lombok.Data;

@Data
public class AlumnoDto {
    private String nombre;
    private String apellidos;
    private String curso;
    private String grupo;

    // Métodos personalizados para facilitar la compatibilidad si se usa mapeo manual
    public void fromEntity(com.iesjandula.convivencia.entity.Alumno entity) {
        this.nombre = entity.getNombre();
        this.apellidos = entity.getApellidos();
        if (entity.getGrupo() != null) {
            this.curso = entity.getGrupo().getCurso();
            this.grupo = entity.getGrupo().getLetra();
        }
    }
}
