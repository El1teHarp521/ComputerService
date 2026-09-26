package ru.mirea.computerservice.repository; // Пакет, в котором находится класс

import ru.mirea.computerservice.exception.DatabaseException; // Импорт кастомного исключения для ошибок БД
import ru.mirea.computerservice.model.Priority; // Импорт enum Priority
import ru.mirea.computerservice.model.RepairRequest; // Импорт модели RepairRequest
import ru.mirea.computerservice.model.RequestStatus; // Импорт enum RequestStatus
import ru.mirea.computerservice.util.DatabaseManager; // Импорт менеджера подключений к БД

import java.sql.Connection; // Импорт Connection — соединение с БД
import java.sql.PreparedStatement; // Импорт PreparedStatement — подготовленный SQL-запрос
import java.sql.ResultSet; // Импорт ResultSet — результат SELECT-запроса
import java.sql.SQLException; // Импорт SQLException — исключение при работе с БД
import java.sql.Timestamp; // Импорт Timestamp — тип для даты/времени из БД
import java.sql.Types; // Импорт Types — константы типов SQL (нужен для setNull)
import java.util.ArrayList; // Импорт ArrayList — для создания списков
import java.util.List; // Импорт List
import java.util.Optional; // Импорт Optional

public class RepairRequestRepositoryJdbc implements RepairRequestRepository { // Класс реализует интерфейс RepairRequestRepository

    private static final String SELECT_BASE = // Константа — базовый SQL-запрос для выборки заявок
            "SELECT r.*, c.full_name AS client_name FROM repair_requests r " + // Выбираем все поля заявки и имя клиента
                    "JOIN clients c ON c.id = r.client_id "; // JOIN с таблицей clients по client_id

    @Override
    public RepairRequest save(RepairRequest request) { // Метод сохранения новой заявки
        String sql = "INSERT INTO repair_requests (client_id, device_type, problem_description, status, priority, cost) " + // SQL-запрос на вставку
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at"; // Возвращаем id и created_at
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение (автоматически закроется)
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement из SQL
            ps.setInt(1, request.getClientId()); // Подставляем client_id в первый ?
            ps.setString(2, request.getDeviceType()); // Подставляем device_type во второй ?
            ps.setString(3, request.getProblemDescription()); // Подставляем problem_description в третий ?
            ps.setString(4, request.getStatus().name()); // Подставляем имя enum (например, "NEW") в четвертый ?
            ps.setString(5, request.getPriority().name()); // Подставляем имя enum (например, "HIGH") в пятый ?
            ps.setBigDecimal(6, request.getCost()); // Подставляем cost в шестой ?
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос, получаем ResultSet
                if (rs.next()) { // Если есть результат (строка)
                    request.setId(rs.getInt("id")); // Устанавливаем сгенерированный id
                    request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime()); // Устанавливаем дату создания
                }
            }
            return request; // Возвращаем обновленный объект
        } catch (SQLException e) { // Если произошла ошибка SQL
            throw new DatabaseException("Ошибка при создании заявки: " + e.getMessage(), e); // Оборачиваем в DatabaseException
        }
    }

    @Override
    public Optional<RepairRequest> findById(int id) { // Метод поиска заявки по id
        String sql = SELECT_BASE + "WHERE r.id = ?"; // Базовый запрос + условие WHERE
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setInt(1, id); // Подставляем id в первый ?
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                if (rs.next()) { // Если нашли строку
                    return Optional.of(mapRow(rs)); // Оборачиваем заявку в Optional и возвращаем
                }
            }
            return Optional.empty(); // Если не нашли — пустой Optional
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при поиске заявки: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public List<RepairRequest> findAll() { // Метод получения всех заявок
        String sql = SELECT_BASE + "ORDER BY r.id"; // Базовый запрос + сортировка по id
        List<RepairRequest> result = new ArrayList<>(); // Создаем пустой список
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql); // Создаем PreparedStatement
             ResultSet rs = ps.executeQuery()) { // Сразу выполняем запрос (executeQuery для SELECT)
            while (rs.next()) { // Пока есть строки
                result.add(mapRow(rs)); // Добавляем заявку в список
            }
            return result; // Возвращаем список
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при получении заявок: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public void update(RepairRequest request) { // Метод обновления заявки
        String sql = "UPDATE repair_requests SET device_type = ?, problem_description = ?, " + // SQL-запрос на обновление
                "status = ?, priority = ?, cost = ?, completed_at = ? WHERE id = ?"; // Обновляем поля и completed_at
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setString(1, request.getDeviceType()); // Подставляем device_type
            ps.setString(2, request.getProblemDescription()); // Подставляем problem_description
            ps.setString(3, request.getStatus().name()); // Подставляем имя enum статуса
            ps.setString(4, request.getPriority().name()); // Подставляем имя enum приоритета
            ps.setBigDecimal(5, request.getCost()); // Подставляем cost
            if (request.getCompletedAt() != null) { // Если дата завершения установлена
                ps.setTimestamp(6, Timestamp.valueOf(request.getCompletedAt())); // Подставляем Timestamp
            } else { // Если дата завершения null
                ps.setNull(6, Types.TIMESTAMP); // Подставляем SQL NULL с типом TIMESTAMP
            }
            ps.setInt(7, request.getId()); // Подставляем id в WHERE
            ps.executeUpdate(); // Выполняем UPDATE (executeUpdate для INSERT/UPDATE/DELETE)
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при обновлении заявки: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public void delete(int id) { // Метод удаления заявки
        String sql = "DELETE FROM repair_requests WHERE id = ?"; // SQL-запрос на удаление
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setInt(1, id); // Подставляем id
            ps.executeUpdate(); // Выполняем DELETE
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при удалении заявки: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    @Override
    public List<RepairRequest> searchByDescription(String keyword) { // Метод поиска заявок по описанию
        String sql = SELECT_BASE + "WHERE LOWER(r.problem_description) LIKE LOWER(?) ORDER BY r.id"; // SQL: поиск без учета регистра
        return searchWithLike(sql, keyword); // Делегируем в общий метод searchWithLike
    }

    @Override
    public List<RepairRequest> searchByClientName(String namePart) { // Метод поиска заявок по имени клиента
        String sql = SELECT_BASE + "WHERE LOWER(c.full_name) LIKE LOWER(?) ORDER BY r.id"; // SQL: поиск по имени клиента
        return searchWithLike(sql, namePart); // Делегируем в общий метод searchWithLike
    }

    private List<RepairRequest> searchWithLike(String sql, String keyword) { // Вспомогательный метод для поиска по LIKE
        List<RepairRequest> result = new ArrayList<>(); // Создаем пустой список
        try (Connection conn = DatabaseManager.getConnection(); // Открываем соединение
             PreparedStatement ps = conn.prepareStatement(sql)) { // Создаем PreparedStatement
            ps.setString(1, "%" + keyword + "%"); // Оборачиваем в % для поиска по подстроке
            try (ResultSet rs = ps.executeQuery()) { // Выполняем запрос
                while (rs.next()) { // Пока есть строки
                    result.add(mapRow(rs)); // Добавляем заявку в список
                }
            }
            return result; // Возвращаем список
        } catch (SQLException e) { // При ошибке SQL
            throw new DatabaseException("Ошибка при поиске заявок: " + e.getMessage(), e); // Бросаем DatabaseException
        }
    }

    private RepairRequest mapRow(ResultSet rs) throws SQLException { // Вспомогательный метод: ResultSet -> RepairRequest
        RepairRequest request = new RepairRequest(); // Создаем пустую заявку
        request.setId(rs.getInt("id")); // Устанавливаем id
        request.setClientId(rs.getInt("client_id")); // Устанавливаем client_id
        request.setClientName(rs.getString("client_name")); // Устанавливаем client_name (из JOIN)
        request.setDeviceType(rs.getString("device_type")); // Устанавливаем device_type
        request.setProblemDescription(rs.getString("problem_description")); // Устанавливаем problem_description
        request.setStatus(RequestStatus.valueOf(rs.getString("status"))); // Строка из БД -> enum RequestStatus
        request.setPriority(Priority.valueOf(rs.getString("priority"))); // Строка из БД -> enum Priority
        request.setCost(rs.getBigDecimal("cost")); // Устанавливаем cost
        Timestamp createdAt = rs.getTimestamp("created_at"); // Получаем Timestamp created_at
        if (createdAt != null) request.setCreatedAt(createdAt.toLocalDateTime()); // Если не null — преобразуем
        Timestamp completedAt = rs.getTimestamp("completed_at"); // Получаем Timestamp completed_at
        if (completedAt != null) request.setCompletedAt(completedAt.toLocalDateTime()); // Если не null — преобразуем
        return request; // Возвращаем заполненную заявку
    }
}