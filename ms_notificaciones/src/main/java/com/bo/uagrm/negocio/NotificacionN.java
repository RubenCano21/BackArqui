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
public class NotificacionN {

    private final NotificacionDao dao    = new NotificacionDao();
    private final EmailSender     mailer = new EmailSender();

    // ── Mensajes predefinidos por tipo ────────────────────────────────────────
    private static final Map<String, String> ASUNTOS = Map.of(
            "LIBRO_DISPONIBLE",      "📚 El libro que esperabas ya está disponible",
            "MULTA_GENERADA",        "⚠️  Se ha generado una multa en tu cuenta",
            "PRESTAMO_POR_VENCER",   "🔔 Tu préstamo vence pronto"
    );

    /**
     * Punto de entrada principal — llamado desde el controller.
     * Registra y envía en un solo paso.
     * @return la notificación con su estado final (ENVIADA o FALLIDA)
     */
    public Notificacion procesarYEnviar(Notificacion notificacion) throws Exception {

        // 1. Persistir como PENDIENTE
        int id = dao.registrar(notificacion);
        if (id < 0) throw new Exception("No se pudo registrar la notificación en BD");
        notificacion.setId(id);

        // 2. Construir el asunto según el tipo
        String asunto = ASUNTOS.getOrDefault(notificacion.getTipo(),
                "Notificación - Biblioteca UAGRM");

        // 3. Intentar envío
        boolean enviado = mailer.enviar(
                notificacion.getEmailDestino(),
                asunto,
                notificacion.getMensaje()
        );

        // 4. Actualizar estado en BD
        if (enviado) {
            dao.marcarEnviada(id);
            notificacion.setEstado("ENVIADA");
        } else {
            dao.marcarFallida(id);
            notificacion.setEstado("FALLIDA");
        }

        return notificacion;
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    public List<Notificacion> listarTodas() throws Exception {
        return dao.listarTodas();
    }

    public List<Notificacion> listarPorUsuario(int usuarioId) throws Exception {
        return dao.listarPorUsuario(usuarioId);
    }

    public Notificacion buscarPorId(int id) throws Exception {
        return dao.buscarPorId(id);
    }

    /**
     * Reintenta el envío de notificaciones que quedaron como PENDIENTE o FALLIDA.
     * Útil para llamar periódicamente si el SMTP estuvo caído.
     */
    public int reintentarPendientes() throws Exception {
        List<Notificacion> pendientes = dao.listarPendientes();
        int exitosos = 0;
        for (Notificacion n : pendientes) {
            String asunto = ASUNTOS.getOrDefault(n.getTipo(), "Notificación - Biblioteca UAGRM");
            boolean ok = mailer.enviar(n.getEmailDestino(), asunto, n.getMensaje());
            if (ok) {
                dao.marcarEnviada(n.getId());
                exitosos++;
            }
        }
        return exitosos;
    }
}