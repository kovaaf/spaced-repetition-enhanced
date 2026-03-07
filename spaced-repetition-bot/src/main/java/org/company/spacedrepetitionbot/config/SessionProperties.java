package org.company.spacedrepetitionbot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.session")
@Configuration
public class SessionProperties {
    private int maxNewCards;
    private int maxReviewCards;
    private String resetCron;
}
