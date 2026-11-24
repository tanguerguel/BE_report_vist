package com.springboot.siremo.modules.monitoreo.controllers;

import com.springboot.siremo.core.responses.ApiResponse;
import com.springboot.siremo.core.shared.ParentController;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaMonitoreoResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.MonitoreoResumenResponse;
import com.springboot.siremo.modules.monitoreo.services.VistaPreviaMonitoreoService;
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

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/v1.0/monitoreo/vista-previa/monitoreo")
@Tag(name = "Monitoreo/Vista previa aplicada", description = "Hoja de un monitoreo ya aplicado (ficha llenada) en JSON, HTML y PDF")
public class VistaPreviaMonitoreoController extends ParentController {

    private final VistaPreviaMonitoreoService service;

    @GetMapping
    @Operation(summary = "Monitoreos aplicados más recientes", description = "Para obtener el id de un monitoreo y ver su hoja")
    public ApiResponse<List<MonitoreoResumenResponse>> listarRecientes(@RequestParam(defaultValue = "20") int limite) {
        return new ApiResponse<>(service.listarRecientes(limite));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Datos de la hoja de un monitoreo aplicado")
    public ApiResponse<HojaMonitoreoResponse> obtenerHoja(@PathVariable UUID id) {
        return new ApiResponse<>(service.obtenerHoja(id));
    }

    @GetMapping(value = "/{id}/html", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Hoja de un monitoreo aplicado en HTML")
    public String generarHtml(@PathVariable UUID id) {
        return service.generarHtml(id);
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Hoja de un monitoreo aplicado en PDF A4", description = "Con descargar=true se descarga en lugar de abrirse")
    public ResponseEntity<byte[]> generarPdf(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean descargar
    ) {
        ContentDisposition disposition = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("monitoreo-" + id + ".pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.generarPdf(id));
    }

    @GetMapping(value = "/{id}/itext", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Hoja de un monitoreo aplicado en PDF A4 con iText 7",
            description = "Misma hoja, armada por código con la API de Layout de iText 7. Con descargar=true se descarga")
    public ResponseEntity<byte[]> generarPdfItext(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean descargar
    ) {
        ContentDisposition disposition = (descargar ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename("monitoreo-" + id + "-itext.pdf")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(service.generarPdfItext(id));
    }
}
