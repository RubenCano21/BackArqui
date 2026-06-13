package com.bo.uagrm.negocio;

import com.bo.uagrm.config.AppConfig;
import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.util.Properties;

/**
 * Encapsula el envío de correos.
 * MAIL_MODE=MOCK → imprime en consola (desarrollo)
 * MAIL_MODE=REAL → envía via SMTP (producción)
 */
public class EmailSender {

    private final String mode;   // MOCK | REAL
    private final String host;
    private final int    port;
    private final String user;
    private final String pass;
    private final String from;

    public EmailSender() {
        this.mode = AppConfig.get("MAIL_MODE").toUpperCase();
        this.host = AppConfig.get("MAIL_HOST");
        this.port = AppConfig.getInt("MAIL_PORT", 587);
        this.user = AppConfig.get("MAIL_USER");
        this.pass = AppConfig.get("MAIL_PASS");
        this.from = AppConfig.get("MAIL_FROM");
    }

    /**
     * Envía (o simula) un correo.
     * @return true si el envío fue exitoso
     */
    public boolean enviar(String destinatario, String asunto, String cuerpo) {
        if ("MOCK".equals(mode)) {
            return enviarMock(destinatario, asunto, cuerpo);
        }
        return enviarReal(destinatario, asunto, cuerpo);
    }

    // ── Modo MOCK — imprime en consola sin tocar SMTP ─────────────────────────
    private boolean enviarMock(String destinatario, String asunto, String cuerpo) {
        System.out.println("┌─────────────────────────────────────────────────");
        System.out.println("│ [MOCK EMAIL]");
        System.out.println("│ Para    : " + destinatario);
        System.out.println("│ Asunto  : " + asunto);
        System.out.println("│ Mensaje : " + cuerpo);
        System.out.println("└─────────────────────────────────────────────────");
        return true;
    }

    // ── Modo REAL — SMTP con TLS ──────────────────────────────────────────────
    private boolean enviarReal(String destinatario, String asunto, String cuerpo) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            host);
        props.put("mail.smtp.port",            String.valueOf(port));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            msg.setSubject(asunto);
            msg.setText(cuerpo);
            Transport.send(msg);
            return true;
        } catch (MessagingException e) {
            System.err.println("[EMAIL ERROR] " + e.getMessage());
            return false;
        }
    }
}