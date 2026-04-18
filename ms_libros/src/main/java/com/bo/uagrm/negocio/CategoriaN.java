package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.CategoriaDao;
import com.bo.uagrm.datos.entity.Categoria;

import java.util.List;

public class CategoriaN {

    private final CategoriaDao categoriaDao = new CategoriaDao();

    public List<Categoria> listarCategorias() throws Exception {
        return categoriaDao.listarCategorias();
    }

    public Categoria buscarCategoriaPorId(int id) throws Exception {
        return categoriaDao.buscarCategoriaPorId(id);
    }

    public boolean registrarCategoria(Categoria categoria) throws Exception {
        return categoriaDao.registrarCategoria(categoria);
    }

    public boolean actualizarCategoria(Categoria categoria) throws Exception {
        return categoriaDao.actualizarCategoria(categoria);
    }

    public boolean eliminarCategoria(int id) throws Exception {
        return categoriaDao.eliminarCategoria(id);
    }
}

