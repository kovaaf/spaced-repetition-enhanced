package org.company.spacedrepetitionbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
/**
 * Configuration component that validates GitHub token at application startup.
 * Logs warnings if token appears invalid but does not prevent application startup.
 */
@Configuration
public class TokenValidationConfig {
    private static final Logger log = LoggerFactory.getLogger(TokenValidationConfig.class);

    @Value("${app.default-deck.repo.token}")
    private String githubToken;

    /**
     * Validates GitHub token configuration when application is ready.
     * Checks for common invalid token patterns and logs appropriate warnings.
     * This validation runs after the application context is fully initialized.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void validateGitHubToken() {
        log.debug("Starting GitHub token validation...");
        
        if (githubToken == null || githubToken.trim().isEmpty()) {
            log.warn("GitHub token is not configured (null or empty). Git synchronization will fail.");
            return;
        }
        
        if ("dummy".equals(githubToken.trim())) {
            log.warn("GitHub token is set to 'dummy' placeholder. Git synchronization will fail.");
            return;
        }
        
        if (githubToken.trim().length() < 10) {
            log.warn("GitHub token appears too short ({} characters). May be invalid.", githubToken.trim().length());
            return;
        }
        
        // Check for common invalid patterns
        if (githubToken.trim().startsWith("ghp_")) {
            log.debug("GitHub token appears to be a valid Personal Access Token (PAT) format.");
        } else if (githubToken.trim().startsWith("github_pat_")) {
            log.debug("GitHub token appears to be a valid Fine-grained Personal Access Token format.");
        } else {
            log.warn("GitHub token does not match expected PAT formats (ghp_ or github_pat_). May be invalid.");
        }
        
        // Mask token for logging (show first 8 chars only)
        String maskedToken = maskToken(githubToken);
        log.info("GitHub token validation completed. Token: {}", maskedToken);
    }
    
    /**
     * Masks a token for safe logging - shows first 8 characters only.
     * 
     * @param token the token to mask
     * @return masked token string
     */
    private String maskToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return "[empty]";
        }
        
        String trimmed = token.trim();
        if (trimmed.length() <= 8) {
            return "[too short to mask]";
        }
        
        return trimmed.substring(0, 8) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}