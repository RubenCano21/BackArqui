package com.bo.uagrm.negocio.observer;

public interface UsuarioObservador {

    void notificarDisponibilidad( int libroId, String tituloLibro);
}
