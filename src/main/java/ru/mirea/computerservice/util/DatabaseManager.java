package ru.mirea.computerservice.util; // Пакет, в котором находится класс

import ru.mirea.computerservice.exception.DatabaseException; // Импорт кастомного исключения для ошибок БД

import java.io.IOException; // Импорт IOException для ошибок ввода-вывода
import java.io.InputStream; // Импорт InputStream для чтения файла
import java.sql.Connection; // Импорт Connection — соединение с БД
import java.sql.DriverManager; // Импорт DriverManager — менеджер JDBC-драйверов
import java.sql.SQLException; // Импорт SQLException — исключение при работе с БД
import java.util.Properties; // Импорт Properties для чтения настроек из файла

public final class DatabaseManager { // Класс-утилита (final — нельзя наследовать)

    private static final Properties PROPERTIES = new Properties(); // Статическое поле для хранения настроек БД

    static { // Статический блок инициализации — выполняется один раз при загрузке класса
        try (InputStream in = DatabaseManager.class.getClassLoader() // Открываем поток чтения файла из classpath
                .getResourceAsStream("database.properties")) { // Ищем файл database.properties
            if (in == null) { // Если файл не найден
                throw new IOException("Файл database.properties не найден в classpath"); // Бросаем IOException
            }
            PROPERTIES.load(in); // Загружаем настройки из файла в объект Properties
        } catch (IOException e) { // Если произошла ошибка ввода-вывода
            throw new ExceptionInInitializerError("Не удалось загрузить настройки БД: " + e.getMessage()); // Бросаем ошибку инициализации
        }
    }

    private DatabaseManager() { // Приватный конструктор — запрет на создание экземпляров класса
    }

    public static Connection getConnection() { // Статический метод получения соединения с БД
        try {
            return DriverManager.getConnection( // Создаем соединение через DriverManager
                    PROPERTIES.getProperty("db.url"), // URL базы данных из настроек
                    PROPERTIES.getProperty("db.user"), // Имя пользователя из настроек
                    PROPERTIES.getProperty("db.password") // Пароль из настроек
            );
        } catch (SQLException e) { // Если произошла ошибка SQL
            throw new DatabaseException("Не удалось подключиться к базе данных: " + e.getMessage(), e); // Оборачиваем в DatabaseException
        }
    }
}