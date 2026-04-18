package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Editorial;
import com.bo.uagrm.negocio.EditorialN;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EditorialController implements HttpHandler {

    private final EditorialN editorialN = new EditorialN();
    private final ObjectMapper mapper = JsonConfig.getMapper();


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();

        // Extraer segmento de ID si existe
        String[] parts = path.replaceAll("/$", "").split("/");
        String lastSegment = parts[parts.length - 1];
        boolean hasId = lastSegment.matches("\\d+");
        Integer id = hasId ? Integer.parseInt(lastSegment) : null;

        try {
            switch (method) {
                case "GET"    -> handleListEditoriales(exchange, id);
                case "POST"   -> handleRegistrarEditorial(exchange);
                case "PUT"    -> handleUpdateEditorial(exchange, id);
                case "DELETE" -> handleDeleteEditorial(exchange, id);
                default       -> sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    private void handleListEditoriales(HttpExchange exchange, Integer id) throws Exception{
        try {
            if (id == null){
                var lista = editorialN.listarEditoriales();
                sendResponse(exchange, 200, mapper.writeValueAsString(lista));
            } else {
                var editorial = editorialN.buscarEditorialPorId(id);
                if (editorial != null) {
                    sendResponse(exchange, 200, mapper.writeValueAsString(editorial));
                } else {
                    sendResponse(exchange, 404, jsonError("Editorial no encontrada"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(exchange, 500, jsonError("Error al listar editoriales"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }


    // POST
    private void handleRegistrarEditorial(HttpExchange exchange) {
        try {
            var editorial = readBody(exchange, Editorial.class);
            boolean success = editorialN.registrarEditorial(editorial);
            if (success) {
                sendResponse(exchange, 201, "{\"message\":\"Editorial registrada\"}");
            } else {
                sendResponse(exchange, 400, jsonError("No se pudo registrar la editorial"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(exchange, 500, jsonError("Error al registrar editorial"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // UPDATE
    private void handleUpdateEditorial(HttpExchange exchange, Integer id) throws Exception{
        if (id == null) {
            try {
                sendResponse(exchange, 400, jsonError("ID es requerido para actualizar"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            var editorial = readBody(exchange, Editorial.class);
            editorial.setId(id); // Asegurar que el ID del path se use
            boolean success = editorialN.actualizarEditorial(editorial);
            if (success) {
                sendResponse(exchange, 200, "{\"message\":\"Editorial actualizada\"}");
            } else {
                sendResponse(exchange, 404, jsonError("Editorial no encontrada"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendResponse(exchange, 500, jsonError("Error al actualizar editorial"));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // DELETE
    private void handleDeleteEditorial(HttpExchange exchange, Integer id) throws IOException {
        if (id == null) {
            sendResponse(exchange, 400, jsonError("ID es requerido para eliminar"));
            return;
        }
        try {
            boolean success = editorialN.eliminarEditorial(id);
            if (success) {
                sendResponse(exchange, 200, "{\"message\":\"Editorial eliminada\"}");
            } else {
                sendResponse(exchange, 404, jsonError("Editorial no encontrada"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, jsonError("Error al eliminar editorial"));
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
