package org.company.spacedrepetitionbot.config;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubAuthConfig {

    @Value("${app.default-deck.repo.token}")
    private String githubToken;

    @Bean
    public UsernamePasswordCredentialsProvider gitCredentials() {
        return new UsernamePasswordCredentialsProvider(githubToken, "");
    }
}
