package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter @Setter
public class Multa {
    private int        id;
    private int        prestamoId;
    private String     tipoCalculo;       // ESTUDIANTE | DOCENTE | INVESTIGADOR
    private int        diasRetraso;
    private int        diasGraciaAplicados;
    private BigDecimal montoPorDia;
    private BigDecimal montoCalculado;
    private String     estado;            // PENDIENTE | PAGADA
    private Date       fechaCreacion;
    private Date       fechaPago;

    public Multa() {}
}