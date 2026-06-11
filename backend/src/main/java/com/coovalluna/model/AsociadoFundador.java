package com.coovalluna.model;

public class AsociadoFundador {
    private String cedula;
    private String numeroActaFundacional;
    private int anioReconocimiento;
    private String beneficiosEspeciales;

    public AsociadoFundador() {
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getNumeroActaFundacional() {
        return numeroActaFundacional;
    }

    public void setNumeroActaFundacional(String numero) {
        this.numeroActaFundacional = numero;
    }

    public int getAnioReconocimiento() {
        return anioReconocimiento;
    }

    public void setAnioReconocimiento(int anio) {
        this.anioReconocimiento = anio;
    }

    public String getBeneficiosEspeciales() {
        return beneficiosEspeciales;
    }

    public void setBeneficiosEspeciales(String beneficios) {
        this.beneficiosEspeciales = beneficios;
    }
}