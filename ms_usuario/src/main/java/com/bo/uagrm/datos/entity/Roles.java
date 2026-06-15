package com.bo.uagrm.datos.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
public class Roles {

    private int id;
    private String nombre;
    private int diasGracia;
    private BigDecimal montoDia;
    private boolean puedePrestar;

    public Roles(int id, String nombre, int diasGracia, BigDecimal montoDia, boolean puedePrestar) {
        this.id           = id;
        this.nombre       = nombre;
        this.diasGracia   = diasGracia;
        this.montoDia     = montoDia;
        this.puedePrestar = puedePrestar;
    }

}
