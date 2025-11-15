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
        String subject = "Verifica tu cuenta";
        String body = "Haz clic en el siguiente enlace para activar tu cuenta:\n\n" + verificationUrl;






        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }


public void sendDeletionEmail (String to,String token){
    String verificationUrl = "http://localhost:8080/api/auth/verify?token=" + token;
    String subject = "Confirme la eliminacion de su cuenta";
    String body = "Haz clic en el siguiente enlace para eliminar su cuenta :\n\n" + verificationUrl;





    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject(subject);
    message.setText(body);

    mailSender.send(message);
}

    public void sendUpdateCode(String to, String code) {
        String subject = "Código de verificación para cambiar tu correo";
        String body = "Tu código de verificación es: " + code + "\n\n" +
                "Este código expira en 10 minutos. Si no lo solicitaste, ignora este correo.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
