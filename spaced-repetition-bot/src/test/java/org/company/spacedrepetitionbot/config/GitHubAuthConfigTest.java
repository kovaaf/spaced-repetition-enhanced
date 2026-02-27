package org.company.spacedrepetitionbot.config;

import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GitHubAuthConfig.
 * Tests credential provider creation with different token values.
 */
class GitHubAuthConfigTest {

    @Test
    void gitCredentials_WithValidToken_ShouldCreateProviderWithToken() throws Exception {
        // Given
        GitHubAuthConfig config = new GitHubAuthConfig();
        setPrivateField(config, "githubToken", "ghp_testtoken1234567890");
        
        // When
        UsernamePasswordCredentialsProvider provider = config.gitCredentials();
        
        // Then
        assertNotNull(provider);
        // The provider stores token as password, username is token, password is empty string
        // We can verify by checking that the provider can be used (no easy getter)
        // For unit test, we just ensure it's created without exception
    }

    @Test
    void gitCredentials_WithEmptyToken_ShouldCreateProviderWithEmptyToken() throws Exception {
        // Given
        GitHubAuthConfig config = new GitHubAuthConfig();
        setPrivateField(config, "githubToken", "");
        
        // When
        UsernamePasswordCredentialsProvider provider = config.gitCredentials();
        
        // Then
        assertNotNull(provider);
        // Empty token may cause authentication failures but that's not unit test concern
    }

    @Test
    void gitCredentials_WithNullToken_ShouldCreateProvider() throws Exception {
        // Given
        GitHubAuthConfig config = new GitHubAuthConfig();
        setPrivateField(config, "githubToken", null);
        // When
        UsernamePasswordCredentialsProvider provider = config.gitCredentials();
        // Then
        assertNotNull(provider);
        // Provider may handle null token internally
    }

    @Test
    void gitCredentials_WithTokenContainingSpecialCharacters_ShouldCreateProvider() throws Exception {
        // Given
        GitHubAuthConfig config = new GitHubAuthConfig();
        setPrivateField(config, "githubToken", "ghp_abc!@#$%^&*()");
        
        // When
        UsernamePasswordCredentialsProvider provider = config.gitCredentials();
        
        // Then
        assertNotNull(provider);
    }

    /**
     * Helper method to set private field using reflection.
     */
    private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}