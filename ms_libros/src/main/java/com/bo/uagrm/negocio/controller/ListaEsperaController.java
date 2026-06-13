package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.negocio.LibroN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ListaEsperaController implements HttpHandler {

    private final LibroN libroN = new LibroN();
    private final ObjectMapper mapper = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        try {
            switch (method) {
                case "POST"   -> handleSuscribir(exchange);
                case "DELETE" -> handleCancelar(exchange);
                default       -> sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Error interno\"}");
        }
    }

    // POST /lista-espera  body: { "libroId": 5, "usuarioId": 12 }
    private void handleSuscribir(HttpExchange exchange) throws Exception {
        var body     = mapper.readTree(exchange.getRequestBody());
        int libroId  = body.path("libroId").asInt();
        int usuarioId = body.path("usuarioId").asInt();
        boolean ok   = libroN.suscribirUsuario(libroId, usuarioId);
        sendResponse(exchange, ok ? 201 : 409, "{\"ok\":" + ok + "}");
    }

    // DELETE /lista-espera  body: { "libroId": 5, "usuarioId": 12 }
    private void handleCancelar(HttpExchange exchange) throws Exception {
        var body     = mapper.readTree(exchange.getRequestBody());
        int libroId  = body.path("libroId").asInt();
        int usuarioId = body.path("usuarioId").asInt();
        boolean ok   = libroN.cancelarSuscripcion(libroId, usuarioId);
        sendResponse(exchange, 200, "{\"ok\":" + ok + "}");
    }

    private void sendResponse(HttpExchange e, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        e.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = e.getResponseBody()) { os.write(bytes); }
    }
}
