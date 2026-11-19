package com.springboot.siremo.modules.monitoreo.dto.response;

import java.math.BigDecimal;

/** Nivel de la matriz de una dimensión: su rúbrica y el rango de puntaje que le corresponde. */
public record RangoNivelResponse(
        Integer nivel,
        String etiqueta,
        String rubrica,
        BigDecimal minimo,
        BigDecimal maximo
) {

    /** El rango como se imprime: «5–7», o «Sin rango» si falta un límite. */
    public String rangoTexto() {
        if (minimo == null || maximo == null) {
            return "Sin rango";
        }
        return minimo.stripTrailingZeros().toPlainString() + "–" + maximo.stripTrailingZeros().toPlainString();
    }
}
