package com.coovalluna.servlet;

import com.coovalluna.dao.ReporteDAO;
import com.coovalluna.util.AuthUtil;
import com.coovalluna.util.CorsUtil;
import com.coovalluna.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

public class ReporteServlet extends HttpServlet {
    private final ReporteDAO dao = new ReporteDAO();

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
        if (token == null || !AuthUtil.esAdminOAsesor(token)) { JsonUtil.sendError(resp, 403, "Sin permiso"); return; }
        try {
            String path = req.getPathInfo();
            if (path == null) { JsonUtil.sendError(resp, 400, "Reporte requerido"); return; }
            switch (path) {
                case "/r1": JsonUtil.sendJson(resp, dao.r1AsociadosPorEstadoYAgencia(req.getParameter("estado"), req.getParameter("agencia"))); break;
                case "/r2": JsonUtil.sendJson(resp, dao.r2ExtractoCuenta(req.getParameter("cuenta"), req.getParameter("desde"), req.getParameter("hasta"), req.getParameter("tipo"), req.getParameter("canal"))); break;
                case "/r3": JsonUtil.sendJson(resp, dao.r3CarteraPorLineaYEstado(req.getParameter("agencia"), req.getParameter("desde"), req.getParameter("hasta"))); break;
                case "/r4": JsonUtil.sendJson(resp, dao.r4AsociadosEnMora()); break;
                case "/r5": JsonUtil.sendJson(resp, dao.r5HistorialPagos(req.getParameter("radicado"))); break;
                case "/r6": JsonUtil.sendJson(resp, dao.r6ProductividadAsesores(req.getParameter("agencia"), req.getParameter("desde"), req.getParameter("hasta"))); break;
                case "/r7": JsonUtil.sendJson(resp, dao.r7CreditosConCodeudor()); break;
                default: JsonUtil.sendError(resp, 404, "Reporte no encontrado");
            }
        } catch (SQLException e) { JsonUtil.sendError(resp, 500, e.getMessage()); }
    }
}