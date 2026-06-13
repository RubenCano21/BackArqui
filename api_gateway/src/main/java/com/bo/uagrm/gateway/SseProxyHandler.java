package com.bo.uagrm.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handler especializado para Server-Sent Events (SSE).
 * El ProxyHandler normal usa HttpClient.send() que espera la respuesta
 * completa — con un stream infinito nunca termina y el gateway se bloquea.
 * Este handler abre un socket TCP directo al ms-notificaciones y hace
 * pipe byte a byte mientras la conexión esté abierta.
 * Ruta que maneja: GET /api/notificaciones/stream/{usuarioId}
 * Auth: el token viene como query param porque EventSource del browser
 * no permite headers personalizados:
 *   GET /api/notificaciones/stream/12?token=eyJhbGci...
 */
public class SseProxyHandler implements HttpHandler {

    private final String msHost;
    private final int    msPort;
    private final RoleValidator roleValidator;

    public SseProxyHandler(String targetBase) {
        // Parsear "http://localhost:8084" → host="localhost", port=8084
        String cleaned = targetBase.replace("http://", "").replace("https://", "");
        String[] parts = cleaned.split(":");
        this.msHost = parts[0];
        this.msPort = parts.length > 1
                ? Integer.parseInt(parts[1].split("/")[0])
                : 80;

        // Reusar el mismo RoleValidator que ProxyHandler
        String usuariosUrl = RouterConfig.getRoutes()
                .getOrDefault("/api/usuarios", "http://localhost:8081");
        this.roleValidator = new RoleValidator(usuariosUrl);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        // Solo GET es válido para SSE
        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Solo GET está permitido en /stream");
            return;
        }

        // ── Extraer usuarioId del path ────────────────────────────────────
        // /api/notificaciones/stream/12 → "12"
        String path     = exchange.getRequestURI().getPath();
        String[] segs   = path.replaceAll("/$", "").split("/");
        String uidStr   = segs[segs.length - 1];

        if (!uidStr.matches("\\d+")) {
            sendError(exchange, 400, "ID de usuario inválido en la URL");
            return;
        }
        long usuarioId = Long.parseLong(uidStr);

        // ── Validar que el token/usuario coincida ─────────────────────────
        // El gateway ya valida el rol en SecurityConfig antes de llegar aquí,
        // pero verificamos además que el usuario solo pueda ver SU propio stream
        String headerUserId = exchange.getRequestHeaders().getFirst("X-Usuario-Id");
        if (headerUserId == null) {
            sendError(exchange, 401, "Se requiere X-Usuario-Id");
            return;
        }

        long headerUid = Long.parseLong(headerUserId);
        String rolReal = roleValidator.getRolReal(headerUid);

        // ADMIN puede ver el stream de cualquier usuario; otros solo el suyo
        boolean esAdmin = "ADMIN".equalsIgnoreCase(rolReal);
        if (!esAdmin && headerUid != usuarioId) {
            sendError(exchange, 403, "Solo puedes conectarte a tu propio stream");
            return;
        }

        // ── Extraer token del query param ─────────────────────────────────
        // EventSource no soporta headers → el token va como ?token=eyJ...
        String query = exchange.getRequestURI().getQuery();
        String token = extraerToken(query);

        // ── Log igual que ProxyHandler ────────────────────────────────────
        System.out.printf("[Gateway][SSE] GET %-35s → %s:%d (usuario=%d rol=%s)%n",
                path, msHost, msPort, usuarioId, rolReal);

        // ── Ruta downstream: /api/notificaciones/stream/12 → /notificaciones/stream/12
        String downstreamPath = path.replaceFirst("^/api", "");

        // ── Abrir socket TCP directo al ms-notificaciones ─────────────────
        try (Socket socket = new Socket(msHost, msPort)) {

            // Request HTTP/1.1 manual — enviamos el token como header Authorization
            String httpRequest = "GET " + downstreamPath + " HTTP/1.1\r\n"
                    + "Host: "          + msHost + ":" + msPort + "\r\n"
                    + "Accept: text/event-stream\r\n"
                    + "Cache-Control: no-cache\r\n"
                    + "Connection: keep-alive\r\n"
                    + "X-Usuario-Id: "  + usuarioId + "\r\n"
                    + "X-Rol: "         + rolReal   + "\r\n"
                    + (token != null ? "Authorization: Bearer " + token + "\r\n" : "")
                    + "\r\n";

            socket.getOutputStream().write(httpRequest.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            InputStream  msIn      = socket.getInputStream();
            OutputStream clientOut = exchange.getResponseBody();

            // Descartar los headers HTTP de la respuesta del ms
            // (el gateway enviará los suyos propios al frontend)
            skipHttpHeaders(msIn);

            // Configurar headers SSE hacia el frontend
            exchange.getResponseHeaders().set("Content-Type",       "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control",      "no-cache");
            exchange.getResponseHeaders().set("Connection",         "keep-alive");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, 0); // 0 = stream abierto sin longitud fija

            // Pipe: ms-notificaciones → gateway → frontend
            // Cada evento SSE que escribe el ms llega aquí y se reenvía inmediatamente
            byte[] buf = new byte[256];
            int n;
            while ((n = msIn.read(buf)) != -1) {
                clientOut.write(buf, 0, n);
                clientOut.flush();
            }

        } catch (IOException e) {
            // El frontend cerró la pestaña o perdió conexión — es comportamiento normal
            System.out.printf("[Gateway][SSE] Stream cerrado para usuario %d: %s%n",
                    usuarioId, e.getMessage());
        }
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Descarta los headers HTTP de la respuesta del microservicio.
     * Lee byte a byte hasta encontrar \r\n\r\n (fin de headers en HTTP/1.1).
     */
    private void skipHttpHeaders(InputStream in) throws IOException {
        int[] last = new int[4];
        int i = 0, b;
        while ((b = in.read()) != -1) {
            last[i % 4] = b;
            i++;
            // Detectar \r\n\r\n
            if (i >= 4
                    && last[(i-4) % 4] == '\r'
                    && last[(i-3) % 4] == '\n'
                    && last[(i-2) % 4] == '\r'
                    && last[(i-1) % 4] == '\n') {
                break;
            }
        }
    }

    /** Extrae el valor de ?token=... del query string */
    private String extraerToken(String query) {
        if (query == null || query.isBlank()) return null;
        for (String param : query.split("&")) {
            if (param.startsWith("token=")) {
                return param.substring(6);
            }
        }
        return null;
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Usuario-Id, Authorization");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        String safe = message.replace("\"", "'");
        byte[] body = ("{\"error\":\"" + safe + "\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
    }
}