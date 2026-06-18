package com.coovalluna.servlet;

import com.coovalluna.dao.AsociadoDAO;
import com.coovalluna.model.Asociado;
import com.coovalluna.model.AsociadoFundador;
import com.coovalluna.model.Beneficiario;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class AsociadoServlet extends HttpServlet {
    private final AsociadoDAO dao = new AsociadoDAO();
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
                String estado = req.getParameter("estado");
                String tipo = req.getParameter("tipo");
                JsonUtil.sendJson(resp, dao.listarTodos(estado, tipo));
            } else {
                String[] parts = path.split("/");
                if (parts.length >= 3 && "beneficiarios".equals(parts[2])) {
                    JsonUtil.sendJson(resp, dao.listarBeneficiarios(parts[1]));
                } else {
                    Asociado a = dao.buscarPorCedula(parts[1]);
                    if (a == null) JsonUtil.sendError(resp, 404, "No encontrado");
                    else JsonUtil.sendJson(resp, a);
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
            if (path != null && path.contains("/beneficiarios")) {
                Beneficiario b = gson.fromJson(req.getReader(), Beneficiario.class);
                dao.insertarBeneficiario(b);
                resp.setStatus(201); JsonUtil.sendJson(resp, b);
            } else if (path != null && path.contains("/fundador")) {
                AsociadoFundador f = gson.fromJson(req.getReader(), AsociadoFundador.class);
                dao.insertarFundador(f);
                resp.setStatus(201); JsonUtil.sendJson(resp, f);
            } else {
                Asociado a = gson.fromJson(req.getReader(), Asociado.class);
                dao.insertar(a);
                resp.setStatus(201); JsonUtil.sendJson(resp, a);
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdminOAsesor(token)) { JsonUtil.sendError(resp, 403, "Sin permiso"); return; }
        try {
            Asociado a = gson.fromJson(req.getReader(), Asociado.class);
            dao.actualizar(a);
            JsonUtil.sendJson(resp, a);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}