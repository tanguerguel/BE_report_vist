package com.springboot.siremo.modules.monitoreo.repositories;

import lombok.AllArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Consultas de solo lectura sobre siremo_v3 para armar la hoja de una ficha.
 */
@Repository
@AllArgsConstructor
public class VistaPreviaFichaRepository {

    private final JdbcClient jdbc;

    public record FichaRow(UUID id, String nombre, boolean variasPublicadas, String denominacion,
                           List<String> niveles, String normaCodigo, String codigoVersion, String estado) {
    }

    public record DimensionRow(UUID id, String nombre, Integer orden) {
    }

    public record ItemRow(UUID dimensionId, UUID id, Integer orden, String enunciado) {
    }

    public record RangoRow(UUID dimensionId, Integer nivel, String etiqueta, String rubrica,
                           BigDecimal minimo, BigDecimal maximo) {
    }

    public Optional<FichaRow> findFicha(UUID fichaVersionId) {
        return jdbc.sql("""
                        SELECT v.id, i.nombre, i.permite_varias_publicadas, v.denominacion, v.niveles,
                               v.norma_codigo, v.codigo_version, v.estado
                          FROM siremo_v3.ficha_version v
                          JOIN siremo_v3.instrumento i ON i.id = v.instrumento_id
                         WHERE v.id = :id
                        """)
                .param("id", fichaVersionId)
                .query((rs, n) -> new FichaRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("nombre"),
                        rs.getBoolean("permite_varias_publicadas"),
                        rs.getString("denominacion"),
                        textos(rs.getArray("niveles")),
                        rs.getString("norma_codigo"),
                        rs.getString("codigo_version"),
                        rs.getString("estado")))
                .optional();
    }

    public List<DimensionRow> findDimensiones(UUID fichaVersionId) {
        return jdbc.sql("""
                        SELECT d.id, d.nombre, d.orden
                          FROM siremo_v3.ficha_dimension d
                         WHERE d.ficha_version_id = :id
                         ORDER BY d.orden
                        """)
                .param("id", fichaVersionId)
                .query((rs, n) -> new DimensionRow(
                        rs.getObject("id", UUID.class), rs.getString("nombre"), rs.getInt("orden")))
                .list();
    }

    public List<ItemRow> findItems(UUID fichaVersionId) {
        return jdbc.sql("""
                        SELECT it.dimension_id, it.id, it.orden, it.enunciado
                          FROM siremo_v3.ficha_item it
                          JOIN siremo_v3.ficha_dimension d ON d.id = it.dimension_id
                         WHERE d.ficha_version_id = :id
                         ORDER BY d.orden, it.orden
                        """)
                .param("id", fichaVersionId)
                .query((rs, n) -> new ItemRow(
                        rs.getObject("dimension_id", UUID.class),
                        rs.getObject("id", UUID.class),
                        rs.getInt("orden"),
                        rs.getString("enunciado")))
                .list();
    }

    /** La matriz de cada dimensión se guarda como JSON en ficha_dimension.matriz_niveles. */
    public List<RangoRow> findMatrices(UUID fichaVersionId) {
        return jdbc.sql("""
                        SELECT d.id AS dimension_id,
                               (r->>'nivel')::int AS nivel,
                               r->>'etiqueta' AS etiqueta,
                               r->>'rubrica' AS rubrica,
                               (r->>'minimo')::numeric AS minimo,
                               (r->>'maximo')::numeric AS maximo
                          FROM siremo_v3.ficha_dimension d
                          CROSS JOIN LATERAL jsonb_array_elements(coalesce(d.matriz_niveles, '[]'::jsonb)) r
                         WHERE d.ficha_version_id = :id
                         ORDER BY d.orden, (r->>'nivel')::int
                        """)
                .param("id", fichaVersionId)
                .query((rs, n) -> new RangoRow(
                        rs.getObject("dimension_id", UUID.class),
                        rs.getObject("nivel", Integer.class),
                        rs.getString("etiqueta"),
                        rs.getString("rubrica"),
                        rs.getBigDecimal("minimo"),
                        rs.getBigDecimal("maximo")))
                .list();
    }

    private static List<String> textos(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        return List.of((String[]) array.getArray());
    }
}
