package com.springboot.siremo.modules.monitoreo.dto.response;

import java.util.UUID;

/** Ítem valorado: el puntaje marcado (vacío si no se observó o no aplica) y su observación. */
public record ItemAplicadoResponse(UUID id, String enunciado, String puntaje, String observacion) {
}
