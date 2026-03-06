package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnswerEventDTOTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Test
    void testBuilderAndGetters() {
        Instant now = Instant.now();
        AnswerEventDTO dto = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        assertEquals("user123", dto.getUserId());
        assertEquals("deck456", dto.getDeckId());
        assertEquals("card789", dto.getCardId());
        assertEquals(AnswerEventDTO.Quality.GOOD, dto.getQuality());
        assertEquals(now, dto.getTimestamp());
    }
    
    @Test
    void testQualityEnum() {
        assertEquals(0, AnswerEventDTO.Quality.AGAIN.getValue());
        assertEquals(3, AnswerEventDTO.Quality.HARD.getValue());
        assertEquals(4, AnswerEventDTO.Quality.GOOD.getValue());
        assertEquals(5, AnswerEventDTO.Quality.EASY.getValue());
        
        assertEquals(AnswerEventDTO.Quality.AGAIN, AnswerEventDTO.Quality.fromValue(0));
        assertEquals(AnswerEventDTO.Quality.HARD, AnswerEventDTO.Quality.fromValue(3));
        assertEquals(AnswerEventDTO.Quality.GOOD, AnswerEventDTO.Quality.fromValue(4));
        assertEquals(AnswerEventDTO.Quality.EASY, AnswerEventDTO.Quality.fromValue(5));
        
        assertThrows(IllegalArgumentException.class, () -> 
                AnswerEventDTO.Quality.fromValue(99));
    }
    
    @Test
    void testJsonSerialization() throws Exception {
        Instant now = Instant.parse("2026-02-20T10:30:00Z");
        AnswerEventDTO dto = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        String json = objectMapper.writeValueAsString(dto);
        
        // Deserialize back
        AnswerEventDTO deserialized = objectMapper.readValue(json, AnswerEventDTO.class);
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getDeckId(), deserialized.getDeckId());
        assertEquals(dto.getCardId(), deserialized.getCardId());
        assertEquals(dto.getQuality(), deserialized.getQuality());
        assertEquals(dto.getTimestamp(), deserialized.getTimestamp());
    }
    
    @Test
    void testEqualsAndHashCode() {
        Instant now = Instant.now();
        AnswerEventDTO dto1 = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        AnswerEventDTO dto2 = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }
}