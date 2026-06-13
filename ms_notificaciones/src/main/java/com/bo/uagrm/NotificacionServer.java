package com.bo.uagrm;

import com.bo.uagrm.config.AppConfig;
import com.bo.uagrm.negocio.controller.NotificacionController;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class NotificacionServer {

    public static void main(String[] args) throws Exception {

        int port = AppConfig.getInt("SERVER_PORT", 8084);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/notificaciones",  new NotificacionController());
        server.createContext("/notificaciones/", new NotificacionController());

        // En NotificacionServer.java — agregar estas dos líneas
        server.createContext("/notificaciones/stream",  new NotificacionController());
        server.createContext("/notificaciones/stream/", new NotificacionController());

        server.setExecutor(null);
        server.start();

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║    ms_notificaciones iniciado en puerto " + port + " ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  POST   /notificaciones              → enviar        ║");
        System.out.println("║  GET    /notificaciones              → listar todas  ║");
        System.out.println("║  GET    /notificaciones/{id}         → buscar por id ║");
        System.out.println("║  GET    /notificaciones/usuario/{id} → por usuario   ║");
        System.out.println("║  POST   /notificaciones/reintentar   → retry fallidas║");
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }
}