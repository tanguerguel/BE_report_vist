package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Dimensión de la ficha aplicada: columnas de valoración (los puntajes que ofrecen sus ítems), ítems
 * con el puntaje marcado y el puntaje parcial.
 */
public record DimensionAplicadaResponse(
        UUID id,
        String nombre,
        List<String> puntajes,
        List<ItemAplicadoResponse> items,
        String parcial
) {
}
