package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.UUID;

public record ItemHojaResponse(UUID id, Integer orden, String enunciado) {
}
