package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * La ficha aplicada tal como se imprime para firmar: datos de identificación, escala, valoraciones por
 * dimensión, observaciones, compromisos y firmas.
 */
public record HojaMonitoreoResponse(
        UUID id,
        String titulo,
        String estado,
        List<SeccionHojaResponse> secciones,
        List<EscalaNivelResponse> escala,
        List<DimensionAplicadaResponse> dimensiones,
        String observaciones,
        String compromisoMonitoreado,
        String compromisoMonitor,
        String retorno,
        String firmaMonitoreado,
        String firmaMonitor
) {
}
