package com.coovalluna.model;

import java.time.LocalDate;

public class Codeudoria {
    private String numeroRadicado;
    private String cedulaCodeudor;
    private LocalDate fechaFirmaPagare;

    public Codeudoria() {
    }

    public String getNumeroRadicado() {
        return numeroRadicado;
    }

    public void setNumeroRadicado(String numeroRadicado) {
        this.numeroRadicado = numeroRadicado;
    }

    public String getCedulaCodeudor() {
        return cedulaCodeudor;
    }

    public void setCedulaCodeudor(String cedulaCodeudor) {
        this.cedulaCodeudor = cedulaCodeudor;
    }

    public LocalDate getFechaFirmaPagare() {
        return fechaFirmaPagare;
    }

    public void setFechaFirmaPagare(LocalDate fecha) {
        this.fechaFirmaPagare = fecha;
    }
}