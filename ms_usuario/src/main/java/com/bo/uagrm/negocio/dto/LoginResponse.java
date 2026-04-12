package com.bo.uagrm.negocio.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LoginResponse {

    private boolean autenticado;
    private String mensaje;
    private UsuarioData usuario;
    private List<String> roles;

    public LoginResponse() {
        this.roles = new ArrayList<>();
    }

    public LoginResponse(boolean autenticado, String mensaje, UsuarioData usuario, List<String> roles) {
        this.autenticado = autenticado;
        this.mensaje = mensaje;
        this.usuario = usuario;
        this.roles = roles;
    }

    @Setter
    @Getter
    public static class UsuarioData {
        private Long id;
        private String nombre;
        private String apellido;
        private String email;
        private String estado;

        public UsuarioData() {
        }

        public UsuarioData(Long id, String nombre, String apellido, String email, String estado) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
            this.email = email;
            this.estado = estado;
        }

    }
}

