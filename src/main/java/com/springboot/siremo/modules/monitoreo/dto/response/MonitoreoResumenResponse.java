package com.springboot.siremo.modules.monitoreo.dto.response;

import java.time.LocalDate;
import java.util.UUID;

/** Monitoreo aplicado, para elegir cuál ver. */
public record MonitoreoResumenResponse(
        UUID id,
        String ficha,
        String actor,
        String observado,
        String institucion,
        LocalDate fechaAplicacion,
        Integer numeroVisita,
        String estado
) {
}
