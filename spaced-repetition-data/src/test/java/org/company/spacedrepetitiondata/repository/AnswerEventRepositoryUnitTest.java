package org.company.spacedrepetitiondata.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for AnswerEventRepository using Mockito to mock JDBC components.
 * Tests all repository methods with mocked DataSource, Connection, PreparedStatement, and ResultSet.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnswerEventRepositoryUnitTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private ResultSet generatedKeysResultSet;

    private AnswerEventRepository repository;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        repository = new AnswerEventRepository(dataSource);
    }

    @Test
    void insert_validRecord_returnsGeneratedId() throws SQLException {
        // Given
        AnswerEventRecord record = new AnswerEventRecord(123L, 456L, 789L, 4, Instant.now());
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeysResultSet);
        when(generatedKeysResultSet.next()).thenReturn(true);
        when(generatedKeysResultSet.getLong(1)).thenReturn(42L);

        // When
        Optional<Long> result = repository.insert(record);

        // Then
        assertTrue(result.isPresent());
        assertEquals(42L, result.get());
        verify(preparedStatement).setLong(1, 123L);
        verify(preparedStatement).setLong(2, 456L);
        verify(preparedStatement).setLong(3, 789L);
        verify(preparedStatement).setInt(4, 4);
        verify(preparedStatement).setTimestamp(eq(5), any(Timestamp.class));
        verify(preparedStatement).executeUpdate();
        verify(generatedKeysResultSet).next();
        verify(generatedKeysResultSet).getLong(1);
        verify(generatedKeysResultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void insert_zeroAffectedRows_returnsEmpty() throws SQLException {
        // Given
        AnswerEventRecord record = new AnswerEventRecord(123L, 456L, 789L, 4, Instant.now());
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        // When
        Optional<Long> result = repository.insert(record);

        // Then
        assertFalse(result.isPresent());
        verify(preparedStatement).executeUpdate();
        verify(preparedStatement).close();
        verify(connection).close();
        verifyNoInteractions(generatedKeysResultSet);
    }

    @Test
    void insert_sqlException_throwsRuntimeException() throws SQLException {
        // Given
        AnswerEventRecord record = new AnswerEventRecord(123L, 456L, 789L, 4, Instant.now());
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.insert(record));
        assertTrue(exception.getMessage().contains("Failed to insert answer event"));
        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void insert_noGeneratedKey_returnsEmpty() throws SQLException {
        // Given
        AnswerEventRecord record = new AnswerEventRecord(123L, 456L, 789L, 4, Instant.now());
        when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS)))
                .thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        when(preparedStatement.getGeneratedKeys()).thenReturn(generatedKeysResultSet);
        when(generatedKeysResultSet.next()).thenReturn(false);

        // When
        Optional<Long> result = repository.insert(record);

        // Then
        assertFalse(result.isPresent());
        verify(generatedKeysResultSet).next();
        verify(generatedKeysResultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void findByUserIdAndTimeRange_withStartAndEnd_returnsEvents() throws SQLException {
        // Given
        Long userId = 123L;
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("user_id")).thenReturn(userId);
        when(resultSet.getLong("deck_id")).thenReturn(456L);
        when(resultSet.getLong("card_id")).thenReturn(789L);
        when(resultSet.getInt("quality")).thenReturn(4);
        when(resultSet.getTimestamp("event_timestamp")).thenReturn(Timestamp.from(endTime.minusSeconds(1800)));

        // When
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(userId, startTime, endTime);

        // Then
        assertEquals(1, results.size());
        AnswerEventRecord record = results.get(0);
        assertEquals(userId, record.getUserId());
        assertEquals(456L, record.getDeckId());
        assertEquals(789L, record.getCardId());
        assertEquals(4, record.getQuality());
        assertEquals(endTime.minusSeconds(1800), record.getTimestamp());
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(startTime));
        verify(preparedStatement).setTimestamp(3, Timestamp.from(endTime));
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void findByUserIdAndTimeRange_noBounds_returnsAllEvents() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getLong("user_id")).thenReturn(userId);
        when(resultSet.getLong("deck_id")).thenReturn(456L);
        when(resultSet.getLong("card_id")).thenReturn(789L);
        when(resultSet.getInt("quality")).thenReturn(4, 5);
        Instant timestamp1 = Instant.now().minusSeconds(300);
        Instant timestamp2 = Instant.now().minusSeconds(200);
        when(resultSet.getTimestamp("event_timestamp")).thenReturn(Timestamp.from(timestamp1), Timestamp.from(timestamp2));

        // When
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(userId, null, null);

        // Then
        assertEquals(2, results.size());
        verify(preparedStatement).setLong(1, userId);
        // No additional parameters for start/end
        verify(preparedStatement, never()).setTimestamp(eq(2), any());
        verify(preparedStatement, never()).setTimestamp(eq(3), any());
        verify(preparedStatement).executeQuery();
        verify(resultSet, times(3)).next();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void findByUserIdAndTimeRange_sqlException_throwsRuntimeException() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.findByUserIdAndTimeRange(userId, null, null));
        assertTrue(exception.getMessage().contains("Failed to retrieve answer events"));
        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void findByUserIdAndTimeRange_startTimeNullEndTimeNotNull_returnsEvents() throws SQLException {
        // Given
        Long userId = 123L;
        Instant endTime = Instant.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("user_id")).thenReturn(userId);
        when(resultSet.getLong("deck_id")).thenReturn(456L);
        when(resultSet.getLong("card_id")).thenReturn(789L);
        when(resultSet.getInt("quality")).thenReturn(4);
        when(resultSet.getTimestamp("event_timestamp")).thenReturn(Timestamp.from(endTime.minusSeconds(1800)));

        // When
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(userId, null, endTime);

        // Then
        assertEquals(1, results.size());
        AnswerEventRecord record = results.get(0);
        assertEquals(userId, record.getUserId());
        assertEquals(456L, record.getDeckId());
        assertEquals(789L, record.getCardId());
        assertEquals(4, record.getQuality());
        assertEquals(endTime.minusSeconds(1800), record.getTimestamp());
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(endTime)); // endTime at position 2
        verify(preparedStatement, never()).setTimestamp(eq(3), any()); // no third parameter
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void findByUserIdAndTimeRange_startTimeNotNullEndTimeNull_returnsEvents() throws SQLException {
        // Given
        Long userId = 123L;
        Instant startTime = Instant.now().minusSeconds(3600);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getLong("user_id")).thenReturn(userId);
        when(resultSet.getLong("deck_id")).thenReturn(456L);
        when(resultSet.getLong("card_id")).thenReturn(789L);
        when(resultSet.getInt("quality")).thenReturn(4);
        when(resultSet.getTimestamp("event_timestamp")).thenReturn(Timestamp.from(startTime.plusSeconds(1800)));

        // When
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(userId, startTime, null);

        // Then
        assertEquals(1, results.size());
        AnswerEventRecord record = results.get(0);
        assertEquals(userId, record.getUserId());
        assertEquals(456L, record.getDeckId());
        assertEquals(789L, record.getCardId());
        assertEquals(4, record.getQuality());
        assertEquals(startTime.plusSeconds(1800), record.getTimestamp());
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(startTime)); // startTime at position 2
        verify(preparedStatement, never()).setTimestamp(eq(3), any()); // no third parameter
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void findByUserIdAndTimeRange_noRows_returnsEmptyList() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        List<AnswerEventRecord> results = repository.findByUserIdAndTimeRange(userId, null, null);

        // Then
        assertTrue(results.isEmpty());
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement, never()).setTimestamp(eq(2), any());
        verify(preparedStatement, never()).setTimestamp(eq(3), any());
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserId_returnsCount() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(42L);

        // When
        long count = repository.countByUserId(userId);

        // Then
        assertEquals(42L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserId_noRows_returnsZero() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        long count = repository.countByUserId(userId);

        // Then
        assertEquals(0L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserId_sqlException_throwsRuntimeException() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.countByUserId(userId));
        assertTrue(exception.getMessage().contains("Failed to count answer events"));
        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void countByUserIdAndTimeRange_withBounds_returnsCount() throws SQLException {
        // Given
        Long userId = 123L;
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(5L);

        // When
        long count = repository.countByUserIdAndTimeRange(userId, startTime, endTime);

        // Then
        assertEquals(5L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(startTime));
        verify(preparedStatement).setTimestamp(3, Timestamp.from(endTime));
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserIdAndTimeRange_noBounds_returnsCount() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(7L);

        // When
        long count = repository.countByUserIdAndTimeRange(userId, null, null);

        // Then
        assertEquals(7L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement, never()).setTimestamp(eq(2), any());
        verify(preparedStatement, never()).setTimestamp(eq(3), any());
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserIdAndTimeRange_sqlException_throwsRuntimeException() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.countByUserIdAndTimeRange(userId, null, null));
        assertTrue(exception.getMessage().contains("Failed to count answer events"));
        assertInstanceOf(SQLException.class, exception.getCause());
    }

    @Test
    void countByUserIdAndTimeRange_startTimeNullEndTimeNotNull_returnsCount() throws SQLException {
        // Given
        Long userId = 123L;
        Instant endTime = Instant.now();
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(3L);

        // When
        long count = repository.countByUserIdAndTimeRange(userId, null, endTime);

        // Then
        assertEquals(3L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(endTime)); // endTime at position 2
        verify(preparedStatement, never()).setTimestamp(eq(3), any()); // no third parameter
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserIdAndTimeRange_startTimeNotNullEndTimeNull_returnsCount() throws SQLException {
        // Given
        Long userId = 123L;
        Instant startTime = Instant.now().minusSeconds(3600);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(2L);

        // When
        long count = repository.countByUserIdAndTimeRange(userId, startTime, null);

        // Then
        assertEquals(2L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement).setTimestamp(2, Timestamp.from(startTime)); // startTime at position 2
        verify(preparedStatement, never()).setTimestamp(eq(3), any()); // no third parameter
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void countByUserIdAndTimeRange_noRows_returnsZero() throws SQLException {
        // Given
        Long userId = 123L;
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        long count = repository.countByUserIdAndTimeRange(userId, null, null);

        // Then
        assertEquals(0L, count);
        verify(preparedStatement).setLong(1, userId);
        verify(preparedStatement, never()).setTimestamp(eq(2), any());
        verify(preparedStatement, never()).setTimestamp(eq(3), any());
        verify(preparedStatement).executeQuery();
        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void fromProto_validProto_returnsRecord() {
        // Given
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent protoEvent =
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("123")
                        .setDeckId("456")
                        .setCardId("789")
                        .setQualityValue(4)
                        .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(1234567890L)
                                .setNanos(123456789)
                                .build())
                        .build();

        // When
        AnswerEventRecord record = AnswerEventRepository.fromProto(protoEvent);

        // Then
        assertEquals(123L, record.getUserId());
        assertEquals(456L, record.getDeckId());
        assertEquals(789L, record.getCardId());
        assertEquals(4, record.getQuality());
        assertEquals(1234567890L, record.getTimestamp().getEpochSecond());
        assertEquals(123456789, record.getTimestamp().getNano());
    }

    @Test
    void fromProto_invalidIdFormat_throwsNumberFormatException() {
        // Given
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent protoEvent =
                org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent.newBuilder()
                        .setUserId("not-a-number")
                        .setDeckId("456")
                        .setCardId("789")
                        .setQualityValue(4)
                        .setTimestamp(com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(1234567890L)
                                .build())
                        .build();

        // When & Then
        assertThrows(NumberFormatException.class, () -> AnswerEventRepository.fromProto(protoEvent));
    }

    @Test
    void toProto_validRecord_returnsProto() {
        // Given
        AnswerEventRecord record = new AnswerEventRecord(123L, 456L, 789L, 4,
                Instant.ofEpochSecond(1234567890L, 123456789));

        // When
        org.company.spacedrepetitiondata.grpc.AnalyticsProto.AnswerEvent protoEvent =
                AnswerEventRepository.toProto(record);

        // Then
        assertEquals("123", protoEvent.getUserId());
        assertEquals("456", protoEvent.getDeckId());
        assertEquals("789", protoEvent.getCardId());
        assertEquals(4, protoEvent.getQualityValue());
        assertEquals(1234567890L, protoEvent.getTimestamp().getSeconds());
        assertEquals(123456789, protoEvent.getTimestamp().getNanos());
    }
}