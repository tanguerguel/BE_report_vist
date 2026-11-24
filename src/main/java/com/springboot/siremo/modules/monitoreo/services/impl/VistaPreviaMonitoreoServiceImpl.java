package com.springboot.siremo.modules.monitoreo.services.impl;

import com.springboot.siremo.core.exceptions.ResourceNotFoundException;
import com.springboot.siremo.modules.monitoreo.dto.response.DatoHojaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.DimensionAplicadaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaMonitoreoResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.ItemAplicadoResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.MonitoreoResumenResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.SeccionHojaResponse;
import com.springboot.siremo.modules.monitoreo.repositories.VistaPreviaFichaRepository;
import com.springboot.siremo.modules.monitoreo.repositories.VistaPreviaMonitoreoRepository;
import com.springboot.siremo.modules.monitoreo.repositories.VistaPreviaMonitoreoRepository.RespuestaRow;
import com.springboot.siremo.modules.monitoreo.services.VistaPreviaMonitoreoService;
import com.springboot.siremo.modules.monitoreo.utils.HojaUtils;
import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class VistaPreviaMonitoreoServiceImpl implements VistaPreviaMonitoreoService {

    private static final String OBSERVADO = "OBSERVADO";
    private static final String NO_APLICA = "NO_APLICA";

    private final VistaPreviaMonitoreoRepository repository;
    private final VistaPreviaFichaRepository fichaRepository;
    private final HojaRenderer renderer;
    private final ItextRenderer itext;
    private final ItextOficialRenderer oficial;

    @Override
    public List<MonitoreoResumenResponse> listarRecientes(int limite) {
        return repository.findRecientes(Math.clamp(limite, 1, 100));
    }

    @Override
    public HojaMonitoreoResponse obtenerHoja(UUID monitoreoId) {
        return hoja(monitoreoId, monitoreo(monitoreoId));
    }

    private Map<String, Object> monitoreo(UUID monitoreoId) {
        return repository.findMonitoreo(monitoreoId)
                .orElseThrow(() -> new ResourceNotFoundException(VistaPreviaFichaConstants.MONITOREO_NO_ENCONTRADO));
    }

    private HojaMonitoreoResponse hoja(UUID monitoreoId, Map<String, Object> m) {
        UUID fichaVersionId = (UUID) m.get("ficha_version_id");
        String nivel = primero(texto(m, "aula_nivel"), texto(m, "institucion_nivel"));
        List<DimensionAplicadaResponse> dimensiones = dimensiones(monitoreoId, fichaVersionId, nivel);

        Map<UUID, List<RangoNivelResponse>> matrices = matrices(fichaVersionId);
        List<RangoNivelResponse> primeraMatriz = dimensiones.isEmpty()
                ? List.of() : matrices.getOrDefault(dimensiones.getFirst().id(), List.of());

        String distintivo = Boolean.TRUE.equals(m.get("permite_varias_publicadas"))
                ? HojaUtils.distintivo(textos(m.get("niveles")), texto(m, "denominacion")) : "";
        String titulo = distintivo.isEmpty() ? texto(m, "ficha") : texto(m, "ficha") + " · " + distintivo;

        return new HojaMonitoreoResponse(
                monitoreoId,
                titulo,
                texto(m, "estado_ficha"),
                secciones(m),
                HojaUtils.escala(primeraMatriz),
                dimensiones,
                texto(m, "observaciones"),
                texto(m, "compromiso_monitoreado"),
                texto(m, "compromiso_monitor"),
                retorno(fecha(m, "fecha_aplicacion"), fecha(m, "fecha_retorno")),
                nombreCompleto(texto(m, "observado_nombres"), texto(m, "observado_apellidos")),
                nombreCompleto(texto(m, "monitor_nombres"), texto(m, "monitor_apellidos")));
    }

    @Override
    public String generarHtml(UUID monitoreoId) {
        HojaMonitoreoResponse hoja = obtenerHoja(monitoreoId);
        return renderer.html(VistaPreviaFichaConstants.PLANTILLA_MONITOREO, Map.of(
                "hoja", hoja,
                "filasSecciones", hoja.secciones().stream()
                        .map(s -> Map.of("titulo", s.titulo(), "filas", HojaUtils.enPares(s.datos())))
                        .toList()));
    }

    @Override
    public byte[] generarPdf(UUID monitoreoId) {
        return renderer.pdf(generarHtml(monitoreoId));
    }

    @Override
    public byte[] generarPdfItext(UUID monitoreoId) {
        Map<String, Object> m = monitoreo(monitoreoId);
        HojaMonitoreoResponse hoja = hoja(monitoreoId, m);
        // Cada instrumento tiene su formato oficial; si el instrumento no tiene uno, la hoja genérica.
        return HojaOficial.Formato.deActor(texto(m, "actor_objetivo"))
                .map(formato -> oficial.pdf(hojaOficial(formato, hoja, m)))
                .orElseGet(() -> itext.pdf(hoja));
    }

    /** La hoja del monitoreo con los datos sueltos que piden los formatos oficiales. */
    private HojaOficial hojaOficial(HojaOficial.Formato formato, HojaMonitoreoResponse hoja, Map<String, Object> m) {
        Map<UUID, List<RangoNivelResponse>> matrices = matrices((UUID) m.get("ficha_version_id"));
        List<HojaOficial.Dimension> dimensiones = hoja.dimensiones().stream()
                .map(d -> {
                    BigDecimal suma = suma(d);
                    return new HojaOficial.Dimension(
                            d.nombre(),
                            d.puntajes().isEmpty() ? HojaOficial.NIVELES : d.puntajes(),
                            d.items().stream()
                                    .map(i -> new HojaOficial.Item(i.enunciado(), Objects.toString(i.puntaje(), ""), i.observacion()))
                                    .toList(),
                            suma == null ? d.parcial() : HojaUtils.numero(suma),
                            matrices.getOrDefault(d.id(), List.of()),
                            suma);
                })
                .toList();

        boolean especialista = formato == HojaOficial.Formato.ESPECIALISTA;
        String grado = texto(m, "grado");
        String seccion = texto(m, "seccion");
        LocalDate aplicacion = fecha(m, "fecha_aplicacion");
        HojaOficial.Datos datos = new HojaOficial.Datos(
                texto(m, "ugel"), "", texto(m, "institucion"), texto(m, "codigo_modular"), "", texto(m, "lugar"),
                Objects.toString(primero(texto(m, "aula_nivel"), texto(m, "institucion_nivel")), ""),
                texto(m, "modelo_servicio"), texto(m, "modalidad"), texto(m, "forma_eib"),
                texto(m, "director_nombre"), texto(m, "director_celular"),
                texto(m, "monitor_nombres"), texto(m, "monitor_apellidos"), texto(m, "monitor_documento"),
                Objects.toString(m.get("numero_visita"), ""),
                aplicacion == null ? "" : HojaUtils.fecha(aplicacion),
                instante(m, "inicio") == null ? "" : HojaUtils.hora(instante(m, "inicio")),
                instante(m, "fin") == null ? "" : HojaUtils.hora(instante(m, "fin")),
                nombreCompleto(texto(m, "observado_nombres"), texto(m, "observado_apellidos")),
                texto(m, "observado_documento"),
                texto(m, "condicion"),
                especialista ? texto(m, "especialista_celular") : texto(m, "celular"),
                grado.isBlank() && seccion.isBlank() ? "" : grado + " / " + seccion,
                cifra(m, "matriculados_varones"), cifra(m, "matriculados_mujeres"),
                cifra(m, "asistentes_varones"), cifra(m, "asistentes_mujeres"),
                cifra(m, "inclusivos_varones"), cifra(m, "inclusivos_mujeres"),
                texto(m, "especialista_nivel"), texto(m, "especialista_correo"), texto(m, "experiencia"));

        return new HojaOficial(formato, hoja.titulo(), datos, hoja.escala(), dimensiones, hoja.observaciones(),
                hoja.compromisoMonitoreado(), hoja.compromisoMonitor(), hoja.firmaMonitoreado(), hoja.firmaMonitor());
    }

    /**
     * Puntaje obtenido en la dimensión, solo si se puede ubicar en su matriz: todos los ítems valorados y
     * ninguno que no aplique (la misma regla que el puntaje parcial).
     */
    private static BigDecimal suma(DimensionAplicadaResponse dimension) {
        String parcial = Objects.toString(dimension.parcial(), "");
        if (parcial.startsWith("Pendiente") || parcial.startsWith("Sin resultado") || parcial.contains("no aplica")) {
            return null;
        }
        return dimension.items().stream()
                .map(ItemAplicadoResponse::puntaje)
                .filter(p -> p != null && !p.isBlank())
                .map(p -> new BigDecimal(p.replace(',', '.')))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String cifra(Map<String, Object> m, String columna) {
        return m.get(columna) instanceof Number n ? String.valueOf(n.intValue()) : "";
    }

    // Datos de identificación, con las mismas secciones que arma el front (hoja-aplicacion.ts).

    private List<SeccionHojaResponse> secciones(Map<String, Object> m) {
        String actor = texto(m, "actor_objetivo");
        SeccionHojaResponse delMonitor = new SeccionHojaResponse("Datos del monitor", List.of(
                dato("Nombres", texto(m, "monitor_nombres")),
                dato("Apellidos", texto(m, "monitor_apellidos")),
                dato("Documento de identidad", texto(m, "monitor_documento")),
                dato("Número de visita", Objects.toString(m.get("numero_visita"), "")),
                dato("Fecha de aplicación", HojaUtils.fecha(fecha(m, "fecha_aplicacion"))),
                dato("Hora de inicio", HojaUtils.hora(instante(m, "inicio"))),
                dato("Hora de fin", HojaUtils.hora(instante(m, "fin")))));
        String observado = nombreCompleto(texto(m, "observado_nombres"), texto(m, "observado_apellidos"));

        if ("ESPECIALISTA_UGEL".equals(actor)) {
            return List.of(
                    new SeccionHojaResponse("Datos del especialista monitoreado", List.of(
                            dato("Nombres y apellidos", observado),
                            dato("Documento de identidad", texto(m, "observado_documento")),
                            dato("UGEL", texto(m, "ugel")),
                            dato("Nivel", texto(m, "especialista_nivel")),
                            dato("N.º de celular", texto(m, "especialista_celular")),
                            dato("Correo electrónico", texto(m, "especialista_correo")),
                            dato("Tiempo de experiencia en el cargo", texto(m, "experiencia")))),
                    delMonitor);
        }

        boolean aDocente = "DOCENTE".equals(actor);
        List<DatoHojaResponse> institucion = new ArrayList<>(List.of(
                dato("GEREDU", "CUSCO"),
                dato("UGEL", texto(m, "ugel")),
                dato("Institución educativa", texto(m, "institucion")),
                dato("Código modular", texto(m, "codigo_modular")),
                dato("Nivel", HojaUtils.etiquetaNivelEducativo(texto(m, "institucion_nivel"))),
                dato("Lugar", texto(m, "lugar"))));
        if (aDocente) {
            institucion.add(dato("Director de la IE", texto(m, "director_nombre")));
            institucion.add(dato("Celular del director", texto(m, "director_celular")));
        }

        List<DatoHojaResponse> delObservado = new ArrayList<>(List.of(
                dato("Nombres y apellidos", observado),
                dato("Documento de identidad", texto(m, "observado_documento")),
                dato("Condición", texto(m, "condicion")),
                dato("N.º de celular", texto(m, "celular"))));
        if (aDocente) {
            delObservado.add(dato("Grado / Sección", texto(m, "grado") + " / " + texto(m, "seccion")));
            delObservado.add(dato("Área", texto(m, "area")));
            delObservado.add(dato("Sesión observada", texto(m, "sesion")));
            delObservado.add(dato("Estudiantes matriculados", porSexo(m, "matriculados")));
            delObservado.add(dato("Estudiantes asistentes", porSexo(m, "asistentes")));
            delObservado.add(dato("Estudiantes inclusivos asistentes", porSexo(m, "inclusivos")));
        }

        return List.of(
                new SeccionHojaResponse(aDocente ? "Datos de identificación" : "Datos de la institución educativa", institucion),
                delMonitor,
                new SeccionHojaResponse(aDocente ? "Datos del docente observado" : "Datos del directivo observado", delObservado));
    }

    // Valoraciones

    private List<DimensionAplicadaResponse> dimensiones(UUID monitoreoId, UUID fichaVersionId, String nivel) {
        Map<UUID, List<String>> puntajes = repository.findPuntajes(fichaVersionId).stream()
                .collect(Collectors.groupingBy(VistaPreviaMonitoreoRepository.PuntajeRow::dimensionId,
                        LinkedHashMap::new,
                        Collectors.mapping(p -> HojaUtils.numero(p.puntaje()), Collectors.toList())));
        Map<UUID, List<RangoNivelResponse>> matrices = matrices(fichaVersionId);

        Map<UUID, List<RespuestaRow>> porDimension = repository.findRespuestas(monitoreoId, fichaVersionId, nivel).stream()
                .collect(Collectors.groupingBy(RespuestaRow::dimensionId, LinkedHashMap::new, Collectors.toList()));

        return porDimension.entrySet().stream()
                .map(e -> {
                    List<RespuestaRow> filas = e.getValue();
                    List<ItemAplicadoResponse> items = filas.stream()
                            .map(r -> new ItemAplicadoResponse(
                                    r.itemId(),
                                    r.enunciado(),
                                    OBSERVADO.equals(r.estado()) ? HojaUtils.numero(r.puntaje()) : null,
                                    observacion(r)))
                            .toList();
                    return new DimensionAplicadaResponse(
                            e.getKey(),
                            filas.getFirst().dimension(),
                            puntajes.getOrDefault(e.getKey(), List.of()),
                            items,
                            parcial(filas, matrices.getOrDefault(e.getKey(), List.of())));
                })
                .toList();
    }

    /** Nota de campo y, si el ítem no aplica, su justificación. */
    private static String observacion(RespuestaRow r) {
        String noAplica = NO_APLICA.equals(r.estado()) ? "No aplica: " + Objects.toString(r.justificacion(), "").trim() : "";
        return Stream.of(Objects.toString(r.nota(), "").trim(), noAplica)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" · "));
    }

    /**
     * Puntaje que suman los ítems valorados de la dimensión, con su nivel de logro si se valoraron
     * todos; la misma regla del front (puntajeParcial).
     */
    private static String parcial(List<RespuestaRow> filas, List<RangoNivelResponse> matriz) {
        if (filas.stream().anyMatch(r -> !OBSERVADO.equals(r.estado()) && !NO_APLICA.equals(r.estado()))) {
            return "Pendiente";
        }
        long noAplican = filas.stream().filter(r -> NO_APLICA.equals(r.estado())).count();
        if (noAplican == filas.size()) {
            return "Sin resultado evaluable";
        }
        BigDecimal suma = filas.stream()
                .filter(r -> OBSERVADO.equals(r.estado()) && r.puntaje() != null)
                .map(RespuestaRow::puntaje)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String puntos = HojaUtils.numero(suma) + (suma.compareTo(BigDecimal.ONE) == 0 ? " punto" : " puntos");
        if (noAplican > 0) {
            return puntos + " · " + HojaUtils.cantidad(noAplican, "ítem no aplica", "ítems no aplican");
        }
        List<RangoNivelResponse> conRangos = matriz.stream()
                .filter(r -> r.minimo() != null && r.maximo() != null)
                .toList();
        if (conRangos.isEmpty()) {
            return puntos;
        }
        return puntos + " · " + conRangos.stream()
                .filter(r -> suma.compareTo(r.minimo()) >= 0 && suma.compareTo(r.maximo()) <= 0)
                .map(RangoNivelResponse::etiqueta)
                .findFirst()
                .orElse("Fuera de rango");
    }

    private Map<UUID, List<RangoNivelResponse>> matrices(UUID fichaVersionId) {
        return fichaRepository.findMatrices(fichaVersionId).stream()
                .collect(Collectors.groupingBy(VistaPreviaFichaRepository.RangoRow::dimensionId,
                        Collectors.mapping(r -> new RangoNivelResponse(
                                        r.nivel(), r.etiqueta(), r.rubrica(), r.minimo(), r.maximo()),
                                Collectors.toList())));
    }

    /** «15/12/2026 · en 30 días», si se acordó una visita de cumplimiento. */
    private static String retorno(LocalDate aplicacion, LocalDate retorno) {
        if (retorno == null) {
            return null;
        }
        String fecha = HojaUtils.fecha(retorno);
        if (aplicacion == null) {
            return fecha;
        }
        return fecha + " · en " + HojaUtils.cantidad(ChronoUnit.DAYS.between(aplicacion, retorno), "día", "días");
    }

    // Lectura de la fila

    private static DatoHojaResponse dato(String etiqueta, String valor) {
        return new DatoHojaResponse(etiqueta, valor == null ? "" : valor);
    }

    private static String porSexo(Map<String, Object> m, String grupo) {
        int varones = entero(m, grupo + "_varones");
        int mujeres = entero(m, grupo + "_mujeres");
        return (varones + mujeres) + " (" + varones + " varones, " + mujeres + " mujeres)";
    }

    private static String nombreCompleto(String nombres, String apellidos) {
        return (Objects.toString(nombres, "") + " " + Objects.toString(apellidos, "")).trim();
    }

    private static String primero(String a, String b) {
        return a != null && !a.isBlank() ? a : (b != null && !b.isBlank() ? b : null);
    }

    private static String texto(Map<String, Object> m, String columna) {
        Object valor = m.get(columna);
        return valor == null ? "" : valor.toString();
    }

    private static int entero(Map<String, Object> m, String columna) {
        return m.get(columna) instanceof Number n ? n.intValue() : 0;
    }

    private static LocalDate fecha(Map<String, Object> m, String columna) {
        return m.get(columna) instanceof java.sql.Date d ? d.toLocalDate() : null;
    }

    private static java.time.Instant instante(Map<String, Object> m, String columna) {
        return m.get(columna) instanceof Timestamp t ? t.toInstant() : null;
    }

    private static List<String> textos(Object array) {
        try {
            return array instanceof Array a ? List.of((String[]) a.getArray()) : List.of();
        } catch (SQLException e) {
            return List.of();
        }
    }
}
