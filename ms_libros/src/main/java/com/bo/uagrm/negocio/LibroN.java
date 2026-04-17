package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.LibroDao;
import com.bo.uagrm.datos.entity.Libro;

import java.util.List;

public class LibroN {

    private final LibroDao libroDao = new LibroDao();

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
}
