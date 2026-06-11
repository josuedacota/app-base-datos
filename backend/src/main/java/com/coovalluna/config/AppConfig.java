package com.coovalluna.config;

public class AppConfig {

    public static final int    PUERTO         = 8080;
    public static final String JWT_SECRET     = "coovalluna_secret_2026";
    public static final long   JWT_EXPIRACION = 28800000L; // 8 horas en ms

    private AppConfig() {}
}