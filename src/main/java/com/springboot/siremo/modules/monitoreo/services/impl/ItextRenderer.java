package com.springboot.siremo.modules.monitoreo.services.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.layout.renderer.IRenderer;
import com.springboot.siremo.modules.monitoreo.dto.response.DimensionHojaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.EscalaNivelResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.HojaFichaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.ItemHojaResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;
import com.springboot.siremo.modules.monitoreo.utils.HojaUtils;
import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

/**
 * Arma la hoja de la ficha en PDF A4 con iText 7 (API de Layout), sin HTML: cada tabla y celda se
 * construye en código. Las medidas reproducen la hoja HTML (fragmentos.html), pasadas de px a pt (1px = 0,75pt).
 */
@Component
public class ItextRenderer {

    private static final float MARGEN_VERTICAL = mm(12);
    private static final float MARGEN_HORIZONTAL = mm(11);

    private static final float TEXTO = 8.25f;
    private static final float TITULO = 13.5f;
    private static final float SECCION = 9.75f;
    private static final float DISTINTIVO = 9.75f;
    private static final float NORMA = 7.5f;
    private static final float INTERLINEADO = 1.35f;

    private static final float BORDE = 0.75f;
    private static final float CASILLA = 30f;
    private static final Color NEGRO = new DeviceRgb(0x11, 0x11, 0x11);
    private static final Color GRIS = new DeviceRgb(0xDD, 0xDD, 0xDD);

    private static final int NIVELES = 4;
    /** ÍTEMS 56 %, NIVEL 1–4 5 % cada uno, OBSERVACIONES 24 %. */
    private static final float[] ANCHOS_ITEMS = {56, 5, 5, 5, 5, 24};

    /** El encabezado institucional, leído una sola vez. */
    private final byte[] encabezado;

    public ItextRenderer() {
        try (InputStream in = new ClassPathResource(VistaPreviaFichaConstants.ENCABEZADO).getInputStream()) {
            this.encabezado = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró el encabezado de la ficha: " + VistaPreviaFichaConstants.ENCABEZADO, e);
        }
    }

    public byte[] pdf(HojaFichaResponse hoja) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document documento = new Document(pdf, PageSize.A4)) {
            pdf.getDocumentInfo().setTitle(hoja.nombre());
            Fuentes fuentes = new Fuentes();
            documento.setMargins(MARGEN_VERTICAL, MARGEN_HORIZONTAL, MARGEN_VERTICAL, MARGEN_HORIZONTAL);
            documento.setFont(fuentes.normal()).setFontSize(TEXTO).setFontColor(NEGRO);

            documento.add(new Image(ImageDataFactory.create(encabezado)).setAutoScale(true));
            float margenCabecera = cabecera(documento, hoja, fuentes);
            Table escala = escala(hoja.escala(), fuentes);
            seccion(documento, "Escala de valoración", Math.max(margenCabecera, 10.5f), escala, fuentes);
            documento.add(escala);
            for (DimensionHojaResponse dimension : hoja.dimensiones()) {
                seccion(documento, dimension.nombre(), 12,
                        items(dimension.items().subList(0, Math.min(1, dimension.items().size())), fuentes), fuentes);
                documento.add(items(dimension.items(), fuentes));
            }
            seccion(documento, "Matriz de puntajes por dimensión", 10.5f,
                    puntajes(hoja.dimensiones().subList(0, Math.min(2, hoja.dimensiones().size())), fuentes), fuentes);
            documento.add(puntajes(hoja.dimensiones(), fuentes));
        }
        return out.toByteArray();
    }

    /**
     * Título, distintivo y norma. iText suma los márgenes de dos párrafos seguidos (el HTML los superpone),
     * así que el último no lleva margen inferior: se devuelve para que lo aplique el título que sigue.
     */
    private float cabecera(Document documento, HojaFichaResponse hoja, Fuentes fuentes) {
        boolean conDistintivo = !vacio(hoja.distintivo());
        boolean conNorma = !vacio(hoja.normaCodigo());
        documento.add(parrafo(mayusculas(hoja.nombre()), fuentes.negrita(), TITULO)
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedLeading(TITULO * 1.25f)
                .setMarginTop(9).setMarginBottom(conDistintivo ? 3 : conNorma ? 10.5f : 0));
        float margen = 10.5f;
        if (conDistintivo) {
            documento.add(parrafo(hoja.distintivo(), fuentes.negrita(), DISTINTIVO)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(0).setMarginBottom(conNorma ? 10.5f : 0));
        }
        if (conNorma) {
            documento.add(new Paragraph()
                    .add(new Text("Norma de respaldo: ").setFont(fuentes.negrita()))
                    .add(new Text(hoja.normaCodigo()))
                    .setFontSize(NORMA)
                    .setMarginTop(0).setMarginBottom(0));
            margen = 13.5f;
        }
        return margen;
    }

    /**
     * Título de sección en mayúsculas. Si al pie de la página no caben el título y el comienzo de su tabla
     * (cabecera y primera fila), pasa a la página siguiente, como «page-break-after: avoid» en la hoja HTML.
     * No se usa keepWithNext porque iText lo aplica a la tabla entera y deja huecos al pie de las páginas.
     */
    private static void seccion(Document documento, String texto, float margenSuperior, Table comienzo, Fuentes fuentes) {
        Paragraph titulo = parrafo(mayusculas(texto), fuentes.negrita(), SECCION)
                .setMarginTop(margenSuperior).setMarginBottom(4.5f);
        float libre = documento.getRenderer().getCurrentArea().getBBox().getHeight();
        if (libre < alto(documento, titulo) + alto(documento, comienzo)) {
            documento.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        }
        documento.add(titulo);
    }

    /** Alto que ocupa un elemento en el ancho útil de la página, sin dibujarlo. */
    private static float alto(Document documento, IBlockElement elemento) {
        float ancho = documento.getPdfDocument().getDefaultPageSize().getWidth()
                - documento.getLeftMargin() - documento.getRightMargin();
        IRenderer renderer = elemento.createRendererSubTree().setParent(documento.getRenderer());
        LayoutResult resultado = renderer.layout(new LayoutContext(new LayoutArea(1, new Rectangle(ancho, 10_000))));
        return resultado.getOccupiedArea() == null ? 0 : resultado.getOccupiedArea().getBBox().getHeight();
    }

    private Table escala(List<EscalaNivelResponse> escala, Fuentes fuentes) {
        Table tabla = tablaCompleta(UnitValue.createPercentArray(Math.max(escala.size(), 1)));
        for (EscalaNivelResponse nivel : escala) {
            tabla.addHeaderCell(celda("Nivel " + nivel.nivel() + " (" + nivel.etiqueta() + ")", fuentes.cursivaNegrita())
                    .setTextAlignment(TextAlignment.CENTER));
        }
        for (EscalaNivelResponse nivel : escala) {
            tabla.addCell(celda(nivel.rubrica(), fuentes.normal()).setTextAlignment(TextAlignment.CENTER));
        }
        return tabla;
    }

    /** Tabla de ítems de una dimensión; la cabecera se repite si la tabla pasa a otra página. */
    private Table items(List<ItemHojaResponse> items, Fuentes fuentes) {
        Table tabla = tablaCompleta(UnitValue.createPercentArray(ANCHOS_ITEMS));
        tabla.addHeaderCell(cabeceraItems("ÍTEMS", 2, 1, fuentes));
        tabla.addHeaderCell(cabeceraItems("NIVEL", 1, NIVELES, fuentes));
        tabla.addHeaderCell(cabeceraItems("OBSERVACIONES", 2, 1, fuentes));
        for (int nivel = 1; nivel <= NIVELES; nivel++) {
            tabla.addHeaderCell(cabeceraItems(String.valueOf(nivel), 1, 1, fuentes));
        }
        int numero = 0;
        for (ItemHojaResponse item : items) {
            numero++;
            Paragraph enunciado = new Paragraph()
                    .add(new Text(numero + ".").setFont(fuentes.negrita()))
                    .add(new Text(" " + (item.enunciado() == null ? "" : item.enunciado())))
                    .setFixedLeading(TEXTO * INTERLINEADO).setMargin(0);
            // Una fila no se parte entre dos páginas.
            tabla.addCell(celda(enunciado).setKeepTogether(true));
            for (int casilla = 0; casilla <= NIVELES; casilla++) {
                tabla.addCell(celda(new Paragraph()).setMinHeight(CASILLA).setKeepTogether(true));
            }
        }
        return tabla;
    }

    private static Cell cabeceraItems(String texto, int filas, int columnas, Fuentes fuentes) {
        return new Cell(filas, columnas)
                .add(parrafo(texto, fuentes.negrita(), TEXTO))
                .setBackgroundColor(GRIS)
                .setBorder(new SolidBorder(NEGRO, BORDE))
                .setPadding(3)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
    }

    /** Matriz de puntajes: dos dimensiones por fila; la de la derecha queda vacía si son impares. */
    private Table puntajes(List<DimensionHojaResponse> dimensiones, Fuentes fuentes) {
        Table tabla = tablaCompleta(UnitValue.createPercentArray(new float[]{50, 50}));
        for (List<DimensionHojaResponse> fila : HojaUtils.enPares(dimensiones)) {
            for (int posicion = 0; posicion < 2; posicion++) {
                Cell celda = new Cell().setBorder(Border.NO_BORDER)
                        .setPadding(0).setPaddingTop(4.5f).setPaddingBottom(4.5f)
                        .setKeepTogether(true);
                if (posicion == 0) {
                    celda.setPaddingRight(4.5f);
                } else {
                    celda.setPaddingLeft(4.5f);
                }
                if (posicion < fila.size()) {
                    celda.add(matriz(fila.get(posicion), fuentes));
                }
                tabla.addCell(celda);
            }
        }
        return tabla;
    }

    private Table matriz(DimensionHojaResponse dimension, Fuentes fuentes) {
        Table tabla = tablaCompleta(UnitValue.createPercentArray(3));
        tabla.addCell(celda(parrafo(mayusculas(dimension.nombre()), fuentes.negrita(), TEXTO), 3)
                .setBackgroundColor(GRIS));
        for (String titulo : List.of("NIVEL", "RANGO", "PUNTAJE")) {
            tabla.addCell(celda(titulo, fuentes.negrita()).setBackgroundColor(GRIS));
        }
        for (RangoNivelResponse rango : dimension.matriz()) {
            tabla.addCell(celda(mayusculas(rango.etiqueta()), fuentes.negrita()).setBackgroundColor(GRIS));
            tabla.addCell(celda(rango.rangoTexto(), fuentes.normal()));
            tabla.addCell(celda("", fuentes.normal()));
        }
        return tabla;
    }

    private static Table tablaCompleta(UnitValue[] anchos) {
        return new Table(anchos).useAllAvailableWidth().setFixedLayout();
    }

    private static Cell celda(String texto, PdfFont fuente) {
        return celda(parrafo(texto == null ? "" : texto, fuente, TEXTO));
    }

    /** Celda con borde de 1px y relleno de 5px 6px, como .tabla td de la hoja HTML. */
    private static Cell celda(Paragraph contenido) {
        return celda(contenido, 1);
    }

    private static Cell celda(Paragraph contenido, int columnas) {
        return new Cell(1, columnas).add(contenido)
                .setBorder(new SolidBorder(NEGRO, BORDE))
                .setPaddingTop(3.75f).setPaddingBottom(3.75f)
                .setPaddingLeft(4.5f).setPaddingRight(4.5f);
    }

    private static Paragraph parrafo(String texto, PdfFont fuente, float tamano) {
        return new Paragraph(texto == null ? "" : texto)
                .setFont(fuente).setFontSize(tamano)
                .setFixedLeading(tamano * INTERLINEADO)
                .setMargin(0);
    }

    private static String mayusculas(String texto) {
        return texto == null ? "" : texto.toUpperCase(Locale.ROOT);
    }

    private static boolean vacio(String texto) {
        return texto == null || texto.isBlank();
    }

    private static float mm(float milimetros) {
        return milimetros * 72f / 25.4f;
    }

    /** Las fuentes de un documento: en iText una PdfFont no se puede compartir entre documentos. */
    private static final class Fuentes {
        private final PdfFont normal = crear(StandardFonts.HELVETICA);
        private final PdfFont negrita = crear(StandardFonts.HELVETICA_BOLD);
        private final PdfFont cursivaNegrita = crear(StandardFonts.HELVETICA_BOLDOBLIQUE);

        PdfFont normal() {
            return normal;
        }

        PdfFont negrita() {
            return negrita;
        }

        PdfFont cursivaNegrita() {
            return cursivaNegrita;
        }

        private static PdfFont crear(String nombre) {
            try {
                // WinAnsi cubre tildes, ñ, «·» y «–».
                return PdfFontFactory.createFont(nombre, PdfEncodings.WINANSI);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
