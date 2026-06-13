package com.bo.uagrm.negocio;

import com.bo.uagrm.commons.JsonConfig;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 * Cliente HTTP interno para validar recursos en otros microservicios.
 */
public class MsClient {

    private static final String MS_USUARIO_URL;
    private static final String MS_LIBROS_URL;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    static {
        Properties props = new Properties();
        try (InputStream is = MsClient.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (is != null) props.load(is);
        } catch (Exception ignored) {}

        MS_USUARIO_URL = getConfig("MS_USUARIO_URL", props.getProperty("MS_USUARIO_URL", "http://localhost:8081"));
        MS_LIBROS_URL  = getConfig("MS_LIBROS_URL",  props.getProperty("MS_LIBROS_URL",  "http://localhost:8082"));
    }

    private static String getConfig(String envKey, String fallback) {
        String v = System.getenv(envKey);
        return (v != null && !v.isBlank()) ? v : fallback;
    }

    // ── Validaciones públicas ─────────────────────────────────────────────────

    /** Verifica que el usuario exista en ms_usuario. */
    public static void validarUsuarioExiste(int usuarioId) throws Exception {
        boolean existe = ping(MS_USUARIO_URL + "/usuarios/" + usuarioId);
        if (!existe) {
            throw new IllegalArgumentException("El estudiante con ID " + usuarioId + " no existe");
        }
    }

    /** Verifica que el libro exista en ms_libros. */
    public static void validarLibroExiste(int libroId) throws Exception {
        boolean existe = ping(MS_LIBROS_URL + "/libros/" + libroId);
        if (!existe) {
            throw new IllegalArgumentException("El libro con ID " + libroId + " no existe");
        }
    }

    /** Obtiene el título de un libro desde ms_libros. Retorna "ID {id}" si no se puede obtener. */
    public static String getTituloLibro(int libroId) throws Exception {
        String urlStr = MS_LIBROS_URL + "/libros/" + libroId;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Accept", "application/json");
            int status = conn.getResponseCode();
            if (status != 200) return "ID " + libroId;
            byte[] body = conn.getInputStream().readAllBytes();
            JsonNode node = JsonConfig.getMapper().readTree(body);
            JsonNode titulo = node.get("titulo");
            return (titulo != null && !titulo.isNull()) ? titulo.asText() : "ID " + libroId;
        } catch (java.net.ConnectException e) {
            return "ID " + libroId;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Actualiza el nroEjemplar de un libro en ms_libros sumando delta (+1 devolucion, -1 prestamo).
     * Usa HttpClient porque HttpURLConnection no soporta el método PATCH.
     */
    public static void actualizarNroEjemplar(int libroId, int delta) throws Exception {
        String urlStr  = MS_LIBROS_URL + "/libros/" + libroId + "/ejemplares";
        String jsonBody = "{\"delta\":" + delta + "}";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 409) {
                throw new IllegalStateException("No hay ejemplares disponibles para prestar (stock = 0)");
            }
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Error al actualizar ejemplares en ms_libros (HTTP " + response.statusCode() + ")");
            }
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("No se pudo conectar a ms_libros para actualizar ejemplares.");
        }
    }

    /**
     * Obtiene el nroEjemplar (copias disponibles) de un libro desde ms_libros.
     * @throws IllegalArgumentException si el libro no existe
     */
    public static int getNroEjemplar(int libroId) throws Exception {
        String urlStr = MS_LIBROS_URL + "/libros/" + libroId;
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();
            if (status == 404) throw new IllegalArgumentException("El libro con ID " + libroId + " no existe");
            if (status != 200) throw new IllegalStateException("Error al consultar ms_libros (HTTP " + status + ")");

            byte[] body = conn.getInputStream().readAllBytes();
            JsonNode node = JsonConfig.getMapper().readTree(body);
            JsonNode nro = node.get("nroEjemplar");
            if (nro == null || nro.isNull()) return 0;
            return nro.asInt(0);

        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("No se pudo conectar a ms_libros. Verifique que el servicio esté activo.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ── HTTP GET → devuelve true si el servidor respondió 200 ────────────────

    private static boolean ping(String urlStr) throws Exception {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Accept", "application/json");
            return conn.getResponseCode() == 200;
        } catch (java.net.ConnectException e) {
            throw new IllegalStateException("No se pudo conectar al microservicio: " + urlStr +
                    ". Verifique que el servicio esté activo.");
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}

