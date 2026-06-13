package com.bo.uagrm.datos.entity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class Notificacion {

    private int           id;
    private int           usuarioId;
    private Integer       libroId;       // nullable: hay notifs que no son de libro
    private String        tipo;          // LIBRO_DISPONIBLE | MULTA_GENERADA | PRESTAMO_POR_VENCER
    private String        mensaje;
    private String        canal;         // EMAIL | APP | SMS
    private String        estado;        // PENDIENTE | ENVIADA | FALLIDA
    private String        emailDestino;  // guardado en el momento del registro
    private LocalDateTime fechaCreada;
    private LocalDateTime fechaEnviada;  // null hasta que se envíe

    public Notificacion() {}

    // Constructor rápido para crear notificaciones desde el Observer
    public Notificacion(int usuarioId, Integer libroId, String tipo,
                        String mensaje, String emailDestino) {
        this.usuarioId    = usuarioId;
        this.libroId      = libroId;
        this.tipo         = tipo;
        this.mensaje      = mensaje;
        this.emailDestino = emailDestino;
        this.canal        = "EMAIL";
        this.estado       = "PENDIENTE";
    }
}