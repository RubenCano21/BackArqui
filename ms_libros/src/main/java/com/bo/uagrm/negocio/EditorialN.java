package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.EditorialDao;
import com.bo.uagrm.datos.entity.Editorial;

import java.util.List;

public class EditorialN {

    private final EditorialDao editorialDao = new EditorialDao();

    public List<Editorial> listarEditoriales() throws Exception {
        return editorialDao.listarEditoriales();
    }

    public boolean registrarEditorial(Editorial editorial) throws Exception {
        return editorialDao.registrarEditorial(editorial);
    }

    public Editorial buscarEditorialPorId(int id) throws Exception {
        return editorialDao.buscarEditorialPorId(id);
    }

    public boolean actualizarEditorial(Editorial editorial) throws Exception {
        return editorialDao.actualizarEditorial(editorial);
    }

    public boolean eliminarEditorial(int id) throws Exception {
        return editorialDao.eliminarEditorial(id);
    }
}
