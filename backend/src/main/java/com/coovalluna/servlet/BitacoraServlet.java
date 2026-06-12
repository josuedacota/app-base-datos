package com.coovalluna.servlet;

import com.coovalluna.dao.BitacoraDAO;
import com.coovalluna.model.Bitacora;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * BitacoraServlet — GET /api/bitacora
 *
 * Filtros opcionales (query params):
 *   fechaDesde  — timestamp de inicio, ej. 2024-01-01 00:00:00
 *   fechaHasta  — timestamp de fin,    ej. 2024-12-31 23:59:59
 *   accion      — tipo de operación (INSERT, UPDATE, DELETE, SELECT…); búsqueda parcial (ILIKE)
 *   tabla       — nombre de la tabla afectada; búsqueda parcial (ILIKE)
 *   idUsuario   — ID exacto del usuario que generó el evento
 */
@WebServlet("/api/bitacora/*")
public class BitacoraServlet extends HttpServlet {

    private final BitacoraDAO bitacoraDAO = new BitacoraDAO();

    // ------------------------------------------------------------------ OPTIONS
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        CorsUtil.setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    // ------------------------------------------------------------------ GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        CorsUtil.setCorsHeaders(resp);

        // Leer filtros desde query parameters
        String fechaDesde = trim(req.getParameter("fechaDesde")); // ej. "2024-01-01 00:00:00"
        String fechaHasta = trim(req.getParameter("fechaHasta")); // ej. "2024-12-31 23:59:59"
        String accion     = trim(req.getParameter("accion"));     // ej. "INSERT"
        String tabla      = trim(req.getParameter("tabla"));      // ej. "CREDITO"
        String idUsuarioP = trim(req.getParameter("idUsuario"));  // ej. "3"

        Integer idUsuario = null;
        if (idUsuarioP != null) {
            try {
                idUsuario = Integer.parseInt(idUsuarioP);
            } catch (NumberFormatException e) {
                JsonUtil.sendError(resp, HttpServletResponse.SC_BAD_REQUEST,
                        "El parámetro idUsuario debe ser un número entero.");
                return;
            }
        }

        try {
            List<Bitacora> registros =
                    bitacoraDAO.listar(fechaDesde, fechaHasta, accion, tabla, idUsuario);
            JsonUtil.sendJson(resp, registros);
        } catch (SQLException e) {
            JsonUtil.sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al consultar la bitácora: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ helper
    /** Devuelve null si el parámetro está vacío o ausente; de lo contrario lo recorta. */
    private String trim(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
