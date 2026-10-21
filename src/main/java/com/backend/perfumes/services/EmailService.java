package com.backend.perfumes.services;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final OrderEmailTemplateService orderEmailTemplateService;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    private static final String UTF_8 = "UTF-8";
    private static final String ORDER_CONFIRMATION_SUBJECT = "✅ Orden Confirmada - ";
    private static final String ORDER_UPDATE_SUBJECT = "📦 Actualización de Orden - ";
    private static final String VERIFICATION_SUBJECT = "Verifica tu cuenta - Perfumes App";
    private static final String DELETE_ACCOUNT_SUBJECT = "Confirmar eliminación de cuenta - Perfumes App";
    private static final String UPDATE_EMAIL_SUBJECT = "Código de verificación para cambiar tu correo - Perfumes App";

    private static final String CONFIRMATION_EMAIL_SENT_MSG = "✅ Email de confirmación enviado a: {} para orden: {}";
    private static final String CONFIRMATION_EMAIL_ERROR_MSG = "❌ Error enviando email de confirmación para orden {}: {}";
    private static final String UPDATE_EMAIL_SENT_MSG = "✅ Email de actualización de estado enviado a: {} para orden: {}";
    private static final String UPDATE_EMAIL_ERROR_MSG = "❌ Error enviando email de actualización para orden {}: {}";

    private static final String VERIFICATION_EMAIL_SENT_LOG = "✅ Email de verificación enviado a: {}";
    private static final String VERIFICATION_EMAIL_ERROR_LOG = "❌ Error enviando email de verificación a {}: {}";

    private static final String VERIFY_ENDPOINT = "/api/auth/verify?token=";
    private static final String DELETE_ACCOUNT_ENDPOINT = "/api/auth/delete-account?token=";

    private static final String VERIFICATION_EMAIL_BODY =
            "¡Bienvenido a Perfumes App!\n\n" +
            "Para activar tu cuenta, haz clic en el siguiente enlace:\n";

    private static final String VERIFICATION_EXPIRATION_MSG =
            "\n\nEste enlace expirará en 24 horas.\n\n";

    private static final String VERIFICATION_IGNORE_MSG =
            "Si no creaste esta cuenta, ignora este mensaje.";

    private static final String DELETE_ACCOUNT_BODY =
            "Has solicitado eliminar tu cuenta.\n\n" +
            "Para confirmar la eliminación, haz clic en el siguiente enlace:\n";

    private static final String DELETE_WARNING_MSG =
            "\n\nEsta acción no se puede deshacer.\n\n";

    private static final String DELETE_IGNORE_MSG =
            "Si no solicitaste esto, ignora este mensaje.";

    private static final String UPDATE_CODE_BODY =
            "Tu código de verificación es: ";

    private static final String UPDATE_CODE_EXPIRATION_MSG =
            "\n\nEste código expira en 10 minutos.\n\n";

    private static final String UPDATE_CODE_IGNORE_MSG =
            "Si no solicitaste cambiar tu correo, ignora este mensaje.";

    public EmailService(JavaMailSender mailSender, OrderEmailTemplateService orderEmailTemplateService) {
        this.mailSender = mailSender;
        this.orderEmailTemplateService = orderEmailTemplateService;
    }

    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);

            helper.setTo(order.getCustomerEmail());
            helper.setSubject(ORDER_CONFIRMATION_SUBJECT + order.getOrderNumber());

            String htmlContent = orderEmailTemplateService.buildOrderConfirmationTemplate(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info(CONFIRMATION_EMAIL_SENT_MSG, order.getCustomerEmail(), order.getOrderNumber());

        } catch (MessagingException e) {
            log.error(CONFIRMATION_EMAIL_ERROR_MSG, order.getOrderNumber(), e.getMessage());
        }
    }

    public void sendOrderStatusUpdateEmail(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, UTF_8);

            helper.setTo(order.getCustomerEmail());
            helper.setSubject(ORDER_UPDATE_SUBJECT + order.getOrderNumber());

            String htmlContent = orderEmailTemplateService.buildOrderStatusUpdateTemplate(order, oldStatus, newStatus);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info(UPDATE_EMAIL_SENT_MSG, order.getCustomerEmail(), order.getOrderNumber());

        } catch (MessagingException e) {
            log.error(UPDATE_EMAIL_ERROR_MSG, order.getOrderNumber(), e.getMessage());
        }
    }

    public void sendVerificationEmail(String to, String token) {
        try {
            String baseUrl = (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : "http://localhost:8080";
            String verificationUrl = baseUrl + VERIFY_ENDPOINT + token;
            String body = VERIFICATION_EMAIL_BODY +
                    verificationUrl +
                    VERIFICATION_EXPIRATION_MSG +
                    VERIFICATION_IGNORE_MSG;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(VERIFICATION_SUBJECT);
            message.setText(body);

            mailSender.send(message);
            log.info(VERIFICATION_EMAIL_SENT_LOG, to);
        } catch (Exception e) {
            log.error(VERIFICATION_EMAIL_ERROR_LOG, to, e.getMessage());
            throw new RuntimeException("Error enviando email de verificación: " + e.getMessage(), e);
        }
    }

    public void sendDeletionEmail(String to, String token) {
        try {
            String baseUrl = (serverUrl != null && !serverUrl.isBlank()) ? serverUrl : "http://localhost:8080";
            String deletionUrl = baseUrl + DELETE_ACCOUNT_ENDPOINT + token;

            String body = DELETE_ACCOUNT_BODY +
                    deletionUrl +
                    DELETE_WARNING_MSG +
                    DELETE_IGNORE_MSG;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(DELETE_ACCOUNT_SUBJECT);
            message.setText(body);

            mailSender.send(message);
            log.info("✅ Email de eliminación enviado a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error enviando email de eliminación a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando email de eliminación: " + e.getMessage(), e);
        }
    }

    public void sendUpdateCode(String to, String code) {
        try {
            String body = UPDATE_CODE_BODY + code +
                    UPDATE_CODE_EXPIRATION_MSG +
                    UPDATE_CODE_IGNORE_MSG;

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(UPDATE_EMAIL_SUBJECT);
            message.setText(body);

            mailSender.send(message);
            log.info("✅ Código de actualización enviado a: {}", to);
        } catch (Exception e) {
            log.error("❌ Error enviando código de actualización a {}: {}", to, e.getMessage());
            throw new RuntimeException("Error enviando código de actualización: " + e.getMessage(), e);
        }
    }
}
