package ru.mirea.computerservice.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import ru.mirea.computerservice.repository.ClientRepository;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class EmailVerificationServer {

    private static final int PORT = 8085;
    private static HttpServer server;
    private static ClientRepository repository;
    private static Runnable onEmailConfirmedCallback;

    public static synchronized void init(ClientRepository clientRepository, Runnable onConfirmedCallback) {
        repository = clientRepository;
        onEmailConfirmedCallback = onConfirmedCallback;
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new UniversalConfirmHandler());
            server.setExecutor(null);
            server.start();
            System.out.println(">>> Сервер подтверждения ссылок активен на порту " + PORT);
        } catch (IOException e) {
            System.err.println("Ошибка старта HTTP-сервера: " + e.getMessage());
        }
    }

    public static String createConfirmationUrl(String email) {
        init(repository, onEmailConfirmedCallback);
        return "http://localhost:" + PORT + "/confirm?email=" + email;
    }

    private static class UniversalConfirmHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String rawQuery = exchange.getRequestURI().getRawQuery();
            System.out.println(">>> Пришёл запрос в браузер: " + exchange.getRequestURI());

            String email = null;

            if (rawQuery != null && !rawQuery.isBlank()) {
                String decoded = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8).trim();

                if (decoded.contains("email=")) {
                    for (String part : decoded.split("&")) {
                        if (part.startsWith("email=")) {
                            email = part.substring(6);
                            break;
                        }
                    }
                } else if (decoded.contains("@")) {
                    email = decoded;
                }
            }

            String responseHtml;

            if (email != null && !email.isBlank()) {
                System.out.println(">>> УСПЕХ: Подтверждаем email: " + email);

                // 1. Обновляем в PostgreSQL
                if (repository != null) {
                    try {
                        repository.confirmEmailByAddress(email);
                    } catch (Exception e) {
                        System.err.println("Ошибка обновления в БД: " + e.getMessage());
                    }
                }

                // 2. Обновляем графическое окно JavaFX в реальном времени
                if (onEmailConfirmedCallback != null) {
                    Platform.runLater(onEmailConfirmedCallback);
                }

                responseHtml = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head>
                        <meta charset="UTF-8">
                        <title>Успешно</title>
                        <style>
                            body { font-family: 'Segoe UI', Tahoma, sans-serif; background-color: #f4f6f8; text-align: center; padding-top: 60px; }
                            .box { background: white; max-width: 480px; margin: 0 auto; padding: 40px; border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); }
                            h1 { color: #2e7d32; margin-bottom: 10px; }
                            p { color: #555; font-size: 16px; line-height: 1.5; }
                            .badge { background: #e8f5e9; color: #2e7d32; padding: 6px 12px; border-radius: 4px; font-weight: bold; }
                        </style>
                    </head>
                    <body>
                        <div class="box">
                            <h1>✔ Почта подтверждена!</h1>
                            <p>Адрес <span class="badge">%s</span> успешно подтверждён в базе данных Компьютерного сервиса.</p>
                            <p>В окне программы статус автоматически стал зелёным.</p>
                        </div>
                    </body>
                    </html>
                    """.formatted(email);
            } else {
                responseHtml = """
                    <!DOCTYPE html>
                    <html lang="ru">
                    <head><meta charset="UTF-8"><title>Внимание</title></head>
                    <body style="font-family: 'Segoe UI', Tahoma, sans-serif; text-align: center; padding-top: 60px;">
                        <h2 style="color: #c62828;">В адресе ссылки не указана почта!</h2>
                        <p>Пожалуйста, перейдите по ссылке прямо из письма, нажав на зеленую кнопку.</p>
                    </body>
                    </html>
                    """;
            }

            byte[] bytes = responseHtml.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}