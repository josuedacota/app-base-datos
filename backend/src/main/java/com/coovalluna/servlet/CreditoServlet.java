package com.coovalluna.servlet;

import com.coovalluna.dao.CreditoDAO;
import com.coovalluna.model.Credito;
import com.coovalluna.model.Codeudoria;
import com.coovalluna.model.PagoCredito;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CreditoServlet extends HttpServlet {
    private final CreditoDAO dao = new CreditoDAO();
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
                return;
            }
            String[] parts = path.split("/");
            if (parts.length >= 3 && "asociado".equals(parts[1])) {
                JsonUtil.sendJson(resp, dao.listarPorAsociado(parts[2]));
                return;
            }
            String radicado = parts[1];
            if (parts.length >= 3 && "pagos".equals(parts[2])) {
                JsonUtil.sendJson(resp, dao.listarPagos(radicado));
                return;
            }
            Credito c = dao.buscarPorRadicado(radicado);
            if (c == null) JsonUtil.sendError(resp, 404, "Crédito no encontrado");
            else JsonUtil.sendJson(resp, c);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdminOAsesor(token)) { JsonUtil.sendError(resp, 403, "Sin permiso"); return; }
        try {
            String path = req.getPathInfo();
            if (path != null && path.contains("/pagos")) {
                PagoCredito p = gson.fromJson(req.getReader(), PagoCredito.class);
                dao.insertarPago(p);
                resp.setStatus(201); JsonUtil.sendJson(resp, p);
            } else if (path != null && path.contains("/codeudor")) {
                String[] parts = path.split("/");
                String radicado = parts[1];
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                Codeudoria cd = new Codeudoria();
                cd.setNumeroRadicado(radicado);
                cd.setCedulaCodeudor((String) body.get("cedulaCodeudor"));
                String fecha = (String) body.get("fechaFirmaPagare");
                cd.setFechaFirmaPagare(fecha != null ? java.time.LocalDate.parse(fecha) : java.time.LocalDate.now());
                dao.insertarCodeudoria(cd);
                resp.setStatus(201); JsonUtil.sendJson(resp, cd);
            } else {
                Credito c = gson.fromJson(req.getReader(), Credito.class);
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
            if (path != null && path.contains("/estado")) {
                String[] parts = path.split("/");
                String radicado = parts[1];
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                dao.actualizarEstado(radicado, (String) body.get("estadoCredito"));
                Map<String, String> r = new HashMap<>(); r.put("mensaje", "Estado actualizado");
                JsonUtil.sendJson(resp, r);
            } else if (path != null && path.length() > 1) {
                String radicado = path.substring(1);
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                dao.actualizarEstado(radicado, (String) body.get("estadoCredito"));
                Map<String, String> r = new HashMap<>(); r.put("mensaje", "Estado actualizado");
                JsonUtil.sendJson(resp, r);
            } else {
                JsonUtil.sendError(resp, 400, "Radicado requerido");
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}