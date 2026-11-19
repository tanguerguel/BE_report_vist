package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.List;

/** Bloque de datos de identificación («Datos del monitor», etc.). */
public record SeccionHojaResponse(String titulo, List<DatoHojaResponse> datos) {
}
