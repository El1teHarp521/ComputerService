package ru.mirea.computerservice.model;

import java.time.LocalDateTime;
import java.util.Objects;

// Класс Client представляет собой модель данных (сущность) для клиента.
public class Client {

    // Приватные поля класса. Инкапсуляция (принцип ООП).
    private int id; // Уникальный идентификатор клиента
    private String fullName; // ФИО клиента
    private String phone; // Телефон
    private String email; // Email
    private LocalDateTime registeredAt; // Дата и время регистрации

    // Пустой конструктор. Нужен для создания объекта без начальных данных (например, при чтении из БД).
    public Client() {
    }

    // Конструктор с параметрами. Нужен для создания нового клиента.
    public Client(String fullName, String phone, String email) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    // Геттеры и сеттеры. Обеспечивают доступ к приватным полям.
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    // Переопределяем equals. Два клиента равны, если у них одинаковый id.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // Проверка на ссылочное равенство
        if (!(o instanceof Client)) return false; // Проверка типа
        Client client = (Client) o; // Приведение типа
        return id == client.id; // Сравнение по id
    }

    // Переопределяем hashCode. Если equals сравнивает по id, то hashCode должен использовать id.
    @Override
    public int hashCode() {
        return Objects.hash(id); // Генерируем хэш на основе id
    }

    // Переопределяем toString для красивого вывода информации о клиенте.
    @Override
    public String toString() {
        return String.format("[%d] %-25s тел: %-15s email: %s",
                id, fullName, phone, email == null ? "-" : email); // Форматированный вывод
    }
}