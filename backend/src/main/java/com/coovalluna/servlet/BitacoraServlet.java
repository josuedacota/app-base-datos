package com.coovalluna.servlet;

import com.coovalluna.dao.BitacoraDAO;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class BitacoraServlet extends HttpServlet {
    private final BitacoraDAO dao = new BitacoraDAO();

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
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            JsonUtil.sendJson(resp, dao.listar(req.getParameter("desde"), req.getParameter("hasta"), req.getParameter("accion")));
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}