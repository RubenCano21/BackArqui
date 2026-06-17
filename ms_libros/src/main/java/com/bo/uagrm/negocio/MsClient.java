package com.bo.uagrm.negocio;


import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

public class MsClient {

    private static final String MS_NOTIFICACIONES_URL;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    static {
        Properties props = new Properties();
        try (InputStream is = MsClient.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}

        MS_NOTIFICACIONES_URL = getConfig(
                "MS_NOTIFICACIONES_URL",
                props.getProperty("MS_NOTIFICACIONES_URL", "http://localhost:8084")
        ).replaceAll("/+$", "");
    }

    private static String getConfig(String envKey, String fallback) {
        String v = System.getenv(envKey);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    // ── Notificaciones ────────────────────────────────────────────────────────

    public static boolean notificarLibroDisponible(int usuarioId, int libroId,
                                                   String titulo, String email) {
        String body = String.format(
                "{\"usuarioId\":%d,\"libroId\":%d,\"tipo\":\"LIBRO_DISPONIBLE\"," +
                        "\"canal\":\"APP\"," +
                        "\"mensaje\":\"El libro '%s' ya está disponible en biblioteca.\"," +
                        "\"emailDestino\":\"%s\"}",
                usuarioId, libroId,
                titulo.replace("\"", "'"),
                email
        );

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(MS_NOTIFICACIONES_URL + "/notificaciones"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 201 || resp.statusCode() == 202) {
                System.out.printf("[MsClient] Notificación enviada → usuario %d, libro %d (HTTP %d)%n",
                        usuarioId, libroId, resp.statusCode());
                return true;
            } else {
                System.err.printf("[MsClient] ms_notificaciones respondió %d para usuario %d: %s%n",
                        resp.statusCode(), usuarioId, resp.body());
                return false;
            }

        } catch (java.net.ConnectException e) {
            System.err.printf("[MsClient] No se pudo conectar a ms_notificaciones (%s): %s%n",
                    MS_NOTIFICACIONES_URL, e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.printf("[MsClient] Error al notificar usuario %d: %s%n",
                    usuarioId, e.getMessage());
            return false;
        }
    }
}