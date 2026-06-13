package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.LibroDao;
import com.bo.uagrm.datos.ListaEsperaDao;
import com.bo.uagrm.datos.entity.Libro;
import com.bo.uagrm.datos.entity.ListaEspera;
import com.bo.uagrm.negocio.observer.LibroObservable;
import com.bo.uagrm.negocio.observer.NotificadorEmail;
import com.bo.uagrm.negocio.observer.UsuarioObservador;

import java.util.ArrayList;
import java.util.List;

public class LibroN implements LibroObservable {

    private final LibroDao libroDao = new LibroDao();
    private final ListaEsperaDao listaDao = new ListaEsperaDao();

    private final List<UsuarioObservador> observadores = new ArrayList<>();

    public List<Libro> listarLibros() throws Exception {
        return libroDao.listarLibros();
    }

    public Libro buscarLibroPorId(int id) throws Exception {
        return libroDao.buscarLibroPorId(id);
    }

    public boolean registrarLibro(Libro nuevo) throws Exception{
        return libroDao.registrarLibro(nuevo);
    }

    public boolean actualizarLibro(Libro nuevo) throws Exception{
        return libroDao.actualizarLibro(nuevo);
    }

    public boolean eliminarLibro(int id) throws Exception{
        return libroDao.eliminarLibro(id);
    }

    public boolean actualizarEjemplares(int libroId, int delta) throws Exception {
        boolean ok = libroDao.actualizarEjemplares(libroId, delta);

        if (ok && delta > 0) {
            Libro libro = libroDao.buscarLibroPorId(libroId);
            if (libro != null && libro.getNroEjemplar() == 1) {
                dispararObserver(libroId, libro.getTitulo());
            }
        }
        return ok;
    }

    public boolean suscribirUsuario(int libroId, int usuarioId) throws Exception {
        return libroDao.buscarLibroPorId(libroId) != null && listaDao.suscribir(libroId, usuarioId);
    }

    public boolean cancelarSuscripcion(int libroId, int usuarioId) throws Exception {
            return listaDao.cancelarSuscripcion(libroId, usuarioId);
    }

    @Override
    public void agregarObservador(UsuarioObservador o) {
        observadores.add(o);
    }

    @Override
    public void notificarObservadores(int libroId, String tituloLibro) {
        for (UsuarioObservador o : observadores) {
            o.notificarDisponibilidad(libroId, tituloLibro);
        }
        observadores.clear();
    }

    // Carga suscriptores desde BD, construye observadores y dispara
    private void dispararObserver(int libroId, String titulo) throws Exception {
        List<ListaEspera> espera = listaDao.listarActivosPorLibro(libroId);
        if (espera.isEmpty()) return;

        for (ListaEspera le : espera) {
            agregarObservador(new NotificadorEmail(le.getIdUsuario(), le.getEmailUsuario()));
        }

        notificarObservadores(libroId, titulo);
        listaDao.marcarNotificados(libroId);
    }
}
