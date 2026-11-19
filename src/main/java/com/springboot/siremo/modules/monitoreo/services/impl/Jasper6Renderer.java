package com.springboot.siremo.modules.monitoreo.services.impl;

import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import lombok.RequiredArgsConstructor;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Llena las plantillas JasperReports 6 de reportes-jasper6 (las mismas que se editan en Jaspersoft Studio).
 * Las plantillas traen su propio SQL: aquí solo se les da la conexión de solo lectura del servicio.
 */
@Component
@RequiredArgsConstructor
public class Jasper6Renderer {

    private final DataSource dataSource;
    private Path compilados;

    public byte[] pdf(String reporte, Map<String, Object> parametros) {
        Path archivo = directorio().resolve(reporte + ".jasper");
        try (Connection conexion = dataSource.getConnection()) {
            // Al llenar desde el archivo, los subreportes (ficha_items.jasper...) y la imagen se buscan
            // en su misma carpeta, igual que en Studio.
            JasperPrint impreso = JasperFillManager.fillReport(archivo.toString(), new HashMap<>(parametros), conexion);
            return JasperExportManager.exportReportToPdf(impreso);
        } catch (SQLException | JRException e) {
            throw new IllegalStateException(VistaPreviaFichaConstants.ERROR_PDF, e);
        }
    }

    /** Copia las plantillas del classpath a una carpeta temporal y las compila, una sola vez. */
    private synchronized Path directorio() {
        if (compilados != null) {
            return compilados;
        }
        try {
            Path dir = Files.createTempDirectory("siremo-jasper6-");
            Resource[] recursos = new PathMatchingResourcePatternResolver()
                    .getResources(VistaPreviaFichaConstants.JASPER6_PLANTILLAS);
            for (Resource recurso : recursos) {
                Path destino = dir.resolve(recurso.getFilename());
                try (InputStream in = recurso.getInputStream()) {
                    Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            for (Resource recurso : recursos) {
                String nombre = recurso.getFilename();
                if (nombre != null && nombre.endsWith(".jrxml")) {
                    JasperCompileManager.compileReportToFile(dir.resolve(nombre).toString(),
                            dir.resolve(nombre.replace(".jrxml", ".jasper")).toString());
                }
            }
            compilados = dir;
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JRException e) {
            throw new IllegalStateException("No se pudieron compilar las plantillas de reportes-jasper6", e);
        }
    }
}
