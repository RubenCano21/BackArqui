package com.bo.uagrm.negocio.observer;

public interface LibroObservable {

    void agregarObservador( UsuarioObserver o);
    void removeObservador( UsuarioObserver o);
    void notificarObservadores(int libroId, String tituloLibro);
}
