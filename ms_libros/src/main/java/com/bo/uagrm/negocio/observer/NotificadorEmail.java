package com.bo.uagrm.negocio.observer;

import com.bo.uagrm.negocio.MsClient;
import lombok.Getter;

/**
 * Observador concreto del patrón Observer.
 * Cuando un libro se vuelve disponible, delega el envío de la
 * notificación a MsClient, que centraliza todas las llamadas HTTP
 * salientes de ms_libros (igual que en ms_prestamos).
 */
public class NotificadorEmail implements UsuarioObservador {

    @Getter
    private final int    usuarioId;
    private final String email;

    public NotificadorEmail(int usuarioId, String email) {
        this.usuarioId = usuarioId;
        this.email     = email;
    }

    @Override
    public void notificarDisponibilidad(int libroId, String tituloLibro) {
        MsClient.notificarLibroDisponible(usuarioId, libroId, tituloLibro, email);
    }
}