package com.bo.uagrm.negocio.observer;

import lombok.Getter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class NotificadorEmail implements UsuarioObservador{

    @Getter
    private final int usuarioId;
    private final String email;

    public NotificadorEmail(int usuarioId, String email) {
        this.usuarioId = usuarioId;
        this.email = email;
    }

    @Override
    public void notificarDisponibilidad(int libroId, String tituloLibro) {
        try {
            String body = String.format(
                    "{\"usuarioId\":%d,\"libroId\":%d,\"tipo\":\"LIBRO_DISPONIBLE\"," +
                            "\"mensaje\":\"El libro '%s' ya está disponible en biblioteca.\"," +
                            "\"emailDestino\":\"%s\"}",
                    usuarioId, libroId, tituloLibro, email
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8084/notificaciones"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            client.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            System.err.println("[Observer] Error al notificar: " + e.getMessage());
        }
    }

}
