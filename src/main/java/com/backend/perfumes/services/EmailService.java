package com.backend.perfumes.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;
        String subject = "Verifica tu cuenta - Perfumes App";
        String body = "¡Bienvenido a Perfumes App!\n\n" +
                "Para activar tu cuenta, haz clic en el siguiente enlace:\n" +
                verificationUrl + "\n\n" +
                "Este enlace expirará en 24 horas.\n\n" +
                "Si no creaste esta cuenta, ignora este mensaje.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }

    public void sendDeletionEmail(String to, String token) {
        String deletionUrl = "http://localhost:8080/api/auth/delete-account?token=" + token;
        String subject = "Confirmar eliminación de cuenta - Perfumes App";
        String body = "Has solicitado eliminar tu cuenta.\n\n" +
                "Para confirmar la eliminación, haz clic en el siguiente enlace:\n" +
                deletionUrl + "\n\n" +
                "Esta acción no se puede deshacer.\n\n" +
                "Si no solicitaste esto, ignora este mensaje.";

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