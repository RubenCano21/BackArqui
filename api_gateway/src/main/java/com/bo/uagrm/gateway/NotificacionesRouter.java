package com.bo.uagrm.gateway;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Router interno para /api/notificaciones.
 * Decide qué handler usar según el path:
 *   /api/notificaciones/stream/{id}  → SseProxyHandler  (stream infinito)
 *   cualquier otra ruta              → ProxyHandler normal
 * Esto resuelve el problema de que HttpServer.createContext() no garantiza
 * prioridad por longitud de path — el contexto /api/notificaciones captura
 * todo, incluyendo /stream, así que delegamos internamente.
 */
public class NotificacionesRouter implements HttpHandler {

    private final ProxyHandler   proxyHandler;
    private final SseProxyHandler sseHandler;

    public NotificacionesRouter(String contextPath, String targetBase,
                                SseProxyHandler sseHandler) {
        this.proxyHandler = new ProxyHandler(contextPath, targetBase);
        this.sseHandler   = sseHandler;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        // /api/notificaciones/stream/12 → SseProxyHandler
        if (path.matches("/api/notificaciones/stream/\\d+")) {
            System.out.printf("[Gateway][SSE] Redirigiendo %s → SseProxyHandler%n", path);
            sseHandler.handle(exchange);
        } else {
            // Todo lo demás: /api/notificaciones, /api/notificaciones/{id},
            // /api/notificaciones/usuario/{id}, /api/notificaciones/reintentar
            proxyHandler.handle(exchange);
        }
    }
}