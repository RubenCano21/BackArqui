package com.bo.uagrm.gateway;

import com.sun.net.httpserver.HttpServer;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;

public class GatewayServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(getConfig());

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Map<String, String> routes = RouterConfig.getRoutes();
        String notifTarget = routes.getOrDefault("/api/notificaciones", "http://localhost:8084");
        SseProxyHandler sseHandler = new SseProxyHandler(notifTarget);

        for (Map.Entry<String, String> entry : routes.entrySet()) {
            String contextPath = entry.getKey();
            String targetBase  = entry.getValue();

            if ("/api/notificaciones".equals(contextPath)) {
                // Para notificaciones: enrutar /stream al SseProxyHandler,
                // el resto al ProxyHandler normal
                server.createContext(contextPath,
                        new NotificacionesRouter(contextPath, targetBase, sseHandler));
            } else {
                server.createContext(contextPath,
                        new ProxyHandler(contextPath, targetBase));
            }
        }

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
                "/api/notificaciones/stream/*", "SseProxyHandler");
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
        String fromProps = props.getProperty("GATEWAY_PORT".toLowerCase().replace("_", "."));
        return (fromProps != null && !fromProps.isBlank()) ? fromProps : "8080";
    }
}