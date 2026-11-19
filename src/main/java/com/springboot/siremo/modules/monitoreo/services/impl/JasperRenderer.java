package com.springboot.siremo.modules.monitoreo.services.impl;

import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Compila las plantillas .jrxml de reportes/ (una sola vez) y llena el reporte en PDF.
 */
@Component
public class JasperRenderer {

    private final Map<String, JasperReport> compilados = new ConcurrentHashMap<>();

    /** La plantilla compilada, por su nombre de archivo sin extensión (p. ej. «fichas/ficha_items»). */
    public JasperReport reporte(String nombre) {
        return compilados.computeIfAbsent(nombre, this::compilar);
    }

    public byte[] pdf(String nombre, Map<String, Object> parametros, Collection<Map<String, ?>> filas) {
        try {
            JasperPrint impreso = JasperFillManager.fillReport(
                    reporte(nombre), new ConcurrentHashMap<>(parametros), new JRMapCollectionDataSource(filas));
            return JasperExportManager.exportReportToPdf(impreso);
        } catch (JRException e) {
            throw new IllegalStateException(VistaPreviaFichaConstants.ERROR_PDF, e);
        }
    }

    private JasperReport compilar(String nombre) {
        String ruta = "/reportes/" + nombre + ".jrxml";
        try (InputStream jrxml = getClass().getResourceAsStream(ruta)) {
            if (jrxml == null) {
                throw new IllegalStateException("No existe la plantilla " + ruta);
            }
            return JasperCompileManager.compileReport(jrxml);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudo compilar la plantilla " + ruta, e);
        }
    }
}
