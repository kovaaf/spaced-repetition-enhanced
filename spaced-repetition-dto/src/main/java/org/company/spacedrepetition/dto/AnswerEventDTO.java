package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for answer events.
 * Mirrors the protobuf AnswerEvent message structure.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnswerEventDTO {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("deck_id")
    private String deckId;
    
    @JsonProperty("card_id")
    private String cardId;
    
    private Quality quality;
    
    private Instant timestamp;
    
    /**
     * Quality enum matching protobuf Quality enum.
     */
    public enum Quality {
        AGAIN(0),
        HARD(3),
        GOOD(4),
        EASY(5);
        
        private final int value;
        
        Quality(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static Quality fromValue(int value) {
            for (Quality quality : Quality.values()) {
                if (quality.getValue() == value) {
                    return quality;
                }
            }
            throw new IllegalArgumentException("Unknown quality value: " + value);
        }
    }
}