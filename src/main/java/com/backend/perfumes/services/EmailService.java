package com.backend.perfumes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String code) {
        String subject = "Código de verificación de  correo - Perfumes App";
        String body = "Tu código de verificación es: " + code + "\n\n" +
                "Este código expira en 10 minutos.\n\n" +
                "Si no solicitaste verificar tu correo, ignora este mensaje.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void sendDeletionEmail(String to, String code) {
        String subject = "Código de verificación para eliminar tu cuenta - Perfumes App";
        String body = "Tu código de verificación es: " + code + "\n\n" +
                "Este código expira en 10 minutos.\n\n" +
                "Si no solicitaste eliminar tu correo, ignora este mensaje.";


        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void sendUpdateCode(String to, String code) {
        String subject = "Código de verificación para cambiar tu correo - Perfumes App";
        String body = "Tu código de verificación es: " + code + "\n\n" +
                "Este código expira en 10 minutos.\n\n" +
                "Si no solicitaste cambiar tu correo, ignora este mensaje.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}