package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Agencia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AgenciaDAO {

    public List<Agencia> listarTodas() throws SQLException {
        List<Agencia> lista = new ArrayList<>();
        String sql = "SELECT * FROM AGENCIA ORDER BY nombre_agencia";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public Agencia buscarPorCodigo(String codigo) throws SQLException {
        String sql = "SELECT * FROM AGENCIA WHERE codigo_agencia = ?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(Agencia a) throws SQLException {
        String sql = "INSERT INTO AGENCIA(codigo_agencia,nombre_agencia,direccion,municipio,telefono,fecha_apertura) " +
                "VALUES(?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getCodigoAgencia());
            ps.setString(2, a.getNombreAgencia());
            ps.setString(3, a.getDireccion());
            ps.setString(4, a.getMunicipio());
            ps.setString(5, a.getTelefono());
            ps.setDate(6, Date.valueOf(a.getFechaApertura()));
            ps.executeUpdate();
        }
    }

    public void actualizar(Agencia a) throws SQLException {
        String sql = "UPDATE AGENCIA SET nombre_agencia=?, direccion=?, telefono=?, estado=? " +
                "WHERE codigo_agencia=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getNombreAgencia());
            ps.setString(2, a.getDireccion());
            ps.setString(3, a.getTelefono());
            ps.setString(4, a.getEstado());
            ps.setString(5, a.getCodigoAgencia());
            ps.executeUpdate();
        }
    }

    private Agencia mapear(ResultSet rs) throws SQLException {
        Agencia a = new Agencia();
        a.setCodigoAgencia(rs.getString("codigo_agencia"));
        a.setNombreAgencia(rs.getString("nombre_agencia"));
        a.setDireccion(rs.getString("direccion"));
        a.setMunicipio(rs.getString("municipio"));
        a.setTelefono(rs.getString("telefono"));
        a.setFechaApertura(rs.getDate("fecha_apertura").toLocalDate());
        a.setEstado(rs.getString("estado"));
        return a;
    }
}