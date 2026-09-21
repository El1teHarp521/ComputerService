package ru.mirea.computerservice.repository;

import ru.mirea.computerservice.exception.DatabaseException;
import ru.mirea.computerservice.model.Client;
import ru.mirea.computerservice.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientRepositoryJdbc implements ClientRepository {

    @Override
    public Client save(Client client) {
        String sql = "INSERT INTO clients (full_name, phone, email) VALUES (?, ?, ?) RETURNING id, registered_at";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    client.setId(rs.getInt("id"));
                    client.setRegisteredAt(rs.getTimestamp("registered_at").toLocalDateTime());
                }
            }
            return client;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при создании клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Client> findById(int id) {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> findAll() {
        String sql = "SELECT * FROM clients ORDER BY id";
        List<Client> clients = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                clients.add(mapRow(rs));
            }
            return clients;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при получении списка клиентов: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(Client client) {
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setInt(4, client.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при обновлении клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при удалении клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByPhone(String phone) {
        String sql = "SELECT 1 FROM clients WHERE phone = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке телефона: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Client> searchByName(String namePart) {
        String sql = "SELECT * FROM clients WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY id";
        List<Client> clients = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + namePart + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(mapRow(rs));
                }
            }
            return clients;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при поиске клиента: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasActiveRequests(int clientId) {
        String sql = "SELECT 1 FROM repair_requests WHERE client_id = ? AND status NOT IN ('DONE', 'CANCELLED')";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке заявок клиента: " + e.getMessage(), e);
        }
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        Client client = new Client();
        client.setId(rs.getInt("id"));
        client.setFullName(rs.getString("full_name"));
        client.setPhone(rs.getString("phone"));
        client.setEmail(rs.getString("email"));
        Timestamp ts = rs.getTimestamp("registered_at");
        if (ts != null) client.setRegisteredAt(ts.toLocalDateTime());
        return client;
    }
}
