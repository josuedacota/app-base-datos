package com.coovalluna.servlet;

import com.coovalluna.dao.AgenciaDAO;
import com.coovalluna.model.Agencia;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class AgenciaServlet extends HttpServlet {
    private final AgenciaDAO dao = new AgenciaDAO();
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
            String id = req.getPathInfo();
            if (id == null || id.equals("/")) {
                JsonUtil.sendJson(resp, dao.listarTodas());
            } else {
                Agencia a = dao.buscarPorCodigo(id.substring(1));
                if (a == null) JsonUtil.sendError(resp, 404, "No encontrada");
                else JsonUtil.sendJson(resp, a);
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            Agencia a = gson.fromJson(req.getReader(), Agencia.class);
            dao.insertar(a);
            resp.setStatus(201);
            JsonUtil.sendJson(resp, a);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            Agencia a = gson.fromJson(req.getReader(), Agencia.class);
            dao.actualizar(a);
            JsonUtil.sendJson(resp, a);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}