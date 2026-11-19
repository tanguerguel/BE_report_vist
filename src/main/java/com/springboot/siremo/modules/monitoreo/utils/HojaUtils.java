package com.springboot.siremo.modules.monitoreo.utils;

import com.springboot.siremo.modules.monitoreo.dto.response.EscalaNivelResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Reglas de presentación comunes a la hoja de la ficha en blanco y a la de la ficha aplicada; son
 * las mismas que usa el front para dibujarlas.
 */
public final class HojaUtils {

    private HojaUtils() {
    }

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * La escala de valoración: los cuatro niveles con la rúbrica de la primera dimensión, porque las
     * rúbricas son comunes a todas.
     */
    public static List<EscalaNivelResponse> escala(List<RangoNivelResponse> matrizPrimeraDimension) {
        return VistaPreviaFichaConstants.NIVELES.keySet().stream().sorted()
                .map(nivel -> new EscalaNivelResponse(
                        nivel,
                        VistaPreviaFichaConstants.NIVELES.get(nivel),
                        matrizPrimeraDimension.stream().filter(r -> nivel.equals(r.nivel()))
                                .map(RangoNivelResponse::rubrica).findFirst().orElse("")))
                .toList();
    }

    /** «Inicial, Primaria y Secundaria · Aulas multigrado»; vacío si no tiene niveles ni denominación. */
    public static String distintivo(List<String> niveles, String denominacion) {
        List<String> etiquetas = VistaPreviaFichaConstants.NIVELES_EDUCATIVOS.stream()
                .filter(n -> niveles.contains(n.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        List<String> partes = new ArrayList<>();
        if (!etiquetas.isEmpty()) {
            partes.add(enumerar(etiquetas));
        }
        if (denominacion != null && !denominacion.isBlank()) {
            partes.add(denominacion.trim());
        }
        return String.join(" · ", partes);
    }

    /** Etiqueta de un nivel educativo («PRIMARIA» → «Primaria»); el código si no es uno conocido. */
    public static String etiquetaNivelEducativo(String codigo) {
        return VistaPreviaFichaConstants.NIVELES_EDUCATIVOS.stream()
                .filter(n -> n.getKey().equals(codigo))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(codigo == null ? "" : codigo);
    }

    /** Agrupa de dos en dos, para imprimir en dos columnas. */
    public static <T> List<List<T>> enPares(List<T> valores) {
        List<List<T>> filas = new ArrayList<>();
        for (int i = 0; i < valores.size(); i += 2) {
            filas.add(valores.subList(i, Math.min(i + 2, valores.size())));
        }
        return filas;
    }

    /** «12 puntos», «1 punto». */
    public static String cantidad(long valor, String singular, String plural) {
        return valor + " " + (valor == 1 ? singular : plural);
    }

    public static String numero(BigDecimal valor) {
        return valor == null ? null : valor.stripTrailingZeros().toPlainString();
    }

    public static String fecha(LocalDate fecha) {
        return fecha == null ? "—" : FECHA.format(fecha);
    }

    public static String hora(java.time.Instant instante) {
        return instante == null ? "—" : HORA.format(instante.atZone(LIMA));
    }

    public static String imagenComoDataUri(String ruta) {
        try (InputStream in = new ClassPathResource(ruta).getInputStream()) {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró el encabezado de la ficha: " + ruta, e);
        }
    }

    private static String enumerar(List<String> valores) {
        if (valores.size() == 1) {
            return valores.getFirst();
        }
        return String.join(", ", valores.subList(0, valores.size() - 1)) + " y " + valores.getLast();
    }
}
