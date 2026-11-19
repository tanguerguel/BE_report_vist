package com.springboot.siremo.modules.monitoreo.services;

import com.springboot.siremo.modules.monitoreo.dto.response.HojaFichaResponse;

import java.util.UUID;

public interface VistaPreviaFichaService {

    /** La hoja de la ficha como datos, para que el front la dibuje si lo prefiere. */
    HojaFichaResponse obtenerHoja(UUID fichaVersionId);

    /** La hoja de la ficha en HTML, para verla en el navegador. */
    String generarHtml(UUID fichaVersionId);

    /** La hoja de la ficha en PDF A4, lista para imprimir. */
    byte[] generarPdf(UUID fichaVersionId);

    /** La misma hoja en PDF A4, generada con JasperReports desde reportes/fichas/ficha.jrxml. */
    byte[] generarPdfJasper(UUID fichaVersionId);
}
