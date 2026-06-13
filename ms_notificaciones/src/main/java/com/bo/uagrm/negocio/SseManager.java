package com.bo.uagrm.negocio;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene las conexiones SSE activas.
 * Clave: usuarioId → conexión HTTP abierta
 */
public class SseManager {

    // Singleton — una sola instancia en todo el servidor
    private static final SseManager INSTANCE = new SseManager();
    public static SseManager getInstance() { return INSTANCE; }

    // usuarioId → stream abierto hacia el browser
    private final Map<Integer, HttpExchange> conexiones = new ConcurrentHashMap<>();

    private SseManager() {}

    /** Registra la conexión cuando el frontend hace GET /notificaciones/stream/{uid} */
    public void registrar(int usuarioId, HttpExchange exchange) {
        conexiones.put(usuarioId, exchange);
        System.out.println("[SSE] Usuario " + usuarioId + " conectado. Activos: " + conexiones.size());
    }

    /** Empuja un evento al usuario si tiene conexión abierta */
    public boolean emitir(int usuarioId, String tipo, String mensaje) {
        HttpExchange exchange = conexiones.get(usuarioId);
        if (exchange == null) {
            System.out.println("[SSE] Usuario " + usuarioId + " no tiene conexión activa");
            return false;
        }

        try {
            // Formato SSE estándar que el browser entiende nativamente
            String evento = "event: " + tipo + "\n"
                    + "data: " + mensaje.replace("\n", " ") + "\n\n";

            byte[] bytes = evento.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseBody().write(bytes);
            exchange.getResponseBody().flush();
            return true;

        } catch (IOException e) {
            // El usuario cerró el browser o perdió conexión
            System.out.println("[SSE] Usuario " + usuarioId + " desconectado");
            conexiones.remove(usuarioId);
            return false;
        }
    }

    /** Llamado cuando el usuario cierra la pestaña */
    public void desregistrar(int usuarioId) {
        conexiones.remove(usuarioId);
        System.out.println("[SSE] Usuario " + usuarioId + " desconectado. Activos: " + conexiones.size());
    }

    public int totalConectados() {
        return conexiones.size();
    }
}