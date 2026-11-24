package com.springboot.siremo.modules.monitoreo.services.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.IBlockElement;
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
import com.springboot.siremo.modules.monitoreo.dto.response.EscalaNivelResponse;
import com.springboot.siremo.modules.monitoreo.dto.response.RangoNivelResponse;
import com.springboot.siremo.modules.monitoreo.services.impl.HojaOficial.Datos;
import com.springboot.siremo.modules.monitoreo.services.impl.HojaOficial.Dimension;
import com.springboot.siremo.modules.monitoreo.services.impl.HojaOficial.Formato;
import com.springboot.siremo.modules.monitoreo.services.impl.HojaOficial.Item;
import com.springboot.siremo.modules.monitoreo.utils.VistaPreviaFichaConstants;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Arma con iText 7 (API de Layout) la ficha en el formato oficial de su instrumento: especialistas UGEL,
 * directivos (gestión escolar) o docentes (práctica pedagógica). Sirve para la ficha en blanco y para el
 * monitoreo aplicado; todas terminan con la matriz de puntajes por dimensión.
 */
@Component
public class ItextOficialRenderer {

    private static final float MARGEN_VERTICAL = mm(12);
    private static final float MARGEN_HORIZONTAL = mm(14);

    private static final float TEXTO = 8;
    private static final float TITULO = 11;
    private static final float SECCION = 8.5f;
    private static final float INTERLINEADO = 1.25f;
    private static final float BORDE = 0.6f;
    private static final float ALTO_FILA = 15;
    private static final float ALTO_RENGLON = 15;
    private static final float ALTO_FIRMA = 70;

    private static final Color NEGRO = new DeviceRgb(0x11, 0x11, 0x11);
    private static final Color GRIS = new DeviceRgb(0xD9, 0xD9, 0xD9);
    /** Colores de la matriz por nivel: inicio, en proceso, satisfactorio, destacado. */
    private static final Color[] COLORES_NIVEL = {
            new DeviceRgb(0xE5, 0x39, 0x35), new DeviceRgb(0xF9, 0xA8, 0x25),
            new DeviceRgb(0x43, 0xA0, 0x47), new DeviceRgb(0x1E, 0x88, 0xE5)};
    private static final String[] ROMANOS = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};

    private static final int CASILLAS_DOCUMENTO = 8;
    private static final int CASILLAS_CODIGO_MODULAR = 7;
    private static final int MATRICES_POR_FILA = 3;

    private final byte[] encabezado;

    public ItextOficialRenderer() {
        try (InputStream in = new ClassPathResource(VistaPreviaFichaConstants.ENCABEZADO).getInputStream()) {
            this.encabezado = in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se encontró el encabezado de la ficha: " + VistaPreviaFichaConstants.ENCABEZADO, e);
        }
    }

    public byte[] pdf(HojaOficial hoja) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(out));
             Document documento = new Document(pdf, PageSize.A4)) {
            pdf.getDocumentInfo().setTitle(hoja.titulo());
            Hoja h = new Hoja(documento, new Fuentes(), hoja);
            float altoEncabezado = encabezadoEnCadaPagina(pdf, h.f.normal);
            documento.setMargins(MARGEN_VERTICAL + altoEncabezado + 4, MARGEN_HORIZONTAL, MARGEN_VERTICAL, MARGEN_HORIZONTAL);
            documento.setFont(h.f.normal).setFontSize(TEXTO).setFontColor(NEGRO);
            documento.add(parrafo(mayusculas(hoja.titulo()), h.f.negrita, TITULO)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(8).setMarginBottom(4));
            switch (hoja.formato()) {
                case ESPECIALISTA -> h.especialista();
                case DIRECTIVO -> h.directivo();
                case DOCENTE -> h.docente();
            }
        }
        return out.toByteArray();
    }

    /**
     * Como en los formatos impresos, cada página lleva el encabezado institucional arriba y su número
     * («-1-») abajo a la derecha. Devuelve el alto del encabezado, para dejarle lugar en el margen superior.
     */
    private float encabezadoEnCadaPagina(PdfDocument pdf, PdfFont fuente) {
        ImageData imagen = ImageDataFactory.create(encabezado);
        float ancho = PageSize.A4.getWidth() - 2 * MARGEN_HORIZONTAL;
        float alto = ancho * imagen.getHeight() / imagen.getWidth();
        pdf.addEventHandler(PdfDocumentEvent.END_PAGE, evento -> {
            PdfPage pagina = ((PdfDocumentEvent) evento).getPage();
            Rectangle tamano = pagina.getPageSize();
            PdfCanvas lienzo = new PdfCanvas(pagina.newContentStreamBefore(), pagina.getResources(), pdf);
            lienzo.addImageFittedIntoRectangle(imagen,
                    new Rectangle(MARGEN_HORIZONTAL, tamano.getTop() - MARGEN_VERTICAL - alto, ancho, alto), false);
            try (Canvas canvas = new Canvas(lienzo, tamano)) {
                canvas.setFont(fuente).setFontSize(TEXTO).setFontColor(NEGRO)
                        .showTextAligned("-" + pdf.getPageNumber(pagina) + "-",
                                tamano.getRight() - MARGEN_HORIZONTAL, MARGEN_VERTICAL / 2, TextAlignment.RIGHT);
            }
            lienzo.release();
        });
        return alto;
    }

    /** Una hoja en construcción: el documento, sus fuentes, los datos y el contador de secciones e ítems. */
    private static final class Hoja {
        private final Document doc;
        private final Fuentes f;
        private final HojaOficial hoja;
        private final Datos d;
        private int seccion;
        private int item;

        Hoja(Document doc, Fuentes f, HojaOficial hoja) {
            this.doc = doc;
            this.f = f;
            this.hoja = hoja;
            this.d = hoja.datos();
        }

        // Formatos

        void especialista() {
            titulo("DATOS DEL ESPECIALISTA MONITOREADO", filas(
                    fila(new float[]{75, 25}, cabecera("NOMBRES Y APELLIDOS"), cabecera("DNI")),
                    fila(new float[]{75, 25}, valor(d.observadoNombre()), valor(d.observadoDocumento()).setTextAlignment(TextAlignment.CENTER)),
                    fila(new float[]{15, 15, 19, 51}, cabecera("UGEL"), cabecera("NIVEL"), cabecera("N° DE CELULAR"), cabecera("CORREO ELECTRÓNICO")),
                    fila(new float[]{15, 15, 19, 51}, valor(d.ugel()), valor(d.especialistaNivel()), valor(d.celular()), valor(d.especialistaCorreo())),
                    fila(new float[]{31, 18, 26, 25}, cabecera("TIEMPO DE EXPERIENCIA EN EL CARGO"), valor(d.experiencia()),
                            cabecera("NÚMERO DE VISITA"), valor(d.numeroVisita()))));
            titulo("DATOS DEL MONITOR", filas(
                    fila(new float[]{75, 25}, cabecera("NOMBRES Y APELLIDOS"), cabecera("DNI")),
                    fila(new float[]{75, 25}, valor(unir(d.monitorNombres(), d.monitorApellidos())),
                            valor(d.monitorDocumento()).setTextAlignment(TextAlignment.CENTER)),
                    fila(new float[]{31, 18, 26, 25}, cabecera("FECHA Y DURACIÓN DE LA APLICACIÓN"),
                            valor(vacio(d.fechaAplicacion()) ? "      /      /" : d.fechaAplicacion()).setTextAlignment(TextAlignment.CENTER),
                            etiquetaValor("Hora Inicio: ", d.horaInicio()), etiquetaValor("Hora Fin: ", d.horaFin()))));

            Table escala = tabla(UnitValue.createPercentArray(Math.max(hoja.escala().size(), 1))).setMarginTop(10);
            escala.addCell(cabecera("NIVELES DE LOGRO", 1, Math.max(hoja.escala().size(), 1))
                    .setBackgroundColor(ColorConstants.WHITE));
            for (EscalaNivelResponse nivel : hoja.escala()) {
                escala.addCell(cabecera(nivel.nivel() + " " + mayusculas(nivel.etiqueta())));
            }
            doc.add(escala);

            for (Dimension dimension : hoja.dimensiones()) {
                List<Table> partes = itemsEspecialista(dimension);
                titulo(dimension.nombre(), partes.get(0), partes.get(1));
            }
            renglones("OBSERVACIONES", hoja.observaciones(), 7);
            renglones("COMPROMISOS", compromisos(), 7);
            firmasSinRecuadro();
            matrices();
        }

        void directivo() {
            titulo("DATOS DE IE", filas(institucion(false)));
            titulo("DATOS DEL MONITOR", filas(monitor()));
            List<Table> observado = new ArrayList<>(List.of(
                    fila(new float[]{60, 14, 26}, cabecera("Nombres Apellidos").setTextAlignment(TextAlignment.LEFT),
                            cabecera("Condición"), cabecera("Documento de Identidad")),
                    fila(anchosConCasillas(new float[]{60, 14}, 26, CASILLAS_DOCUMENTO),
                            concat(List.of(valor(d.observadoNombre()), valor(d.condicion())), casillas(d.observadoDocumento(), CASILLAS_DOCUMENTO))),
                    fila(new float[]{20, 24, 18, 18, 20}, etiqueta("N° de celular"), valor(d.celular()),
                            cabecera("Varones"), cabecera("Mujeres"), cabecera("Total"))));
            observado.addAll(estudiantes());
            titulo("DATOS DEL DIRECTIVO OBSERVADO", filas(observado));
            Table escala = escala(true);
            titulo("ESCALA DE VALORACIÓN", escala);
            itemsNumerados();
            observacionesYCompromisos();
            firmasEnRecuadro("DIRECTIVO MONITOREADO");
            matrices();
        }

        void docente() {
            titulo("DATOS DE IDENTIFICACIÓN", filas(institucion(true)));
            titulo("DATOS DEL MONITOR", filas(monitor()));
            List<Table> observado = new ArrayList<>(List.of(
                    fila(new float[]{54, 16, 30}, cabecera("Nombres Apellidos"), cabecera("Nro. Celular"), cabecera("Documento de Identidad")),
                    fila(anchosConCasillas(new float[]{54, 16}, 30, CASILLAS_DOCUMENTO),
                            concat(List.of(valor(d.observadoNombre()), valor(d.celular())), casillas(d.observadoDocumento(), CASILLAS_DOCUMENTO))),
                    fila(new float[]{20, 24, 18, 18, 20}, etiqueta("Grado/Sección"), valor(d.gradoSeccion()),
                            cabecera("Varones"), cabecera("Mujeres"), cabecera("Total"))));
            observado.addAll(estudiantes());
            Table niveles = fila(new float[]{100}, etiqueta("Niveles de valoración").setBackgroundColor(ColorConstants.WHITE));
            observado.add(niveles);
            titulo("DATOS DEL DOCENTE OBSERVADO", filas(observado));
            doc.add(escala(false));
            itemsNumerados();
            renglones("OBSERVACIONES/ RECOMENDACIONES:", hoja.observaciones(), 6);
            if (!vacio(compromisos())) {
                renglones("COMPROMISOS:", compromisos(), 4);
            }
            firmasEnRecuadro("DOCENTE MONITOREADO");
            matrices();
        }

        // Datos de identificación

        /** GEREDU, UGEL, IE, código modular, dirección, lugar, nivel/modalidad y formas de atención EIB. */
        List<Table> institucion(boolean conDirector) {
            List<Table> filas = new ArrayList<>();
            filas.add(fila(new float[]{15, 12, 8, 19, 17, 29}, etiqueta("GEREDU"), valor("CUSCO"), etiqueta("UGEL"),
                    valor(d.ugel()), etiqueta("Distrito"), valor(d.distrito())));
            filas.add(fila(anchosConCasillas(new float[]{15, 39, 17}, 29, CASILLAS_CODIGO_MODULAR),
                    concat(List.of(etiqueta("IE"), valor(d.institucion()), etiqueta("Código Modular")),
                            casillas(d.codigoModular(), CASILLAS_CODIGO_MODULAR))));
            if (conDirector) {
                filas.add(fila(new float[]{15, 39, 17, 29}, etiqueta("Director de la IE"), valor(d.directorNombre()),
                        etiqueta("Celular"), valor(d.directorCelular())));
            }
            filas.add(fila(new float[]{15, 39, 17, 29}, etiqueta("Dirección de la IE"), valor(d.direccion()),
                    etiqueta("Lugar"), valor(d.lugar())));
            List<Cell> nivel = new ArrayList<>(List.of(etiqueta("Nivel/Modalidad"),
                    etiqueta("Inicial"), marca(es(d.nivel(), "INICIAL")),
                    etiqueta("Primaria"), marca(es(d.nivel(), "PRIMARIA")),
                    etiqueta("Secundaria"), marca(es(d.nivel(), "SECUNDARIA"))));
            for (String modalidad : List.of("JEC", "JER", "SFT", "SRE", "EBA", "EBE")) {
                boolean marcada = es(d.modeloServicio(), modalidad) || es(d.modalidad(), modalidad);
                nivel.add(cabecera(modalidad + (marcada ? "  X" : "")));
            }
            filas.add(fila(new float[]{15, 8, 4, 8, 4, 10, 4, 7.8f, 7.8f, 7.8f, 7.8f, 7.8f, 8}, nivel.toArray(Cell[]::new)));
            filas.add(fila(new float[]{19, 16, 19, 17, 29}, etiqueta("Formas de atención EIB"),
                    etiqueta("Fortalecimiento").setBackgroundColor(ColorConstants.WHITE),
                    marca(es(d.formaEib(), "FORTALECIMIENTO")),
                    etiqueta("Revitalización").setBackgroundColor(ColorConstants.WHITE),
                    marca(es(d.formaEib(), "REVITALIZACION") || es(d.formaEib(), "REVITALIZACIÓN"))));
            return filas;
        }

        List<Table> monitor() {
            String[] fecha = vacio(d.fechaAplicacion()) ? new String[]{"", "", ""} : (d.fechaAplicacion() + "//").split("/", -1);
            return List.of(
                    fila(new float[]{26, 46, 28}, cabecera("Nombres"), cabecera("Apellidos"), cabecera("Documento de Identidad")),
                    fila(anchosConCasillas(new float[]{26, 46}, 28, CASILLAS_DOCUMENTO),
                            concat(List.of(valor(d.monitorNombres()), valor(d.monitorApellidos())),
                                    casillas(d.monitorDocumento(), CASILLAS_DOCUMENTO))),
                    fila(new float[]{21, 5, 17, 6, 6, 7, 10, 8, 9, 11},
                            etiqueta("Número de visita a la IE"), valor(d.numeroVisita()).setTextAlignment(TextAlignment.CENTER),
                            etiqueta("Fecha de aplicación"),
                            valor(fecha[0]).setTextAlignment(TextAlignment.CENTER),
                            valor(fecha[1]).setTextAlignment(TextAlignment.CENTER),
                            valor(fecha[2]).setTextAlignment(TextAlignment.CENTER),
                            etiqueta("Hora inicio"), valor(d.horaInicio()), etiqueta("Hora fin"), valor(d.horaFin())));
        }

        /** Estudiantes matriculados, asistentes e inclusivos asistentes, por sexo y en total. */
        List<Table> estudiantes() {
            float[] anchos = {44, 18, 18, 20};
            return List.of(
                    fila(anchos, etiqueta("Estudiantes matriculados"), numero(d.matriculadosVarones()),
                            numero(d.matriculadosMujeres()), numero(total(d.matriculadosVarones(), d.matriculadosMujeres()))),
                    fila(anchos, etiqueta("Estudiantes asistentes"), numero(d.asistentesVarones()),
                            numero(d.asistentesMujeres()), numero(total(d.asistentesVarones(), d.asistentesMujeres()))),
                    fila(anchos, etiqueta("N° de estudiantes inclusivos asistentes"), numero(d.inclusivosVarones()),
                            numero(d.inclusivosMujeres()), numero(total(d.inclusivosVarones(), d.inclusivosMujeres()))));
        }

        /** Escala con el nivel y su descripción; en cursiva en la ficha de directivos. */
        Table escala(boolean cursiva) {
            int columnas = Math.max(hoja.escala().size(), 1);
            Table tabla = tabla(UnitValue.createPercentArray(columnas)).setMarginTop(cursiva ? 0 : 4);
            for (EscalaNivelResponse nivel : hoja.escala()) {
                tabla.addCell(celda(parrafo("Nivel " + nivel.nivel() + " (" + nivel.etiqueta() + ")",
                        cursiva ? f.cursivaNegrita : f.negrita, TEXTO)).setTextAlignment(TextAlignment.CENTER));
            }
            for (EscalaNivelResponse nivel : hoja.escala()) {
                tabla.addCell(celda(parrafo(nivel.rubrica(), cursiva ? f.cursiva : f.normal, TEXTO))
                        .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE));
            }
            return tabla;
        }

        // Ítems

        /** Ítems de directivos y docentes: «n. enunciado», NIVEL 1–4, observaciones y «SUMATORIA TOTAL =». */
        void itemsNumerados() {
            for (Dimension dimension : hoja.dimensiones()) {
                // La primera fila solo sirve para medir; la numeración sigue desde la tabla completa.
                int inicio = item;
                Table primera = itemsNumerados(dimension, dimension.items().subList(0, Math.min(1, dimension.items().size())), false);
                item = inicio;
                titulo(dimension.nombre(), primera, itemsNumerados(dimension, dimension.items(), true));
            }
        }

        private Table itemsNumerados(Dimension dimension, List<Item> items, boolean conTotal) {
            List<String> columnas = dimension.columnas();
            float[] anchos = new float[columnas.size() + 2];
            anchos[0] = 58;
            for (int i = 1; i <= columnas.size(); i++) {
                anchos[i] = 16f / columnas.size();
            }
            anchos[anchos.length - 1] = 26;
            Table tabla = tabla(UnitValue.createPercentArray(anchos));
            tabla.addHeaderCell(cabecera("ÍTEMS", 2, 1));
            tabla.addHeaderCell(cabecera("NIVEL", 1, columnas.size()));
            tabla.addHeaderCell(cabecera("OBSERVACIONES", 2, 1));
            for (String columna : columnas) {
                tabla.addHeaderCell(cabecera(columna));
            }
            for (Item it : items) {
                item++;
                Paragraph enunciado = new Paragraph()
                        .add(new Text(item + ". ").setFont(f.negrita))
                        .add(new Text(texto(it.enunciado())))
                        .setFixedLeading(TEXTO * INTERLINEADO).setMargin(0)
                        .setTextAlignment(TextAlignment.JUSTIFIED);
                tabla.addCell(celda(enunciado).setKeepTogether(true));
                marcas(tabla, columnas, it);
                tabla.addCell(valor(it.observacion()).setKeepTogether(true));
            }
            if (conTotal) {
                tabla.addCell(celda(parrafo("SUMATORIA TOTAL =", f.negrita, TEXTO)).setTextAlignment(TextAlignment.CENTER));
                tabla.addCell(celda(parrafo(dimension.total(), f.negrita, TEXTO), columnas.size() + 1));
            }
            return tabla;
        }

        /** Ítems de especialistas: N°, ítems, 1–4, observaciones y «PUNTAJE TOTAL PARCIAL». */
        List<Table> itemsEspecialista(Dimension dimension) {
            int inicio = item;
            Table primera = itemsEspecialista(dimension, dimension.items().subList(0, Math.min(1, dimension.items().size())), false);
            item = inicio;
            Table tabla = itemsEspecialista(dimension, dimension.items(), true);
            return List.of(primera, tabla);
        }

        private Table itemsEspecialista(Dimension dimension, List<Item> items, boolean conTotal) {
            List<String> columnas = dimension.columnas();
            float[] anchos = new float[columnas.size() + 3];
            anchos[0] = 5;
            anchos[1] = 53;
            for (int i = 2; i < columnas.size() + 2; i++) {
                anchos[i] = 14f / columnas.size();
            }
            anchos[anchos.length - 1] = 28;
            Table tabla = tabla(UnitValue.createPercentArray(anchos));
            tabla.addHeaderCell(cabecera("N°"));
            tabla.addHeaderCell(cabecera("ÍTEMS"));
            for (String columna : columnas) {
                tabla.addHeaderCell(cabecera(columna));
            }
            tabla.addHeaderCell(cabecera("OBSERVACIONES"));
            for (Item it : items) {
                item++;
                tabla.addCell(valor(String.valueOf(item)).setTextAlignment(TextAlignment.CENTER)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE).setKeepTogether(true));
                tabla.addCell(celda(parrafo(texto(it.enunciado()), f.normal, TEXTO).setTextAlignment(TextAlignment.JUSTIFIED))
                        .setKeepTogether(true));
                marcas(tabla, columnas, it);
                tabla.addCell(valor(it.observacion()).setKeepTogether(true));
            }
            if (conTotal) {
                tabla.addCell(celda(parrafo("PUNTAJE TOTAL PARCIAL", f.negrita, TEXTO), 2).setTextAlignment(TextAlignment.CENTER));
                tabla.addCell(celda(parrafo(dimension.total(), f.negrita, TEXTO), columnas.size() + 1));
            }
            return tabla;
        }

        private void marcas(Table tabla, List<String> columnas, Item it) {
            for (String columna : columnas) {
                boolean marcada = !vacio(it.marcado()) && columna.equals(it.marcado());
                tabla.addCell(celda(parrafo(marcada ? "X" : "", f.negrita, TEXTO))
                        .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setKeepTogether(true));
            }
        }

        // Cierre

        String compromisos() {
            List<String> partes = new ArrayList<>();
            if (!vacio(hoja.compromisoMonitoreado())) {
                partes.add("Del monitoreado: " + hoja.compromisoMonitoreado().trim());
            }
            if (!vacio(hoja.compromisoMonitor())) {
                partes.add("Del monitor: " + hoja.compromisoMonitor().trim());
            }
            return String.join("\n", partes);
        }

        /** Título y renglones para escribir a mano; si hay texto, va en los primeros. */
        void renglones(String titulo, String texto, int cantidad) {
            Table tabla = tabla(UnitValue.createPercentArray(1)).setKeepTogether(true);
            int vacios = cantidad;
            if (!vacio(texto)) {
                tabla.addCell(celda(parrafo(texto, f.normal, TEXTO)).setMinHeight(ALTO_RENGLON));
                vacios = Math.max(cantidad - 1 - texto.length() / 110, 1);
            }
            for (int i = 0; i < vacios; i++) {
                tabla.addCell(celda(new Paragraph()).setHeight(ALTO_RENGLON));
            }
            Paragraph encabezadoRenglones = parrafo(titulo, f.negrita, SECCION).setMarginTop(10).setMarginBottom(3);
            saltoSiNoCabe(encabezadoRenglones, tabla);
            doc.add(encabezadoRenglones);
            doc.add(tabla);
        }

        /** Directivos: observaciones y compromisos en dos recuadros, uno al lado del otro. */
        void observacionesYCompromisos() {
            Table tabla = tabla(UnitValue.createPercentArray(2)).setMarginTop(12).setKeepTogether(true);
            tabla.addCell(cabecera("OBSERVACIONES"));
            tabla.addCell(cabecera("COMPROMISOS"));
            tabla.addCell(valor(hoja.observaciones()).setHeight(110));
            tabla.addCell(valor(compromisos()).setHeight(110));
            doc.add(tabla);
        }

        /** Directivos y docentes: recuadros de firma con el rol debajo. */
        void firmasEnRecuadro(String monitoreado) {
            Table tabla = tabla(new UnitValue[]{UnitValue.createPercentValue(45), UnitValue.createPercentValue(10),
                    UnitValue.createPercentValue(45)}).setMarginTop(24).setKeepTogether(true);
            tabla.addCell(celda(new Paragraph()).setHeight(ALTO_FIRMA));
            tabla.addCell(new Cell().setBorder(Border.NO_BORDER));
            tabla.addCell(celda(new Paragraph()).setHeight(ALTO_FIRMA));
            tabla.addCell(firma(monitoreado, hoja.firmaMonitoreado()));
            tabla.addCell(new Cell().setBorder(Border.NO_BORDER));
            tabla.addCell(firma("ESPECIALISTA MONITOR", hoja.firmaMonitor()));
            doc.add(tabla);
        }

        private Cell firma(String rol, String nombre) {
            Cell celda = celda(parrafo(rol, f.negrita, TEXTO + 1)).setTextAlignment(TextAlignment.CENTER);
            if (!vacio(nombre)) {
                celda.add(parrafo(nombre, f.normal, TEXTO));
            }
            return celda;
        }

        /** Especialistas: «MONITOREADO / FIRMA Y SELLO» y «MONITOR / FIRMA Y SELLO», sin recuadro. */
        void firmasSinRecuadro() {
            Table tabla = tabla(UnitValue.createPercentArray(2)).setMarginTop(50).setKeepTogether(true);
            for (String[] firma : List.of(new String[]{"MONITOREADO", hoja.firmaMonitoreado()},
                    new String[]{"MONITOR", hoja.firmaMonitor()})) {
                Cell celda = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
                        .add(parrafo(firma[0], f.normal, TEXTO))
                        .add(parrafo("FIRMA  Y  SELLO", f.normal, TEXTO));
                if (!vacio(firma[1])) {
                    celda.add(parrafo(firma[1], f.normal, TEXTO));
                }
                tabla.addCell(celda);
            }
            doc.add(tabla);
        }

        // Matriz de puntajes

        /** Matriz por dimensión, tres por fila, con los niveles en sus colores y el puntaje obtenido marcado. */
        void matrices() {
            List<Dimension> conMatriz = hoja.dimensiones().stream().filter(x -> !x.matriz().isEmpty()).toList();
            if (conMatriz.isEmpty()) {
                return;
            }
            // Todas las matrices en una misma página, como en el formato impreso.
            Table bloque = tabla(UnitValue.createPercentArray(1));
            bloque.setMarginTop(24).setKeepTogether(true);
            for (int inicio = 0; inicio < conMatriz.size(); inicio += MATRICES_POR_FILA) {
                List<Dimension> grupo = conMatriz.subList(inicio, Math.min(inicio + MATRICES_POR_FILA, conMatriz.size()));
                bloque.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(filaMatrices(grupo)));
            }
            doc.add(bloque);
        }

        private Table filaMatrices(List<Dimension> grupo) {
            float[] anchos = new float[MATRICES_POR_FILA * 3];
            for (int i = 0; i < MATRICES_POR_FILA; i++) {
                anchos[i * 3] = 15.33f;
                anchos[i * 3 + 1] = 8;
                anchos[i * 3 + 2] = 10;
            }
            Table tabla = tabla(UnitValue.createPercentArray(anchos));
            int niveles = grupo.stream().mapToInt(x -> x.matriz().size()).max().orElse(0);
            for (int i = 0; i < MATRICES_POR_FILA; i++) {
                tabla.addCell(i < grupo.size()
                        ? cabecera(mayusculas(grupo.get(i).nombre()), 1, 3)
                        : new Cell(1, 3).setBorder(Border.NO_BORDER));
            }
            for (int i = 0; i < MATRICES_POR_FILA; i++) {
                for (String titulo : List.of("NIVEL", "RANGO", "PUNTAJE")) {
                    tabla.addCell(i < grupo.size() ? cabecera(titulo) : sinBorde());
                }
            }
            for (int n = 0; n < niveles; n++) {
                for (int i = 0; i < MATRICES_POR_FILA; i++) {
                    if (i >= grupo.size() || n >= grupo.get(i).matriz().size()) {
                        for (int c = 0; c < 3; c++) {
                            tabla.addCell(i < grupo.size() ? celda(new Paragraph()) : sinBorde());
                        }
                        continue;
                    }
                    Dimension dimension = grupo.get(i);
                    RangoNivelResponse rango = dimension.matriz().get(n);
                    Color color = COLORES_NIVEL[Math.min(n, COLORES_NIVEL.length - 1)];
                    Color letra = n == 1 ? NEGRO : ColorConstants.WHITE;
                    boolean obtenido = dimension.suma() != null && rango.minimo() != null && rango.maximo() != null
                            && dimension.suma().compareTo(rango.minimo()) >= 0 && dimension.suma().compareTo(rango.maximo()) <= 0;
                    tabla.addCell(celda(parrafo(mayusculas(rango.etiqueta()), f.negrita, TEXTO - 0.5f)).setBackgroundColor(color).setFontColor(letra));
                    tabla.addCell(celda(parrafo(rango.rangoTexto(), f.negrita, TEXTO)).setBackgroundColor(color).setFontColor(letra));
                    tabla.addCell(celda(parrafo(obtenido ? dimension.suma().stripTrailingZeros().toPlainString() : "", f.negrita, TEXTO))
                            .setBackgroundColor(color).setFontColor(letra).setTextAlignment(TextAlignment.CENTER));
                }
            }
            return tabla;
        }

        // Títulos de sección y saltos de página

        /** «I. TÍTULO» seguido de su tabla; pasa de página si el título y el comienzo de la tabla no caben. */
        void titulo(String texto, Table tabla) {
            titulo(texto, tabla, tabla);
        }

        void titulo(String texto, Table comienzo, Table tabla) {
            Paragraph titulo = new Paragraph()
                    .add(new Text(ROMANOS[Math.min(seccion, ROMANOS.length - 1)] + ".").setFont(f.negrita))
                    .add(new Text("   " + mayusculas(texto)).setFont(f.negrita))
                    .setFontSize(SECCION).setFixedLeading(SECCION * INTERLINEADO)
                    .setMarginTop(9).setMarginBottom(3).setMarginLeft(0);
            seccion++;
            saltoSiNoCabe(titulo, comienzo);
            doc.add(titulo);
            if (tabla != null) {
                doc.add(tabla);
            }
        }

        private void saltoSiNoCabe(Paragraph titulo, IBlockElement comienzo) {
            float libre = doc.getRenderer().getCurrentArea().getBBox().getHeight();
            float necesario = (titulo == null ? 0 : alto(titulo)) + alto(comienzo);
            if (libre < necesario) {
                doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
            }
        }

        private float alto(IBlockElement elemento) {
            float ancho = doc.getPdfDocument().getDefaultPageSize().getWidth() - doc.getLeftMargin() - doc.getRightMargin();
            IRenderer renderer = elemento.createRendererSubTree().setParent(doc.getRenderer());
            LayoutResult resultado = renderer.layout(new LayoutContext(new LayoutArea(1, new Rectangle(ancho, 10_000))));
            return resultado.getOccupiedArea() == null ? 0 : resultado.getOccupiedArea().getBBox().getHeight();
        }

        // Celdas

        /** Varias filas de anchos distintos, una debajo de otra, como una sola tabla. */
        private Table filas(Table... filas) {
            return filas(List.of(filas));
        }

        private Table filas(List<Table> filas) {
            Table tabla = tabla(UnitValue.createPercentArray(1));
            for (Table fila : filas) {
                tabla.addCell(new Cell().setBorder(Border.NO_BORDER).setPadding(0).add(fila));
            }
            return tabla;
        }

        private Table fila(float[] anchos, Cell... celdas) {
            return fila(anchos, List.of(celdas));
        }

        private Table fila(float[] anchos, List<Cell> celdas) {
            Table tabla = tabla(UnitValue.createPercentArray(anchos));
            for (Cell celda : celdas) {
                tabla.addCell(celda.setMinHeight(ALTO_FILA));
            }
            return tabla;
        }

        private Cell cabecera(String texto) {
            return cabecera(texto, 1, 1);
        }

        private Cell cabecera(String texto, int filas, int columnas) {
            return celda(parrafo(texto, f.negrita, TEXTO), filas, columnas).setBackgroundColor(GRIS)
                    .setTextAlignment(TextAlignment.CENTER).setVerticalAlignment(VerticalAlignment.MIDDLE);
        }

        private Cell etiqueta(String texto) {
            return celda(parrafo(texto, f.negrita, TEXTO)).setBackgroundColor(GRIS).setVerticalAlignment(VerticalAlignment.MIDDLE);
        }

        private Cell valor(String texto) {
            return celda(parrafo(texto(texto), f.normal, TEXTO)).setVerticalAlignment(VerticalAlignment.MIDDLE);
        }

        private Cell numero(String texto) {
            return valor(texto).setTextAlignment(TextAlignment.CENTER);
        }

        private Cell marca(boolean marcada) {
            return valor(marcada ? "X" : "").setTextAlignment(TextAlignment.CENTER).setFont(f.negrita);
        }

        private Cell etiquetaValor(String etiqueta, String valor) {
            return celda(new Paragraph().add(new Text(etiqueta).setFont(f.negrita)).add(new Text(texto(valor)))
                    .setFixedLeading(TEXTO * INTERLINEADO).setMargin(0)).setVerticalAlignment(VerticalAlignment.MIDDLE);
        }

        /** Un carácter por casilla, como el DNI o el código modular en el formato impreso. */
        private List<Cell> casillas(String texto, int cantidad) {
            String valor = texto(texto).trim();
            List<Cell> celdas = new ArrayList<>();
            for (int i = 0; i < cantidad; i++) {
                celdas.add(numero(i < valor.length() ? String.valueOf(valor.charAt(i)) : ""));
            }
            return celdas;
        }

        private Cell sinBorde() {
            return new Cell().setBorder(Border.NO_BORDER);
        }

        private Paragraph parrafo(String texto, PdfFont fuente, float tamano) {
            return ItextOficialRenderer.parrafo(texto, fuente, tamano);
        }
    }

    // Utilidades

    private static Table tabla(UnitValue[] anchos) {
        return new Table(anchos).useAllAvailableWidth().setFixedLayout();
    }

    private static Cell celda(Paragraph contenido) {
        return celda(contenido, 1);
    }

    private static Cell celda(Paragraph contenido, int columnas) {
        return celda(contenido, 1, columnas);
    }

    private static Cell celda(Paragraph contenido, int filas, int columnas) {
        return new Cell(filas, columnas).add(contenido)
                .setBorder(new SolidBorder(NEGRO, BORDE))
                .setPaddingTop(2).setPaddingBottom(2).setPaddingLeft(3).setPaddingRight(3);
    }

    private static Paragraph parrafo(String texto, PdfFont fuente, float tamano) {
        return new Paragraph(texto(texto)).setFont(fuente).setFontSize(tamano)
                .setFixedLeading(tamano * INTERLINEADO).setMargin(0);
    }

    /** Anchos de una fila cuyas últimas columnas son casillas de un carácter. */
    private static float[] anchosConCasillas(float[] anteriores, float anchoCasillas, int cantidad) {
        float[] anchos = new float[anteriores.length + cantidad];
        System.arraycopy(anteriores, 0, anchos, 0, anteriores.length);
        for (int i = 0; i < cantidad; i++) {
            anchos[anteriores.length + i] = anchoCasillas / cantidad;
        }
        return anchos;
    }

    private static List<Cell> concat(List<Cell> a, List<Cell> b) {
        List<Cell> todas = new ArrayList<>(a);
        todas.addAll(b);
        return todas;
    }

    private static String total(String varones, String mujeres) {
        if (vacio(varones) && vacio(mujeres)) {
            return "";
        }
        try {
            return String.valueOf(Integer.parseInt(vacio(varones) ? "0" : varones.trim())
                    + Integer.parseInt(vacio(mujeres) ? "0" : mujeres.trim()));
        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static boolean es(String valor, String esperado) {
        return valor != null && valor.trim().equalsIgnoreCase(esperado);
    }

    private static String unir(String a, String b) {
        return (texto(a) + " " + texto(b)).trim();
    }

    private static String texto(String texto) {
        return texto == null ? "" : texto;
    }

    private static String mayusculas(String texto) {
        return texto(texto).toUpperCase(Locale.ROOT);
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
        private final PdfFont cursiva = crear(StandardFonts.HELVETICA_OBLIQUE);
        private final PdfFont cursivaNegrita = crear(StandardFonts.HELVETICA_BOLDOBLIQUE);

        private static PdfFont crear(String nombre) {
            try {
                return PdfFontFactory.createFont(nombre, PdfEncodings.WINANSI);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
