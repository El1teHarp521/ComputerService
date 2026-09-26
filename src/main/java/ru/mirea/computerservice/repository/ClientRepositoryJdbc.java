package ru.mirea.computerservice.repository; // Пакет

import ru.mirea.computerservice.exception.DatabaseException; // Импорт кастомного исключения для ошибок БД
import ru.mirea.computerservice.model.Client; // Импорт модели Client
import ru.mirea.computerservice.util.DatabaseManager; // Импорт менеджера подключений к БД

import java.sql.Connection; // Импорт Connection — соединение с БД
import java.sql.PreparedStatement; // Импорт PreparedStatement — подготовленный SQL-запрос
import java.sql.ResultSet; // Импорт ResultSet — результат SELECT-запроса
import java.sql.SQLException; // Импорт SQLException — исключение при работе с БД
import java.sql.Timestamp; // Импорт Timestamp — тип для даты/времени из БД
import java.util.ArrayList; // Импорт ArrayList — для создания списков
import java.util.List; // Импорт List
import java.util.Optional; // Импорт Optional

public class ClientRepositoryJdbc implements ClientRepository { // Класс реализует интерфейс ClientRepository

    @Override
    public Client save(Client client) { // Метод сохранения нового клиента
        String sql = "INSERT INTO clients (full_name, phone, email) VALUES (?, ?, ?) RETURNING id, registered_at"; // SQL-запрос на вставку с возвратом id и даты
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение с БД (автоматически закроется)
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement из SQL
            ps.setString(1, client.getFullName()); // Подставляем fullName в первый ?
            ps.setString(2, client.getPhone()); // Подставляем phone во второй ?
            ps.setString(3, client.getEmail()); // Подставляем email в третий ?
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос, получаем ResultSet
                if (rs.next()) { // Если есть результат (строка)
                    client.setId(rs.getInt("id")); // Устанавливаем сгенерированный id
                    client.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime()); // Устанавливаем дату регистрации
                }
            }
            return client; // Возвращаем обновленный объект
        } catch (SQLException e) { // Если произошла ошибка SQL
            throw new DatabaseException("Ошибка при создании клиента: " + e.getMessage(), e); // Оборачиваем в DatabaseException
        }
    }

    @Override
    public Optional<Client> findById(int id) { // Метод поиска клиента по id
        String sql = "SELECT * FROM clients WHERE id = ?"; // SQL-запрос на выборку по id
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setInt(1, id); // Подставляем id в первый ?
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                if (rs.next()) { // Если нашли строку
                    return Optional.of(mapRow(rs)); // Оборачиваем клиента в Optional и возвращаем
                }
            }
            return Optional.empty(); // Если не нашли — пустой Optional
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при поиске клиента: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public List<Client> findAll() { // Метод получения всех клиентов
        String sql = "SELECT * FROM clients ORDER BY id"; // SQL-запрос на выборку всех с сортировкой по id
        List<Client> clients = new ArrayList<>(); // Создаем пустой список
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql); // Создаем PreparedStatement
             ResultSet rs = ps.executeQuery()) { // Сразу выполняем запрос (executeQuery для SELECT)
            while (rs.next()) { // Пока есть строки
                clients.add(mapRow(rs)); // Добавляем клиента в список
            }
            return clients; // Возвращаем список
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при получении списка клиентов: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public void update(Client client) { // Метод обновления клиента
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?"; // SQL-запрос на обновление
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setString(1, client.getFullName()); // Подставляем fullName
            ps.setString(2, client.getPhone()); // Подставляем phone
            ps.setString(3, client.getEmail()); // Подставляем email
            ps.setInt(4, client.getId()); // Подставляем id в WHERE
            ps.executeUpdate(); // Выполняем UPDATE (executeUpdate для INSERT/UPDATE/DELETE)
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при обновлении клиента: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public void delete(int id) { // Метод удаления клиента
        String sql = "DELETE FROM clients WHERE id = ?"; // SQL-запрос на удаление
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setInt(1, id); // Подставляем id
            ps.executeUpdate(); // Выполняем DELETE
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при удалении клиента: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public boolean existsByPhone(String phone) { // Метод проверки существования телефона
        String sql = "SELECT 1 FROM clients WHERE phone = ?"; // SQL-запрос: вернуть 1, если есть
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setString(1, phone); // Подставляем телефон
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                return rs.next(); // true, если есть хотя бы одна строка
            }
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при проверке телефона: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public List<Client> searchByName(String namePart) { // Метод поиска клиентов по части имени
        String sql = "SELECT * FROM clients WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY id"; // SQL: поиск без учета регистра
        List<Client> clients = new ArrayList<>(); // Создаем пустой список
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setString(1, "%" + namePart + "%"); // Оборачиваем в % для поиска по подстроке
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                while (rs.next()) { // Пока есть строки
                    clients.add(mapRow(rs)); // Добавляем клиента
                }
            }
            return clients; // Возвращаем список
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при поиске клиента: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public boolean hasActiveRequests(int clientId) { // Метод проверки активных заявок клиента
        String sql = "SELECT 1 FROM repair_requests WHERE client_id = ? AND status NOT IN ('DONE', 'CANCELLED')"; // SQL: есть ли незавершенные заявки
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setInt(1, clientId); // Подставляем clientId
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                return rs.next(); // true, если есть активные заявки
            }
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при проверке заявок клиента: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    private Client mapRow(ResultSet rs) throws SQLException { // Вспомогательный метод: ResultSet -> Client
        Client client = new Client(); // Создаем пустого клиента
        client.setId(rs.getInt("id")); // Устанавливаем id
        client.setFullName(rs.getString("full_name")); // Устанавливаем fullName
        client.setPhone(rs.getString("phone")); // Устанавливаем phone
        client.setEmail(rs.getString("email")); // Устанавливаем email
        Timestamp ts = rs.getTimestamp("registered_at"); // Получаем Timestamp из БД
        if (ts != null) client.setRegisteredAt(ts.toLocalDateTime()); // Если не null — преобразуем в LocalDateTime
        return client; // Возвращаем заполненного клиента
    }
}