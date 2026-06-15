package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Multa;
import com.bo.uagrm.datos.entity.Prestamo;
import com.bo.uagrm.datos.entity.PrestamoItem;
import com.bo.uagrm.negocio.DevolucionN;
import com.bo.uagrm.negocio.PrestamoN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PrestamoController implements HttpHandler {

    private final PrestamoN prestamoN = new PrestamoN();
    private final ObjectMapper mapper  = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath().replaceAll("/$", "");

        try {
            // GET /prestamos/estudiante/{id}
            if (path.matches("^/prestamos/estudiante/\\d+$") && "GET".equals(method)) {
                int estId = Integer.parseInt(path.split("/")[3]);
                sendJson(exchange, 200, prestamoN.listarPorUsuario(estId));
                return;
            }

            // PUT /prestamos/{id}/devolver
            if (path.matches("^/prestamos/\\d+/devolver$") && "PUT".equals(method)) {
                int id = Integer.parseInt(path.split("/")[2]);
                Multa multa = new DevolucionN().registrarDevolucion(id);
                sendJson(exchange, 200, multa);
                return;
            }

            // DELETE /prestamos/{id}/items/{libroId}
            if (path.matches("^/prestamos/\\d+/items/\\d+$") && "DELETE".equals(method)) {
                String[] parts = path.split("/");
                int prestamoId = Integer.parseInt(parts[2]);
                int libroId    = Integer.parseInt(parts[4]);
                sendJson(exchange, 200, prestamoN.eliminarItem(prestamoId, libroId));
                return;
            }

            // POST /prestamos/{id}/items
            if (path.matches("^/prestamos/\\d+/items$") && "POST".equals(method)) {
                int prestamoId = Integer.parseInt(path.split("/")[2]);
                PrestamoItem item = readBody(exchange, PrestamoItem.class);
                sendJson(exchange, 201, prestamoN.agregarItem(prestamoId, item.getIdLibro()));
                return;
            }

            // Extraer ID si el path es /prestamos/{id}
            String[] parts = path.split("/");
            boolean hasId  = parts.length >= 3 && parts[2].matches("\\d+");
            Integer id     = hasId ? Integer.parseInt(parts[2]) : null;

            // Leer headers de identidad inyectados por el API Gateway
            String xRol       = exchange.getRequestHeaders().getFirst("X-Rol");
            String xUsuarioId = exchange.getRequestHeaders().getFirst("X-Usuario-Id");
            boolean esAdmin   = "ADMIN".equalsIgnoreCase(xRol);

            switch (method) {
                case "GET" -> {
                    if (id == null) {
                        if (esAdmin) {
                            // ADMIN → ve todos los préstamos
                            sendJson(exchange, 200, prestamoN.listarPrestamos());
                        } else {
                            // ESTUDIANTE → solo sus propios préstamos
                            if (xUsuarioId == null || !xUsuarioId.matches("\\d+")) {
                                sendJson(exchange, 401, jsonError("Se requiere X-Usuario-Id"));
                                return;
                            }
                            sendJson(exchange, 200, prestamoN.listarPorUsuario(Integer.parseInt(xUsuarioId)));
                        }
                    } else {
                        Prestamo p = prestamoN.buscarPorId(id);
                        if (p == null) {
                            sendJson(exchange, 404, jsonError("Préstamo no encontrado"));
                        } else if (!esAdmin && xUsuarioId != null && p.getUsuarioId() != Integer.parseInt(xUsuarioId)) {
                            // ESTUDIANTE intentando ver el préstamo de otro
                            sendJson(exchange, 403, jsonError("No tienes permiso para ver este préstamo"));
                        } else {
                            sendJson(exchange, 200, p);
                        }
                    }
                }
                case "POST" -> {
                    Prestamo nuevo = readBody(exchange, Prestamo.class);
                    sendJson(exchange, 201, prestamoN.registrarPrestamo(nuevo));
                }
                case "DELETE" -> {
                    if (id == null) { sendJson(exchange, 400, jsonError("Se requiere el ID")); return; }
                    sendJson(exchange, 200, prestamoN.cancelarPrestamo(id));
                }
                default -> sendJson(exchange, 405, jsonError("Método no permitido"));
            }

        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, jsonError(e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, jsonError(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("Error interno: " + e.getMessage()));
        }
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        String json = body instanceof String s && s.startsWith("{")
                ? s
                : mapper.writeValueAsString(body);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private <T> T readBody(HttpExchange ex, Class<T> clazz) throws Exception {
        try (InputStream is = ex.getRequestBody()) { return mapper.readValue(is, clazz); }
    }

    private String jsonError(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }
}

