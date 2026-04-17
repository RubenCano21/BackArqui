package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.datos.entity.Usuario;
import com.bo.uagrm.negocio.AuthException;
import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.negocio.UsuarioN;
import com.bo.uagrm.negocio.dto.LoginRequest;
import com.bo.uagrm.negocio.dto.LoginResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class UsuarioController implements HttpHandler {

    private final UsuarioN usuarioN = new UsuarioN();
    private final ObjectMapper mapper = JsonConfig.getMapper();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path   = exchange.getRequestURI().getPath();
        boolean isLoginPath   = path.matches("^/usuarios/login/?$");
        boolean isRolesPath   = path.matches("^/usuarios/\\d+/roles/?$");

        // Extraer segmento de ID si existe: /usuarios/42 → "42"
        String[] parts = path.replaceAll("/$", "").split("/");
        String lastSegment = parts[parts.length - 1];
        boolean hasId = lastSegment.matches("\\d+");
        Long id = hasId ? Long.parseLong(lastSegment) : null;

        // Para /usuarios/{id}/roles el id está en la posición 2
        if (isRolesPath && parts.length >= 3) {
            id = Long.parseLong(parts[2]);
        }

        try {
            // GET /usuarios/{id}/roles — endpoint interno para el API Gateway
            if (isRolesPath) {
                if (!"GET".equalsIgnoreCase(method)) {
                    sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
                    return;
                }
                handleGetRoles(exchange, id);
                return;
            }

            if (isLoginPath && !"POST".equals(method)) {
                sendResponse(exchange, 405, "{\"error\":\"Metodo no permitido\"}");
                return;
            }

            switch (method) {
                case "GET"    -> handleUsuarioGet(exchange, id);
                case "POST"   -> {
                    if (isLoginPath) {
                        handleLogin(exchange);
                    } else {
                        handleUsuarioPost(exchange);
                    }
                }
                case "PUT"    -> handleUsuarioPut(exchange, id);
                case "DELETE" -> handleUsuarioDelete(exchange, id);
                default       -> sendResponse(exchange, 405, "{\"error\":\"Método no permitido\"}");
            }
        } catch (AuthException e) {
            sendResponse(exchange, 401, jsonError(e.getMessage()));
        } catch (SecurityException e) {
            sendResponse(exchange, 403, jsonError(e.getMessage()));
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, jsonError(e.getMessage()));
        } catch (IllegalStateException e) {
            sendResponse(exchange, 409, jsonError(e.getMessage()));
        } catch (Exception e) {
            sendResponse(exchange, 500, jsonError("Error interno: " + e.getMessage()));
        }
    }



    // GET /usuarios/{id}/roles — endpoint interno para el API Gateway
    private void handleGetRoles(HttpExchange exchange, Long id) throws Exception {
        if (id == null) {
            sendResponse(exchange, 400, jsonError("Se requiere el ID del usuario"));
            return;
        }
        List<String> roles = usuarioN.obtenerRoles(id);
        sendResponse(exchange, 200, mapper.writeValueAsString(roles));
    }

    // GET
    private void handleUsuarioGet(HttpExchange exchange, Long id) throws Exception {
        if (id == null){
            List<Usuario> lista = usuarioN.listarUsuarios();
            sendResponse(exchange, 200, mapper.writeValueAsString(lista));
        } else {
            Usuario usuario = usuarioN.buscarPorId(id);
            sendResponse(exchange, 200, mapper.writeValueAsString(usuario));
        }
    }

    // POST
    private void handleUsuarioPost(HttpExchange exchange) throws Exception {
        Usuario nuevo = readBody(exchange, Usuario.class);
        boolean creado = usuarioN.registrarUsuario(nuevo);
        sendResponse(exchange, 201, mapper.writeValueAsString(creado));
    }

    // POST /usuarios/login
    private void handleLogin(HttpExchange exchange) throws Exception {
        LoginRequest request = readBody(exchange, LoginRequest.class);
        LoginResponse response = usuarioN.login(request);
        sendResponse(exchange, 200, mapper.writeValueAsString(response));
    }

    // PUT
    private void handleUsuarioPut(HttpExchange exchange, Long id) throws Exception {
        if (id == null){
            sendResponse(exchange, 400, jsonError(" Se requiere el ID en la URL"));
            return;
        }
        Usuario datos = readBody(exchange, Usuario.class);
        datos.setId(id);
        boolean actualizado = usuarioN.actualizarUsuario(datos);
        sendResponse(exchange, 200, mapper.writeValueAsString(actualizado));
    }

    // DELETE — la autorización ADMIN ya fue validada por el API Gateway
    private void handleUsuarioDelete(HttpExchange exchange, Long id) {
        if (id == null) {
            try {
                sendResponse(exchange, 400, jsonError("Se requiere el ID en la URL"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        try {
            boolean eliminado = usuarioN.eliminarUsuario(id);
            sendResponse(exchange, 200, mapper.writeValueAsString(eliminado));
        } catch (Exception e) {
            try {
                sendResponse(exchange, 500, jsonError("Error interno: " + e.getMessage()));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }



    //Utils
    private <T> T readBody(HttpExchange exchange, Class<T> clazz) throws Exception {
        try (InputStream is = exchange.getRequestBody()) {
            return mapper.readValue(is, clazz);
        }
    }

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

    private String jsonError(String message){
        return "{\"error\":\"" + message.replace("\"", "'") + "\"}";
    }
}
