package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.CreditoDAO;
import com.coovalluna.model.Codeudoria;
import com.coovalluna.model.Credito;
import com.coovalluna.model.PagoCredito;
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
 * CreditoServlet — gestión de créditos, pagos y codeudores.
 *
 * Rutas (todas requieren JWT excepto OPTIONS):
 *   GET    /api/creditos/?asociado={cedula}       → listar créditos de un asociado
 *   GET    /api/creditos/{radicado}               → obtener un crédito
 *   POST   /api/creditos/                         → solicitar/crear crédito
 *   PUT    /api/creditos/{radicado}/estado        → cambiar estado del crédito
 *
 *   GET    /api/creditos/{radicado}/pagos         → listar pagos
 *   POST   /api/creditos/{radicado}/pagos         → registrar pago de cuota
 *
 *   POST   /api/creditos/{radicado}/codeudores    → agregar codeudor
 */
@WebServlet("/api/creditos/*")
public class CreditoServlet extends HttpServlet {

    private final CreditoDAO creditoDAO = new CreditoDAO();

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

        String[] seg    = parsePath(req);
        String radicado = seg[0];
        String subruta  = seg[1];

        try {
            if (radicado == null) {
                // GET /api/creditos/?asociado=CEDULA
                String cedula = req.getParameter("asociado");
                if (cedula == null || cedula.isBlank()) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                            "Parámetro 'asociado' requerido para listar créditos");
                    return;
                }
                List<Credito> lista = creditoDAO.listarPorAsociado(cedula);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else if ("pagos".equals(subruta)) {
                // GET /api/creditos/{radicado}/pagos
                if (!existeCredito(radicado, res)) return;
                List<PagoCredito> pagos = creditoDAO.listarPagos(radicado);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, pagos);

            } else {
                // GET /api/creditos/{radicado}
                Credito c = creditoDAO.buscarPorRadicado(radicado);
                if (c == null) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Crédito no encontrado: " + radicado);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.sendJson(res, c);
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

        String[] seg    = parsePath(req);
        String radicado = seg[0];
        String subruta  = seg[1];

        try {
            if (radicado == null) {
                crearCredito(req, res);
            } else if ("pagos".equals(subruta)) {
                registrarPago(radicado, req, res);
            } else if ("codeudores".equals(subruta)) {
                agregarCodeudor(radicado, req, res);
            } else {
                JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                        "Ruta no encontrada: " + req.getPathInfo());
            }
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error de base de datos: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ PUT
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        if (!tokenValido(req, res)) return;

        String[] seg    = parsePath(req);
        String radicado = seg[0];
        String subruta  = seg[1];

        if (radicado == null || !"estado".equals(subruta)) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Ruta esperada: PUT /api/creditos/{radicado}/estado");
            return;
        }

        try {
            if (!existeCredito(radicado, res)) return;

            EstadoRequest body = JsonUtil.getGson().fromJson(req.getReader(), EstadoRequest.class);
            if (body == null || body.estado == null || body.estado.isBlank()) {
                JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                        "El campo estado es obligatorio");
                return;
            }

            List<String> estadosValidos = List.of(
                    "solicitado", "aprobado", "desembolsado", "al_dia", "en_mora", "cancelado", "rechazado");
            String estadoNorm = body.estado.toLowerCase().trim();
            if (!estadosValidos.contains(estadoNorm)) {
                JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                        "Estado inválido. Valores permitidos: " + estadosValidos);
                return;
            }

            creditoDAO.actualizarEstado(radicado, estadoNorm);
            res.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.sendJson(res, Map.of("mensaje", "Estado actualizado a: " + estadoNorm));

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar estado: " + e.getMessage());
        }
    }

    // ============================================================ Handlers internos

    private void crearCredito(HttpServletRequest req, HttpServletResponse res)
            throws IOException, SQLException {

        Credito nuevo;
        try {
            nuevo = JsonUtil.getGson().fromJson(req.getReader(), Credito.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        String err = validarCredito(nuevo);
        if (err != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, err);
            return;
        }

        if (creditoDAO.buscarPorRadicado(nuevo.getNumeroRadicado()) != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                    "Ya existe un crédito con el radicado: " + nuevo.getNumeroRadicado());
            return;
        }

        // Estado inicial si no viene en el body
        if (nuevo.getEstadoCredito() == null || nuevo.getEstadoCredito().isBlank()) {
            nuevo.setEstadoCredito("solicitado");
        }

        creditoDAO.insertar(nuevo);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of(
                "mensaje",        "Crédito creado correctamente",
                "numeroRadicado", nuevo.getNumeroRadicado()));
    }

    private void registrarPago(String radicado, HttpServletRequest req, HttpServletResponse res)
            throws IOException, SQLException {

        if (!existeCredito(radicado, res)) return;

        PagoCredito pago;
        try {
            pago = JsonUtil.getGson().fromJson(req.getReader(), PagoCredito.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        if (pago == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El cuerpo de la solicitud no puede estar vacío");
            return;
        }
        if (pago.getNumeroCuota() <= 0) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "numeroCuota debe ser mayor a 0");
            return;
        }
        if (pago.getValorPagado() == null || pago.getValorPagado().signum() <= 0) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "valorPagado debe ser mayor a 0");
            return;
        }

        List<String> estadosPago = List.of("pagado", "pagado_con_mora", "pendiente");
        if (pago.getEstadoPago() == null || !estadosPago.contains(pago.getEstadoPago().toLowerCase())) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "estadoPago inválido. Valores permitidos: " + estadosPago);
            return;
        }

        pago.setNumeroRadicado(radicado);
        creditoDAO.insertarPago(pago);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of("mensaje", "Pago registrado correctamente",
                                      "numeroCuota", pago.getNumeroCuota()));
    }

    private void agregarCodeudor(String radicado, HttpServletRequest req, HttpServletResponse res)
            throws IOException, SQLException {

        if (!existeCredito(radicado, res)) return;

        Codeudoria cd;
        try {
            cd = JsonUtil.getGson().fromJson(req.getReader(), Codeudoria.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        if (cd == null || cd.getCedulaCodeudor() == null || cd.getCedulaCodeudor().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo cedulaCodeudor es obligatorio");
            return;
        }
        if (cd.getFechaFirmaPagare() == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo fechaFirmaPagare es obligatorio (formato YYYY-MM-DD)");
            return;
        }

        cd.setNumeroRadicado(radicado);
        creditoDAO.insertarCodeudoria(cd);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of("mensaje", "Codeudor agregado correctamente"));
    }

    // ============================================================ Helpers

    private String validarCredito(Credito c) {
        if (c == null) return "El cuerpo de la solicitud no puede estar vacío";
        if (c.getNumeroRadicado() == null || c.getNumeroRadicado().isBlank())
            return "El campo numeroRadicado es obligatorio";
        if (c.getValorSolicitado() == null || c.getValorSolicitado().signum() <= 0)
            return "valorSolicitado debe ser mayor a 0";
        if (c.getPlazoMeses() <= 0)
            return "plazoMeses debe ser mayor a 0";
        if (c.getTasaInteresMensual() == null || c.getTasaInteresMensual().signum() < 0)
            return "tasaInteresMensual es obligatoria y debe ser >= 0";
        if (c.getLineaCredito() == null || c.getLineaCredito().isBlank())
            return "El campo lineaCredito es obligatorio";
        if (c.getCedulaAsociado() == null || c.getCedulaAsociado().isBlank())
            return "El campo cedulaAsociado es obligatorio";
        if (c.getCodigoAgencia() == null || c.getCodigoAgencia().isBlank())
            return "El campo codigoAgencia es obligatorio";
        if (c.getCedulaAsesor() == null || c.getCedulaAsesor().isBlank())
            return "El campo cedulaAsesor es obligatorio";
        return null;
    }

    private boolean existeCredito(String radicado, HttpServletResponse res)
            throws IOException, SQLException {
        if (creditoDAO.buscarPorRadicado(radicado) == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                    "Crédito no encontrado: " + radicado);
            return false;
        }
        return true;
    }

    /**
     * Divide el pathInfo en [radicado, subruta].
     *   /                         → [null, null]
     *   /RAD-001                  → ["RAD-001", null]
     *   /RAD-001/pagos            → ["RAD-001", "pagos"]
     *   /RAD-001/estado           → ["RAD-001", "estado"]
     *   /RAD-001/codeudores       → ["RAD-001", "codeudores"]
     */
    private String[] parsePath(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return new String[]{null, null};
        String[] partes = info.substring(1).split("/", 2);
        String id      = partes[0].isBlank() ? null : partes[0];
        String subruta = (partes.length > 1 && !partes[1].isBlank()) ? partes[1] : null;
        return new String[]{id, subruta};
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

    private static class EstadoRequest {
        String estado;
    }
}
