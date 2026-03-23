package org.company.data.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.data.model.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC implementation of {@link UserRepository}.
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcUserRepository implements UserRepository {
    private final DataSource dataSource;

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_chat_id, user_name FROM user_info ORDER BY user_name NULLS LAST, user_chat_id";

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Long id = rs.getLong("user_chat_id");
                String name = rs.getString("user_name");
                users.add(new User(id, name));
            }
        } catch (SQLException e) {
            log.error("Failed to fetch users", e);
            throw new RuntimeException("Database error", e);
        }

        return users;
    }
}