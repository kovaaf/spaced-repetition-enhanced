package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnalyticsRequestDTOTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    @Test
    void testBuilderAndGetters() {
        Instant startTime = Instant.parse("2026-02-20T00:00:00Z");
        Instant endTime = Instant.parse("2026-02-20T23:59:59Z");
        
        AnalyticsRequestDTO dto = AnalyticsRequestDTO.builder()
                .userId("user123")
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        assertEquals("user123", dto.getUserId());
        assertEquals(startTime, dto.getStartTime());
        assertEquals(endTime, dto.getEndTime());
    }
    
    @Test
    void testJsonSerialization() throws Exception {
        Instant startTime = Instant.parse("2026-02-20T00:00:00Z");
        Instant endTime = Instant.parse("2026-02-20T23:59:59Z");
        
        AnalyticsRequestDTO dto = AnalyticsRequestDTO.builder()
                .userId("user123")
                .startTime(startTime)
                .endTime(endTime)
                .build();
        
        String json = objectMapper.writeValueAsString(dto);
        
        // Deserialize back
        AnalyticsRequestDTO deserialized = objectMapper.readValue(json, AnalyticsRequestDTO.class);
        assertEquals(dto.getUserId(), deserialized.getUserId());
        assertEquals(dto.getStartTime(), deserialized.getStartTime());
        assertEquals(dto.getEndTime(), deserialized.getEndTime());
    }
    
    @Test
    void testNullFields() {
        // Test that null fields are allowed
        AnalyticsRequestDTO dto = AnalyticsRequestDTO.builder()
                .userId("user123")
                .build();
        
        assertEquals("user123", dto.getUserId());
        assertNull(dto.getStartTime());
        assertNull(dto.getEndTime());
    }
}