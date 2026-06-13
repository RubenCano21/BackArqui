package com.bo.uagrm.negocio.controller;

import com.bo.uagrm.commons.JsonConfig;
import com.bo.uagrm.datos.entity.Notificacion;
import com.bo.uagrm.negocio.NotificacionN;
import com.bo.uagrm.negocio.SseManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Endpoints:
 *  POST  /notificaciones                  → registrar y enviar
 *  GET   /notificaciones                  → listar todas
 *  GET   /notificaciones/{id}             → buscar por id
 *  GET   /notificaciones/usuario/{uid}    → historial de un usuario
 *  POST  /notificaciones/reintentar       → reintenta PENDIENTES / FALLIDAS
 */
public class NotificacionController implements HttpHandler {

    private final NotificacionN notifN = new NotificacionN();
    private final ObjectMapper  mapper = JsonConfig.getMapper();

    // ── Enrutador principal ───────────────────────────────────────────────────
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        // Normalizar: quitar trailing slash y dividir
        String[] segments = ex.getRequestURI().getPath()
                .replaceAll("/$", "")
                .split("/");
        // segments[0]="" segments[1]="notificaciones" segments[2]=? segments[3]=?

        try {
            route(ex, method, segments);
        } catch (NumberFormatException e) {
            send(ex, 400, jsonError("ID inválido — debe ser un número entero"));
        } catch (IllegalArgumentException e) {
            send(ex, 400, jsonError(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, jsonError("Error interno del servidor"));
        }
    }

    /**
     * Tabla de rutas explícita.
     * segments: ["", "notificaciones", ...partes extra...]
     */
    private void route(HttpExchange ex, String method, String[] seg) throws Exception {

        int depth = seg.length; // cuántos segmentos hay

        // /notificaciones  (depth == 2)
        if (depth == 2) {
            switch (method) {
                case "GET"  -> handleListarTodas(ex);
                case "POST" -> handleEnviar(ex);
                default     -> methodNotAllowed(ex, "GET, POST");
            }
            return;
        }

        // /notificaciones/{algo}  (depth == 3)
        if (depth == 3) {
            String tercero = seg[2];

            // /notificaciones/reintentar
            if ("reintentar".equals(tercero)) {
                if ("POST".equals(method)) handleReintentar(ex);
                else methodNotAllowed(ex, "POST");
                return;
            }

            // /notificaciones/{id}  — debe ser numérico
            int id = parseId(tercero);
            if (method.equals("GET")) {
                handleBuscarPorId(ex, id);
            } else {
                methodNotAllowed(ex, "GET");
            }
            return;
        }

        // /notificaciones/stream/{uid}  → conexión SSE
        if (depth == 4 && "stream".equals(seg[2])) {
            int uid = parseId(seg[3]);
            if ("GET".equals(method)) handleStream(ex, uid);
            else methodNotAllowed(ex, "GET");
            return;
        }

        // /notificaciones/usuario/{uid}  (depth == 4)
        if (depth == 4 && "usuario".equals(seg[2])) {
            int uid = parseId(seg[3]);
            if (method.equals("GET")) {
                handleListarPorUsuario(ex, uid);
            } else {
                methodNotAllowed(ex, "GET");
            }
            return;
        }

        send(ex, 404, jsonError("Ruta no encontrada"));
    }

    // ── Handlers ─────────────────────────────────────────────────────────────

    /** GET /notificaciones */
    private void handleListarTodas(HttpExchange ex) throws Exception {
        List<Notificacion> lista = notifN.listarTodas();
        send(ex, 200, mapper.writeValueAsString(lista));
    }

    /**
     * POST /notificaciones
     * Body esperado:
     * {
     *   "usuarioId"    : 12,
     *   "libroId"      : 5,           (opcional — null si no aplica)
     *   "tipo"         : "LIBRO_DISPONIBLE",
     *   "mensaje"      : "El libro 'Cálculo' ya está disponible.",
     *   "emailDestino" : "juan@uagrm.bo"
     * }
     * Tipos válidos: LIBRO_DISPONIBLE | MULTA_GENERADA | PRESTAMO_POR_VENCER
     */
    private void handleEnviar(HttpExchange ex) throws Exception {
        Notificacion nueva = readBody(ex, Notificacion.class);
        validar(nueva);

        Notificacion resultado = notifN.procesarYEnviar(nueva);

        // 201 si fue enviada correctamente, 202 si quedó PENDIENTE/FALLIDA
        int status = "ENVIADA".equals(resultado.getEstado()) ? 201 : 202;
        send(ex, status, mapper.writeValueAsString(resultado));
    }

    /** GET /notificaciones/{id} */
    private void handleBuscarPorId(HttpExchange ex, int id) throws Exception {
        Notificacion n = notifN.buscarPorId(id);
        if (n == null) {
            send(ex, 404, jsonError("Notificación con ID " + id + " no encontrada"));
        } else {
            send(ex, 200, mapper.writeValueAsString(n));
        }
    }

    /** GET /notificaciones/usuario/{uid} */
    private void handleListarPorUsuario(HttpExchange ex, int usuarioId) throws Exception {
        List<Notificacion> lista = notifN.listarPorUsuario(usuarioId);
        send(ex, 200, mapper.writeValueAsString(lista));
    }

    /** POST /notificaciones/reintentar */
    private void handleReintentar(HttpExchange ex) throws Exception {
        int exitosos = notifN.reintentarPendientes();
        send(ex, 200, "{\"reintentadas\":" + exitosos + "}");
    }

    // ── Validación del body ───────────────────────────────────────────────────

    private static final java.util.Set<String> TIPOS_VALIDOS = java.util.Set.of(
            "LIBRO_DISPONIBLE", "MULTA_GENERADA", "PRESTAMO_POR_VENCER"
    );

    private void validar(Notificacion n) {
        if (n.getUsuarioId() <= 0)
            throw new IllegalArgumentException("El campo 'usuarioId' es obligatorio y debe ser > 0");

        if (n.getTipo() == null || n.getTipo().isBlank())
            throw new IllegalArgumentException("El campo 'tipo' es obligatorio");

        if (!TIPOS_VALIDOS.contains(n.getTipo()))
            throw new IllegalArgumentException(
                    "Tipo inválido '" + n.getTipo() + "'. Valores aceptados: " + TIPOS_VALIDOS);

        if (n.getMensaje() == null || n.getMensaje().isBlank())
            throw new IllegalArgumentException("El campo 'mensaje' es obligatorio");

        if (n.getEmailDestino() == null || n.getEmailDestino().isBlank())
            throw new IllegalArgumentException("El campo 'emailDestino' es obligatorio");

        if (!n.getEmailDestino().contains("@"))
            throw new IllegalArgumentException("El campo 'emailDestino' no tiene formato de correo válido");
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private int parseId(String s) {
        try {
            int id = Integer.parseInt(s);
            if (id <= 0) throw new IllegalArgumentException("El ID debe ser mayor a 0");
            return id;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("ID inválido: '" + s + "'");
        }
    }

    private void methodNotAllowed(HttpExchange ex, String allowed) throws IOException {
        ex.getResponseHeaders().set("Allow", allowed);
        send(ex, 405, jsonError("Método no permitido. Permitidos: " + allowed));
    }

    private void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private <T> T readBody(HttpExchange ex, Class<T> clazz) throws Exception {
        try (InputStream is = ex.getRequestBody()) {
            return mapper.readValue(is, clazz);
        }
    }

    private String jsonError(String msg) {
        return "{\"error\":\"" + msg.replace("\"", "'") + "\"}";
    }

    private void handleStream(HttpExchange ex, int usuarioId) throws Exception {
        // Headers SSE obligatorios
        ex.getResponseHeaders().set("Content-Type",  "text/event-stream; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.getResponseHeaders().set("Connection",    "keep-alive");
        // Necesario para que Tailwind/nginx no buffer la respuesta
        ex.getResponseHeaders().set("X-Accel-Buffering", "no");
        ex.sendResponseHeaders(200, 0); // 0 = longitud desconocida, stream abierto

        SseManager sse = SseManager.getInstance();
        sse.registrar(usuarioId, ex);

        // Enviar notificaciones que quedaron pendientes mientras estaba offline
        notifN.enviarPendientesAlConectar(usuarioId);

        // Mantener la conexión viva con un ping cada 20 segundos
        // Si el browser cierra la pestaña, el write() lanzará IOException
        // y SseManager.emitir() lo detecta y limpia la conexión
        try {
            while (true) {
                Thread.sleep(20_000);
                // Ping SSE — el browser lo ignora pero detecta la desconexión
                byte[] ping = ": ping\n\n".getBytes(StandardCharsets.UTF_8);
                ex.getResponseBody().write(ping);
                ex.getResponseBody().flush();
            }
        } catch (IOException | InterruptedException e) {
            sse.desregistrar(usuarioId);
        }
    }
}