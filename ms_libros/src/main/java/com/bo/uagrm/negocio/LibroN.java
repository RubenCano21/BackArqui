package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.LibroDao;
import com.bo.uagrm.datos.ListaEsperaDao;
import com.bo.uagrm.datos.entity.Libro;
import com.bo.uagrm.datos.entity.ListaEspera;
import com.bo.uagrm.negocio.observer.LibroObservable;
import com.bo.uagrm.negocio.observer.LibroObservableImpl;
import com.bo.uagrm.negocio.observer.NotificadorObserver;

import java.util.List;

public class LibroN {

    private final LibroDao libroDao = new LibroDao();
    private final ListaEsperaDao listaDao = new ListaEsperaDao();

    private final LibroObservable libroObservable = new LibroObservableImpl();

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
            if (libro != null) {
                dispararObserver(libroId, libro.getTitulo());
            }
        }
        return ok;
    }

    public boolean suscribirUsuario(int libroId, int usuarioId, String emailUsuario) throws Exception {
        if (libroDao.buscarLibroPorId(libroId) == null) return false;
        return listaDao.suscribir(libroId, usuarioId, emailUsuario);
    }

    public boolean cancelarSuscripcion(int libroId, int usuarioId) throws Exception {
            return listaDao.cancelarSuscripcion(libroId, usuarioId);
    }

    // Carga suscriptores desde BD, construye observadores y dispara
    private void dispararObserver(int libroId, String titulo) throws Exception {
        List<ListaEspera> espera = listaDao.listarActivosPorLibro(libroId);

        if (espera.isEmpty()) {
            System.out.printf("[Observer] Libro %d devuelto — sin suscriptores en espera%n", libroId);
            return;
        }

        System.out.printf("[Observer] Libro %d devuelto — notificando %d suscriptor(es)%n",
                libroId, espera.size());

        for (ListaEspera le : espera) {
            libroObservable.agregarObservador(new NotificadorObserver(le.getIdUsuario(), le.getEmailUsuario()));
        }

        libroObservable.notificarObservadores(libroId, titulo);
        listaDao.marcarNotificados(libroId);
    }
}
