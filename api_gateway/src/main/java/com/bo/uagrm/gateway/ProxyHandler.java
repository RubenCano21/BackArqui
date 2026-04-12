package com.bo.uagrm.gateway;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler genérico de proxy reverso.
 * Recibe una petición del cliente, la reenvía al microservicio destino
 * conservando método, headers y body, y devuelve la respuesta al cliente.
 */
public class ProxyHandler implements HttpHandler {

    // Headers que no se deben reenviar (hop-by-hop)
    private static final Set<String> SKIP_REQUEST_HEADERS = Set.of(
            "connection", "host", "transfer-encoding", "content-length"
    );
    private static final Set<String> SKIP_RESPONSE_HEADERS = Set.of(
            "connection", "transfer-encoding"
    );

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /** Prefijo que registra el gateway, ej: /api/usuarios */
    private final String gatewayPrefix;

    /** URL base del microservicio destino, ej: http://localhost:8081 */
    private final String targetBase;

    public ProxyHandler(String gatewayPrefix, String targetBase) {
        this.gatewayPrefix = gatewayPrefix;
        this.targetBase    = targetBase.replaceAll("/+$", "");
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method       = exchange.getRequestMethod();
        String incomingPath = exchange.getRequestURI().getPath();
        String query        = exchange.getRequestURI().getQuery();

        // CORS preflight: responde inmediatamente sin reenviar al microservicio
        if ("OPTIONS".equalsIgnoreCase(method)) {
            sendCorsPreflightResponse(exchange);
            return;
        }

        // /api/usuarios/login → /usuarios/login
        String downstreamPath = incomingPath.replaceFirst("^/api", "");
        String targetUrl = targetBase + downstreamPath + (query != null ? "?" + query : "");

        System.out.printf("[Gateway] %-7s %-35s → %s%n", method, incomingPath, targetUrl);

        try {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();

            // Construir la petición downstream
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(30));

            // Reenviar headers del cliente (excepto hop-by-hop)
            for (Map.Entry<String, List<String>> entry : exchange.getRequestHeaders().entrySet()) {
                String name = entry.getKey().toLowerCase();
                if (SKIP_REQUEST_HEADERS.contains(name)) continue;
                for (String val : entry.getValue()) {
                    try { reqBuilder.header(entry.getKey(), val); }
                    catch (IllegalArgumentException ignored) {}
                }
            }

            // Asignar método y body
            if (requestBody.length == 0) {
                reqBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                reqBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(requestBody));
            }

            HttpResponse<byte[]> msResponse = HTTP_CLIENT.send(
                    reqBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());

            // Reenviar headers de respuesta
            Headers outHeaders = exchange.getResponseHeaders();
            msResponse.headers().map().forEach((name, values) -> {
                if (!SKIP_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                    outHeaders.put(name, values);
                }
            });
            addCorsHeaders(outHeaders);

            byte[] body = msResponse.body();
            int contentLength = (body == null || body.length == 0) ? -1 : body.length;
            exchange.sendResponseHeaders(msResponse.statusCode(), contentLength);

            if (body != null && body.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            }

        } catch (Exception e) {
            String tipo = e.getClass().getSimpleName();
            System.err.printf("[Gateway] ERROR al contactar %s: %s%n", targetUrl, e.getMessage());
            sendError(exchange, 502,
                    "No se pudo contactar el servicio destino [" + tipo + "]: " + e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private void sendCorsPreflightResponse(HttpExchange exchange) throws IOException {
        Headers h = exchange.getResponseHeaders();
        addCorsHeaders(h);
        exchange.sendResponseHeaders(204, -1);
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String safe = message == null ? "error desconocido" : message.replace("\"", "'");
        byte[] body = ("{\"error\":\"" + safe + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(exchange.getResponseHeaders());
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void addCorsHeaders(Headers headers) {
        headers.set("Access-Control-Allow-Origin",  "*");
        headers.set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        headers.set("Access-Control-Allow-Headers", "Content-Type, X-Usuario-Id, Authorization");
        headers.set("Access-Control-Max-Age",       "3600");
    }
}

