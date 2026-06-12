package com.coovalluna.model;

import java.math.BigDecimal;

public class Beneficiario {
    private int idBeneficiario;
    private String documentoBeneficiario;
    private String nombreCompleto;
    private String parentesco;
    private BigDecimal porcentajeParticipacion;
    private String telefono;
    private String cedulaAsociado;

    public Beneficiario() {
    }

    public int getIdBeneficiario() {
        return idBeneficiario;
    }

    public void setIdBeneficiario(int idBeneficiario) {
        this.idBeneficiario = idBeneficiario;
    }

    public String getDocumentoBeneficiario() {
        return documentoBeneficiario;
    }

    public void setDocumentoBeneficiario(String documento) {
        this.documentoBeneficiario = documento;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getParentesco() {
        return parentesco;
    }

    public void setParentesco(String parentesco) {
        this.parentesco = parentesco;
    }

    public BigDecimal getPorcentajeParticipacion() {
        return porcentajeParticipacion;
    }

    public void setPorcentajeParticipacion(BigDecimal porcentaje) {
        this.porcentajeParticipacion = porcentaje;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCedulaAsociado() {
        return cedulaAsociado;
    }

    public void setCedulaAsociado(String cedulaAsociado) {
        this.cedulaAsociado = cedulaAsociado;
    }
}