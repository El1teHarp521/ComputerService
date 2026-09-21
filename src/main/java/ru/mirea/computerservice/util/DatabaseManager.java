package ru.mirea.computerservice.util;

import ru.mirea.computerservice.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseManager {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (in == null) {
                throw new IOException("Файл database.properties не найден в classpath");
            }
            PROPERTIES.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Не удалось загрузить настройки БД: " + e.getMessage());
        }
    }

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    PROPERTIES.getProperty("db.url"),
                    PROPERTIES.getProperty("db.user"),
                    PROPERTIES.getProperty("db.password")
            );
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к базе данных: " + e.getMessage(), e);
        }
    }
}
