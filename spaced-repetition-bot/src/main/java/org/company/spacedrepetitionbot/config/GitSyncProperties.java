package org.company.spacedrepetitionbot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "git.sync")
@Data
public class GitSyncProperties {
    private boolean enabled = true;
}