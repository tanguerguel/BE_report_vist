package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * La ficha tal como se imprime: título, distintivo, norma, escala de valoración y dimensiones con sus
 * ítems y su matriz de puntajes.
 */
public record HojaFichaResponse(
        UUID id,
        String nombre,
        String distintivo,
        String normaCodigo,
        String codigoVersion,
        String estado,
        List<EscalaNivelResponse> escala,
        List<DimensionHojaResponse> dimensiones
) {
}
