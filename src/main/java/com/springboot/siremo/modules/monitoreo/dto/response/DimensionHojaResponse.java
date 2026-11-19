package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.List;
import java.util.UUID;

public record DimensionHojaResponse(
        UUID id,
        String nombre,
        Integer orden,
        List<ItemHojaResponse> items,
        List<RangoNivelResponse> matriz
) {
}
