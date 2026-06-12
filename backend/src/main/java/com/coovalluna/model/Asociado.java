package com.coovalluna.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Asociado {

    private String cedula;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String direccion;
    private String municipio;
    private String telefono;
    private String correoElectronico;
    private LocalDate fechaAfiliacion;
    private String estado;
    private BigDecimal cuotaSostenimiento;
    private String tipoAsociado;

    public Asociado() {
    }

    public Asociado(String cedula, String nombres, String apellidos,
                    LocalDate fechaNacimiento, String direccion, String municipio,
                    String telefono, String correoElectronico, LocalDate fechaAfiliacion,
                    String estado, BigDecimal cuotaSostenimiento, String tipoAsociado) {
        this.cedula = cedula;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.fechaNacimiento = fechaNacimiento;
        this.direccion = direccion;
        this.municipio = municipio;
        this.telefono = telefono;
        this.correoElectronico = correoElectronico;
        this.fechaAfiliacion = fechaAfiliacion;
        this.estado = estado;
        this.cuotaSostenimiento = cuotaSostenimiento;
        this.tipoAsociado = tipoAsociado;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate f) {
        this.fechaNacimiento = f;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreoElectronico() {
        return correoElectronico;
    }

    public void setCorreoElectronico(String correo) {
        this.correoElectronico = correo;
    }

    public LocalDate getFechaAfiliacion() {
        return fechaAfiliacion;
    }

    public void setFechaAfiliacion(LocalDate f) {
        this.fechaAfiliacion = f;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getCuotaSostenimiento() {
        return cuotaSostenimiento;
    }

    public void setCuotaSostenimiento(BigDecimal c) {
        this.cuotaSostenimiento = c;
    }

    public String getTipoAsociado() {
        return tipoAsociado;
    }

    public void setTipoAsociado(String tipo) {
        this.tipoAsociado = tipo;
    }
}