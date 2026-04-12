package com.bo.uagrm.negocio.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UsuarioAuth {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String estado;

    public UsuarioAuth() {
    }

    public UsuarioAuth(Long id, String nombre, String apellido, String email, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.estado = estado;
    }

}

