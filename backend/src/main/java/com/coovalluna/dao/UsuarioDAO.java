package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.UsuarioSistema;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    public UsuarioSistema buscarPorUsername(String username) throws SQLException {
        String sql = "SELECT * FROM USUARIO_SISTEMA WHERE username=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public List<UsuarioSistema> listarTodos() throws SQLException {
        List<UsuarioSistema> lista = new ArrayList<>();
        String sql = "SELECT * FROM USUARIO_SISTEMA ORDER BY username";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public void insertar(UsuarioSistema u) throws SQLException {
        String sql = "INSERT INTO USUARIO_SISTEMA(username,password_hash,rol,debe_cambiar_clave," +
                "cedula_empleado,cedula_asociado) VALUES(?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRol());
            ps.setBoolean(4, u.isDebeCambiarClave());
            ps.setString(5, u.getCedulaEmpleado());
            ps.setString(6, u.getCedulaAsociado());
            ps.executeUpdate();
        }
    }

    public void actualizarClave(int idUsuario, String passwordHash) throws SQLException {
        String sql = "UPDATE USUARIO_SISTEMA SET password_hash=?, debe_cambiar_clave=FALSE WHERE id_usuario=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    public void actualizarRol(int idUsuario, String rol) throws SQLException {
        String sql = "UPDATE USUARIO_SISTEMA SET rol=? WHERE id_usuario=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rol);
            ps.setInt(2, idUsuario);
            ps.executeUpdate();
        }
    }

    private UsuarioSistema mapear(ResultSet rs) throws SQLException {
        UsuarioSistema u = new UsuarioSistema();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRol(rs.getString("rol"));
        u.setDebeCambiarClave(rs.getBoolean("debe_cambiar_clave"));
        u.setCedulaEmpleado(rs.getString("cedula_empleado"));
        u.setCedulaAsociado(rs.getString("cedula_asociado"));
        return u;
    }
}