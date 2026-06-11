package com.coovalluna.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    // Cambiar dependiendo el localhost, usuario y contraseña de la consola que esta trabajando
    private static final String URL      = "jdbc:postgresql://localhost:5432/coovalluna";
    private static final String USUARIO  = "postgres";
    private static final String PASSWORD = "password";
    private DatabaseConfig() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USUARIO, PASSWORD);
    }
}