package com.bo.uagrm.gateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Validador de roles centralizado del API Gateway.
 *
 * Consulta al ms_usuario el endpoint GET /usuarios/{id}/roles
 * para verificar si un usuario posee un rol determinado.
 * De esta forma, NINGÚN microservicio necesita reimplementar esta lógica.
 *
 * Flujo:
 *   Cliente → Gateway (verifica rol aquí) → Microservicio destino
 */
public class RoleValidator {

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** URL base del ms_usuario, ej: http://localhost:8081 */
    private final String usuariosBaseUrl;

    public RoleValidator(String usuariosBaseUrl) {
        this.usuariosBaseUrl = usuariosBaseUrl.replaceAll("/+$", "");
    }

    /**
     * Verifica si el usuario con el ID dado tiene el rol especificado.
     *
     * @param usuarioId ID del usuario (viene del header X-Usuario-Id)
     * @param rol       Nombre del rol a verificar, ej: "ADMIN"
     * @return true si el usuario tiene el rol, false en caso contrario o error
     */
    public boolean tieneRol(Long usuarioId, String rol) {
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

            // La respuesta es un JSON array: ["ADMIN","USER"]
            // Búsqueda sin Jackson: "ADMIN" está dentro del array
            String body = response.body().toUpperCase();
            String rolUpper = "\"" + rol.toUpperCase() + "\"";
            return body.contains(rolUpper);

        } catch (Exception e) {
            System.err.printf("[Gateway][RoleValidator] Error al consultar roles del usuario %d: %s%n",
                    usuarioId, e.getMessage());
            return false;
        }
    }
}

