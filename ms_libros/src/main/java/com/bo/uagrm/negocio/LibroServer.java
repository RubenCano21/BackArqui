package com.bo.uagrm.negocio;

import com.bo.uagrm.negocio.controller.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class LibroServer {

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8082), 0);

        server.createContext("/libros",           new LibroController());
        server.createContext("/libros/",          new LibroController());
        server.createContext("/autores",          new AutorController());
        server.createContext("/autores/",         new AutorController());
        server.createContext("/categorias",       new CategoriaController());
        server.createContext("/categorias/",      new CategoriaController());
        server.createContext("/editoriales",      new EditorialController());
        server.createContext("/editoriales/",     new EditorialController());

        server.createContext("/lista-espera", new ListaEsperaController());
        server.createContext("/lista-espera/", new ListaEsperaController());

        server.setExecutor(null);
        server.start();

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║      ms_libros iniciado en puerto 8082   ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  /libros        [GET, POST, PUT, DELETE] ║");
        System.out.println("║  /autores       [GET, POST, PUT, DELETE] ║");
        System.out.println("║  /categorias    [GET, POST, PUT, DELETE] ║");
        System.out.println("║  /editoriales   [GET, POST, PUT, DELETE] ║");
        System.out.println("║  /lista-espera  [GET, POST, PUT, DELETE] ║");
        System.out.println("╚══════════════════════════════════════════╝");
    }
}
