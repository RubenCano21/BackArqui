package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Libro {

    private int id;
    private int codigo;
    private String titulo;
    private int anio;
    private String edicion;
    private int nroEjemplar;      // columna en libros
    private Integer categoriaId;  // FK → categorias(id)
    private Integer editorialId;  // FK → editoriales(id)

    public Libro() {
    }
}
