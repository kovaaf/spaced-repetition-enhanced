package org.company.spacedrepetitiondata.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.model.AnswerEvent;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of {@link AnswerEventRepository}.
 */
@Slf4j
@RequiredArgsConstructor
public class JdbcAnswerEventRepository implements AnswerEventRepository {
    private final DataSource dataSource;

    @Override
    public Optional<Long> save(AnswerEvent event) {
        String sql = "INSERT INTO answer_events (user_id, deck_id, card_id, quality, event_timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, event.userId());
            stmt.setLong(2, event.deckId());
            stmt.setLong(3, event.cardId());
            stmt.setInt(4, event.quality());
            stmt.setTimestamp(5, Timestamp.from(event.timestamp()));

            int affected = stmt.executeUpdate();
            if (affected == 0) {
                return Optional.empty();
            }

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return Optional.of(keys.getLong(1));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            log.error("Failed to save answer event", e);
            throw new RuntimeException("Database error", e);
        }
    }

    @Override
    public List<AnswerEvent> findByUserAndTimeRange(Long userId, Instant startTime, Instant endTime) {
        List<AnswerEvent> results = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT ae.user_id, ae.deck_id, ae.card_id, ae.quality, ae.event_timestamp, " +
                        "       ui.user_name, d.name AS deck_name, c.front AS card_title " +
                        "FROM answer_events ae " +
                        "LEFT JOIN user_info ui ON ae.user_id = ui.user_chat_id " +
                        "LEFT JOIN deck d ON ae.deck_id = d.deck_id " +
                        "LEFT JOIN card c ON ae.card_id = c.card_id " +
                        "WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (userId != null) {
            sql.append(" AND ae.user_id = ?");
            params.add(userId);
        }
        if (startTime != null) {
            sql.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sql.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }
        sql.append(" ORDER BY ae.event_timestamp ASC");

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to query answer events", e);
            throw new RuntimeException("Database error", e);
        }

        return results;
    }

    @Override
    public long countByUserAndTimeRange(Long userId, Instant startTime, Instant endTime) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM answer_events ae WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (userId != null) {
            sql.append(" AND ae.user_id = ?");
            params.add(userId);
        }
        if (startTime != null) {
            sql.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sql.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }

        try (Connection conn = dataSource.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0;
            }
        } catch (SQLException e) {
            log.error("Failed to count answer events", e);
            throw new RuntimeException("Database error", e);
        }
    }

    private AnswerEvent mapRow(ResultSet rs) throws SQLException {
        Long userId = rs.getLong("user_id");
        Long deckId = rs.getLong("deck_id");
        Long cardId = rs.getLong("card_id");
        int quality = rs.getInt("quality");
        Timestamp ts = rs.getTimestamp("event_timestamp");
        Instant timestamp = ts != null ? ts.toInstant() : null;
        String userName = rs.getString("user_name");
        String deckName = rs.getString("deck_name");
        String cardTitle = rs.getString("card_title");

        return AnswerEvent.builder()
                .userId(userId)
                .deckId(deckId)
                .cardId(cardId)
                .quality(quality)
                .timestamp(timestamp)
                .userName(userName)
                .deckName(deckName)
                .cardTitle(cardTitle)
                .build();
    }
}