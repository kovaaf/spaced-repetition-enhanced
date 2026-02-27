package org.company.spacedrepetition.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object for analytics requests.
 * Mirrors the protobuf AnalyticsRequest message structure.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsRequestDTO {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("start_time")
    private Instant startTime;
    
    @JsonProperty("end_time")
    private Instant endTime;
}