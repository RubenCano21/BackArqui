package com.bo.uagrm.negocio;

import com.bo.uagrm.negocio.controller.UsuarioController;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class UsuarioServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

        // EndPoints
        server.createContext("/usuarios", new UsuarioController());
        server.createContext("/usuarios/", new UsuarioController());

        server.setExecutor(null);
        server.start();

        System.out.println("Servidor iniciado en el puerto 8081");
        System.out.println("-> /usuarios [GET, POST]");
        System.out.println("-> /usuarios/{id} [GET, PUT, DELETE]");
        System.out.println("-> /usuarios/login [POST]");
    }
}
