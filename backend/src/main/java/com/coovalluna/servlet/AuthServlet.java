package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.UsuarioDAO;
import com.coovalluna.model.UsuarioSistema;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * AuthServlet — gestiona la autenticación de usuarios del sistema.
 *
 * Rutas:
 *   POST /api/auth/login          → inicia sesión, devuelve JWT
 *   POST /api/auth/cambiar-clave  → cambia la contraseña (requiere JWT)
 */
@WebServlet("/api/auth/*")
public class AuthServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // ------------------------------------------------------------------ CORS
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        CorsUtil.setCorsHeaders(res);
        res.setStatus(HttpServletResponse.SC_OK);
    }

    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        String path = req.getPathInfo() == null ? "/" : req.getPathInfo();

        switch (path) {
            case "/login"         -> handleLogin(req, res);
            case "/cambiar-clave" -> handleCambiarClave(req, res);
            default               -> JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                                         "Ruta no encontrada: " + path);
        }
    }

    // ============================================================ /login
    private void handleLogin(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        // Leer cuerpo JSON  { "username": "...", "password": "..." }
        LoginRequest body;
        try {
            body = JsonUtil.getGson().fromJson(req.getReader(), LoginRequest.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        if (body == null || body.username == null || body.password == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "username y password son obligatorios");
            return;
        }

        UsuarioSistema usuario;
        try {
            usuario = usuarioDAO.buscarPorUsername(body.username.trim());
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error de base de datos: " + e.getMessage());
            return;
        }

        if (usuario == null || !hashSha256(body.password).equals(usuario.getPasswordHash())) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Credenciales incorrectas");
            return;
        }

        // Generar JWT
        String token = JWT.create()
                .withSubject(String.valueOf(usuario.getIdUsuario()))
                .withClaim("username", usuario.getUsername())
                .withClaim("rol", usuario.getRol())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + AppConfig.JWT_EXPIRACION))
                .sign(Algorithm.HMAC256(AppConfig.JWT_SECRET));

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("token", token);
        respuesta.put("idUsuario", usuario.getIdUsuario());
        respuesta.put("username", usuario.getUsername());
        respuesta.put("rol", usuario.getRol());
        respuesta.put("debeCambiarClave", usuario.isDebeCambiarClave());

        res.setStatus(HttpServletResponse.SC_OK);
        JsonUtil.sendJson(res, respuesta);
    }

    // ============================================================ /cambiar-clave
    private void handleCambiarClave(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        // Validar JWT
        UsuarioSistema solicitante = resolverToken(req, res);
        if (solicitante == null) return;

        // Leer body  { "claveActual": "...", "claveNueva": "..." }
        CambiarClaveRequest body;
        try {
            body = JsonUtil.getGson().fromJson(req.getReader(), CambiarClaveRequest.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        if (body == null || body.claveActual == null || body.claveNueva == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "claveActual y claveNueva son obligatorios");
            return;
        }

        if (!hashSha256(body.claveActual).equals(solicitante.getPasswordHash())) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "La clave actual es incorrecta");
            return;
        }

        if (body.claveNueva.length() < 6) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "La clave nueva debe tener al menos 6 caracteres");
            return;
        }

        try {
            usuarioDAO.actualizarClave(solicitante.getIdUsuario(), hashSha256(body.claveNueva));
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar la clave: " + e.getMessage());
            return;
        }

        Map<String, String> ok = Map.of("mensaje", "Clave actualizada correctamente");
        res.setStatus(HttpServletResponse.SC_OK);
        JsonUtil.sendJson(res, ok);
    }

    // ============================================================ Helpers

    /** Extrae y valida el Bearer JWT; devuelve null (y escribe error) si falla. */
    private UsuarioSistema resolverToken(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token de autorización requerido");
            return null;
        }

        String token = header.substring(7);
        try {
            var decoded = JWT.require(Algorithm.HMAC256(AppConfig.JWT_SECRET))
                    .build()
                    .verify(token);

            int idUsuario = Integer.parseInt(decoded.getSubject());
            UsuarioSistema u = new UsuarioSistema();
            u.setIdUsuario(idUsuario);
            u.setUsername(decoded.getClaim("username").asString());
            u.setRol(decoded.getClaim("rol").asString());

            // Recargar desde BD para obtener el hash actual de la clave
            UsuarioSistema fromDb = usuarioDAO.buscarPorUsername(u.getUsername());
            if (fromDb == null) {
                JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Usuario no encontrado");
                return null;
            }
            return fromDb;

        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token inválido o expirado");
            return null;
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error de base de datos: " + e.getMessage());
            return null;
        }
    }

    /** SHA-256 en hexadecimal para almacenar contraseñas. */
    private String hashSha256(String texto) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    // ============================================================ DTOs internos
    private static class LoginRequest {
        String username;
        String password;
    }

    private static class CambiarClaveRequest {
        String claveActual;
        String claveNueva;
    }
}
