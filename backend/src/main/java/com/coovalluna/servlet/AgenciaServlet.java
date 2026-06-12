package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.AgenciaDAO;
import com.coovalluna.model.Agencia;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * AgenciaServlet — CRUD de agencias.
 *
 * Rutas (todas requieren JWT excepto OPTIONS):
 *   GET    /api/agencias/          → listar todas las agencias
 *   GET    /api/agencias/{codigo}  → obtener una agencia por código
 *   POST   /api/agencias/          → crear nueva agencia
 *   PUT    /api/agencias/{codigo}  → actualizar agencia existente
 */
@WebServlet("/api/agencias/*")
public class AgenciaServlet extends HttpServlet {

    private final AgenciaDAO agenciaDAO = new AgenciaDAO();

    // ------------------------------------------------------------------ CORS
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        CorsUtil.setCorsHeaders(res);
        res.setStatus(HttpServletResponse.SC_OK);
    }

    // ------------------------------------------------------------------ GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        if (!tokenValido(req, res)) return;

        String codigo = extraerId(req);

        try {
            if (codigo == null) {
                // GET /api/agencias/  — listar todas
                List<Agencia> lista = agenciaDAO.listarTodas();
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else {
                // GET /api/agencias/{codigo}
                Agencia agencia = agenciaDAO.buscarPorCodigo(codigo);
                if (agencia == null) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Agencia no encontrada: " + codigo);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.sendJson(res, agencia);
                }
            }
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error de base de datos: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        if (!tokenValido(req, res)) return;

        Agencia nueva;
        try {
            nueva = JsonUtil.getGson().fromJson(req.getReader(), Agencia.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        // Validaciones básicas
        if (nueva == null || nueva.getCodigoAgencia() == null || nueva.getCodigoAgencia().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo codigoAgencia es obligatorio");
            return;
        }
        if (nueva.getNombreAgencia() == null || nueva.getNombreAgencia().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo nombreAgencia es obligatorio");
            return;
        }
        if (nueva.getFechaApertura() == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo fechaApertura es obligatorio (formato YYYY-MM-DD)");
            return;
        }

        try {
            // Verificar que no exista ya
            if (agenciaDAO.buscarPorCodigo(nueva.getCodigoAgencia()) != null) {
                JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                        "Ya existe una agencia con el código: " + nueva.getCodigoAgencia());
                return;
            }

            agenciaDAO.insertar(nueva);
            res.setStatus(HttpServletResponse.SC_CREATED);
            JsonUtil.sendJson(res, Map.of("mensaje", "Agencia creada correctamente",
                                          "codigo", nueva.getCodigoAgencia()));

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al crear la agencia: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ PUT
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        if (!tokenValido(req, res)) return;

        String codigo = extraerId(req);
        if (codigo == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar el código de la agencia en la URL: /api/agencias/{codigo}");
            return;
        }

        Agencia cambios;
        try {
            cambios = JsonUtil.getGson().fromJson(req.getReader(), Agencia.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        if (cambios == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El cuerpo de la solicitud no puede estar vacío");
            return;
        }

        // Forzar el código desde la URL para evitar inconsistencias
        cambios.setCodigoAgencia(codigo);

        try {
            Agencia existente = agenciaDAO.buscarPorCodigo(codigo);
            if (existente == null) {
                JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                        "Agencia no encontrada: " + codigo);
                return;
            }

            agenciaDAO.actualizar(cambios);
            res.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.sendJson(res, Map.of("mensaje", "Agencia actualizada correctamente"));

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar la agencia: " + e.getMessage());
        }
    }

    // ============================================================ Helpers

    /**
     * Extrae el segmento de path después de /api/agencias/.
     * Devuelve null si la ruta termina en "/" (listado).
     */
    private String extraerId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return null;
        return info.substring(1); // quitar la barra inicial
    }

    /**
     * Valida el token Bearer JWT.
     * Si falla escribe la respuesta de error y devuelve false.
     */
    private boolean tokenValido(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token de autorización requerido");
            return false;
        }

        try {
            JWT.require(Algorithm.HMAC256(AppConfig.JWT_SECRET))
               .build()
               .verify(header.substring(7));
            return true;
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token inválido o expirado");
            return false;
        }
    }
}
