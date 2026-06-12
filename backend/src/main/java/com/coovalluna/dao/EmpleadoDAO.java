package com.coovalluna.dao;

import com.coovalluna.config.DatabaseConfig;
import com.coovalluna.model.Empleado;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    public List<Empleado> listarTodos(String codigoAgencia) throws SQLException {
        List<Empleado> lista = new ArrayList<>();
        String sql = codigoAgencia != null
                ? "SELECT * FROM EMPLEADO WHERE codigo_agencia=? ORDER BY apellidos,nombres"
                : "SELECT * FROM EMPLEADO ORDER BY apellidos,nombres";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            if (codigoAgencia != null) ps.setString(1, codigoAgencia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Empleado buscarPorCedula(String cedula) throws SQLException {
        String sql = "SELECT * FROM EMPLEADO WHERE cedula_empleado=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cedula);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        }
        return null;
    }

    public void insertar(Empleado e) throws SQLException {
        String sql = "INSERT INTO EMPLEADO(cedula_empleado,nombres,apellidos,cargo,tipo_empleado," +
                "fecha_ingreso,salario_base,correo_corporativo,estado_laboral,codigo_agencia,cedula_supervisor) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getCedulaEmpleado());
            ps.setString(2, e.getNombres());
            ps.setString(3, e.getApellidos());
            ps.setString(4, e.getCargo());
            ps.setString(5, e.getTipoEmpleado());
            ps.setDate(6, Date.valueOf(e.getFechaIngreso()));
            ps.setBigDecimal(7, e.getSalarioBase());
            ps.setString(8, e.getCorreoCorporativo());
            ps.setString(9, e.getEstadoLaboral());
            ps.setString(10, e.getCodigoAgencia());
            ps.setString(11, e.getCedulaSupervisor());
            ps.executeUpdate();
        }
    }

    public void actualizar(Empleado e) throws SQLException {
        String sql = "UPDATE EMPLEADO SET nombres=?,apellidos=?,cargo=?,tipo_empleado=?," +
                "salario_base=?,correo_corporativo=?,estado_laboral=?,codigo_agencia=?,cedula_supervisor=? " +
                "WHERE cedula_empleado=?";
        try (Connection con = DatabaseConfig.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, e.getNombres());
            ps.setString(2, e.getApellidos());
            ps.setString(3, e.getCargo());
            ps.setString(4, e.getTipoEmpleado());
            ps.setBigDecimal(5, e.getSalarioBase());
            ps.setString(6, e.getCorreoCorporativo());
            ps.setString(7, e.getEstadoLaboral());
            ps.setString(8, e.getCodigoAgencia());
            ps.setString(9, e.getCedulaSupervisor());
            ps.setString(10, e.getCedulaEmpleado());
            ps.executeUpdate();
        }
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
        Empleado e = new Empleado();
        e.setCedulaEmpleado(rs.getString("cedula_empleado"));
        e.setNombres(rs.getString("nombres"));
        e.setApellidos(rs.getString("apellidos"));
        e.setCargo(rs.getString("cargo"));
        e.setTipoEmpleado(rs.getString("tipo_empleado"));
        e.setFechaIngreso(rs.getDate("fecha_ingreso").toLocalDate());
        e.setSalarioBase(rs.getBigDecimal("salario_base"));
        e.setCorreoCorporativo(rs.getString("correo_corporativo"));
        e.setEstadoLaboral(rs.getString("estado_laboral"));
        e.setCodigoAgencia(rs.getString("codigo_agencia"));
        e.setCedulaSupervisor(rs.getString("cedula_supervisor"));
        return e;
    }
}