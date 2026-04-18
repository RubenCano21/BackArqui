package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Categoria;
import com.bo.uagrm.negocio.CategoriaN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class CategoriaController implements HttpHandler {

    private final CategoriaN categoriaN = new CategoriaN();
    private final ObjectMapper mapper = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        String[] parts = path.replaceAll("/$", "").split("/");
        String lastSegment = parts[parts.length - 1];
        Integer id = lastSegment.matches("\\d+") ? Integer.parseInt(lastSegment) : null;

        try {
            switch (method) {
                case "GET"    -> handleListCategorias(exchange, id);
                case "POST"   -> handleRegistrarCategoria(exchange);
                case "PUT"    -> handleUpdateCategoria(exchange, id);
                case "DELETE" -> handleDeleteCategoria(exchange, id);
                default       -> sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    // GET
    private void handleListCategorias(HttpExchange exchange, Integer id) throws Exception {
        if (id == null) {
            sendResponse(exchange, 200, mapper.writeValueAsString(categoriaN.listarCategorias()));
        } else {
            Categoria cat = categoriaN.buscarCategoriaPorId(id);
            if (cat != null) sendResponse(exchange, 200, mapper.writeValueAsString(cat));
            else             sendResponse(exchange, 404, jsonError("Categoria no encontrada"));
        }
    }

    // POST
    private void handleRegistrarCategoria(HttpExchange exchange) throws Exception {
        Categoria cat = readBody(exchange, Categoria.class);
        boolean ok = categoriaN.registrarCategoria(cat);
        sendResponse(exchange, ok ? 201 : 400,
                ok ? "{\"message\":\"Categoria registrada\"}" : jsonError("No se pudo registrar la categoria"));
    }

    // PUT
    private void handleUpdateCategoria(HttpExchange exchange, Integer id) throws Exception {
        if (id == null) { sendResponse(exchange, 400, jsonError("ID es requerido")); return; }
        Categoria cat = readBody(exchange, Categoria.class);
        cat.setId(id);
        boolean ok = categoriaN.actualizarCategoria(cat);
        sendResponse(exchange, ok ? 200 : 404,
                ok ? "{\"message\":\"Categoria actualizada\"}" : jsonError("Categoria no encontrada"));
    }

    // DELETE
    private void handleDeleteCategoria(HttpExchange exchange, Integer id) throws Exception {
        if (id == null) { sendResponse(exchange, 400, jsonError("ID es requerido")); return; }
        boolean ok = categoriaN.eliminarCategoria(id);
        sendResponse(exchange, ok ? 200 : 404,
                ok ? "{\"message\":\"Categoria eliminada\"}" : jsonError("Categoria no encontrada"));
    }

    // Utils
    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private <T> T readBody(HttpExchange exchange, Class<T> clazz) throws Exception {
        try (InputStream is = exchange.getRequestBody()) { return mapper.readValue(is, clazz); }
    }

    private String jsonError(String message) {
        return "{\"error\":\"" + message.replace("\"", "'") + "\"}";
    }
}

