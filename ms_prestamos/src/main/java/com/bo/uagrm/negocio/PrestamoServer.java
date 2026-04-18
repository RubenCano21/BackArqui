package com.bo.uagrm.negocio;

import com.bo.uagrm.negocio.controller.PrestamoController;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class PrestamoServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8083), 0);

        server.createContext("/prestamos", new PrestamoController());

        server.setExecutor(null);
        server.start();

        System.out.println("=== ms_prestamos iniciado en puerto 8083 ===");
        System.out.println("GET    /prestamos                        → listar todos");
        System.out.println("GET    /prestamos/{id}                   → buscar por ID");
        System.out.println("GET    /prestamos/estudiante/{id}        → préstamos de un estudiante");
        System.out.println("POST   /prestamos                        → crear préstamo con items");
        System.out.println("PUT    /prestamos/{id}/devolver          → marcar como DEVUELTO");
        System.out.println("POST   /prestamos/{id}/items             → agregar libro a préstamo");
        System.out.println("DELETE /prestamos/{id}/items/{libroId}   → quitar libro de préstamo");
        System.out.println("DELETE /prestamos/{id}                   → cancelar préstamo");
    }
}

