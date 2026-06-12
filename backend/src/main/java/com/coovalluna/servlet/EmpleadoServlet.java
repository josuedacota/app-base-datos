package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.EmpleadoDAO;
import com.coovalluna.model.Empleado;
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
 * EmpleadoServlet — CRUD de empleados.
 *
 * Rutas (todas requieren JWT excepto OPTIONS):
 *   GET    /api/empleados/            → listar todos los empleados
 *                                       (param. opcional: ?agencia=COD)
 *   GET    /api/empleados/{cedula}    → obtener un empleado por cédula
 *   POST   /api/empleados/            → crear nuevo empleado
 *   PUT    /api/empleados/{cedula}    → actualizar empleado existente
 */
@WebServlet("/api/empleados/*")
public class EmpleadoServlet extends HttpServlet {

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

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

        String cedula = extraerId(req);

        try {
            if (cedula == null) {
                // GET /api/empleados/  — listar (filtro opcional por agencia)
                String codigoAgencia = req.getParameter("agencia");
                List<Empleado> lista = empleadoDAO.listarTodos(codigoAgencia);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else {
                // GET /api/empleados/{cedula}
                Empleado empleado = empleadoDAO.buscarPorCedula(cedula);
                if (empleado == null) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Empleado no encontrado: " + cedula);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.sendJson(res, empleado);
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

        Empleado nuevo;
        try {
            nuevo = JsonUtil.getGson().fromJson(req.getReader(), Empleado.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        // Validaciones básicas
        String error = validarEmpleado(nuevo, true);
        if (error != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        try {
            // Verificar duplicado
            if (empleadoDAO.buscarPorCedula(nuevo.getCedulaEmpleado()) != null) {
                JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                        "Ya existe un empleado con la cédula: " + nuevo.getCedulaEmpleado());
                return;
            }

            empleadoDAO.insertar(nuevo);
            res.setStatus(HttpServletResponse.SC_CREATED);
            JsonUtil.sendJson(res, Map.of(
                    "mensaje", "Empleado creado correctamente",
                    "cedula", nuevo.getCedulaEmpleado()));

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al crear el empleado: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ PUT
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse res)
            throws IOException {

        CorsUtil.setCorsHeaders(res);
        if (!tokenValido(req, res)) return;

        String cedula = extraerId(req);
        if (cedula == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar la cédula en la URL: /api/empleados/{cedula}");
            return;
        }

        Empleado cambios;
        try {
            cambios = JsonUtil.getGson().fromJson(req.getReader(), Empleado.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Cuerpo JSON inválido");
            return;
        }

        String error = validarEmpleado(cambios, false);
        if (error != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        // Forzar la cédula desde la URL
        cambios.setCedulaEmpleado(cedula);

        try {
            if (empleadoDAO.buscarPorCedula(cedula) == null) {
                JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                        "Empleado no encontrado: " + cedula);
                return;
            }

            empleadoDAO.actualizar(cambios);
            res.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.sendJson(res, Map.of("mensaje", "Empleado actualizado correctamente"));

        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar el empleado: " + e.getMessage());
        }
    }

    // ============================================================ Helpers

    /**
     * Extrae la cédula del segmento de path después de /api/empleados/.
     * Devuelve null si la ruta termina en "/" (listado).
     */
    private String extraerId(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return null;
        return info.substring(1);
    }

    /**
     * Valida campos obligatorios del empleado.
     *
     * @param e        objeto Empleado a validar
     * @param esNuevo  true = incluye validación de cédula (POST)
     * @return mensaje de error o null si todo está bien
     */
    private String validarEmpleado(Empleado e, boolean esNuevo) {
        if (e == null) return "El cuerpo de la solicitud no puede estar vacío";
        if (esNuevo && (e.getCedulaEmpleado() == null || e.getCedulaEmpleado().isBlank()))
            return "El campo cedulaEmpleado es obligatorio";
        if (e.getNombres() == null || e.getNombres().isBlank())
            return "El campo nombres es obligatorio";
        if (e.getApellidos() == null || e.getApellidos().isBlank())
            return "El campo apellidos es obligatorio";
        if (e.getCargo() == null || e.getCargo().isBlank())
            return "El campo cargo es obligatorio";
        if (e.getTipoEmpleado() == null || e.getTipoEmpleado().isBlank())
            return "El campo tipoEmpleado es obligatorio";
        if (esNuevo && e.getFechaIngreso() == null)
            return "El campo fechaIngreso es obligatorio (formato YYYY-MM-DD)";
        if (e.getSalarioBase() == null || e.getSalarioBase().signum() < 0)
            return "El campo salarioBase es obligatorio y debe ser >= 0";
        if (e.getCodigoAgencia() == null || e.getCodigoAgencia().isBlank())
            return "El campo codigoAgencia es obligatorio";
        return null;
    }

    /**
     * Valida el token Bearer JWT.
     * Si falla escribe la respuesta de error y devuelve false.
     */
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
}
