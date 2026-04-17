package com.bo.uagrm.negocio;

import com.bo.uagrm.negocio.controller.LibroController;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class LibroServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

        server.createContext("/libros", new LibroController());
        server.createContext("/libros/", new LibroController());

        server.setExecutor(null);
        server.start();

        System.out.println("Servidor iniciado en el puerto 8082");
        System.out.println("-> /libros [GET, POST]");
        System.out.println("-> /libros/{id} [GET, PUT, DELETE]");
    }
}
