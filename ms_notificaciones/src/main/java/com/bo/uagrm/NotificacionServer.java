package com.bo.uagrm;

import com.bo.uagrm.config.AppConfig;
import com.bo.uagrm.negocio.controller.NotificacionController;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class NotificacionServer {

    public static void main(String[] args) throws Exception {

        int port = AppConfig.getInt("SERVER_PORT", 8084);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // ── UNA sola instancia compartida ─────────────────────────────────────
        NotificacionController controller = new NotificacionController();
        server.createContext("/notificaciones", controller);

        // Pool de hilos para manejar streams SSE concurrentes sin bloquear el servidor
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║    ms_notificaciones iniciado en puerto " + port + " ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  POST   /notificaciones              → enviar        ║");
        System.out.println("║  GET    /notificaciones              → listar todas  ║");
        System.out.println("║  GET    /notificaciones/{id}         → buscar por id ║");
        System.out.println("║  GET    /notificaciones/usuario/{id} → por usuario   ║");
        System.out.println("║  GET    /notificaciones/stream/{id}  → SSE stream    ║");
        System.out.println("║  POST   /notificaciones/reintentar   → retry fallidas║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}