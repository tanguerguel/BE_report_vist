package com.springboot.siremo.modules.monitoreo.services;

import com.springboot.siremo.modules.monitoreo.dto.response.HojaMonitoreoResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.MonitoreoResumenResponse;

import java.util.List;
import java.util.UUID;

public interface VistaPreviaMonitoreoService {

    /** Los monitoreos aplicados más recientes, para elegir cuál ver. */
    List<MonitoreoResumenResponse> listarRecientes(int limite);

    /** La hoja del monitoreo aplicado como datos. */
    HojaMonitoreoResponse obtenerHoja(UUID monitoreoId);

    /** La hoja del monitoreo aplicado en HTML. */
    String generarHtml(UUID monitoreoId);

    /** La hoja del monitoreo aplicado en PDF A4, lista para imprimir y firmar. */
    byte[] generarPdf(UUID monitoreoId);
}
