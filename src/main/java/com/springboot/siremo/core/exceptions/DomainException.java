package com.springboot.siremo.core.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
public abstract class DomainException extends RuntimeException {

    /** Código HTTP asociado a la excepción */
    private final HttpStatus status;
    
    /** Código de error de negocio para identificación programática */
    private final String code;

    /**
     * Constructor protegido para clases derivadas.
     * 
     * @param message Descripción del error
     * @param status Código HTTP asociado
     * @param code Código de error de negocio
     */
    protected DomainException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;

    }
}