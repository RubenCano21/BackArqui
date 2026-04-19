package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Setter
@Getter
public class Usuario {

    private Long id;
    private int ci;
    private String nombre;
    private String apellido;
    private String email;
    private int telefono;
    private Date fechaNac;
    private String genero;
    private String estado;

    public Usuario() {
    }
//    public Usuario(Long id, int ci, String nombre, String apellido, String email, int telefono, Date fechaNac, String genero, String estado) {
//        this.id = id;
//        this.ci = ci;
//        this.nombre = nombre;
//        this.apellido = apellido;
//        this.email = email;
//        this.telefono = telefono;
//        this.fechaNac = fechaNac;
//        this.genero = genero;
//        this.estado = estado;
//    }

}
