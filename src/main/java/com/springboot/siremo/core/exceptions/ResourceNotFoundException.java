package com.springboot.siremo.core.exceptions;

import org.springframework.http.HttpStatus;


public class ResourceNotFoundException extends DomainException {

    /**
     * Constructor con mensaje personalizado.
     * 
     * @param message Mensaje descriptivo del recurso no encontrado
     */
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    /**
     * Constructor con nombre del recurso e ID.
     * 
     * @param resourceName Nombre del tipo de recurso
     * @param id Identificador del recurso
     */
    public ResourceNotFoundException(String resourceName, Long id) {
        super(
                String.format("%s con id %d no encontrado", resourceName, id),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }

    /**
     * Constructor con mensaje y estado HTTP personalizado.
     * 
     * @param message Mensaje descriptivo
     * @param httpStatus Estado HTTP personalizado
     */
    public ResourceNotFoundException(String message, HttpStatus httpStatus) {
        super(message, httpStatus, "RESOURCE_NOT_FOUND");
    }

    /**
     * Constructor con mensaje, estado HTTP y código de error personalizados.
     * 
     * @param message Mensaje descriptivo
     * @param httpStatus Estado HTTP personalizado
     * @param errorCode Código de error personalizado
     */
    public ResourceNotFoundException(String message, HttpStatus httpStatus, String errorCode) {
        super(message, httpStatus, errorCode);
    }
}
