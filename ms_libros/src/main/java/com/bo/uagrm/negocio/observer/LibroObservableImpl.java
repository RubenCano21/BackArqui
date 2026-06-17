package com.bo.uagrm.negocio.observer;

import java.util.ArrayList;
import java.util.List;

public class LibroObservableImpl implements LibroObservable{

    private final List<UsuarioObserver> observadores = new ArrayList<>();


    @Override
    public void agregarObservador(UsuarioObserver o) {
        observadores.add(o);
    }

    @Override
    public void removeObservador(UsuarioObserver o) {
        observadores.remove(o);
    }

    @Override
    public void notificarObservadores(int libroId, String tituloLibro) {
        for (UsuarioObserver observador : observadores) {
            observador.notificarDisponibilidad(libroId, tituloLibro);
        }
        observadores.clear();
    }
}
