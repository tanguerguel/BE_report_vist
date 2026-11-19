package com.springboot.siremo.core.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Excepción para requests inválidos (datos faltantes, formato incorrecto, etc.).
 *
 * <p>Se lanza cuando el request no cumple con las validaciones básicas.</p>
 *
 * <p>Código HTTP: 400</p>
 */
public class BadRequestException extends DomainException {

    public BadRequestException(String message) {
        super(message,
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST");
    }

    public BadRequestException(String message, String code) {
        super(message,
                HttpStatus.BAD_REQUEST,
                code);
    }
}
