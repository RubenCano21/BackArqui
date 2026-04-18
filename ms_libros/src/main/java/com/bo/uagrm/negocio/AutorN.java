package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.AutorDao;
import com.bo.uagrm.datos.entity.Autor;

import java.util.List;

public class AutorN {

    private final AutorDao autorDao = new AutorDao();

    public List<Autor> listarAutores() throws Exception {
        return autorDao.listarAutores();
    }

    public Autor buscarAutorPorId(int id) throws Exception {
        return autorDao.buscarAutorPorId(id);
    }

    public boolean registrarAutor(Autor autor) throws Exception {
        return autorDao.registrarAutor(autor);
    }

    public boolean actualizarAutor(Autor autor) throws Exception {
        return autorDao.actualizarAutor(autor);
    }

    public boolean eliminarAutor(int id) throws Exception {
        return autorDao.eliminarAutor(id);
    }
}

