package com.springboot.siremo.modules.monitoreo.controllers;

import com.springboot.siremo.core.responses.ApiResponse;
import com.springboot.siremo.core.shared.ParentController;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaFichaResponse;
import com.springboot.siremo.modules.monitoreo.services.VistaPreviaFichaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1.0/monitoreo/vista-previa/ficha")
@Tag(name = "Monitoreo/Vista previa", description = "Hoja de una ficha de monitoreo en JSON, HTML y PDF")
public class VistaPreviaFichaController extends ParentController {

    private final VistaPreviaFichaService service;

    @GetMapping("/{id}")
    @Operation(summary = "Datos de la hoja de una ficha")
    public ApiResponse<HojaFichaResponse> obtenerHoja(@PathVariable UUID id) {
        return new ApiResponse<>(service.obtenerHoja(id));
    }

    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Hoja de una ficha en HTML")
    public String generarHtml(@PathVariable UUID id) {
        return service.generarHtml(id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Hoja de una ficha en PDF A4", description = "Con descargar=true se descarga en lugar de abrirse")
    public ResponseEntity<byte[]> generarPdf(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean descargar
    ) {
        String nombre = "ficha-" + id + ".pdf";
        ContentDisposition disposition = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(nombre)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.generarPdf(id));
    }

    @GetMapping(value = "/{id}/jasper6", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Hoja de una ficha en PDF A4 con JasperReports 6",
            description = "Misma hoja, generada desde reportes-jasper6/fichas/ficha.jrxml (Jaspersoft Studio 6.21.5). Con descargar=true se descarga")
    public ResponseEntity<byte[]> generarPdfJasper6(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean descargar
    ) {
        ContentDisposition disposition = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("ficha-" + id + "-jasper6.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.generarPdfJasper6(id));
    }
}
