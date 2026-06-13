package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ListaEspera {
        private int id;
        private int idLibro;
        private int idUsuario;
        private String emailUsuario;
        private String estado;
}
