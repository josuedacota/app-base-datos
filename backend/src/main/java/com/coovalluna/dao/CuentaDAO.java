package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.CuentaAhorro;
import com.coovalluna.model.Movimiento;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CuentaDAO {

    public List<CuentaAhorro> listarPorAsociado(String cedula) throws SQLException {
        List<CuentaAhorro> lista = new ArrayList<>();
        String sql = "SELECT * FROM CUENTA_AHORRO WHERE cedula_asociado=? ORDER BY fecha_apertura DESC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public CuentaAhorro buscarPorNumero(String numeroCuenta) throws SQLException {
        String sql = "SELECT * FROM CUENTA_AHORRO WHERE numero_cuenta=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(CuentaAhorro c) throws SQLException {
        String sql = "INSERT INTO CUENTA_AHORRO(numero_cuenta,fecha_apertura,estado," +
                "cedula_asociado,codigo_agencia) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNumeroCuenta());
            ps.setDate(2, Date.valueOf(c.getFechaApertura()));
            ps.setString(3, c.getEstado());
            ps.setString(4, c.getCedulaAsociado());
            ps.setString(5, c.getCodigoAgencia());
            ps.executeUpdate();
        }
    }

    public void actualizarEstado(String numeroCuenta, String estado) throws SQLException {
        String sql = "UPDATE CUENTA_AHORRO SET estado=? WHERE numero_cuenta=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, numeroCuenta);
            ps.executeUpdate();
        }
    }

    public void insertarMovimiento(Movimiento m) throws SQLException {
        String sql = "INSERT INTO MOVIMIENTO(numero_transaccion,numero_cuenta,tipo_movimiento," +
                "valor_transaccion,fecha_hora,canal,numero_cuenta_relacionada) VALUES(?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getNumeroTransaccion());
            ps.setString(2, m.getNumeroCuenta());
            ps.setString(3, m.getTipoMovimiento());
            ps.setBigDecimal(4, m.getValorTransaccion());
            ps.setTimestamp(5, Timestamp.valueOf(m.getFechaHora()));
            ps.setString(6, m.getCanal());
            ps.setString(7, m.getNumeroCuentaRelacionada());
            ps.executeUpdate();
        }
    }

    public List<Movimiento> listarMovimientos(String numeroCuenta, String tipo, String canal) throws SQLException {
        List<Movimiento> lista = new ArrayList<>();
        String sql = "SELECT * FROM MOVIMIENTO WHERE numero_cuenta=?" +
                (tipo != null ? " AND tipo_movimiento=?" : "") +
                (canal != null ? " AND canal=?" : "") +
                " ORDER BY fecha_hora DESC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, numeroCuenta);
            if (tipo != null) ps.setString(i++, tipo);
            if (canal != null) ps.setString(i, canal);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Movimiento mv = new Movimiento();
                    mv.setNumeroTransaccion(rs.getString("numero_transaccion"));
                    mv.setNumeroCuenta(rs.getString("numero_cuenta"));
                    mv.setTipoMovimiento(rs.getString("tipo_movimiento"));
                    mv.setValorTransaccion(rs.getBigDecimal("valor_transaccion"));
                    mv.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                    mv.setCanal(rs.getString("canal"));
                    mv.setNumeroCuentaRelacionada(rs.getString("numero_cuenta_relacionada"));
                    lista.add(mv);
                }
            }
        }
        return lista;
    }

    public java.math.BigDecimal calcularSaldo(String numeroCuenta) throws SQLException {
        String sql = "SELECT COALESCE(SUM(CASE WHEN tipo_movimiento IN ('deposito','transferencia_entrante') " +
                "THEN valor_transaccion ELSE -valor_transaccion END), 0) AS saldo " +
                "FROM MOVIMIENTO WHERE numero_cuenta=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numeroCuenta);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("saldo");
            }
        }
        return java.math.BigDecimal.ZERO;
    }

    private CuentaAhorro mapear(ResultSet rs) throws SQLException {
        CuentaAhorro c = new CuentaAhorro();
        c.setNumeroCuenta(rs.getString("numero_cuenta"));
        c.setFechaApertura(rs.getDate("fecha_apertura").toLocalDate());
        c.setEstado(rs.getString("estado"));
        c.setCedulaAsociado(rs.getString("cedula_asociado"));
        c.setCodigoAgencia(rs.getString("codigo_agencia"));
        return c;
    }
}