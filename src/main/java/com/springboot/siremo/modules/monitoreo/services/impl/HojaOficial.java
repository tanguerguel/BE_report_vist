package com.springboot.siremo.modules.monitoreo.services.impl;

import com.springboot.siremo.modules.monitoreo.dto.response.EscalaNivelResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaFichaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Lo que necesita el formato oficial de cada instrumento (ItextOficialRenderer), sea la ficha en blanco
 * o un monitoreo aplicado. En la ficha en blanco los datos, marcas y totales van vacíos.
 */
record HojaOficial(
        Formato formato,
        String titulo,
        Datos datos,
        List<EscalaNivelResponse> escala,
        List<Dimension> dimensiones,
        String observaciones,
        String compromisoMonitoreado,
        String compromisoMonitor,
        String firmaMonitoreado,
        String firmaMonitor
) {

    /** Las columnas de valoración de la ficha en blanco. */
    static final List<String> NIVELES = List.of("1", "2", "3", "4");

    /** Un formato por instrumento, según a quién se monitorea (instrumento.actor_objetivo). */
    enum Formato {
        ESPECIALISTA, DIRECTIVO, DOCENTE;

        static Optional<Formato> deActor(String actorObjetivo) {
            return Optional.ofNullable(switch (actorObjetivo == null ? "" : actorObjetivo) {
                case "ESPECIALISTA_UGEL" -> ESPECIALISTA;
                case "DIRECTIVO" -> DIRECTIVO;
                case "DOCENTE" -> DOCENTE;
                default -> null;
            });
        }
    }

    /**
     * Dimensión con sus columnas de valoración, sus ítems, el total que se imprime al pie y su matriz.
     * {@code suma} es el puntaje obtenido, si se puede ubicar en la matriz; si no, null.
     */
    record Dimension(String nombre, List<String> columnas, List<Item> items, String total,
                     List<RangoNivelResponse> matriz, BigDecimal suma) {
    }

    /** Ítem con la columna marcada (vacía si no hay) y su observación. */
    record Item(String enunciado, String marcado, String observacion) {
    }

    /** Datos de identificación que piden los formatos oficiales; los que no se registran quedan vacíos. */
    record Datos(
            String ugel, String distrito, String institucion, String codigoModular, String direccion, String lugar,
            String nivel, String modeloServicio, String modalidad, String formaEib,
            String directorNombre, String directorCelular,
            String monitorNombres, String monitorApellidos, String monitorDocumento,
            String numeroVisita, String fechaAplicacion, String horaInicio, String horaFin,
            String observadoNombre, String observadoDocumento, String condicion, String celular, String gradoSeccion,
            String matriculadosVarones, String matriculadosMujeres,
            String asistentesVarones, String asistentesMujeres,
            String inclusivosVarones, String inclusivosMujeres,
            String especialistaNivel, String especialistaCorreo, String experiencia
    ) {
        static Datos vacios() {
            return new Datos("", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
                    "", "", "", "", "", "", "", "", "", "", "", "", "", "");
        }
    }

    /** La ficha en blanco en el formato de su instrumento. */
    static HojaOficial deFicha(HojaFichaResponse hoja, Formato formato) {
        List<Dimension> dimensiones = hoja.dimensiones().stream()
                .map(d -> new Dimension(d.nombre(), NIVELES,
                        d.items().stream().map(i -> new Item(i.enunciado(), "", "")).toList(),
                        "", d.matriz(), null))
                .toList();
        String titulo = hoja.distintivo() == null || hoja.distintivo().isBlank()
                ? hoja.nombre() : hoja.nombre() + " · " + hoja.distintivo();
        return new HojaOficial(formato, titulo, Datos.vacios(), hoja.escala(), dimensiones,
                "", "", "", "", "");
    }
}
