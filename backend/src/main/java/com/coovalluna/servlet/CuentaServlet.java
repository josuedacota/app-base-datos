package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.CuentaDAO;
import com.coovalluna.model.CuentaAhorro;
import com.coovalluna.model.Movimiento;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CuentaServlet — gestión de cuentas de ahorro y movimientos.
 *
 * Rutas (todas requieren JWT excepto OPTIONS):
 *   GET    /api/cuentas/{numero}                → obtener cuenta
 *   GET    /api/cuentas/{numero}/saldo          → saldo calculado
 *   GET    /api/cuentas/{numero}/movimientos    → historial (?tipo=X &canal=Y)
 *   POST   /api/cuentas/                        → crear cuenta
 *   POST   /api/cuentas/{numero}/movimientos    → registrar movimiento
 *   PUT    /api/cuentas/{numero}/estado         → cambiar estado (activa/inactiva/bloqueada)
 *
 * Nota: el listado de cuentas por asociado se expone a través de
 *       GET /api/asociados/{cedula} (datos del asociado) y normalmente el
 *       frontend consulta las cuentas pasando el parámetro ?asociado=CEDULA.
 *       Si se necesita ese filtro, agregarlo como GET /api/cuentas/?asociado=X.
 */
@WebServlet("/api/cuentas/*")
public class CuentaServlet extends HttpServlet {

    private final CuentaDAO cuentaDAO = new CuentaDAO();

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

        String[] seg = parsePath(req);
        String numero  = seg[0];
        String subruta = seg[1];

        try {
            if (numero == null) {
                // GET /api/cuentas/?asociado=CEDULA  — listado por asociado
                String cedula = req.getParameter("asociado");
                if (cedula == null || cedula.isBlank()) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                            "Parámetro 'asociado' requerido para listar cuentas");
                    return;
                }
                List<CuentaAhorro> lista = cuentaDAO.listarPorAsociado(cedula);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else if ("saldo".equals(subruta)) {
                // GET /api/cuentas/{numero}/saldo
                if (!existeCuenta(numero, res)) return;
                BigDecimal saldo = cuentaDAO.calcularSaldo(numero);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, Map.of("numeroCuenta", numero, "saldo", saldo));

            } else if ("movimientos".equals(subruta)) {
                // GET /api/cuentas/{numero}/movimientos  (?tipo=X &canal=Y)
                if (!existeCuenta(numero, res)) return;
                String tipo  = req.getParameter("tipo");
                String canal = req.getParameter("canal");
                List<Movimiento> lista = cuentaDAO.listarMovimientos(numero, tipo, canal);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else {
                // GET /api/cuentas/{numero}
                CuentaAhorro cuenta = cuentaDAO.buscarPorNumero(numero);
                if (cuenta == null) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Cuenta no encontrada: " + numero);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.sendJson(res, cuenta);
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

        String[] seg = parsePath(req);
        String numero  = seg[0];
        String subruta = seg[1];

        try {
            if (numero == null) {
                // POST /api/cuentas/  — crear cuenta
                crearCuenta(req, res);

            } else if ("movimientos".equals(subruta)) {
                // POST /api/cuentas/{numero}/movimientos
                registrarMovimiento(numero, req, res);

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

        String[] seg = parsePath(req);
        String numero  = seg[0];
        String subruta = seg[1];

        if (numero == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar el número de cuenta en la URL");
            return;
        }

        if (!"estado".equals(subruta)) {
            JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                    "Ruta no encontrada: " + req.getPathInfo());
            return;
        }

        // PUT /api/cuentas/{numero}/estado  — body: { "estado": "inactiva" }
        try {
            if (!existeCuenta(numero, res)) return;

            EstadoRequest body = JsonUtil.getGson().fromJson(req.getReader(), EstadoRequest.class);
            if (body == null || body.estado == null || body.estado.isBlank()) {
                JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                        "El campo estado es obligatorio");
                return;
            }

            String estadoNorm = body.estado.toLowerCase().trim();
            if (!List.of("activa", "inactiva", "bloqueada").contains(estadoNorm)) {
                JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                        "Estado inválido. Valores permitidos: activa, inactiva, bloqueada");
                return;
            }

            cuentaDAO.actualizarEstado(numero, estadoNorm);
            res.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.sendJson(res, Map.of("mensaje", "Estado actualizado a: " + estadoNorm));

        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar estado: " + e.getMessage());
        }
    }

    // ============================================================ Handlers internos

    private void crearCuenta(HttpServletRequest req, HttpServletResponse res)
            throws IOException, SQLException {

        CuentaAhorro nueva;
        try {
            nueva = JsonUtil.getGson().fromJson(req.getReader(), CuentaAhorro.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        if (nueva == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El cuerpo de la solicitud no puede estar vacío");
            return;
        }
        if (nueva.getNumeroCuenta() == null || nueva.getNumeroCuenta().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo numeroCuenta es obligatorio");
            return;
        }
        if (nueva.getCedulaAsociado() == null || nueva.getCedulaAsociado().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo cedulaAsociado es obligatorio");
            return;
        }
        if (nueva.getCodigoAgencia() == null || nueva.getCodigoAgencia().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo codigoAgencia es obligatorio");
            return;
        }
        if (nueva.getFechaApertura() == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo fechaApertura es obligatorio (formato YYYY-MM-DD)");
            return;
        }

        if (cuentaDAO.buscarPorNumero(nueva.getNumeroCuenta()) != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                    "Ya existe una cuenta con el número: " + nueva.getNumeroCuenta());
            return;
        }

        // Estado inicial: activa
        if (nueva.getEstado() == null || nueva.getEstado().isBlank()) {
            nueva.setEstado("activa");
        }

        cuentaDAO.insertar(nueva);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of(
                "mensaje",      "Cuenta creada correctamente",
                "numeroCuenta", nueva.getNumeroCuenta()));
    }

    private void registrarMovimiento(String numeroCuenta,
                                     HttpServletRequest req,
                                     HttpServletResponse res)
            throws IOException, SQLException {

        CuentaAhorro cuenta = cuentaDAO.buscarPorNumero(numeroCuenta);
        if (cuenta == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                    "Cuenta no encontrada: " + numeroCuenta);
            return;
        }
        if (!"activa".equalsIgnoreCase(cuenta.getEstado())) {
            JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                    "No se pueden registrar movimientos en una cuenta " + cuenta.getEstado());
            return;
        }

        Movimiento m;
        try {
            m = JsonUtil.getGson().fromJson(req.getReader(), Movimiento.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        // Validaciones
        if (m == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El cuerpo de la solicitud no puede estar vacío");
            return;
        }

        List<String> tiposValidos = List.of(
                "deposito", "retiro", "transferencia_entrante", "transferencia_saliente");
        if (m.getTipoMovimiento() == null
                || !tiposValidos.contains(m.getTipoMovimiento().toLowerCase())) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "tipoMovimiento inválido. Valores permitidos: " + tiposValidos);
            return;
        }
        if (m.getValorTransaccion() == null || m.getValorTransaccion().compareTo(BigDecimal.ZERO) <= 0) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "valorTransaccion debe ser mayor a 0");
            return;
        }
        if (m.getCanal() == null || m.getCanal().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo canal es obligatorio");
            return;
        }

        // Verificar saldo suficiente para retiros y transferencias salientes
        String tipo = m.getTipoMovimiento().toLowerCase();
        if (tipo.equals("retiro") || tipo.equals("transferencia_saliente")) {
            BigDecimal saldo = cuentaDAO.calcularSaldo(numeroCuenta);
            if (saldo.compareTo(m.getValorTransaccion()) < 0) {
                JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                        "Saldo insuficiente. Saldo disponible: " + saldo);
                return;
            }
        }

        // Completar campos auto-generados
        m.setNumeroCuenta(numeroCuenta);
        if (m.getNumeroTransaccion() == null || m.getNumeroTransaccion().isBlank()) {
            m.setNumeroTransaccion("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (m.getFechaHora() == null) {
            m.setFechaHora(LocalDateTime.now());
        }

        cuentaDAO.insertarMovimiento(m);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of(
                "mensaje",            "Movimiento registrado correctamente",
                "numeroTransaccion",  m.getNumeroTransaccion(),
                "fechaHora",          m.getFechaHora().toString()));
    }

    // ============================================================ Helpers

    /**
     * Divide el pathInfo en [numeroCuenta, subruta].
     *   /                        → [null, null]
     *   /ACC-001                 → ["ACC-001", null]
     *   /ACC-001/movimientos     → ["ACC-001", "movimientos"]
     *   /ACC-001/saldo           → ["ACC-001", "saldo"]
     *   /ACC-001/estado          → ["ACC-001", "estado"]
     */
    private String[] parsePath(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return new String[]{null, null};
        String[] partes = info.substring(1).split("/", 2);
        String numero  = partes[0].isBlank() ? null : partes[0];
        String subruta = (partes.length > 1 && !partes[1].isBlank()) ? partes[1] : null;
        return new String[]{numero, subruta};
    }

    /** Comprueba si la cuenta existe; si no escribe 404 y devuelve false. */
    private boolean existeCuenta(String numero, HttpServletResponse res)
            throws IOException, SQLException {
        if (cuentaDAO.buscarPorNumero(numero) == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                    "Cuenta no encontrada: " + numero);
            return false;
        }
        return true;
    }

    /** Valida el token Bearer JWT. */
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

    // DTO interno
    private static class EstadoRequest {
        String estado;
    }
}
