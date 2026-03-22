package com.iesjandula.convivencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpulsionPdfItemDto {
    private Integer expulsionId;
    private Integer alumnoId;
    private String alumnoNombreCompleto;
    private String curso;
    private String grupo;
    private boolean puedeGenerarPdf;
}
