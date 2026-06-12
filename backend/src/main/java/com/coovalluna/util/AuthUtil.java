package com.coovalluna.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coovalluna.config.AppConfig;
import com.coovalluna.model.UsuarioSistema;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

public class AuthUtil {

    private static final Algorithm algorithm =
            Algorithm.HMAC256(AppConfig.JWT_SECRET);

    private AuthUtil() {
    }


    public static String generarToken(UsuarioSistema usuario) {

        return JWT.create()
                .withSubject(usuario.getUsername())
                .withClaim("rol", usuario.getRol())
                .withClaim("idUsuario", usuario.getIdUsuario())
                .withExpiresAt(
                        new Date(
                                System.currentTimeMillis()
                                        + AppConfig.JWT_EXPIRACION
                        )
                )
                .sign(algorithm);
    }


    public static DecodedJWT validarJWT(String token) {

        return JWT.require(algorithm)
                .build()
                .verify(token);
    }


    public static String obtenerRol(String token) {

        return validarJWT(token)
                .getClaim("rol")
                .asString();
    }


    public static boolean esAdmin(String token) {

        return "ADMIN"
                .equalsIgnoreCase(
                        obtenerRol(token)
                );
    }


    public static boolean esAdminOAsesor(String token) {

        String rol = obtenerRol(token);

        return "ADMIN".equalsIgnoreCase(rol)
                || "ASESOR".equalsIgnoreCase(rol);
    }


    public static String hashPassword(String password) {

        try {

            MessageDigest md =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    md.digest(
                            password.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder sb =
                    new StringBuilder();

            for (byte b : hash) {
                sb.append(
                        String.format("%02x", b)
                );
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new RuntimeException(
                    "Error generando hash",
                    e
            );
        }
    }


    public static String generarClaveTemporal() {

        String caracteres =
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "abcdefghijklmnopqrstuvwxyz"
                + "0123456789";

        Random random =
                new Random();

        StringBuilder clave =
                new StringBuilder();

        for (int i = 0; i < 10; i++) {

            clave.append(
                    caracteres.charAt(
                            random.nextInt(
                                    caracteres.length()
                            )
                    )
            );
        }

        return clave.toString();
    }
}