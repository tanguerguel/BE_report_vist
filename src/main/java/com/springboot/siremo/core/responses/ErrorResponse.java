package com.springboot.siremo.core.responses;


import com.springboot.siremo.core.exceptions.utils.ValidationItemError;
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
public class ErrorResponse {

    /** Código HTTP del error */
    private Integer status;
    
    /** Código de error de negocio */
    private String code;
    
    /** Mensaje descriptivo para el usuario */
    private String message;
    
    /** Ruta de la petición que causó el error */
    private String path;
    
    /** Mapa de errores de validación por campo */
    private Map<String, List<String>> errors;

    private List<ValidationItemError> itemErrors;

}
