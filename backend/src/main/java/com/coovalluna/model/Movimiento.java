package com.coovalluna.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Movimiento {
    private String numeroTransaccion;
    private String numeroCuenta;
    private String tipoMovimiento;
    private BigDecimal valorTransaccion;
    private LocalDateTime fechaHora;
    private String canal;
    private String numeroCuentaRelacionada;

    public Movimiento() {
    }

    public String getNumeroTransaccion() {
        return numeroTransaccion;
    }

    public void setNumeroTransaccion(String numeroTransaccion) {
        this.numeroTransaccion = numeroTransaccion;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public String getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(String tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public BigDecimal getValorTransaccion() {
        return valorTransaccion;
    }

    public void setValorTransaccion(BigDecimal valorTransaccion) {
        this.valorTransaccion = valorTransaccion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public String getNumeroCuentaRelacionada() {
        return numeroCuentaRelacionada;
    }

    public void setNumeroCuentaRelacionada(String cuentaRelacionada) {
        this.numeroCuentaRelacionada = cuentaRelacionada;
    }
}