package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Setter
@Getter
public class Prestamo {

    private int id;
    private Date fechaPrestamo;
    private Date fechaEntregaPrevista;
    private Date fechaDevolucionReal;
    private String estado;
    private int usuarioId;
    private List<PrestamoItem> items = new ArrayList<>();

    public Prestamo() {}

    public Prestamo(int id, Date fechaPrestamo,
                    Date fechaEntregaPrevista,
                    Date fechaDevolucionReal,
                    String estado, int usuarioId) {
        this.id = id;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaEntregaPrevista = fechaEntregaPrevista;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = estado;
        this.usuarioId = usuarioId;
    }
}
