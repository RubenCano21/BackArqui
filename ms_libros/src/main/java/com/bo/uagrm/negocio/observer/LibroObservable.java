package com.bo.uagrm.negocio.observer;

public interface LibroObservable {

    void agregarObservador( UsuarioObservador o);
    void notificarObservadores(int libroId, String tituloLibro);
}
