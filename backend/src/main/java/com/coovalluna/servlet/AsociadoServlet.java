package com.coovalluna.servlet;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.coovalluna.config.AppConfig;
import com.coovalluna.dao.AsociadoDAO;
import com.coovalluna.model.Asociado;
import com.coovalluna.model.AsociadoFundador;
import com.coovalluna.model.Beneficiario;
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
 * AsociadoServlet — gestión de asociados, beneficiarios y fundadores.
 *
 * Rutas (todas requieren JWT excepto OPTIONS):
 *   GET    /api/asociados/                          → listar (?estado=X &tipo=Y)
 *   GET    /api/asociados/{cedula}                  → obtener un asociado
 *   POST   /api/asociados/                          → crear asociado
 *   PUT    /api/asociados/{cedula}                  → actualizar asociado
 *
 *   GET    /api/asociados/{cedula}/beneficiarios    → listar beneficiarios
 *   POST   /api/asociados/{cedula}/beneficiarios    → agregar beneficiario
 *
 *   POST   /api/asociados/{cedula}/fundador         → registrar como fundador
 */
@WebServlet("/api/asociados/*")
public class AsociadoServlet extends HttpServlet {

    private final AsociadoDAO asociadoDAO = new AsociadoDAO();

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

        // /api/asociados/                     → null, null
        // /api/asociados/{cedula}             → "12345", null
        // /api/asociados/{cedula}/beneficiarios → "12345", "beneficiarios"
        String[] segmentos = parsePath(req);
        String cedula   = segmentos[0];
        String subruta  = segmentos[1];

        try {
            if (cedula == null) {
                // Listar todos con filtros opcionales
                String estado = req.getParameter("estado");
                String tipo   = req.getParameter("tipo");
                List<Asociado> lista = asociadoDAO.listarTodos(estado, tipo);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else if ("beneficiarios".equals(subruta)) {
                // GET /api/asociados/{cedula}/beneficiarios
                if (!existeAsociado(cedula, res)) return;
                List<Beneficiario> lista = asociadoDAO.listarBeneficiarios(cedula);
                res.setStatus(HttpServletResponse.SC_OK);
                JsonUtil.sendJson(res, lista);

            } else {
                // GET /api/asociados/{cedula}
                Asociado a = asociadoDAO.buscarPorCedula(cedula);
                if (a == null) {
                    JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                            "Asociado no encontrado: " + cedula);
                } else {
                    res.setStatus(HttpServletResponse.SC_OK);
                    JsonUtil.sendJson(res, a);
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

        String[] segmentos = parsePath(req);
        String cedula  = segmentos[0];
        String subruta = segmentos[1];

        try {
            if (cedula == null) {
                // POST /api/asociados/  — crear asociado
                crearAsociado(req, res);

            } else if ("beneficiarios".equals(subruta)) {
                // POST /api/asociados/{cedula}/beneficiarios
                agregarBeneficiario(cedula, req, res);

            } else if ("fundador".equals(subruta)) {
                // POST /api/asociados/{cedula}/fundador
                registrarFundador(cedula, req, res);

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

        String[] segmentos = parsePath(req);
        String cedula = segmentos[0];

        if (cedula == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "Debe indicar la cédula en la URL: /api/asociados/{cedula}");
            return;
        }

        Asociado cambios;
        try {
            cambios = JsonUtil.getGson().fromJson(req.getReader(), Asociado.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        String error = validarAsociado(cambios, false);
        if (error != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        cambios.setCedula(cedula);

        try {
            if (!existeAsociado(cedula, res)) return;
            asociadoDAO.actualizar(cambios);
            res.setStatus(HttpServletResponse.SC_OK);
            JsonUtil.sendJson(res, Map.of("mensaje", "Asociado actualizado correctamente"));
        } catch (SQLException e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Error al actualizar el asociado: " + e.getMessage());
        }
    }

    // ============================================================ Handlers internos

    private void crearAsociado(HttpServletRequest req, HttpServletResponse res)
            throws IOException, SQLException {

        Asociado nuevo;
        try {
            nuevo = JsonUtil.getGson().fromJson(req.getReader(), Asociado.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        String error = validarAsociado(nuevo, true);
        if (error != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, error);
            return;
        }

        if (asociadoDAO.buscarPorCedula(nuevo.getCedula()) != null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_CONFLICT,
                    "Ya existe un asociado con la cédula: " + nuevo.getCedula());
            return;
        }

        asociadoDAO.insertar(nuevo);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of(
                "mensaje", "Asociado creado correctamente",
                "cedula",  nuevo.getCedula()));
    }

    private void agregarBeneficiario(String cedulaAsociado,
                                     HttpServletRequest req,
                                     HttpServletResponse res)
            throws IOException, SQLException {

        if (!existeAsociado(cedulaAsociado, res)) return;

        Beneficiario b;
        try {
            b = JsonUtil.getGson().fromJson(req.getReader(), Beneficiario.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        if (b == null || b.getNombreCompleto() == null || b.getNombreCompleto().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo nombreCompleto es obligatorio");
            return;
        }
        if (b.getPorcentajeParticipacion() == null
                || b.getPorcentajeParticipacion().compareTo(java.math.BigDecimal.ZERO) <= 0
                || b.getPorcentajeParticipacion().compareTo(new java.math.BigDecimal("100")) > 0) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "porcentajeParticipacion debe ser un valor entre 0.01 y 100");
            return;
        }

        b.setCedulaAsociado(cedulaAsociado);
        asociadoDAO.insertarBeneficiario(b);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of("mensaje", "Beneficiario agregado correctamente"));
    }

    private void registrarFundador(String cedula,
                                   HttpServletRequest req,
                                   HttpServletResponse res)
            throws IOException, SQLException {

        if (!existeAsociado(cedula, res)) return;

        AsociadoFundador f;
        try {
            f = JsonUtil.getGson().fromJson(req.getReader(), AsociadoFundador.class);
        } catch (Exception e) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST, "Cuerpo JSON inválido");
            return;
        }

        if (f == null || f.getNumeroActaFundacional() == null || f.getNumeroActaFundacional().isBlank()) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo numeroActaFundacional es obligatorio");
            return;
        }
        if (f.getAnioReconocimiento() < 1900) {
            JsonUtil.sendError(res, HttpServletResponse.SC_BAD_REQUEST,
                    "El campo anioReconocimiento debe ser un año válido");
            return;
        }

        f.setCedula(cedula);
        asociadoDAO.insertarFundador(f);
        res.setStatus(HttpServletResponse.SC_CREATED);
        JsonUtil.sendJson(res, Map.of("mensaje", "Asociado registrado como fundador"));
    }

    // ============================================================ Helpers

    /**
     * Divide el pathInfo en [cedula, subruta].
     *   /               → [null, null]
     *   /12345          → ["12345", null]
     *   /12345/beneficiarios → ["12345", "beneficiarios"]
     */
    private String[] parsePath(HttpServletRequest req) {
        String info = req.getPathInfo();
        if (info == null || info.equals("/")) return new String[]{null, null};
        String[] partes = info.substring(1).split("/", 2);
        String cedula  = partes[0].isBlank() ? null : partes[0];
        String subruta = (partes.length > 1 && !partes[1].isBlank()) ? partes[1] : null;
        return new String[]{cedula, subruta};
    }

    /** Comprueba si el asociado existe; si no escribe 404 y devuelve false. */
    private boolean existeAsociado(String cedula, HttpServletResponse res)
            throws IOException, SQLException {
        if (asociadoDAO.buscarPorCedula(cedula) == null) {
            JsonUtil.sendError(res, HttpServletResponse.SC_NOT_FOUND,
                    "Asociado no encontrado: " + cedula);
            return false;
        }
        return true;
    }

    /** Valida campos obligatorios del Asociado. */
    private String validarAsociado(Asociado a, boolean esNuevo) {
        if (a == null) return "El cuerpo de la solicitud no puede estar vacío";
        if (esNuevo && (a.getCedula() == null || a.getCedula().isBlank()))
            return "El campo cedula es obligatorio";
        if (a.getNombres() == null || a.getNombres().isBlank())
            return "El campo nombres es obligatorio";
        if (a.getApellidos() == null || a.getApellidos().isBlank())
            return "El campo apellidos es obligatorio";
        if (a.getFechaNacimiento() == null)
            return "El campo fechaNacimiento es obligatorio (formato YYYY-MM-DD)";
        if (esNuevo && a.getFechaAfiliacion() == null)
            return "El campo fechaAfiliacion es obligatorio (formato YYYY-MM-DD)";
        if (a.getTipoAsociado() == null || a.getTipoAsociado().isBlank())
            return "El campo tipoAsociado es obligatorio";
        if (a.getCuotaSostenimiento() == null || a.getCuotaSostenimiento().signum() < 0)
            return "El campo cuotaSostenimiento es obligatorio y debe ser >= 0";
        return null;
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
}
