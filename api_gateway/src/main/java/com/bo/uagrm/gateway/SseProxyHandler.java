package com.bo.uagrm.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Handler especializado para Server-Sent Events (SSE).
 * Ruta: GET /api/notificaciones/stream/{usuarioId}?userId={id}
 *
 * El sistema NO usa JWT — la autenticación es por sesión en el backend.
 * EventSource no puede enviar headers, por eso el userId viaja como query param.
 * La validación consiste en verificar que el userId existe y tiene rol activo
 * consultando directamente a ms_usuario (mismo patrón que RoleValidator).
 */
public class SseProxyHandler implements HttpHandler {

    private final String msHost;
    private final int    msPort;
    private final HttpClient httpClient;
    private final String usuariosBaseUrl;

    public SseProxyHandler(String targetBase) {
        String cleaned = targetBase.replace("http://", "").replace("https://", "");
        String[] parts = cleaned.split(":");
        this.msHost = parts[0];
        this.msPort = parts.length > 1
                ? Integer.parseInt(parts[1].split("/")[0])
                : 80;

        // Para validar el userId contra ms_usuario (sin JWT)
        this.usuariosBaseUrl = RouterConfig.getRoutes()
                .getOrDefault("/api/usuarios", "http://localhost:8081")
                .replaceAll("/+$", "");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            sendError(exchange, 405, "Solo GET está permitido en /stream");
            return;
        }

        // ── Extraer usuarioId del path ────────────────────────────────────
        String path   = exchange.getRequestURI().getPath();
        String[] segs = path.replaceAll("/$", "").split("/");
        String uidStr = segs[segs.length - 1];

        if (!uidStr.matches("\\d+")) {
            sendError(exchange, 400, "ID de usuario inválido en la URL");
            return;
        }
        long usuarioId = Long.parseLong(uidStr);

        // ── Validar que el usuario existe en ms_usuario ───────────────────
        // Sin JWT, verificamos que el userId sea un usuario real y activo.
        // Reutiliza el mismo endpoint interno que usa RoleValidator.
        if (!usuarioExiste(usuarioId)) {
            sendError(exchange, 401, "Usuario no autorizado o no existe");
            return;
        }

        System.out.printf("[Gateway][SSE] GET %-35s → %s:%d (usuario=%d)%n",
                path, msHost, msPort, usuarioId);

        // ── Ruta downstream ───────────────────────────────────────────────
        String downstreamPath = path.replaceFirst("^/api", "");

        try (Socket socket = new Socket(msHost, msPort)) {

            // Sin JWT: enviamos X-Usuario-Id como identidad verificada
            String httpRequest = "GET " + downstreamPath + " HTTP/1.1\r\n"
                    + "Host: "         + msHost + ":" + msPort + "\r\n"
                    + "Accept: text/event-stream\r\n"
                    + "Cache-Control: no-cache\r\n"
                    + "Connection: keep-alive\r\n"
                    + "X-Usuario-Id: " + usuarioId + "\r\n"
                    + "\r\n";

            socket.getOutputStream().write(httpRequest.getBytes(StandardCharsets.UTF_8));
            socket.getOutputStream().flush();

            InputStream  msIn      = socket.getInputStream();
            OutputStream clientOut = exchange.getResponseBody();

            skipHttpHeaders(msIn);

            exchange.getResponseHeaders().set("Content-Type",       "text/event-stream; charset=UTF-8");
            exchange.getResponseHeaders().set("Cache-Control",      "no-cache");
            exchange.getResponseHeaders().set("Connection",         "keep-alive");
            exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
            addCorsHeaders(exchange);
            exchange.sendResponseHeaders(200, 0);

            byte[] buf = new byte[256];
            int n;
            while ((n = msIn.read(buf)) != -1) {
                clientOut.write(buf, 0, n);
                clientOut.flush();
            }

        } catch (IOException e) {
            System.out.printf("[Gateway][SSE] Stream cerrado para usuario %d: %s%n",
                    usuarioId, e.getMessage());
        }
    }

    /**
     * Verifica que el usuarioId existe en ms_usuario consultando sus roles.
     * Si el usuario no existe o ms_usuario no responde, retorna false.
     * Mismo patrón que RoleValidator.tieneRol().
     */
    private boolean usuarioExiste(long usuarioId) {
        try {
            String url = usuariosBaseUrl + "/usuarios/" + usuarioId + "/roles";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            boolean existe = response.statusCode() == 200;
            if (!existe) {
                System.err.printf("[Gateway][SSE] Usuario %d no encontrado en ms_usuario (status %d)%n",
                        usuarioId, response.statusCode());
            }
            return existe;
        } catch (Exception e) {
            System.err.printf("[Gateway][SSE] Error al verificar usuario %d: %s%n",
                    usuarioId, e.getMessage());
            return false;
        }
    }

    private void skipHttpHeaders(InputStream in) throws IOException {
        int[] last = new int[4];
        int i = 0, b;
        while ((b = in.read()) != -1) {
            last[i % 4] = b;
            i++;
            if (i >= 4
                    && last[(i-4) % 4] == '\r'
                    && last[(i-3) % 4] == '\n'
                    && last[(i-2) % 4] == '\r'
                    && last[(i-1) % 4] == '\n') break;
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers",
                "Content-Type, X-Usuario-Id, Authorization");
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        byte[] body = ("{\"error\":\"" + message.replace("\"", "'") + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        addCorsHeaders(exchange);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(body); }
    }
}