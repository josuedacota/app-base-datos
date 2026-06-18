package com.coovalluna.servlet;

import com.coovalluna.dao.UsuarioDAO;
import com.coovalluna.model.UsuarioSistema;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class UsuarioServlet extends HttpServlet {
    private final UsuarioDAO dao = new UsuarioDAO();
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
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            JsonUtil.sendJson(resp, dao.listarTodos());
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        String path = req.getPathInfo();
        if (path != null && path.equals("/cambiarClave")) {
            if (token == null) { JsonUtil.sendError(resp, 401, "No autorizado"); return; }
            try { AuthUtil.validarJWT(token); } catch (Exception e) { JsonUtil.sendError(resp, 401, "Token inválido"); return; }
            try {
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                String nuevaPassword = (String) body.get("nuevaPassword");
                if (nuevaPassword == null || nuevaPassword.length() < 8) { JsonUtil.sendError(resp, 400, "Mínimo 8 caracteres"); return; }
                int idUsuario = AuthUtil.validarJWT(token).getClaim("idUsuario").asInt();
                String hash = BCrypt.hashpw(nuevaPassword, BCrypt.gensalt());
                dao.actualizarClave(idUsuario, hash);
                Map<String, String> r = new HashMap<>(); r.put("mensaje", "Contraseña actualizada");
                JsonUtil.sendJson(resp, r);
            } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
            return;
        }
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            UsuarioSistema u = gson.fromJson(req.getReader(), UsuarioSistema.class);
            String raw = u.getPasswordHash();
            u.setPasswordHash(BCrypt.hashpw(raw, BCrypt.gensalt()));
            u.setDebeCambiarClave(true);
            dao.insertar(u);
            u.setPasswordHash(null);
            resp.setStatus(201); JsonUtil.sendJson(resp, u);
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }

    @Override protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        String token = getToken(req);
        if (token == null || !AuthUtil.esAdmin(token)) { JsonUtil.sendError(resp, 403, "Solo administradores"); return; }
        try {
            String path = req.getPathInfo();
            if (path != null && path.length() > 1) {
                int id = Integer.parseInt(path.substring(1));
                Map<?,?> body = gson.fromJson(req.getReader(), Map.class);
                if (body.containsKey("rol")) dao.actualizarRol(id, (String) body.get("rol"));
                Map<String, String> r = new HashMap<>(); r.put("mensaje", "Usuario actualizado");
                JsonUtil.sendJson(resp, r);
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}