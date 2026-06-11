package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReporteDAO {

    public List<Map<String, Object>> r1AsociadosPorEstadoYAgencia(String estado, String agencia) throws SQLException {
        String sql = "SELECT a.cedula, a.nombres, a.apellidos, a.tipo_asociado, a.fecha_afiliacion, a.estado, " +
                "COUNT(DISTINCT ca.numero_cuenta) AS productos_activos " +
                "FROM ASOCIADO a " +
                "LEFT JOIN CUENTA_AHORRO ca ON ca.cedula_asociado=a.cedula AND ca.estado='activa' " +
                "WHERE 1=1" +
                (estado != null ? " AND a.estado=?" : "") +
                (agencia != null ? " AND ca.codigo_agencia=?" : "") +
                " GROUP BY a.cedula ORDER BY a.apellidos, a.nombres";
        return ejecutar(sql, estado, agencia);
    }

    public List<Map<String, Object>> r2ExtractoCuenta(String numeroCuenta, String desde, String hasta,
                                                      String tipo, String canal) throws SQLException {
        String sql = "SELECT numero_transaccion, tipo_movimiento, canal, valor_transaccion, fecha_hora " +
                "FROM MOVIMIENTO WHERE numero_cuenta=?" +
                (desde != null ? " AND fecha_hora >= ?::timestamp" : "") +
                (hasta != null ? " AND fecha_hora <= ?::timestamp" : "") +
                (tipo != null ? " AND tipo_movimiento=?" : "") +
                (canal != null ? " AND canal=?" : "") +
                " ORDER BY fecha_hora";
        return ejecutar(sql, numeroCuenta, desde, hasta, tipo, canal);
    }

    public List<Map<String, Object>> r3CarteraPorLineaYEstado(String agencia, String desde, String hasta) throws SQLException {
        String sql = "SELECT linea_credito, estado_credito, COUNT(*) AS num_creditos, " +
                "SUM(valor_aprobado) AS total_aprobado, " +
                "ROUND(100.0 * COUNT(*) / SUM(COUNT(*)) OVER(), 2) AS porcentaje " +
                "FROM CREDITO WHERE 1=1" +
                (agencia != null ? " AND codigo_agencia=?" : "") +
                (desde != null ? " AND fecha_aprobacion >= ?" : "") +
                (hasta != null ? " AND fecha_aprobacion <= ?" : "") +
                " GROUP BY linea_credito, estado_credito ORDER BY linea_credito, estado_credito";
        return ejecutar(sql, agencia, desde, hasta);
    }

    public List<Map<String, Object>> r4AsociadosEnMora() throws SQLException {
        String sql = "SELECT a.nombres, a.apellidos, c.numero_radicado, pc.numero_cuota, " +
                "c.fecha_primer_vencimiento, " +
                "CURRENT_DATE - (c.fecha_primer_vencimiento + (pc.numero_cuota - 1) * INTERVAL '1 month')::date AS dias_mora, " +
                "e.nombres AS asesor_nombres, e.apellidos AS asesor_apellidos " +
                "FROM PAGO_CREDITO pc " +
                "JOIN CREDITO c ON c.numero_radicado = pc.numero_radicado " +
                "JOIN ASOCIADO a ON a.cedula = c.cedula_asociado " +
                "JOIN EMPLEADO e ON e.cedula_empleado = c.cedula_asesor " +
                "WHERE pc.estado_pago IN ('pendiente','pagado_con_mora') " +
                "AND (c.fecha_primer_vencimiento + (pc.numero_cuota - 1) * INTERVAL '1 month')::date < CURRENT_DATE " +
                "ORDER BY dias_mora DESC";
        return ejecutar(sql);
    }

    public List<Map<String, Object>> r5HistorialPagos(String radicado) throws SQLException {
        String sql = "SELECT pc.numero_cuota, " +
                "(c.fecha_primer_vencimiento + (pc.numero_cuota - 1) * INTERVAL '1 month')::date AS fecha_vencimiento, " +
                "pc.fecha_pago, pc.valor_pagado, pc.estado_pago " +
                "FROM PAGO_CREDITO pc " +
                "JOIN CREDITO c ON c.numero_radicado = pc.numero_radicado " +
                "WHERE pc.numero_radicado=? ORDER BY pc.numero_cuota";
        return ejecutar(sql, radicado);
    }

    public List<Map<String, Object>> r6ProductividadAsesores(String agencia, String desde, String hasta) throws SQLException {
        String sql = "SELECT e.cedula_empleado, e.nombres, e.apellidos, e.codigo_agencia, " +
                "COUNT(DISTINCT as2.cedula_asociado) AS asociados_atendidos, " +
                "COUNT(DISTINCT cr.numero_radicado) AS creditos_radicados, " +
                "COALESCE(SUM(cr.valor_aprobado),0) AS valor_total_aprobado, " +
                "COUNT(DISTINCT ca.numero_cuenta) AS cuentas_abiertas " +
                "FROM EMPLEADO e " +
                "LEFT JOIN ASESORIA as2 ON as2.cedula_empleado = e.cedula_empleado " +
                (desde != null ? "AND as2.fecha_inicio >= ? " : "") +
                (hasta != null ? "AND as2.fecha_inicio <= ? " : "") +
                "LEFT JOIN CREDITO cr ON cr.cedula_asesor = e.cedula_empleado " +
                "LEFT JOIN CUENTA_AHORRO ca ON ca.codigo_agencia = e.codigo_agencia " +
                "WHERE e.tipo_empleado='asesor'" +
                (agencia != null ? " AND e.codigo_agencia=?" : "") +
                " GROUP BY e.cedula_empleado ORDER BY creditos_radicados DESC";
        return ejecutar(sql, desde, hasta, agencia);
    }

    public List<Map<String, Object>> r7CreditosConCodeudor() throws SQLException {
        String sql = "SELECT a.nombres AS titular_nombres, a.apellidos AS titular_apellidos, " +
                "c.numero_radicado, c.valor_aprobado, c.estado_credito, " +
                "cd.fecha_firma_pagare, " +
                "ac.nombres AS codeudor_nombres, ac.apellidos AS codeudor_apellidos " +
                "FROM CODEUDORIA cd " +
                "JOIN CREDITO c ON c.numero_radicado = cd.numero_radicado " +
                "JOIN ASOCIADO a ON a.cedula = c.cedula_asociado " +
                "JOIN ASOCIADO ac ON ac.cedula = cd.cedula_codeudor " +
                "ORDER BY c.estado_credito, a.apellidos";
        return ejecutar(sql);
    }

    private List<Map<String, Object>> ejecutar(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> resultado = new ArrayList<>();
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Object p : params) {
                if (p != null) ps.setObject(i++, p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    for (int j = 1; j <= cols; j++) {
                        fila.put(meta.getColumnName(j), rs.getObject(j));
                    }
                    resultado.add(fila);
                }
            }
        }
        return resultado;
    }
}