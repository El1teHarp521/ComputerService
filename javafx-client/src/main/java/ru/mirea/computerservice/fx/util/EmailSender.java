package ru.mirea.computerservice.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class EmailSender {

    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = EmailSender.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (in != null) {
                CONFIG.load(in);
            }
        } catch (Exception e) {
            System.err.println("Не удалось прочитать настройки SMTP: " + e.getMessage());
        }
    }

    private EmailSender() {
    }

    public static void sendConfirmationEmail(String toEmail, String clientName, String confirmationLink) throws MessagingException {
        String host = CONFIG.getProperty("mail.smtp.host", "smtp.mail.ru");
        String port = CONFIG.getProperty("mail.smtp.port", "465");
        String username = CONFIG.getProperty("mail.smtp.username");
        String password = CONFIG.getProperty("mail.smtp.password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            System.out.println("[ВНИМАНИЕ] Настройки SMTP не заполнены в database.properties!");
            System.out.println(">>> ТЕСТОВАЯ ССЫЛКА ПОДТВЕРЖДЕНИЯ: " + confirmationLink);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.ssl.enable", "true");
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
        message.setSubject("Подтверждение регистрации | Компьютерный сервис", StandardCharsets.UTF_8.name());

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 550px; padding: 25px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #2c3e50; margin-top: 0;">Здравствуйте, %s!</h2>
                <p style="color: #444; font-size: 15px; line-height: 1.5;">
                    Вы были успешно внесены в систему <b>«Компьютерный сервис»</b>.
                </p>
                <p style="color: #444; font-size: 15px;">
                    Для подтверждения вашего адреса электронной почты нажмите кнопку:
                </p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background-color: #2e7d32; color: #ffffff; padding: 12px 28px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 15px; display: inline-block;">
                        Подтвердить Email
                    </a>
                </div>
                <hr style="border: none; border-top: 1px solid #eee; margin: 25px 0;">
                <p style="color: #888; font-size: 12px;">
                    Прямая ссылка:<br><a href="%s" style="color: #3b7dd8;">%s</a>
                </p>
            </div>
            """.formatted(clientName, confirmationLink, confirmationLink, confirmationLink);

        message.setContent(htmlContent, "text/html; charset=UTF-8");
        Transport.send(message);
        System.out.println("Письмо с подтверждением успешно отправлено на: " + toEmail);
    }
}