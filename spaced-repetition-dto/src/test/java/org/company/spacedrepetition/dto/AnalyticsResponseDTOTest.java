package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsResponseDTOTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Test
    void testBuilderAndGetters() {
        Instant now = Instant.now();
        AnswerEventDTO event1 = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        AnswerEventDTO event2 = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card999")
                .quality(AnswerEventDTO.Quality.EASY)
                .timestamp(now.plusSeconds(3600))
                .build();
        
        List<AnswerEventDTO> events = Arrays.asList(event1, event2);
        
        AnalyticsResponseDTO dto = AnalyticsResponseDTO.builder()
                .events(events)
                .totalCount(2)
                .build();
        
        assertEquals(2, dto.getEvents().size());
        assertEquals(event1, dto.getEvents().get(0));
        assertEquals(event2, dto.getEvents().get(1));
        assertEquals(2, dto.getTotalCount());
    }
    
    @Test
    void testJsonSerialization() throws Exception {
        Instant now = Instant.parse("2026-02-20T10:30:00Z");
        AnswerEventDTO event = AnswerEventDTO.builder()
                .userId("user123")
                .deckId("deck456")
                .cardId("card789")
                .quality(AnswerEventDTO.Quality.GOOD)
                .timestamp(now)
                .build();
        
        List<AnswerEventDTO> events = Arrays.asList(event);
        
        AnalyticsResponseDTO dto = AnalyticsResponseDTO.builder()
                .events(events)
                .totalCount(1)
                .build();
        
        String json = objectMapper.writeValueAsString(dto);
        
        // Deserialize back
        AnalyticsResponseDTO deserialized = objectMapper.readValue(json, AnalyticsResponseDTO.class);
        assertEquals(dto.getTotalCount(), deserialized.getTotalCount());
        assertEquals(dto.getEvents().size(), deserialized.getEvents().size());
        
        AnswerEventDTO deserializedEvent = deserialized.getEvents().get(0);
        assertEquals(event.getUserId(), deserializedEvent.getUserId());
        assertEquals(event.getDeckId(), deserializedEvent.getDeckId());
        assertEquals(event.getCardId(), deserializedEvent.getCardId());
        assertEquals(event.getQuality(), deserializedEvent.getQuality());
        assertEquals(event.getTimestamp(), deserializedEvent.getTimestamp());
    }
    
    @Test
    void testEmptyEvents() {
        // Test with empty events list
        AnalyticsResponseDTO dto = AnalyticsResponseDTO.builder()
                .events(Arrays.asList())
                .totalCount(0)
                .build();
        
        assertTrue(dto.getEvents().isEmpty());
        assertEquals(0, dto.getTotalCount());
    }
}