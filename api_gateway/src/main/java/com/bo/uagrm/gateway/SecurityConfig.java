package com.bo.uagrm.gateway;

import java.util.List;

public class SecurityConfig {

    public static final List<SecurityRule> RULES = List.of(

            // ── Usuarios ──────────────────────────────────────────────────────
            new SecurityRule("/api/usuarios/\\d+", "DELETE", "ADMIN"),

            // ── Libros ────────────────────────────────────────────────────────
            new SecurityRule("/api/libros",        "POST",   "ADMIN"),
            new SecurityRule("/api/libros/\\d+",   "PUT",    "ADMIN"),
            new SecurityRule("/api/libros/\\d+",   "DELETE", "ADMIN"),

            // ── Préstamos ─────────────────────────────────────────────────────
            new SecurityRule("/api/prestamos",                         "GET",    "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/\\d+",                    "GET",    "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/estudiante/\\d+",         "GET",    "ESTUDIANTE"),
            new SecurityRule("/api/prestamos",                         "POST",   "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/\\d+/devolver",           "PUT",    "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/\\d+/items",              "POST",   "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/\\d+/items/\\d+",         "DELETE", "ESTUDIANTE"),
            new SecurityRule("/api/prestamos/\\d+",                    "DELETE", "ADMIN"),

            // ── Lista de espera ───────────────────────────────────────────────
            new SecurityRule("/api/lista-espera",  "POST",   "ESTUDIANTE"),
            new SecurityRule("/api/lista-espera",  "DELETE",  "ESTUDIANTE"),

            // ── Notificaciones ────────────────────────────────────────────────
            // IMPORTANTE: /api/notificaciones/stream/** NO va aquí
            // EventSource no puede enviar headers → el token va como ?token=
            // La validación la hace SseProxyHandler directamente
            new SecurityRule("/api/notificaciones/usuario/\\d+",  "GET",  "ESTUDIANTE"),
            new SecurityRule("/api/notificaciones/reintentar",     "POST", "ADMIN"),
            new SecurityRule("/api/notificaciones",                "GET",  "ADMIN"),
            new SecurityRule("/api/notificaciones",                "POST", "ADMIN")

            // ── /api/notificaciones/stream/{id} → sin regla aquí ─────────────
            // SseProxyHandler valida el token manualmente desde el query param
    );

    public static SecurityRule findRule(String path, String method) {
        return RULES.stream()
                .filter(rule -> rule.matches(path, method))
                .findFirst()
                .orElse(null);
    }
}