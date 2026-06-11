package com.coovalluna.model;

import java.time.LocalDate;

public class Agencia {

    private String codigoAgencia;
    private String nombreAgencia;
    private String direccion;
    private String municipio;
    private String telefono;
    private LocalDate fechaApertura;
    private String estado;

    public Agencia() {
    }

    public Agencia(String codigoAgencia, String nombreAgencia, String direccion,
                   String municipio, String telefono, LocalDate fechaApertura, String estado) {
        this.codigoAgencia = codigoAgencia;
        this.nombreAgencia = nombreAgencia;
        this.direccion = direccion;
        this.municipio = municipio;
        this.telefono = telefono;
        this.fechaApertura = fechaApertura;
        this.estado = estado;
    }

    public String getCodigoAgencia() {
        return codigoAgencia;
    }

    public void setCodigoAgencia(String codigoAgencia) {
        this.codigoAgencia = codigoAgencia;
    }

    public String getNombreAgencia() {
        return nombreAgencia;
    }

    public void setNombreAgencia(String nombreAgencia) {
        this.nombreAgencia = nombreAgencia;
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

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}