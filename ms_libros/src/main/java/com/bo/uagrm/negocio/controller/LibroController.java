package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Libro;
import com.bo.uagrm.negocio.LibroN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LibroController implements HttpHandler {

    private final LibroN libroN = new LibroN();
    private final ObjectMapper mapper = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        // Extraer segmento de ID si existe: /libros/42 → "42"
        String[] parts = path.replaceAll("/$", "").split("/");
        String lastSegment = parts[parts.length - 1];
        boolean hasId = lastSegment.matches("\\d+");
        Integer id = hasId ? Integer.parseInt(lastSegment) : null;

        try {
                switch (method) {
                    case "GET"    -> handleListLibros(exchange, id);
                    case "POST"   -> handleResgistrarLibro(exchange);
                    case "PUT"    -> handleUpdateLibro(exchange, id);
                    case "DELETE" -> handleDeleteLibro(exchange, id);
                    default       -> sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
                }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Error interno del servidor\"}");
        }

    }

    // GET
    private void handleListLibros(HttpExchange exchange, Integer id) throws Exception {
        if (id == null){
            List<Libro> lista = libroN.listarLibros();
            sendResponse(exchange, 200, mapper.writeValueAsString(lista));
        } else {
            Libro libro = libroN.buscarLibroPorId(id);
            sendResponse(exchange, 200, mapper.writeValueAsString(libro));
        }
    }

    // POST
    private void handleResgistrarLibro(HttpExchange exchange) throws Exception{
        Libro nuevo = readBody(exchange, Libro.class);
        boolean registrado = libroN.registrarLibro(nuevo);
        sendResponse(exchange, 201, mapper.writeValueAsString(registrado));
    }

    // PUT
    private void handleUpdateLibro(HttpExchange exchange, Integer id) throws Exception{
        if (id == null){
            sendResponse(exchange, 400, jsonError(" Se requiere el ID en la URL"));
            return;
        }
        Libro datos = readBody(exchange, Libro.class);
        datos.setId(id);
        boolean actualizado = libroN.actualizarLibro(datos);
        sendResponse(exchange, 200, mapper.writeValueAsString(actualizado));
    }

    // DELETE
    private void handleDeleteLibro(HttpExchange exchange, Integer id) {
        if (id == null) {
            try {
                sendResponse(exchange, 400, jsonError("Se requiere el ID en la URL"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            boolean eliminado = libroN.eliminarLibro(id);
            sendResponse(exchange, 200, mapper.writeValueAsString(eliminado));
        } catch (Exception e) {
            try {
                sendResponse(exchange, 500, jsonError("Error interno: " + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    // Utils
    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? 0 : bytes.length);

        if (bytes.length > 0) {
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private <T> T readBody(HttpExchange exchange, Class<T> clazz) throws Exception {
        try (InputStream is = exchange.getRequestBody()) {
            return mapper.readValue(is, clazz);
        }
    }

    private String jsonError(String message){
        return "{\"error\":\"" + message.replace("\"", "'") + "\"}";
    }
}

