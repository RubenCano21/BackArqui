package com.bo.uagrm.negocio;

import com.bo.uagrm.datos.NotificacionDao;
import com.bo.uagrm.datos.entity.Notificacion;

import java.util.List;
import java.util.Map;

/**
 * Capa de negocio del ms-notificaciones.
 * Responsabilidades:
 *  1. Recibir la solicitud de notificación desde otro microservicio
 *  2. Persistir la notificación como PENDIENTE
 *  3. Intentar el envío
 *  4. Actualizar el estado (ENVIADA / FALLIDA)
 */
// negocio/NotificacionN.java
public class NotificacionN {

    private final NotificacionDao dao        = new NotificacionDao();
    private final SseManager      sseManager = SseManager.getInstance();

    private static final Map<String, String> ASUNTOS = Map.of(
            "LIBRO_DISPONIBLE",    "El libro que esperabas ya está disponible 📚",
            "MULTA_GENERADA",      "Se ha generado una multa en tu cuenta ⚠️",
            "PRESTAMO_POR_VENCER", "Tu préstamo vence pronto 🔔"
    );

    public Notificacion procesarYEnviar(Notificacion notificacion) throws Exception {

        // 1. Persistir como PENDIENTE
        int id = dao.registrar(notificacion);
        if (id < 0) throw new Exception("No se pudo registrar la notificación en BD");
        notificacion.setId(id);

        // 2. Intentar push SSE al usuario si está conectado
        String titulo  = ASUNTOS.getOrDefault(notificacion.getTipo(), "Notificación");
        String payload = String.format(
                "{\"id\":%d,\"tipo\":\"%s\",\"titulo\":\"%s\",\"mensaje\":\"%s\"}",
                id,
                notificacion.getTipo(),
                titulo,
                notificacion.getMensaje().replace("\"", "'")
        );

        boolean enviado = sseManager.emitir(notificacion.getUsuarioId(),
                notificacion.getTipo(),
                payload);

        // 3. Actualizar estado
        if (enviado) {
            dao.marcarEnviada(id);
            notificacion.setEstado("ENVIADA");
        } else {
            // El usuario no está conectado — queda PENDIENTE para cuando entre
            notificacion.setEstado("PENDIENTE");
        }

        return notificacion;
    }

    // Al conectarse el usuario, le mandamos sus notificaciones pendientes
    public void enviarPendientesAlConectar(int usuarioId) throws Exception {
        List<Notificacion> pendientes = dao.listarPendientesPorUsuario(usuarioId);
        for (Notificacion n : pendientes) {
            String titulo  = ASUNTOS.getOrDefault(n.getTipo(), "Notificación");
            String payload = String.format(
                    "{\"id\":%d,\"tipo\":\"%s\",\"titulo\":\"%s\",\"mensaje\":\"%s\"}",
                    n.getId(), n.getTipo(), titulo,
                    n.getMensaje().replace("\"", "'")
            );
            boolean ok = sseManager.emitir(usuarioId, n.getTipo(), payload);
            if (ok) dao.marcarEnviada(n.getId());
        }
    }

    public List<Notificacion> listarTodas()                    throws Exception { return dao.listarTodas(); }
    public List<Notificacion> listarPorUsuario(int uid)        throws Exception { return dao.listarPorUsuario(uid); }
    public Notificacion       buscarPorId(int id)              throws Exception { return dao.buscarPorId(id); }
    public int                reintentarPendientes()           throws Exception {
        // Con SSE: reintenta solo a usuarios conectados ahora
        List<Notificacion> pendientes = dao.listarPendientes();
        int exitosos = 0;
        for (Notificacion n : pendientes) {
            String payload = String.format(
                    "{\"id\":%d,\"tipo\":\"%s\",\"mensaje\":\"%s\"}",
                    n.getId(), n.getTipo(), n.getMensaje().replace("\"", "'")
            );
            boolean ok = sseManager.emitir(n.getUsuarioId(), n.getTipo(), payload);
            if (ok) { dao.marcarEnviada(n.getId()); exitosos++; }
        }
        return exitosos;
    }
}