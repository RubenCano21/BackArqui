package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrestamoItem {

    private int id;
    private int idPrestamo;
    private int idLibro;

    public PrestamoItem() {
    }
    public PrestamoItem(int id, int idPrestamo, int idLibro) {
        this.id = id;
        this.idPrestamo = idPrestamo;
        this.idLibro = idLibro;
    }
}
