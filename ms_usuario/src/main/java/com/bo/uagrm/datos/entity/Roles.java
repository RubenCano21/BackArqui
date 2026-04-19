package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Roles {

    private int id;
    private String nombre;

    public Roles() {
    }
    public Roles(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

}
