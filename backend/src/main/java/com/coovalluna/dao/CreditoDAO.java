package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Credito;
import com.coovalluna.model.PagoCredito;
import com.coovalluna.model.Codeudoria;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CreditoDAO {

    public List<Credito> listarPorAsociado(String cedula) throws SQLException {
        List<Credito> lista = new ArrayList<>();
        String sql = "SELECT * FROM CREDITO WHERE cedula_asociado=? ORDER BY fecha_aprobacion DESC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Credito buscarPorRadicado(String radicado) throws SQLException {
        String sql = "SELECT * FROM CREDITO WHERE numero_radicado=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, radicado);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(Credito c) throws SQLException {
        String sql = "INSERT INTO CREDITO(numero_radicado,valor_solicitado,valor_aprobado,plazo_meses," +
                "tasa_interes_mensual,fecha_aprobacion,fecha_primer_vencimiento,estado_credito," +
                "linea_credito,cedula_asociado,codigo_agencia,cedula_asesor) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNumeroRadicado());
            ps.setBigDecimal(2, c.getValorSolicitado());
            ps.setBigDecimal(3, c.getValorAprobado());
            ps.setInt(4, c.getPlazoMeses());
            ps.setBigDecimal(5, c.getTasaInteresMensual());
            ps.setDate(6, c.getFechaAprobacion() != null ? Date.valueOf(c.getFechaAprobacion()) : null);
            ps.setDate(7, c.getFechaPrimerVencimiento() != null ? Date.valueOf(c.getFechaPrimerVencimiento()) : null);
            ps.setString(8, c.getEstadoCredito());
            ps.setString(9, c.getLineaCredito());
            ps.setString(10, c.getCedulaAsociado());
            ps.setString(11, c.getCodigoAgencia());
            ps.setString(12, c.getCedulaAsesor());
            ps.executeUpdate();
        }
    }

    public void actualizarEstado(String radicado, String estado) throws SQLException {
        String sql = "UPDATE CREDITO SET estado_credito=? WHERE numero_radicado=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, estado);
            ps.setString(2, radicado);
            ps.executeUpdate();
        }
    }

    public void insertarPago(PagoCredito p) throws SQLException {
        String sql = "INSERT INTO PAGO_CREDITO(numero_radicado,numero_cuota,fecha_pago,valor_pagado,estado_pago) " +
                "VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, p.getNumeroRadicado());
            ps.setInt(2, p.getNumeroCuota());
            ps.setDate(3, p.getFechaPago() != null ? Date.valueOf(p.getFechaPago()) : null);
            ps.setBigDecimal(4, p.getValorPagado());
            ps.setString(5, p.getEstadoPago());
            ps.executeUpdate();
        }
    }

    public List<PagoCredito> listarPagos(String radicado) throws SQLException {
        List<PagoCredito> lista = new ArrayList<>();
        String sql = "SELECT * FROM PAGO_CREDITO WHERE numero_radicado=? ORDER BY numero_cuota";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, radicado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PagoCredito p = new PagoCredito();
                    p.setNumeroRadicado(rs.getString("numero_radicado"));
                    p.setNumeroCuota(rs.getInt("numero_cuota"));
                    Date fp = rs.getDate("fecha_pago");
                    if (fp != null) p.setFechaPago(fp.toLocalDate());
                    p.setValorPagado(rs.getBigDecimal("valor_pagado"));
                    p.setEstadoPago(rs.getString("estado_pago"));
                    lista.add(p);
                }
            }
        }
        return lista;
    }

    public void insertarCodeudoria(Codeudoria cd) throws SQLException {
        String sql = "INSERT INTO CODEUDORIA(numero_radicado,cedula_codeudor,fecha_firma_pagare) VALUES(?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cd.getNumeroRadicado());
            ps.setString(2, cd.getCedulaCodeudor());
            ps.setDate(3, Date.valueOf(cd.getFechaFirmaPagare()));
            ps.executeUpdate();
        }
    }

    private Credito mapear(ResultSet rs) throws SQLException {
        Credito c = new Credito();
        c.setNumeroRadicado(rs.getString("numero_radicado"));
        c.setValorSolicitado(rs.getBigDecimal("valor_solicitado"));
        c.setValorAprobado(rs.getBigDecimal("valor_aprobado"));
        c.setPlazoMeses(rs.getInt("plazo_meses"));
        c.setTasaInteresMensual(rs.getBigDecimal("tasa_interes_mensual"));
        Date fa = rs.getDate("fecha_aprobacion");
        if (fa != null) c.setFechaAprobacion(fa.toLocalDate());
        Date fv = rs.getDate("fecha_primer_vencimiento");
        if (fv != null) c.setFechaPrimerVencimiento(fv.toLocalDate());
        c.setEstadoCredito(rs.getString("estado_credito"));
        c.setLineaCredito(rs.getString("linea_credito"));
        c.setCedulaAsociado(rs.getString("cedula_asociado"));
        c.setCodigoAgencia(rs.getString("codigo_agencia"));
        c.setCedulaAsesor(rs.getString("cedula_asesor"));
        return c;
    }
}