package com.springboot.siremo.core.exceptions.handlers;

import com.springboot.siremo.core.exceptions.DomainException;
import com.springboot.siremo.core.responses.ErrorResponse;
import com.springboot.siremo.core.responses.ErrorResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Subconjunto del manejador global del back de SIREMO: los errores que puede producir este servicio.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ErrorResponseFactory factory;

    @Value("${production:false}")
    private boolean production;

    public GlobalExceptionHandler(ErrorResponseFactory factory) {
        this.factory = factory;
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        ErrorResponse response = factory.build(ex.getStatus(), ex.getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        ErrorResponse response = factory.build(
                HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Tipo de argumento es invalido", request.getRequestURI());
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse response = factory.build(
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Recurso no encontrado", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse response = factory.build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                production ? "Ha ocurrido un error interno en el servidor" : ex.getMessage(),
                request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(response);
    }
}
