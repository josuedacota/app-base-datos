package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Bitacora;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BitacoraDAO {

    public void registrar(int idUsuario, String accion, String tabla, String detalle) throws SQLException {
        String sql = "INSERT INTO BITACORA(id_usuario,accion,tabla_afectada,detalle) VALUES(?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setString(2, accion);
            ps.setString(3, tabla);
            ps.setString(4, detalle);
            ps.executeUpdate();
        }
    }

    public List<Bitacora> listar(String fechaDesde, String fechaHasta, String accion) throws SQLException {
        List<Bitacora> lista = new ArrayList<>();
        String sql = "SELECT * FROM BITACORA WHERE 1=1" +
                (fechaDesde != null ? " AND fecha_hora >= ?::timestamp" : "") +
                (fechaHasta != null ? " AND fecha_hora <= ?::timestamp" : "") +
                (accion != null ? " AND accion ILIKE ?" : "") +
                " ORDER BY fecha_hora DESC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (fechaDesde != null) ps.setString(i++, fechaDesde);
            if (fechaHasta != null) ps.setString(i++, fechaHasta);
            if (accion != null) ps.setString(i, "%" + accion + "%");
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