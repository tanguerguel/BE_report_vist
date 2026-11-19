package com.springboot.siremo.modules.monitoreo.services.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.springboot.siremo.modules.monitoreo.utils.HojaUtils;
import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/** Convierte una plantilla de hoja en HTML y el HTML en PDF A4. */
@Component
public class HojaRenderer {

    private final ITemplateEngine templateEngine;
    /** El encabezado institucional embebido, para que el HTML y el PDF no dependan de rutas. */
    private final String encabezado;

    public HojaRenderer(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.encabezado = HojaUtils.imagenComoDataUri(VistaPreviaFichaConstants.ENCABEZADO);
    }

    public String html(String plantilla, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        context.setVariable("encabezado", encabezado);
        return templateEngine.process(plantilla, context);
    }

    public byte[] pdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(VistaPreviaFichaConstants.ERROR_PDF, e);
        }
    }
}
