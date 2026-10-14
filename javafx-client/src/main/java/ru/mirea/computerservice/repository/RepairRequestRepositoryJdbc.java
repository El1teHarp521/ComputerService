package ru.mirea.computerservice.repository;

import ru.mirea.computerservice.exception.DatabaseException;
import ru.mirea.computerservice.model.Priority;
import ru.mirea.computerservice.model.RepairRequest;
import ru.mirea.computerservice.model.RequestStatus;
import ru.mirea.computerservice.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация репозитория заявок. Запросы к repair_requests сразу делают JOIN
 * с clients, чтобы в таблице интерфейса можно было показать ФИО клиента без
 * дополнительных обращений к базе.
 */
public class RepairRequestRepositoryJdbc implements RepairRequestRepository {

    private static final String SELECT_BASE =
            "SELECT r.*, c.full_name AS client_name FROM repair_requests r " +
                    "JOIN clients c ON c.id = r.client_id ";

    @Override
    public RepairRequest save(RepairRequest request) {
        String sql = "INSERT INTO repair_requests (client_id, device_type, problem_description, status, priority, cost) " +
                "VALUES (?, ?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.getClientId());
            ps.setString(2, request.getDeviceType());
            ps.setString(3, request.getProblemDescription());
            ps.setString(4, request.getStatus().name());
            ps.setString(5, request.getPriority().name());
            ps.setBigDecimal(6, request.getCost());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    request.setId(rs.getInt("id"));
                    request.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                }
            }
            return request;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании заявки: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<RepairRequest> findById(int id) {
        String sql = SELECT_BASE + "WHERE r.id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске заявки: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RepairRequest> findAll() {
        return query(SELECT_BASE + "ORDER BY r.id", ps -> { });
    }

    @Override
    public List<RepairRequest> findByClientId(int clientId) {
        return query(SELECT_BASE + "WHERE r.client_id = ? ORDER BY r.created_at DESC, r.id DESC",
                ps -> ps.setInt(1, clientId));
    }

    @Override
    public void update(RepairRequest request) {
        String sql = "UPDATE repair_requests SET device_type = ?, problem_description = ?, " +
                "status = ?, priority = ?, cost = ?, completed_at = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, request.getDeviceType());
            ps.setString(2, request.getProblemDescription());
            ps.setString(3, request.getStatus().name());
            ps.setString(4, request.getPriority().name());
            ps.setBigDecimal(5, request.getCost());
            if (request.getCompletedAt() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(request.getCompletedAt()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            ps.setInt(7, request.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении заявки: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM repair_requests WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении заявки: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RepairRequest> searchByDescription(String keyword) {
        return query(SELECT_BASE + "WHERE LOWER(r.problem_description) LIKE LOWER(?) ESCAPE '\\' ORDER BY r.id",
                ps -> ps.setString(1, SqlUtil.likeContains(keyword)));
    }

    @Override
    public List<RepairRequest> searchByClientName(String namePart) {
        return query(SELECT_BASE + "WHERE LOWER(c.full_name) LIKE LOWER(?) ESCAPE '\\' ORDER BY r.id",
                ps -> ps.setString(1, SqlUtil.likeContains(namePart)));
    }

    @Override
    public List<RepairRequest> searchAnyField(String keyword) {
        String sql = SELECT_BASE + "WHERE LOWER(r.problem_description) LIKE LOWER(?) ESCAPE '\\' " +
                "OR LOWER(r.device_type) LIKE LOWER(?) ESCAPE '\\' " +
                "OR LOWER(c.full_name) LIKE LOWER(?) ESCAPE '\\' ORDER BY r.id";
        String pattern = SqlUtil.likeContains(keyword);
        return query(sql, ps -> {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
        });
    }

    /** Общий метод выполнения SELECT и преобразования строк в объекты. */
    private List<RepairRequest> query(String sql, SqlUtil.ParamSetter setter) {
        List<RepairRequest> result = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении заявок: " + e.getMessage(), e);
        }
    }

    private RepairRequest mapRow(ResultSet rs) throws SQLException {
        RepairRequest request = new RepairRequest();
        request.setId(rs.getInt("id"));
        request.setClientId(rs.getInt("client_id"));
        request.setClientName(rs.getString("client_name"));
        request.setDeviceType(rs.getString("device_type"));
        request.setProblemDescription(rs.getString("problem_description"));
        request.setStatus(RequestStatus.valueOf(rs.getString("status")));
        request.setPriority(Priority.valueOf(rs.getString("priority")));
        request.setCost(rs.getBigDecimal("cost"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) request.setCreatedAt(createdAt.toLocalDateTime());
        Timestamp completedAt = rs.getTimestamp("completed_at");
        if (completedAt != null) request.setCompletedAt(completedAt.toLocalDateTime());
        return request;
    }
}
