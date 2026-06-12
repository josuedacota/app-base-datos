package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Bitacora;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BitacoraDAO {

    /** Registra un nuevo evento en la bitácora. */
    public void registrar(int idUsuario, String accion, String tabla, String detalle)
            throws SQLException {
        String sql = "INSERT INTO BITACORA(id_usuario, accion, tabla_afectada, detalle) " +
                     "VALUES(?, ?, ?, ?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, accion);
            ps.setString(3, tabla);
            ps.setString(4, detalle);
            ps.executeUpdate();
        }
    }

    /**
     * Consulta la bitácora con filtros opcionales.
     *
     * @param fechaDesde  timestamp de inicio  (ej. "2024-01-01 00:00:00"), nullable
     * @param fechaHasta  timestamp de fin      (ej. "2024-12-31 23:59:59"), nullable
     * @param accion      tipo de operación     (ej. "INSERT"), búsqueda parcial ILIKE, nullable
     * @param tabla       tabla afectada        (ej. "CREDITO"),  búsqueda parcial ILIKE, nullable
     * @param idUsuario   ID exacto del usuario, nullable
     */
    public List<Bitacora> listar(String fechaDesde,
                                  String fechaHasta,
                                  String accion,
                                  String tabla,
                                  Integer idUsuario) throws SQLException {
        List<Bitacora> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT * FROM BITACORA WHERE 1=1");
        if (fechaDesde != null) sql.append(" AND fecha_hora >= ?::timestamp");
        if (fechaHasta != null) sql.append(" AND fecha_hora <= ?::timestamp");
        if (accion     != null) sql.append(" AND accion          ILIKE ?");
        if (tabla      != null) sql.append(" AND tabla_afectada  ILIKE ?");
        if (idUsuario  != null) sql.append(" AND id_usuario      = ?");
        sql.append(" ORDER BY fecha_hora DESC");

        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1;
            if (fechaDesde != null) ps.setString(i++, fechaDesde);
            if (fechaHasta != null) ps.setString(i++, fechaHasta);
            if (accion     != null) ps.setString(i++, "%" + accion + "%");
            if (tabla      != null) ps.setString(i++, "%" + tabla  + "%");
            if (idUsuario  != null) ps.setInt(i,    idUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bitacora b = new Bitacora();
                    b.setIdRegistro(rs.getInt("id_registro"));
                    b.setIdUsuario(rs.getInt("id_usuario"));
                    b.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                    b.setAccion(rs.getString("accion"));
                    b.setTablaAfectada(rs.getString("tabla_afectada"));
                    b.setDetalle(rs.getString("detalle"));
                    lista.add(b);
                }
            }
        }
        return lista;
    }
}
