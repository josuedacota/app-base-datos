package com.coovalluna.servlet;

import com.coovalluna.dao.UsuarioDAO;
import com.coovalluna.model.LoginRequest;
import com.coovalluna.model.UsuarioSistema;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import com.google.gson.Gson;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AuthServlet extends HttpServlet {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final Gson gson = JsonUtil.getGson();

    @Override
    protected void doOptions(HttpServletRequest req,
                             HttpServletResponse resp) throws IOException {
        CorsUtil.setCorsHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        CorsUtil.setCorsHeaders(resp);

        String path = req.getPathInfo();

        if ("/login".equals(path)) {
            login(req, resp);
            return;
        }

        JsonUtil.sendError(resp, 404, "Ruta no encontrada");
    }

    private void login(HttpServletRequest req,
                       HttpServletResponse resp)
            throws IOException {

        try {

            LoginRequest datos =
                    gson.fromJson(req.getReader(), LoginRequest.class);

            UsuarioSistema usuario =
                    usuarioDAO.buscarPorUsername(datos.getUsername());

            if (usuario == null) {
                JsonUtil.sendError(resp, 401, "Usuario o contraseña incorrectos");
                return;
            }

            boolean passwordValida =
                    BCrypt.checkpw(
                            datos.getPassword(),
                            usuario.getPasswordHash()
                    );

            System.out.println("PASSWORD INGRESADA: " + datos.getPassword());
            System.out.println("HASH EN BD: " + usuario.getPasswordHash());
            System.out.println("RESULTADO BCRYPT: " + passwordValida);

            if (!passwordValida) {
                JsonUtil.sendError(resp, 401, "Usuario o contraseña incorrectos");
                return;
            }

            String rolFrontend;

            switch (usuario.getRol().toLowerCase()) {
                case "administrador":
                    rolFrontend = "ADMIN";
                    break;

                case "asesor":
                    rolFrontend = "ASESOR";
                    break;

                case "asociado":
                    rolFrontend = "ASOCIADO";
                    break;

                default:
                    rolFrontend = usuario.getRol();
            }

            usuario.setRol(rolFrontend);

            String token =
                    AuthUtil.generarToken(usuario);

            Map<String, Object> respuesta =
                    new HashMap<>();

            respuesta.put("token", token);
            respuesta.put("usuario", usuario);

            JsonUtil.sendJson(resp, respuesta);

        } catch (SQLException e) {

            e.printStackTrace();
            JsonUtil.sendError(resp, 500, e.getMessage());
        }
    }
}