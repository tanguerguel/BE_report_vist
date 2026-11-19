package com.springboot.siremo.core.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Clase genérica para envolver las respuestas de la API.
 * 
 * <p>Proporciona una estructura consistente para todas las respuestas:</p>
 * <ul>
 *   <li><b>success</b>: Indica si la operación fue exitosa</li>
 *   <li><b>data</b>: Datos de la respuesta</li>
 *   <li><b>message</b>: Mensaje descriptivo</li>
 * </ul>
 * 
 * <p>Esta clase se utiliza para estandarizar las respuestas HTTP
 * de todos los endpoints de la API.</p>
 * 
 * @param <T> Tipo de los datos contenidos en la respuesta
 */
@Getter
@Setter
@NoArgsConstructor
public class ApiResponse<T> {

    /** Indica si la operación fue exitosa */
    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private boolean success;
    
    /** Datos de la respuesta */
//    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Datos de la respuesta")
    private T data;
    
    /** Mensaje descriptivo de la respuesta */
    @Schema(description = "Mensaje descriptivo de la respuesta", example = "Operación realizada correctamente")
    private String message;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Schema(hidden = true)
    private Map<String, Object> meta = new HashMap<>();

    /**
     * Crea una respuesta exitosa sin datos y mensaje personalizado.
     *
     * @param message El mensaje de la respuesta
     */
    public ApiResponse(String message) {
        this.success = true;
        this.data = null;
        this.message = message;
    }

    /**
     * Crea una respuesta exitosa con datos y mensaje predeterminado.
     * 
     * @param data Los datos de la respuesta
     */
    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.message = "Ok";
    }

    /**
     * Crea una respuesta exitosa con datos y mensaje personalizado.
     * 
     * @param data Los datos de la respuesta
     * @param message Mensaje descriptivo
     */
    public ApiResponse(T data, String message) {
        this.success = true;
        this.data = data;
        this.message = message;
    }

    public ApiResponse<T> addMeta(String key, Object value) {
        this.meta.put(key, value);
        return this;
    }

    public ApiResponse<T> addMeta(Map<String, Object> meta) {
        this.meta.putAll(meta);
        return this;
    }

}
