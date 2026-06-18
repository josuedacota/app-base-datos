package com.coovalluna.servlet;

import com.coovalluna.dao.CuentaDAO;
import com.coovalluna.model.CuentaAhorro;
import com.coovalluna.model.Movimiento;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CuentaServlet extends HttpServlet {
    private final CuentaDAO dao = new CuentaDAO();
    private final Gson gson = JsonUtil.getGson();

    @Override protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp); resp.setStatus(200);
    }

    private String getToken(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return (h != null && h.startsWith("Bearer ")) ? h.substring(7) : null;
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null) { JsonUtil.sendError(resp, 401, "No autorizado"); return; }
        try { AuthUtil.validarJWT(token); } catch (Exception e) { JsonUtil.sendError(resp, 401, "Token inválido"); return; }
        try {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                String cedula = req.getParameter("cedula");
                if (cedula != null) JsonUtil.sendJson(resp, dao.listarPorAsociado(cedula));
                else JsonUtil.sendError(resp, 400, "Parámetro cedula requerido");
            } else {
                String[] parts = path.split("/");
                String numeroCuenta = parts[1];
                if (parts.length >= 3) {
                    if ("movimientos".equals(parts[2])) {
                        String tipo = req.getParameter("tipo");
                        String canal = req.getParameter("canal");
                        JsonUtil.sendJson(resp, dao.listarMovimientos(numeroCuenta, tipo, canal));
                    } else if ("saldo".equals(parts[2])) {
                        BigDecimal saldo = dao.calcularSaldo(numeroCuenta);
                        Map<String, Object> r = new HashMap<>();
                        r.put("numeroCuenta", numeroCuenta);
                        r.put("saldo", saldo);
                        JsonUtil.sendJson(resp, r);
                    } else {
                        JsonUtil.sendError(resp, 404, "Ruta no encontrada");
                    }
                } else {
                    CuentaAhorro c = dao.buscarPorNumero(numeroCuenta);
                    if (c == null) JsonUtil.sendError(resp, 404, "Cuenta no encontrada");
                    else JsonUtil.sendJson(resp, c);
                }
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdminOAsesor(token)) { JsonUtil.sendError(resp, 403, "Sin permiso"); return; }
        try {
            String path = req.getPathInfo();
            if (path != null && path.contains("/movimientos")) {
                Movimiento m = gson.fromJson(req.getReader(), Movimiento.class);
                dao.insertarMovimiento(m);
                resp.setStatus(201); JsonUtil.sendJson(resp, m);
            } else {
                CuentaAhorro c = gson.fromJson(req.getReader(), CuentaAhorro.class);
                dao.insertar(c);
                resp.setStatus(201); JsonUtil.sendJson(resp, c);
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdminOAsesor(token)) { JsonUtil.sendError(resp, 403, "Sin permiso"); return; }
        try {
            String path = req.getPathInfo();
            if (path != null && path.length() > 1) {
                String numero = path.substring(1);
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                dao.actualizarEstado(numero, (String) body.get("estado"));
                Map<String, String> r = new HashMap<>(); r.put("mensaje", "Estado actualizado");
                JsonUtil.sendJson(resp, r);
            } else {
                JsonUtil.sendError(resp, 400, "Número de cuenta requerido");
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}