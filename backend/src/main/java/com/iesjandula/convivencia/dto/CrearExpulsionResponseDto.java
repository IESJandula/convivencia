package com.iesjandula.convivencia.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrearExpulsionResponseDto {
    private Integer expulsionId;
    private Integer partesComputados;
    private Integer tareasPendientesGeneradas;
}
