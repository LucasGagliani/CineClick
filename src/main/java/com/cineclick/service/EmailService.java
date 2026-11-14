package com.cineclick.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String remitente;

    public void enviar(String destino, String asunto, String texto) {
        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException("Falta configurar MAIL_USERNAME");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(texto, false);
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo preparar el email", e);
        }
    }

    public void enviarAdjunto(String destino, String asunto, String texto, byte[] archivo, String nombre) {
        if (remitente == null || remitente.isBlank()) {
            throw new IllegalStateException("Falta configurar MAIL_USERNAME");
        }
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente);
            helper.setTo(destino);
            helper.setSubject(asunto);
            helper.setText(texto, false);
            helper.addAttachment(nombre, new org.springframework.core.io.ByteArrayResource(archivo), "application/pdf");
            mailSender.send(mensaje);
        } catch (MessagingException e) {
            throw new IllegalStateException("No se pudo preparar el email", e);
        }
    }
}
