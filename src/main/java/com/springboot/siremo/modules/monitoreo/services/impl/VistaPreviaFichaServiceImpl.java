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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class VistaPreviaFichaServiceImpl implements VistaPreviaFichaService {

    private final VistaPreviaFichaRepository repository;
    private final HojaRenderer renderer;
    private final Jasper6Renderer jasper;
    private final ItextRenderer itext;

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
    public byte[] generarPdfJasper6(UUID fichaVersionId) {
        repository.findFicha(fichaVersionId)
                .orElseThrow(() -> new ResourceNotFoundException(VistaPreviaFichaConstants.NO_ENCONTRADA));
        // La plantilla consulta la base por su cuenta; solo necesita el id.
        return jasper.pdf(VistaPreviaFichaConstants.JASPER6_FICHA, Map.of("FICHA_VERSION_ID", fichaVersionId.toString()));
    }

    @Override
    public byte[] generarPdfItext(UUID fichaVersionId) {
        return itext.pdf(obtenerHoja(fichaVersionId));
    }

    private Map<UUID, List<RangoNivelResponse>> matricesPorDimension(UUID fichaVersionId) {
        return repository.findMatrices(fichaVersionId).stream()
                .collect(Collectors.groupingBy(VistaPreviaFichaRepository.RangoRow::dimensionId,
                        Collectors.mapping(r -> new RangoNivelResponse(
                                        r.nivel(), r.etiqueta(), r.rubrica(), r.minimo(), r.maximo()),
                                Collectors.toList())));
    }
}
