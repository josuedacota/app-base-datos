package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Asociado;
import com.coovalluna.model.AsociadoFundador;
import com.coovalluna.model.Beneficiario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AsociadoDAO {

    public List<Asociado> listarTodos(String estado, String tipo) throws SQLException {
        List<Asociado> lista = new ArrayList<>();
        String sql = "SELECT * FROM ASOCIADO WHERE 1=1" +
                (estado != null ? " AND estado=?" : "") +
                (tipo != null ? " AND tipo_asociado=?" : "") +
                " ORDER BY apellidos,nombres";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            if (estado != null) ps.setString(i++, estado);
            if (tipo != null) ps.setString(i, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Asociado buscarPorCedula(String cedula) throws SQLException {
        String sql = "SELECT * FROM ASOCIADO WHERE cedula=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(Asociado a) throws SQLException {
        String sql = "INSERT INTO ASOCIADO(cedula,nombres,apellidos,fecha_nacimiento,direccion," +
                "municipio,telefono,correo_electronico,fecha_afiliacion,estado," +
                "cuota_sostenimiento,tipo_asociado) VALUES(?,?,?,?,?,?,?,?,?,'activo',?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getCedula());
            ps.setString(2, a.getNombres());
            ps.setString(3, a.getApellidos());
            ps.setDate(4, Date.valueOf(a.getFechaNacimiento()));
            ps.setString(5, a.getDireccion());
            ps.setString(6, a.getMunicipio());
            ps.setString(7, a.getTelefono());
            ps.setString(8, a.getCorreoElectronico());
            ps.setDate(9, Date.valueOf(a.getFechaAfiliacion()));
            ps.setBigDecimal(10, a.getCuotaSostenimiento());
            ps.setString(11, a.getTipoAsociado());
            ps.executeUpdate();
        }
    }

    public void actualizar(Asociado a) throws SQLException {
        String sql = "UPDATE ASOCIADO SET nombres=?,apellidos=?,fecha_nacimiento=?,direccion=?," +
                "municipio=?,telefono=?,correo_electronico=?,estado=?,cuota_sostenimiento=? " +
                "WHERE cedula=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getNombres());
            ps.setString(2, a.getApellidos());
            ps.setDate(3, Date.valueOf(a.getFechaNacimiento()));
            ps.setString(4, a.getDireccion());
            ps.setString(5, a.getMunicipio());
            ps.setString(6, a.getTelefono());
            ps.setString(7, a.getCorreoElectronico());
            ps.setString(8, a.getEstado());
            ps.setBigDecimal(9, a.getCuotaSostenimiento());
            ps.setString(10, a.getCedula());
            ps.executeUpdate();
        }
    }

    public void insertarFundador(AsociadoFundador f) throws SQLException {
        String sql = "INSERT INTO ASOCIADO_FUNDADOR(cedula,numero_acta_fundacional," +
                "anio_reconocimiento,beneficios_especiales) VALUES(?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, f.getCedula());
            ps.setString(2, f.getNumeroActaFundacional());
            ps.setInt(3, f.getAnioReconocimiento());
            ps.setString(4, f.getBeneficiosEspeciales());
            ps.executeUpdate();
        }
    }

    public List<Beneficiario> listarBeneficiarios(String cedulaAsociado) throws SQLException {
        List<Beneficiario> lista = new ArrayList<>();
        String sql = "SELECT * FROM BENEFICIARIO WHERE cedula_asociado=? ORDER BY porcentaje_participacion DESC";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedulaAsociado);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Beneficiario b = new Beneficiario();
                    b.setIdBeneficiario(rs.getInt("id_beneficiario"));
                    b.setDocumentoBeneficiario(rs.getString("documento_beneficiario"));
                    b.setNombreCompleto(rs.getString("nombre_completo"));
                    b.setParentesco(rs.getString("parentesco"));
                    b.setPorcentajeParticipacion(rs.getBigDecimal("porcentaje_participacion"));
                    b.setTelefono(rs.getString("telefono"));
                    b.setCedulaAsociado(rs.getString("cedula_asociado"));
                    lista.add(b);
                }
            }
        }
        return lista;
    }

    public void insertarBeneficiario(Beneficiario b) throws SQLException {
        String sql = "INSERT INTO BENEFICIARIO(documento_beneficiario,nombre_completo,parentesco," +
                "porcentaje_participacion,telefono,cedula_asociado) VALUES(?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, b.getDocumentoBeneficiario());
            ps.setString(2, b.getNombreCompleto());
            ps.setString(3, b.getParentesco());
            ps.setBigDecimal(4, b.getPorcentajeParticipacion());
            ps.setString(5, b.getTelefono());
            ps.setString(6, b.getCedulaAsociado());
            ps.executeUpdate();
        }
    }

    private Asociado mapear(ResultSet rs) throws SQLException {
        Asociado a = new Asociado();
        a.setCedula(rs.getString("cedula"));
        a.setNombres(rs.getString("nombres"));
        a.setApellidos(rs.getString("apellidos"));
        a.setFechaNacimiento(rs.getDate("fecha_nacimiento").toLocalDate());
        a.setDireccion(rs.getString("direccion"));
        a.setMunicipio(rs.getString("municipio"));
        a.setTelefono(rs.getString("telefono"));
        a.setCorreoElectronico(rs.getString("correo_electronico"));
        a.setFechaAfiliacion(rs.getDate("fecha_afiliacion").toLocalDate());
        a.setEstado(rs.getString("estado"));
        a.setCuotaSostenimiento(rs.getBigDecimal("cuota_sostenimiento"));
        a.setTipoAsociado(rs.getString("tipo_asociado"));
        return a;
    }
}