package com.bo.uagrm.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Validador de roles centralizado del API Gateway.
 * Consulta al ms_usuario el endpoint GET /usuarios/{id}/roles
 * para verificar si un usuario posee un rol determinado.
 * De esta forma, NINGÚN microservicio necesita reimplementar esta lógica.
 * Flujo:
 *   Cliente → Gateway (verifica rol aquí) → Microservicio destino
 */
public class RoleValidator {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** URL base del ms_usuario, ej: <a href="http://localhost:8081">...</a> */
    private final String usuariosBaseUrl;

    public RoleValidator(String usuariosBaseUrl) {
        this.usuariosBaseUrl = usuariosBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Verifica si el usuario tiene el rol especificado.
     * Jerarquía: ADMIN satisface cualquier regla (USER, ESTUDIANTE, etc.)
     *
     * @param usuarioId ID del usuario (viene del header X-Usuario-Id)
     * @param rolRequerido Rol mínimo necesario, ej: "USER" o "ADMIN"
     * @return true si el usuario tiene el rol requerido o un rol superior
     */
    public boolean tieneRol(Long usuarioId, String rolRequerido) {
        try {
            String url = usuariosBaseUrl + "/usuarios/" + usuarioId + "/roles";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = CLIENT.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.printf("[Gateway][RoleValidator] ms_usuario respondió %d para usuario %d%n",
                        response.statusCode(), usuarioId);
                return false;
            }

            // La respuesta es un JSON array: ["ADMIN"] o ["ESTUDIANTE"] etc.
            String body    = response.body().toUpperCase();
            String rolUp   = rolRequerido.toUpperCase();

            // ADMIN tiene acceso a TODO (jerarquía más alta)
            if (body.contains("\"ADMIN\"")) return true;

            // Para los demás, verificar el rol exacto (solo ESTUDIANTE en este sistema)
            return body.contains("\"" + rolUp + "\"");

        } catch (Exception e) {
            System.err.printf("[Gateway][RoleValidator] Error al consultar roles del usuario %d: %s%n",
                    usuarioId, e.getMessage());
            return false;
        }
    }

    /**
     * Devuelve el rol más alto del usuario: "ADMIN" si tiene ADMIN, sino "ESTUDIANTE" u otro.
     * Usado para inyectar X-Rol en el header downstream.
     */
    public String getRolReal(Long usuarioId) {
        try {
            String url = usuariosBaseUrl + "/usuarios/" + usuarioId + "/roles";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) return "ESTUDIANTE";
            String body = response.body().toUpperCase();
            if (body.contains("\"ADMIN\""))      return "ADMIN";
            if (body.contains("\"ESTUDIANTE\"")) return "ESTUDIANTE";
            return "ESTUDIANTE";  // fallback seguro — no hay rol USER en el sistema
        } catch (Exception e) {
            return "ESTUDIANTE";
        }
    }
}

