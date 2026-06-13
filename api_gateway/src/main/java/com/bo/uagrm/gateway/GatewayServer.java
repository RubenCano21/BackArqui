package com.bo.uagrm.gateway;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

/**
 * Punto de entrada del API Gateway.
 * Levanta un servidor HTTP en el puerto configurado y registra
 * un ProxyHandler por cada ruta definida en RouterConfig.
 */
public class GatewayServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(getConfig());

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Handler especializado para SSE — no usa ProxyHandler normal
        String notifTarget = RouterConfig.getRoutes()
                .getOrDefault("/api/notificaciones", "http://localhost:8084");
        server.createContext("/api/notificaciones/stream", new SseProxyHandler(notifTarget));

        // Registrar cada ruta: prefijo del gateway → microservicio destino
        Map<String, String> routes = RouterConfig.getRoutes();
        for (Map.Entry<String, String> entry : routes.entrySet()) {
            String contextPath = entry.getKey();   // /api/usuarios
            String targetBase  = entry.getValue(); // http://localhost:8081
            server.createContext(contextPath, new ProxyHandler(contextPath, targetBase));
        }

        // Thread pool para manejar concurrencia
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║         API Gateway iniciado                 ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf ("║  Puerto : %-34d ║%n", port);
        System.out.println("╠══════════════════════════════════════════════╣");
        routes.forEach((path, target) ->
                System.out.printf("║  %-18s  →  %-20s ║%n", path, target));
        System.out.printf(  "║  %-18s  →  %-20s ║%n",
                "/api/notificaciones/stream", "SseProxyHandler");
        System.out.println("╚══════════════════════════════════════════════╝");
    }

    private static String getConfig() {
        Properties props = new Properties();
        try (InputStream is = GatewayServer.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}

        String env = System.getenv("GATEWAY_PORT");
        if (env != null && !env.isBlank()) return env;
        String sys = System.getProperty("GATEWAY_PORT");
        if (sys != null && !sys.isBlank()) return sys;
        // Intentar desde properties (clave en minúsculas, ej: gateway_port → GATEWAY_PORT no matchea, usamos el fallback del properties)
        String fromProps = props.getProperty("GATEWAY_PORT".toLowerCase().replace("_", "."));
        return (fromProps != null && !fromProps.isBlank()) ? fromProps : "8080";
    }
}

// El diseño debe ser totalmente independiente de cada CU,
// no importa si el CU depende de otros CU