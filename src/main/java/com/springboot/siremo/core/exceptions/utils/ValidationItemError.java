package com.springboot.siremo.core.exceptions.utils;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationItemError {

    /**
     * Índice del elemento dentro del listado.
     */
    private Integer index;

    /**
     * Errores agrupados por nombre de campo.
     */
    private Map<String, List<String>> fields;

}