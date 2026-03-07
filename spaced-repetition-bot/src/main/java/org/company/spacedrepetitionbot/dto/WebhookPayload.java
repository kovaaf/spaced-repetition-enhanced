package org.company.spacedrepetitionbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record WebhookPayload(Repository repository, String ref, List<Commit> commits) {
    public record Repository(@JsonProperty("full_name") String fullName) { }

    public record Commit(List<String> added, List<String> modified, List<String> removed) { }
}
