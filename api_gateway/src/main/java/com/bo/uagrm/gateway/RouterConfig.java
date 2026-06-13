package com.bo.uagrm.gateway;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tabla de rutas del API Gateway.
 * Cada entrada mapea un prefijo de path del gateway a la URL base del microservicio destino.
 * Configuración en application.properties:
 *   route.<nombre>.path   = prefijo del gateway  (ej: /api/usuarios)
 *   route.<nombre>.target = URL base del ms       (ej: <a href="http://localhost:8081">...</a>)
 */
public class RouterConfig {

    // Orden de inserción importante: rutas más específicas primero
    private static final Map<String, String> ROUTES = new LinkedHashMap<>();

    static {
        Properties props = new Properties();
        try (InputStream is = RouterConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}

        ROUTES.put(
                env("ROUTE_USUARIOS_PATH",  props.getProperty("route.usuarios.path",  "/api/usuarios")),
                env("ROUTE_USUARIOS_TARGET", props.getProperty("route.usuarios.target", "http://localhost:8081"))
        );
        ROUTES.put(
                env("ROUTE_LIBROS_PATH",  props.getProperty("route.libros.path",  "/api/libros")),
                env("ROUTE_LIBROS_TARGET", props.getProperty("route.libros.target", "http://localhost:8082"))
        );
        ROUTES.put(
                env("ROUTE_AUTORES_PATH",  props.getProperty("route.autores.path",  "/api/autores")),
                env("ROUTE_AUTORES_TARGET", props.getProperty("route.autores.target", "http://localhost:8082"))
        );
        ROUTES.put(
                env("ROUTE_CATEGORIAS_PATH",  props.getProperty("route.categorias.path",  "/api/categorias")),
                env("ROUTE_CATEGORIAS_TARGET", props.getProperty("route.categorias.target", "http://localhost:8082"))
        );
        ROUTES.put(
                env("ROUTE_EDITORIALES_PATH",  props.getProperty("route.editoriales.path",  "/api/editoriales")),
                env("ROUTE_EDITORIALES_TARGET", props.getProperty("route.editoriales.target", "http://localhost:8082"))
        );
        ROUTES.put(
                env("ROUTE_PRESTAMOS_PATH",  props.getProperty("route.prestamos.path",  "/api/prestamos")),
                env("ROUTE_PRESTAMOS_TARGET", props.getProperty("route.prestamos.target", "http://localhost:8083"))
        );
    }

    public static Map<String, String> getRoutes() {
        return ROUTES;
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return (v != null && !v.isBlank()) ? v : fallback;
    }
}

