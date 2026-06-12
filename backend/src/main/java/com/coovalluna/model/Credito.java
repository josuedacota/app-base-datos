package com.coovalluna.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Credito {
    private String numeroRadicado;
    private BigDecimal valorSolicitado;
    private BigDecimal valorAprobado;
    private int plazoMeses;
    private BigDecimal tasaInteresMensual;
    private LocalDate fechaAprobacion;
    private LocalDate fechaPrimerVencimiento;
    private String estadoCredito;
    private String lineaCredito;
    private String cedulaAsociado;
    private String codigoAgencia;
    private String cedulaAsesor;

    public Credito() {
    }

    public String getNumeroRadicado() {
        return numeroRadicado;
    }

    public void setNumeroRadicado(String numeroRadicado) {
        this.numeroRadicado = numeroRadicado;
    }

    public BigDecimal getValorSolicitado() {
        return valorSolicitado;
    }

    public void setValorSolicitado(BigDecimal valorSolicitado) {
        this.valorSolicitado = valorSolicitado;
    }

    public BigDecimal getValorAprobado() {
        return valorAprobado;
    }

    public void setValorAprobado(BigDecimal valorAprobado) {
        this.valorAprobado = valorAprobado;
    }

    public int getPlazoMeses() {
        return plazoMeses;
    }

    public void setPlazoMeses(int plazoMeses) {
        this.plazoMeses = plazoMeses;
    }

    public BigDecimal getTasaInteresMensual() {
        return tasaInteresMensual;
    }

    public void setTasaInteresMensual(BigDecimal tasa) {
        this.tasaInteresMensual = tasa;
    }

    public LocalDate getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDate fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }

    public LocalDate getFechaPrimerVencimiento() {
        return fechaPrimerVencimiento;
    }

    public void setFechaPrimerVencimiento(LocalDate fecha) {
        this.fechaPrimerVencimiento = fecha;
    }

    public String getEstadoCredito() {
        return estadoCredito;
    }

    public void setEstadoCredito(String estadoCredito) {
        this.estadoCredito = estadoCredito;
    }

    public String getLineaCredito() {
        return lineaCredito;
    }

    public void setLineaCredito(String lineaCredito) {
        this.lineaCredito = lineaCredito;
    }

    public String getCedulaAsociado() {
        return cedulaAsociado;
    }

    public void setCedulaAsociado(String cedulaAsociado) {
        this.cedulaAsociado = cedulaAsociado;
    }

    public String getCodigoAgencia() {
        return codigoAgencia;
    }

    public void setCodigoAgencia(String codigoAgencia) {
        this.codigoAgencia = codigoAgencia;
    }

    public String getCedulaAsesor() {
        return cedulaAsesor;
    }

    public void setCedulaAsesor(String cedulaAsesor) {
        this.cedulaAsesor = cedulaAsesor;
    }
}