package com.springboot.siremo.core.responses;


import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;


public class ErrorResponseFactory {

    /**
     * Crea un ErrorResponse básico con la información del error.
     *
     * @param status  Estado HTTP del error
     * @param code    Código de error de negocio
     * @param message Mensaje descriptivo
     * @param path    Ruta de la petición
     * @return ErrorResponse configurado
     */
    public ErrorResponse build(
        HttpStatus status,
        String code,
        String message,
        String path
    ) {
        return ErrorResponse.builder()
            .status(status.value())
            .code(code)
            .message(message)
            .path(path)
            .build();
    }

    /**
     * Crea un ErrorResponse con errores de validación por campo.
     *
     * @param status  Estado HTTP del error
     * @param code    Código de error de negocio
     * @param message Mensaje descriptivo
     * @param path    Ruta de la petición
     * @param errors  Mapa de errores de validación (campo -> lista de mensajes)
     * @return ErrorResponse configurado con errores
     */
    public ErrorResponse build(
        HttpStatus status,
        String code,
        String message,
        String path,
        Map<String, List<String>> errors
    ) {
        return ErrorResponse.builder()
            .status(status.value())
            .code(code)
            .message(message)
            .path(path)
            .errors(errors)
            .build();
    }
}
