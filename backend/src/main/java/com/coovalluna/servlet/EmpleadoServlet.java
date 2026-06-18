package com.coovalluna.servlet;

import com.coovalluna.dao.EmpleadoDAO;
import com.coovalluna.model.Empleado;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class EmpleadoServlet extends HttpServlet {
    private final EmpleadoDAO dao = new EmpleadoDAO();
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
                String agencia = req.getParameter("agencia");
                JsonUtil.sendJson(resp, dao.listarTodos(agencia));
            } else {
                Empleado e = dao.buscarPorCedula(path.substring(1));
                if (e == null) JsonUtil.sendError(resp, 404, "No encontrado");
                else JsonUtil.sendJson(resp, e);
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            Empleado e = gson.fromJson(req.getReader(), Empleado.class);
            dao.insertar(e);
            resp.setStatus(201);
            JsonUtil.sendJson(resp, e);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            Empleado e = gson.fromJson(req.getReader(), Empleado.class);
            dao.actualizar(e);
            JsonUtil.sendJson(resp, e);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}