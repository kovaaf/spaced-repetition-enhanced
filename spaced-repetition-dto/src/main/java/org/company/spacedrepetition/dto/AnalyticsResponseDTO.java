package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for analytics responses.
 * Mirrors the protobuf AnalyticsResponse message structure.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponseDTO {
    
    private List<AnswerEventDTO> events;
    
    @JsonProperty("total_count")
    private int totalCount;
}