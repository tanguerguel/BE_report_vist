package com.springboot.siremo.modules.monitoreo.utils;

import java.util.List;
import java.util.Map;

public final class VistaPreviaFichaConstants {

    private VistaPreviaFichaConstants() {
    }

    public static final String NO_ENCONTRADA = "Ficha no encontrada";
    public static final String ERROR_PDF = "No se pudo generar el PDF de la ficha";

    public static final String MONITOREO_NO_ENCONTRADO = "Monitoreo no encontrado";

    public static final String PLANTILLA = "fichas/hoja-ficha";
    public static final String PLANTILLA_MONITOREO = "fichas/hoja-monitoreo";
    /** Plantillas JasperReports (resources/reportes/...jrxml). */
    public static final String JASPER_FICHA = "fichas/ficha";
    public static final String JASPER_ITEMS = "fichas/ficha_items";
    public static final String JASPER_MATRIZ = "fichas/ficha_matriz";
    public static final String JASPER_RANGOS = "fichas/ficha_rangos";

    public static final String ENCABEZADO = "static/fichas/encabezado-institucional.png";

    /** Niveles de logro de la escala ordinal, en orden (los mismos que muestra el front). */
    public static final Map<Integer, String> NIVELES = Map.of(
            1, "Inicio",
            2, "En proceso",
            3, "Satisfactorio",
            4, "Destacado");

    /** Niveles educativos en orden, con su etiqueta. */
    public static final List<Map.Entry<String, String>> NIVELES_EDUCATIVOS = List.of(
            Map.entry("INICIAL", "Inicial"),
            Map.entry("PRIMARIA", "Primaria"),
            Map.entry("SECUNDARIA", "Secundaria"));
}
