package com.springboot.siremo.modules.monitoreo.repositories;

import com.springboot.siremo.modules.monitoreo.dto.response.MonitoreoResumenResponse;
import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de solo lectura sobre siremo_v3 para armar la hoja de un monitoreo aplicado. Los datos de
 * identificación salen de la copia (snapshot) que se guardó al aplicar, no de los datos actuales.
 */
@Repository
@AllArgsConstructor
public class VistaPreviaMonitoreoRepository {

    private final JdbcClient jdbc;

    public record RespuestaRow(UUID dimensionId, String dimension, UUID itemId, String enunciado,
                               String estado, BigDecimal puntaje, String nota, String justificacion) {
    }

    public record PuntajeRow(UUID dimensionId, BigDecimal puntaje) {
    }

    /** El monitoreo con sus datos de identificación, como columnas de texto. */
    public Optional<Map<String, Object>> findMonitoreo(UUID monitoreoId) {
        return jdbc.sql("""
                        SELECT m.id, m.ficha_version_id, i.nombre AS ficha, i.actor_objetivo,
                               i.permite_varias_publicadas, v.denominacion, v.niveles,
                               m.estado_ficha, m.numero_visita, m.fecha_aplicacion, m.inicio, m.fin, m.observaciones,
                               m.snapshot #>> '{monitor,nombres}'            AS monitor_nombres,
                               m.snapshot #>> '{monitor,apellidos}'          AS monitor_apellidos,
                               m.snapshot #>> '{monitor,numero_documento}'   AS monitor_documento,
                               m.snapshot #>> '{observado,nombres}'          AS observado_nombres,
                               m.snapshot #>> '{observado,apellidos}'        AS observado_apellidos,
                               m.snapshot #>> '{observado,numero_documento}' AS observado_documento,
                               m.snapshot #>> '{institucion,ugel}'           AS ugel,
                               m.snapshot #>> '{institucion,nombre}'         AS institucion,
                               m.snapshot #>> '{institucion,codigo_modular}' AS codigo_modular,
                               m.snapshot #>> '{institucion,nivel}'          AS institucion_nivel,
                               m.snapshot #>> '{datos_visita,lugar}'          AS lugar,
                               m.snapshot #>> '{datos_visita,directorNombre}' AS director_nombre,
                               m.snapshot #>> '{datos_visita,directorCelular}' AS director_celular,
                               m.snapshot #>> '{datos_especialista,nivel}'   AS especialista_nivel,
                               m.snapshot #>> '{datos_especialista,celular}' AS especialista_celular,
                               m.snapshot #>> '{datos_especialista,correo}'  AS especialista_correo,
                               m.snapshot ->> 'experiencia_cargo_texto'     AS experiencia,
                               m.aula ->> 'condicion' AS condicion,
                               m.aula ->> 'celular'   AS celular,
                               m.aula ->> 'nivel'     AS aula_nivel,
                               m.aula ->> 'grado'     AS grado,
                               m.aula ->> 'seccion'   AS seccion,
                               m.aula ->> 'area'      AS area,
                               m.aula ->> 'sesion'    AS sesion,
                               (m.aula #>> '{estudiantes,matriculadosVarones}')::int AS matriculados_varones,
                               (m.aula #>> '{estudiantes,matriculadosMujeres}')::int AS matriculados_mujeres,
                               (m.aula #>> '{estudiantes,asistentesVarones}')::int   AS asistentes_varones,
                               (m.aula #>> '{estudiantes,asistentesMujeres}')::int   AS asistentes_mujeres,
                               (m.aula #>> '{estudiantes,inclusivosVarones}')::int   AS inclusivos_varones,
                               (m.aula #>> '{estudiantes,inclusivosMujeres}')::int   AS inclusivos_mujeres,
                               m.compromisos_ambos ->> 'monitoreado' AS compromiso_monitoreado,
                               m.compromisos_ambos ->> 'monitor'     AS compromiso_monitor,
                               coalesce((SELECT vc.fecha_retorno
                                           FROM siremo_v3.visita_cumplimiento vc
                                          WHERE vc.monitoreo_origen_id = m.id AND vc.cancelada_en IS NULL
                                          ORDER BY vc.creado_en DESC LIMIT 1),
                                        (m.compromisos_ambos ->> 'fechaRetorno')::date) AS fecha_retorno
                          FROM siremo_v3.monitoreo m
                          JOIN siremo_v3.ficha_version v ON v.id = m.ficha_version_id
                          JOIN siremo_v3.instrumento i ON i.id = v.instrumento_id
                         WHERE m.id = :id
                        """)
                .param("id", monitoreoId)
                .query()
                .listOfRows()
                .stream()
                .findFirst();
    }

    /**
     * Ítems de la ficha con la respuesta del monitoreo. Se muestran los que tienen respuesta y los que
     * se valoran en el nivel educativo del monitoreo (sin nivel, todos), como en el front.
     */
    public List<RespuestaRow> findRespuestas(UUID monitoreoId, UUID fichaVersionId, String nivel) {
        return jdbc.sql("""
                        SELECT d.id AS dimension_id, d.nombre AS dimension, it.id AS item_id, it.enunciado,
                               r.estado, o.puntaje, r.nota_campo, r.justificacion
                          FROM siremo_v3.ficha_dimension d
                          JOIN siremo_v3.ficha_item it ON it.dimension_id = d.id
                          LEFT JOIN LATERAL (SELECT r.*
                                               FROM siremo_v3.respuesta r
                                              WHERE r.monitoreo_id = :m AND r.item_id = it.id
                                              ORDER BY r.registrada_en DESC LIMIT 1) r ON true
                          LEFT JOIN siremo_v3.item_opcion o ON o.id = r.opcion_id
                         WHERE d.ficha_version_id = :v
                           AND (r.id IS NOT NULL
                                OR cast(:nivel AS text) IS NULL
                                OR (cast(:nivel AS text) = 'INICIAL' AND it.aplica_inicial)
                                OR (cast(:nivel AS text) = 'PRIMARIA' AND it.aplica_primaria)
                                OR (cast(:nivel AS text) = 'SECUNDARIA' AND it.aplica_secundaria))
                         ORDER BY d.orden, it.orden
                        """)
                .param("m", monitoreoId)
                .param("v", fichaVersionId)
                .param("nivel", nivel)
                .query((rs, n) -> new RespuestaRow(
                        rs.getObject("dimension_id", UUID.class),
                        rs.getString("dimension"),
                        rs.getObject("item_id", UUID.class),
                        rs.getString("enunciado"),
                        rs.getString("estado"),
                        rs.getBigDecimal("puntaje"),
                        rs.getString("nota_campo"),
                        rs.getString("justificacion")))
                .list();
    }

    /** Puntajes que ofrecen los ítems de cada dimensión: las columnas de valoración. */
    public List<PuntajeRow> findPuntajes(UUID fichaVersionId) {
        return jdbc.sql("""
                        SELECT DISTINCT it.dimension_id, o.puntaje
                          FROM siremo_v3.item_opcion o
                          JOIN siremo_v3.ficha_item it ON it.id = o.item_id
                          JOIN siremo_v3.ficha_dimension d ON d.id = it.dimension_id
                         WHERE d.ficha_version_id = :v AND o.puntaje IS NOT NULL
                         ORDER BY o.puntaje
                        """)
                .param("v", fichaVersionId)
                .query((rs, n) -> new PuntajeRow(rs.getObject("dimension_id", UUID.class), rs.getBigDecimal("puntaje")))
                .list();
    }

    /** Los monitoreos más recientes, para elegir cuál ver. */
    public List<MonitoreoResumenResponse> findRecientes(int limite) {
        return jdbc.sql("""
                        SELECT m.id, i.nombre AS ficha, i.actor_objetivo,
                               concat_ws(' ', m.snapshot #>> '{observado,nombres}', m.snapshot #>> '{observado,apellidos}') AS observado,
                               coalesce(m.snapshot #>> '{institucion,nombre}', m.snapshot #>> '{institucion,ugel}') AS institucion,
                               m.fecha_aplicacion, m.numero_visita, m.estado_ficha
                          FROM siremo_v3.monitoreo m
                          JOIN siremo_v3.ficha_version v ON v.id = m.ficha_version_id
                          JOIN siremo_v3.instrumento i ON i.id = v.instrumento_id
                         ORDER BY m.creado_en DESC
                         LIMIT :limite
                        """)
                .param("limite", limite)
                .query((rs, n) -> new MonitoreoResumenResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("ficha"),
                        rs.getString("actor_objetivo"),
                        rs.getString("observado"),
                        rs.getString("institucion"),
                        rs.getObject("fecha_aplicacion", Date.class) == null ? null
                                : rs.getObject("fecha_aplicacion", Date.class).toLocalDate(),
                        rs.getObject("numero_visita", Integer.class),
                        rs.getString("estado_ficha")))
                .list();
    }
}
