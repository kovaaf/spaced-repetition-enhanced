package org.company.spacedrepetitiondata.repository;

import lombok.extern.slf4j.Slf4j;
import org.company.spacedrepetitiondata.config.DatabaseConfig;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC repository for answer_events table.
 * Uses HikariCP connection pool via DatabaseConfig.
 * All methods throw RuntimeException on SQL errors.
 */
@Slf4j
public class AnswerEventRepository {
    private final DataSource dataSource;

    public AnswerEventRepository() {
        this.dataSource = DatabaseConfig.getDataSource();
    }

    /**
     * Inserts a new answer event record into the database.
     *
     * @param record the event record to insert
     * @throws RuntimeException if SQL error occurs
     */
    public void insert(AnswerEventRecord record) {
        log.info("Inserting answer event: userId={}, deckId={}, cardId={}, quality={}, timestamp={}",
                record.userId(), record.deckId(), record.cardId(), record.quality(), record.timestamp());
        String sql = "INSERT INTO answer_events (user_id, deck_id, card_id, quality, event_timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, record.userId());
            stmt.setLong(2, record.deckId());
            stmt.setLong(3, record.cardId());
            stmt.setInt(4, record.quality());
            stmt.setTimestamp(5, Timestamp.from(record.timestamp()));

            int affectedRows = stmt.executeUpdate();
            log.info("INSERT affected rows: {}", affectedRows);
            if (affectedRows == 0) {
                log.error("Failed to insert answer event, no rows affected: {}", record);
                return;
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long eventId = generatedKeys.getLong(1);
                    log.info("Inserted answer event with ID: {}", eventId);
                } else {
                    log.info("No generated key returned for answer event insert");
                }
            }
        } catch (SQLException e) {
            log.error("SQL error inserting answer event: {}", record, e);
            throw new RuntimeException("Failed to insert answer event", e);
        }
    }

    /**
     * Finds answer events for a specific user within a time range (inclusive).
     * Results are ordered by timestamp ascending.
     *
     * @param userId user ID to filter
     * @param startTime start of time range (inclusive), can be null for no lower bound
     * @param endTime end of time range (inclusive), can be null for no upper bound
     * @return list of matching answer events, empty list if none found
     * @throws RuntimeException if SQL error occurs
     */
    public List<AnswerEventRecord> findByUserIdAndTimeRange(Long userId, Instant startTime, Instant endTime) {
        List<AnswerEventRecord> results = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT ae.event_id, ae.user_id, ae.deck_id, ae.card_id, ae.quality, ae.event_timestamp, " +
                "       user_info.user_name AS user_name, " +
                "       deck.name AS deck_name, " +
                "       card.front AS card_title " +
                "FROM answer_events ae " +
                "LEFT JOIN user_info ON ae.user_id = user_info.user_chat_id " +
                "LEFT JOIN deck ON ae.deck_id = deck.deck_id " +
                "LEFT JOIN card ON ae.card_id = card.card_id " +
                "WHERE ae.user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }
        sqlBuilder.append(" ORDER BY ae.event_timestamp ASC");

        String sql = sqlBuilder.toString();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + param.getClass());
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AnswerEventRecord record = mapRow(rs);
                    results.add(record);
                }
            }
            log.debug("Found {} answer events for user {} in time range {} - {}",
                    results.size(), userId, startTime, endTime);
        } catch (SQLException e) {
            log.error("SQL error finding answer events for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve answer events", e);
        }
        return results;
    }

    /**
     * Counts answer events for a specific user within a time range (inclusive).
     *
     * @param userId user ID to count
     * @param startTime start of time range (inclusive), can be null for no lower bound
     * @param endTime end of time range (inclusive), can be null for no upper bound
     * @return total count of answer events for the user within the time range
     * @throws RuntimeException if SQL error occurs
     */
    public long countByUserIdAndTimeRange(Long userId, Instant startTime, Instant endTime) {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT COUNT(*) FROM answer_events ae WHERE user_id = ?");
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }

        String sql = sqlBuilder.toString();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Long) {
                    stmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + param.getClass());
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            log.debug("Counted answer events for user {} in time range {} - {}",
                    userId, startTime, endTime);
        } catch (SQLException e) {
            log.error("SQL error counting answer events for user {} in time range: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to count answer events", e);
        }
        return 0;
    }

    /**
     * Finds answer events within a time range (inclusive) for all users.
     * Results are ordered by timestamp ascending.
     *
     * @param startTime start of time range (inclusive), can be null for no lower bound
     * @param endTime end of time range (inclusive), can be null for no upper bound
     * @return list of matching answer events, empty list if none found
     * @throws RuntimeException if SQL error occurs
     */
    public List<AnswerEventRecord> findByTimeRange(Instant startTime, Instant endTime) {
        List<AnswerEventRecord> results = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT ae.event_id, ae.user_id, ae.deck_id, ae.card_id, ae.quality, ae.event_timestamp, " +
                "       user_info.user_name AS user_name, " +
                "       deck.name AS deck_name, " +
                "       card.front AS card_title " +
                "FROM answer_events ae " +
                "LEFT JOIN user_info ON ae.user_id = user_info.user_chat_id " +
                "LEFT JOIN deck ON ae.deck_id = deck.deck_id " +
                "LEFT JOIN card ON ae.card_id = card.card_id " +
                "WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (startTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }
        sqlBuilder.append(" ORDER BY ae.event_timestamp ASC");
        String sql = sqlBuilder.toString();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + param.getClass());
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    AnswerEventRecord record = mapRow(rs);
                    results.add(record);
                }
            }
            log.debug("Found {} answer events in time range {} - {}",
                    results.size(), startTime, endTime);
        } catch (SQLException e) {
            log.error("SQL error finding answer events in time range: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve answer events", e);
        }
        return results;
    }

    /**
     * Counts total answer events within a time range (inclusive) for all users.
     *
     * @param startTime start of time range (inclusive), can be null for no lower bound
     * @param endTime end of time range (inclusive), can be null for no upper bound
     * @return total count of answer events within the time range
     * @throws RuntimeException if SQL error occurs
     */
    public long countByTimeRange(Instant startTime, Instant endTime) {
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT COUNT(*) FROM answer_events ae WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (startTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp >= ?");
            params.add(startTime);
        }
        if (endTime != null) {
            sqlBuilder.append(" AND ae.event_timestamp <= ?");
            params.add(endTime);
        }

        String sql = sqlBuilder.toString();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);
                if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, Timestamp.from((Instant) param));
                } else {
                    throw new IllegalArgumentException("Unsupported parameter type: " + param.getClass());
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
            log.debug("Counted answer events in time range {} - {}",
                    startTime, endTime);
        } catch (SQLException e) {
            log.error("SQL error counting answer events in time range: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to count answer events", e);
        }
        return 0;
    }

    /**
     * Maps a ResultSet row to AnswerEventRecord.
     * Assumes columns: event_id, user_id, deck_id, card_id, quality, event_timestamp,
     * plus optional joined columns: user_name, deck_name, card_title.
     *
     * @param rs ResultSet positioned at current row
     * @return mapped AnswerEventRecord
     * @throws SQLException if column access fails
     */
    private AnswerEventRecord mapRow(ResultSet rs) throws SQLException {
        Long userId = rs.getLong("user_id");
        Long deckId = rs.getLong("deck_id");
        Long cardId = rs.getLong("card_id");
        int quality = rs.getInt("quality");
        Timestamp timestamp = rs.getTimestamp("event_timestamp");
        Instant instant = timestamp != null ? timestamp.toInstant() : null;
        // optional joined columns (may be null)
        String userName = rs.getString("user_name");
        String deckName = rs.getString("deck_name");
        String cardTitle = rs.getString("card_title");
        // event_id is not stored in AnswerEventRecord currently
        return new AnswerEventRecord(userId, deckId, cardId, quality, instant,
                userName, deckName, cardTitle);
    }

    /**
     * Utility method to convert protobuf AnswerEvent (string IDs) to AnswerEventRecord (Long IDs).
     * Parses string IDs as Long; throws NumberFormatException if invalid.
     *
     * @param protoAnswerEvent protobuf AnswerEvent message
     * @return AnswerEventRecord with parsed IDs
     * @throws NumberFormatException if any ID cannot be parsed as Long
     */
    public static AnswerEventRecord fromProto(org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent protoAnswerEvent) {
        Long userId = Long.parseLong(protoAnswerEvent.getUserId());
        Long deckId = Long.parseLong(protoAnswerEvent.getDeckId());
        Long cardId = Long.parseLong(protoAnswerEvent.getCardId());
        int quality = protoAnswerEvent.getQualityValue(); // enum numeric value
        Instant timestamp = Instant.ofEpochSecond(
                protoAnswerEvent.getTimestamp().getSeconds(),
                protoAnswerEvent.getTimestamp().getNanos());
        return new AnswerEventRecord(userId, deckId, cardId, quality, timestamp);
    }

    /**
     * Retrieves all users from the user_info table.
     * @return list of UserRecord objects, empty list if no users found
     */
    public List<UserRecord> findAllUsers() {
        List<UserRecord> results = new ArrayList<>();
        String sql = "SELECT user_chat_id, user_name FROM user_info ORDER BY user_name NULLS LAST, user_chat_id";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Long id = rs.getLong("user_chat_id");
                String name = rs.getString("user_name");
                results.add(new UserRecord(id, name));
            }
        } catch (SQLException e) {
            log.error("SQL error fetching users", e);
            throw new RuntimeException("Failed to fetch users", e);
        }
        return results;
    }

    /**
     * Utility method to convert AnswerEventRecord to protobuf AnswerEvent.
     * Converts Long IDs to strings.
     *
     * @param record AnswerEventRecord
     * @return protobuf AnswerEvent message
     */
    public static org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent toProto(AnswerEventRecord record) {
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent.Builder builder = 
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent.newBuilder()
                .setUserId(String.valueOf(record.userId()))
                .setDeckId(String.valueOf(record.deckId()))
                .setCardId(String.valueOf(record.cardId()))
                .setQualityValue(record.quality())
                .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                        .setSeconds(record.timestamp().getEpochSecond())
                        .setNanos(record.timestamp().getNano())
                        .build());
        
        if (record.userName() != null) {
            builder.setUserName(record.userName());
        }
        if (record.deckName() != null) {
            builder.setDeckName(record.deckName());
        }
        if (record.cardTitle() != null) {
            builder.setCardTitle(record.cardTitle());
        }
        
        return builder.build();
    }
}