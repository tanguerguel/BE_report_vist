package com.springboot.siremo.modules.monitoreo.services.impl;

import com.springboot.siremo.core.exceptions.ResourceNotFoundException;
import com.springboot.siremo.modules.monitoreo.dto.response.DimensionHojaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaFichaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.ItemHojaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;
import com.springboot.siremo.modules.monitoreo.repositories.VistaPreviaFichaRepository;
import com.springboot.siremo.modules.monitoreo.repositories.VistaPreviaFichaRepository.FichaRow;
import com.springboot.siremo.modules.monitoreo.services.VistaPreviaFichaService;
import com.springboot.siremo.modules.monitoreo.utils.HojaUtils;
import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class VistaPreviaFichaServiceImpl implements VistaPreviaFichaService {

    private final VistaPreviaFichaRepository repository;
    private final HojaRenderer renderer;
    private final JasperRenderer jasper;

    @Override
    public HojaFichaResponse obtenerHoja(UUID fichaVersionId) {
        FichaRow ficha = repository.findFicha(fichaVersionId)
                .orElseThrow(() -> new ResourceNotFoundException(VistaPreviaFichaConstants.NO_ENCONTRADA));

        Map<UUID, List<ItemHojaResponse>> items = repository.findItems(fichaVersionId).stream()
                .collect(Collectors.groupingBy(VistaPreviaFichaRepository.ItemRow::dimensionId,
                        Collectors.mapping(i -> new ItemHojaResponse(i.id(), i.orden(), i.enunciado()),
                                Collectors.toList())));

        Map<UUID, List<RangoNivelResponse>> matrices = matricesPorDimension(fichaVersionId);

        List<DimensionHojaResponse> dimensiones = repository.findDimensiones(fichaVersionId).stream()
                .map(d -> new DimensionHojaResponse(d.id(), d.nombre(), d.orden(),
                        items.getOrDefault(d.id(), List.of()),
                        matrices.getOrDefault(d.id(), List.of())))
                .toList();

        return new HojaFichaResponse(
                ficha.id(),
                ficha.nombre(),
                ficha.variasPublicadas() ? HojaUtils.distintivo(ficha.niveles(), ficha.denominacion()) : "",
                ficha.normaCodigo(),
                ficha.codigoVersion(),
                ficha.estado(),
                HojaUtils.escala(dimensiones.isEmpty() ? List.of() : dimensiones.getFirst().matriz()),
                dimensiones);
    }

    @Override
    public String generarHtml(UUID fichaVersionId) {
        HojaFichaResponse hoja = obtenerHoja(fichaVersionId);
        return renderer.html(VistaPreviaFichaConstants.PLANTILLA, Map.of(
                "hoja", hoja,
                "niveles", List.of(1, 2, 3, 4),
                "filasPuntajes", HojaUtils.enPares(hoja.dimensiones())));
    }

    @Override
    public byte[] generarPdf(UUID fichaVersionId) {
        return renderer.pdf(generarHtml(fichaVersionId));
    }

    @Override
    public byte[] generarPdfJasper(UUID fichaVersionId) {
        HojaFichaResponse hoja = obtenerHoja(fichaVersionId);

        List<Map<String, ?>> dimensiones = hoja.dimensiones().stream()
                .<Map<String, ?>>map(d -> Map.of(
                        "nombre", d.nombre(),
                        "items", d.items().stream()
                                .<Map<String, ?>>map(i -> Map.of("orden", i.orden(), "enunciado", i.enunciado()))
                                .toList(),
                        "matriz", d.matriz().stream()
                                .<Map<String, ?>>map(r -> Map.of("etiqueta", r.etiqueta(), "rango", r.rangoTexto()))
                                .toList()))
                .toList();

        Map<String, Object> parametros = new HashMap<>();
        parametros.put("NOMBRE", hoja.nombre());
        parametros.put("DISTINTIVO", hoja.distintivo());
        parametros.put("NORMA", hoja.normaCodigo());
        parametros.put("ESCALA_TITULOS", hoja.escala().stream()
                .map(n -> "Nivel " + n.nivel() + " (" + n.etiqueta() + ")").toList());
        parametros.put("ESCALA_RUBRICAS", hoja.escala().stream()
                .map(n -> n.rubrica() == null ? "" : n.rubrica()).toList());
        parametros.put("MATRIZ_PARES", HojaUtils.enPares(dimensiones).stream()
                .<Map<String, ?>>map(par -> {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("izquierdaNombre", par.getFirst().get("nombre"));
                    fila.put("izquierdaMatriz", par.getFirst().get("matriz"));
                    if (par.size() > 1) {
                        fila.put("derechaNombre", par.get(1).get("nombre"));
                        fila.put("derechaMatriz", par.get(1).get("matriz"));
                    }
                    return fila;
                })
                .toList());
        parametros.put("SUB_ITEMS", jasper.reporte(VistaPreviaFichaConstants.JASPER_ITEMS));
        parametros.put("SUB_MATRIZ", jasper.reporte(VistaPreviaFichaConstants.JASPER_MATRIZ));
        parametros.put("SUB_RANGOS", jasper.reporte(VistaPreviaFichaConstants.JASPER_RANGOS));

        return jasper.pdf(VistaPreviaFichaConstants.JASPER_FICHA, parametros, dimensiones);
    }

    private Map<UUID, List<RangoNivelResponse>> matricesPorDimension(UUID fichaVersionId) {
        return repository.findMatrices(fichaVersionId).stream()
                .collect(Collectors.groupingBy(VistaPreviaFichaRepository.RangoRow::dimensionId,
                        Collectors.mapping(r -> new RangoNivelResponse(
                                        r.nivel(), r.etiqueta(), r.rubrica(), r.minimo(), r.maximo()),
                                Collectors.toList())));
    }
}
