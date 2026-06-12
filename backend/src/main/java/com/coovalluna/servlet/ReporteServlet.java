package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.ReporteDAO;
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
 * ReporteServlet — expone los 7 reportes gerenciales y operativos.
 *
 * Todas las rutas son GET y requieren JWT.
 *
 *   GET /api/reportes/asociados
 *         ?estado=activo  &agencia=COD
 *         → R1: asociados por estado y agencia
 *
 *   GET /api/reportes/extracto
 *         ?cuenta=NUM  [&desde=YYYY-MM-DD  &hasta=YYYY-MM-DD  &tipo=X  &canal=Y]
 *         → R2: extracto de cuenta
 *
 *   GET /api/reportes/cartera
 *         [&agencia=COD  &desde=YYYY-MM-DD  &hasta=YYYY-MM-DD]
 *         → R3: cartera por línea y estado
 *
 *   GET /api/reportes/mora
 *         → R4: asociados en mora
 *
 *   GET /api/reportes/pagos
 *         ?radicado=NUM
 *         → R5: historial de pagos de un crédito
 *
 *   GET /api/reportes/productividad
 *         [&agencia=COD  &desde=YYYY-MM-DD  &hasta=YYYY-MM-DD]
 *         → R6: productividad de asesores
 *
 *   GET /api/reportes/codeudores
 *         → R7: créditos con codeudor
 */
@WebServlet("/api/reportes/*")
public class ReporteServlet extends HttpServlet {

    private final ReporteDAO reporteDAO = new ReporteDAO();

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

        // Extraer el nombre del reporte del path: /api/reportes/{nombre}
        String nombre = nombreReporte(req);
        if (nombre == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar el reporte en la URL. Ej: /api/reportes/mora");
            return;
        }

        try {
            List<Map<String, Object>> resultado = switch (nombre) {

                case "asociados" -> {
                    // ?estado=X  &agencia=Y
                    String estado  = param(req, "estado");
                    String agencia = param(req, "agencia");
                    yield reporteDAO.r1AsociadosPorEstadoYAgencia(estado, agencia);
                }

                case "extracto" -> {
                    // ?cuenta=NUM  [&desde  &hasta  &tipo  &canal]
                    String cuenta = param(req, "cuenta");
                    if (cuenta == null) {
                        JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                                "Parámetro 'cuenta' obligatorio para el extracto");
                        yield null;
                    }
                    yield reporteDAO.r2ExtractoCuenta(
                            cuenta,
                            param(req, "desde"),
                            param(req, "hasta"),
                            param(req, "tipo"),
                            param(req, "canal"));
                }

                case "cartera" -> {
                    // [?agencia  &desde  &hasta]
                    yield reporteDAO.r3CarteraPorLineaYEstado(
                            param(req, "agencia"),
                            param(req, "desde"),
                            param(req, "hasta"));
                }

                case "mora" ->
                    // sin parámetros
                    reporteDAO.r4AsociadosEnMora();

                case "pagos" -> {
                    // ?radicado=NUM
                    String radicado = param(req, "radicado");
                    if (radicado == null) {
                        JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                                "Parámetro 'radicado' obligatorio para el historial de pagos");
                        yield null;
                    }
                    yield reporteDAO.r5HistorialPagos(radicado);
                }

                case "productividad" -> {
                    // [?agencia  &desde  &hasta]
                    yield reporteDAO.r6ProductividadAsesores(
                            param(req, "agencia"),
                            param(req, "desde"),
                            param(req, "hasta"));
                }

                case "codeudores" ->
                    // sin parámetros
                    reporteDAO.r7CreditosConCodeudor();

                default -> {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Reporte no reconocido: '" + nombre + "'. " +
                            "Disponibles: asociados, extracto, cartera, mora, pagos, productividad, codeudores");
                    yield null;
                }
            };

            // null significa que ya se escribió un error arriba
            if (resultado != null) {
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, resultado);
            }

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al generar el reporte: " + e.getMessage());
        }
    }

    // ============================================================ Helpers

    /**
     * Extrae el nombre del reporte del path info.
     *   /mora            → "mora"
     *   /extracto        → "extracto"
     *   / o null         → null
     */
    private String nombreReporte(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return null;
        // Quitar barra inicial y posibles segmentos extra
        String[] partes = info.substring(1).split("/");
        return partes[0].isBlank() ? null : partes[0].toLowerCase();
    }

    /**
     * Devuelve el parámetro de query si no está en blanco, null en caso contrario.
     * Simplifica las llamadas al DAO que usan null como "sin filtro".
     */
    private String param(HttpServletRequest req, String nombre) {
        String v = req.getParameter(nombre);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private boolean tokenValido(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String header = req.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token de autorización requerido");
            return false;
        }
        try {
            JWT.require(Algorithm.HMAC256(AppConfig.JWT_SECRET)).build().verify(header.substring(7));
            return true;
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
            return false;
        }
    }
}
