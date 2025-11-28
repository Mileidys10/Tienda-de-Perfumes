package com.backend.perfumes.services;

import com.backend.perfumes.model.Order;
import com.backend.perfumes.model.OrderItem;
import com.backend.perfumes.model.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    public void sendOrderConfirmationEmail(Order order) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(order.getCustomerEmail());
            helper.setSubject("✅ Orden Confirmada - " + order.getOrderNumber());

            String htmlContent = buildOrderConfirmationHtml(order);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email de confirmación enviado a: " + order.getCustomerEmail());

        } catch (MessagingException e) {
            System.err.println("❌ Error enviando email de confirmación: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void sendOrderStatusUpdateEmail(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(order.getCustomerEmail());
            helper.setSubject("📦 Actualización de Orden - " + order.getOrderNumber());

            String htmlContent = buildOrderStatusUpdateHtml(order, oldStatus, newStatus);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            System.out.println("✅ Email de actualización enviado a: " + order.getCustomerEmail());

        } catch (MessagingException e) {
            System.err.println("❌ Error enviando email de actualización: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String buildOrderConfirmationHtml(Order order) {
        StringBuilder items = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            items.append(String.format(
                    "<tr>" +
                            "  <td style='padding: 10px; border-bottom: 1px solid #eee;'>%s</td>" +
                            "  <td style='padding: 10px; border-bottom: 1px solid #eee; text-align: center;'>%d</td>" +
                            "  <td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>$%.2f</td>" +
                            "  <td style='padding: 10px; border-bottom: 1px solid #eee; text-align: right;'>$%.2f</td>" +
                            "</tr>",
                    item.getPerfume().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotalPrice()
            ));
        }

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "  <meta charset='UTF-8'>" +
                        "</head>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                        "  <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                        "    <div style='background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                        "      <h1 style='color: white; margin: 0;'>¡Gracias por tu compra!</h1>" +
                        "    </div>" +
                        "    <div style='background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;'>" +
                        "      <h2 style='color: #667eea; margin-top: 0;'>Orden #%s</h2>" +
                        "      <p><strong>Estado:</strong> <span style='color: #28a745;'>%s</span></p>" +
                        "      <p><strong>Fecha:</strong> %s</p>" +
                        "      " +
                        "      <h3 style='border-bottom: 2px solid #667eea; padding-bottom: 10px;'>Productos</h3>" +
                        "      <table style='width: 100%%; border-collapse: collapse; margin: 20px 0;'>" +
                        "        <thead>" +
                        "          <tr style='background: #667eea; color: white;'>" +
                        "            <th style='padding: 10px; text-align: left;'>Producto</th>" +
                        "            <th style='padding: 10px; text-align: center;'>Cantidad</th>" +
                        "            <th style='padding: 10px; text-align: right;'>Precio Unit.</th>" +
                        "            <th style='padding: 10px; text-align: right;'>Total</th>" +
                        "          </tr>" +
                        "        </thead>" +
                        "        <tbody>" +
                        "          %s" +
                        "        </tbody>" +
                        "      </table>" +
                        "      " +
                        "      <div style='margin-top: 20px; padding: 20px; background: white; border-radius: 5px;'>" +
                        "        <table style='width: 100%%;'>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Subtotal:</td>" +
                        "            <td style='padding: 5px; text-align: right;'>$%.2f</td>" +
                        "          </tr>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Impuestos:</td>" +
                        "            <td style='padding: 5px; text-align: right;'>$%.2f</td>" +
                        "          </tr>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Envío:</td>" +
                        "            <td style='padding: 5px; text-align: right;'>$%.2f</td>" +
                        "          </tr>" +
                        "          <tr style='border-top: 2px solid #667eea;'>" +
                        "            <td style='padding: 10px; font-size: 18px; font-weight: bold;'>TOTAL:</td>" +
                        "            <td style='padding: 10px; text-align: right; font-size: 18px; font-weight: bold; color: #667eea;'>$%.2f</td>" +
                        "          </tr>" +
                        "        </table>" +
                        "      </div>" +
                        "      " +
                        "      <div style='margin-top: 30px; padding: 20px; background: #e8f4f8; border-left: 4px solid #667eea; border-radius: 5px;'>" +
                        "        <h4 style='margin-top: 0; color: #667eea;'>📍 Dirección de Envío</h4>" +
                        "        <p style='margin: 0;'>%s</p>" +
                        "      </div>" +
                        "      " +
                        "      <div style='margin-top: 20px; text-align: center; color: #666;'>" +
                        "        <p>Recibirás actualizaciones sobre tu orden a este correo.</p>" +
                        "        <p style='font-size: 12px;'>Si tienes alguna pregunta, contacta con nosotros.</p>" +
                        "      </div>" +
                        "    </div>" +
                        "  </div>" +
                        "</body>" +
                        "</html>",
                order.getOrderNumber(),
                getStatusInSpanish(order.getStatus()),
                order.getCreatedAt().format(FORMATTER),
                items.toString(),
                order.getSubtotal(),
                order.getTax(),
                order.getShipping(),
                order.getTotal(),
                order.getShippingAddress()
        );
    }


    private String buildOrderStatusUpdateHtml(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        String statusColor = getStatusColor(newStatus);
        String statusIcon = getStatusIcon(newStatus);
        String statusMessage = getStatusMessage(newStatus);

        return String.format(
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "  <meta charset='UTF-8'>" +
                        "</head>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                        "  <div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                        "    <div style='background: linear-gradient(135deg, %s 0%%, %s 100%%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;'>" +
                        "      <h1 style='color: white; margin: 0;'>%s Actualización de Orden</h1>" +
                        "    </div>" +
                        "    <div style='background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px;'>" +
                        "      <h2 style='color: %s; margin-top: 0;'>Orden #%s</h2>" +
                        "      " +
                        "      <div style='background: white; padding: 20px; border-radius: 10px; margin: 20px 0; border-left: 5px solid %s;'>" +
                        "        <p style='font-size: 18px; margin: 0;'><strong>Nuevo Estado:</strong></p>" +
                        "        <p style='font-size: 24px; color: %s; font-weight: bold; margin: 10px 0;'>%s %s</p>" +
                        "        <p style='color: #666; margin: 10px 0 0 0;'><small>Estado anterior: %s</small></p>" +
                        "      </div>" +
                        "      " +
                        "      <div style='background: #e8f4f8; padding: 20px; border-radius: 10px; margin: 20px 0;'>" +
                        "        <p style='margin: 0;'>%s</p>" +
                        "      </div>" +
                        "      " +
                        "      <div style='margin-top: 30px;'>" +
                        "        <h3 style='border-bottom: 2px solid %s; padding-bottom: 10px;'>Resumen de tu Orden</h3>" +
                        "        <table style='width: 100%%; margin: 10px 0;'>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Total de productos:</td>" +
                        "            <td style='padding: 5px; text-align: right; font-weight: bold;'>%d</td>" +
                        "          </tr>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Total a pagar:</td>" +
                        "            <td style='padding: 5px; text-align: right; font-weight: bold; color: %s;'>$%.2f</td>" +
                        "          </tr>" +
                        "          <tr>" +
                        "            <td style='padding: 5px;'>Fecha de orden:</td>" +
                        "            <td style='padding: 5px; text-align: right;'>%s</td>" +
                        "          </tr>" +
                        "        </table>" +
                        "      </div>" +
                        "      " +
                        "      <div style='margin-top: 30px; padding: 20px; background: #fff3cd; border-left: 4px solid #ffc107; border-radius: 5px;'>" +
                        "        <p style='margin: 0;'><strong>📍 Dirección de Envío:</strong></p>" +
                        "        <p style='margin: 5px 0 0 0;'>%s</p>" +
                        "      </div>" +
                        "      " +
                        "      <div style='margin-top: 30px; text-align: center; color: #666;'>" +
                        "        <p>Gracias por tu preferencia</p>" +
                        "        <p style='font-size: 12px;'>Este es un correo automático, por favor no responder.</p>" +
                        "      </div>" +
                        "    </div>" +
                        "  </div>" +
                        "</body>" +
                        "</html>",
                statusColor, statusColor,
                statusIcon,
                statusColor,
                order.getOrderNumber(),
                statusColor,
                statusColor,
                statusIcon,
                getStatusInSpanish(newStatus),
                getStatusInSpanish(oldStatus),
                statusMessage,
                statusColor,
                order.getItems().size(),
                statusColor,
                order.getTotal(),
                order.getCreatedAt().format(FORMATTER),
                order.getShippingAddress()
        );
    }


    private String getStatusColor(OrderStatus status) {
        switch (status) {
            case PENDING: return "#ffc107";
            case CONFIRMED: return "#28a745";
            case PREPARING: return "#17a2b8";
            case SHIPPED: return "#007bff";
            case DELIVERED: return "#28a745";
            case CANCELLED: return "#dc3545";
            case REFUNDED: return "#6c757d";
            default: return "#667eea";
        }
    }


    private String getStatusIcon(OrderStatus status) {
        switch (status) {
            case PENDING: return "⏳";
            case CONFIRMED: return "✅";
            case PREPARING: return "📦";
            case SHIPPED: return "🚚";
            case DELIVERED: return "✅";
            case CANCELLED: return "❌";
            case REFUNDED: return "💰";
            default: return "📋";
        }
    }


    private String getStatusMessage(OrderStatus status) {
        switch (status) {
            case PENDING:
                return "Tu orden está pendiente de confirmación. Te notificaremos cuando sea procesada.";
            case CONFIRMED:
                return "¡Tu orden ha sido confirmada! Comenzaremos a preparar tu pedido pronto.";
            case PREPARING:
                return "Tu orden está siendo preparada con cuidado. Pronto estará lista para envío.";
            case SHIPPED:
                return "¡Tu orden está en camino! Recibirás tu pedido pronto.";
            case DELIVERED:
                return "¡Tu orden ha sido entregada! Esperamos que disfrutes tu compra.";
            case CANCELLED:
                return "Tu orden ha sido cancelada. Si tienes dudas, contáctanos.";
            case REFUNDED:
                return "Tu orden ha sido reembolsada. El dinero será devuelto a tu método de pago original.";
            default:
                return "Tu orden ha sido actualizada.";
        }
    }


    private String getStatusInSpanish(OrderStatus status) {
        switch (status) {
            case PENDING: return "Pendiente";
            case CONFIRMED: return "Confirmada";
            case PREPARING: return "Preparando";
            case SHIPPED: return "Enviada";
            case DELIVERED: return "Entregada";
            case CANCELLED: return "Cancelada";
            case REFUNDED: return "Reembolsada";
            default: return status.name();
        }
    }



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