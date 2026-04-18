package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
public class Prestamo {

    private int id;
    private Date fechaPrestamo;
    private Date fechaEntrega;
    private String estado;
    private int estudianteId;
    // Items del préstamo (se cargan en la capa de negocio)
    private List<PrestamoItem> items = new ArrayList<>();

    public Prestamo() {}

    public Prestamo(int id, Date fechaPrestamo, Date fechaEntrega, String estado, int estudianteId) {
        this.id = id;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
        this.estudianteId = estudianteId;
    }
}
