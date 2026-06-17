package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Multa;
import com.bo.uagrm.negocio.DevolucionN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class DevolucionController implements HttpHandler {

    private final DevolucionN devolucionN = new DevolucionN();
    private final ObjectMapper mapper      = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath().replaceAll("/$", "");

        // Solo acepta PUT /prestamos/{id}/devolver
        if (!"PUT".equalsIgnoreCase(method)) {
            sendJson(exchange, 405, "{\"error\":\"Solo se acepta PUT\"}");
            return;
        }

        try {
            handleDevolver(exchange, path);
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 404, jsonError(e.getMessage()));
        } catch (IllegalStateException e) {
            sendJson(exchange, 409, jsonError(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, jsonError("Error interno: " + e.getMessage()));
        }
    }

    private void handleDevolver(HttpExchange exchange, String path) throws Exception {
        // Extraer {id} de /prestamos/{id}/devolver
        String[] parts = path.split("/");
        if (parts.length < 3 || !parts[2].matches("\\d+")) {
            sendJson(exchange, 400, jsonError("ID de préstamo inválido"));
            return;
        }
        int prestamoId = Integer.parseInt(parts[2]);

        System.out.printf("[DevolucionController] PUT /prestamos/%d/devolver%n", prestamoId);

        Multa multa = devolucionN.registrarDevolucion(prestamoId);

        // Respuesta con detalles de la multa generada
        sendJson(exchange, 200, mapper.writeValueAsString(multa));
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private String jsonError(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }
}