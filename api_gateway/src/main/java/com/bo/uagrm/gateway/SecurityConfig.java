package com.bo.uagrm.gateway;

import java.util.List;

/**
 * Configuración central de seguridad del API Gateway.
 * Aquí se declaran TODAS las rutas que requieren un rol específico.
 * Para proteger una nueva ruta en cualquier microservicio,
 * solo se agrega una entrada aquí — sin tocar el microservicio.
 * Formato: new SecurityRule(pathRegex, método HTTP, rolRequerido)
 * Ejemplos de rutas    Regex                                  Método  Rol
 * ──────────────────   ─────────────────────────────────────  ──────  ─────
 * /api/usuarios/{id}   /api/usuarios/\\d+                     DELETE  ADMIN
 * /api/libros/{id}     /api/libros/\\d+                       DELETE  ADMIN
 * /api/libros          /api/libros                            POST    ADMIN
 */
public class SecurityConfig {

    public static final List<SecurityRule> RULES = List.of(

        // ── Usuarios ──────────────────────────────────────────────────────
        // Solo ADMIN puede eliminar usuarios
        new SecurityRule("/api/usuarios/\\d+", "DELETE", "ADMIN"),

        // ── Libros ────────────────────────────────────────────────────────
        // Solo ADMIN puede crear, actualizar o eliminar libros
        new SecurityRule("/api/libros",        "POST",   "ADMIN"),
        new SecurityRule("/api/libros/\\d+",   "PUT",    "ADMIN"),
        new SecurityRule("/api/libros/\\d+",   "DELETE", "ADMIN"),

        // ── Préstamos ─────────────────────────────────────────────────────────
        // Ver préstamos → ADMIN ve todos, ESTUDIANTE solo los suyos
        new SecurityRule("/api/prestamos",                          "GET",    "ESTUDIANTE"),
        new SecurityRule("/api/prestamos/\\d+",                     "GET",    "ESTUDIANTE"),
        new SecurityRule("/api/prestamos/estudiante/\\d+",          "GET",    "ESTUDIANTE"),
        // Crear préstamo → ESTUDIANTE o ADMIN
        new SecurityRule("/api/prestamos",                          "POST",   "ESTUDIANTE"),
        // Devolver préstamo → ESTUDIANTE o ADMIN
        new SecurityRule("/api/prestamos/\\d+/devolver",            "PUT",    "ESTUDIANTE"),
        // Agregar/quitar libro de un préstamo → ESTUDIANTE o ADMIN
        new SecurityRule("/api/prestamos/\\d+/items",               "POST",   "ESTUDIANTE"),
        new SecurityRule("/api/prestamos/\\d+/items/\\d+",          "DELETE", "ESTUDIANTE"),
        // Cancelar préstamo → solo ADMIN
        new SecurityRule("/api/prestamos/\\d+",                     "DELETE", "ADMIN")

        // ── Agregar más reglas aquí según crezca el sistema ───────────────
    );

    /**
     * Devuelve la primera regla que coincida con el path y método dados,
     * o null si la ruta es pública.
     */
    public static SecurityRule findRule(String path, String method) {
        return RULES.stream()
                .filter(rule -> rule.matches(path, method))
                .findFirst()
                .orElse(null);
    }
}

