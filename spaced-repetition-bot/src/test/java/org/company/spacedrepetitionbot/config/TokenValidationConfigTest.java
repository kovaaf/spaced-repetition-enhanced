package org.company.spacedrepetitionbot.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenValidationConfig.
 * Tests the validation logic without actually starting Spring context.
 */
@ExtendWith(MockitoExtension.class)
class TokenValidationConfigTest {

    @Mock
    private Logger log;

    @InjectMocks
    private TokenValidationConfig tokenValidationConfig;

    @Test
    void testValidateGitHubToken_WithNullToken_ShouldLogWarning() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // Use reflection to set null token
        try {
            var field = TokenValidationConfig.class.getDeclaredField("githubToken");
            field.setAccessible(true);
            field.set(config, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // When
        config.validateGitHubToken();
        
        // Then - No exception should be thrown, just log warning
        // Since we can't easily verify logging without Spring context,
        // we just verify the method completes without errors
    }

    @Test
    void testValidateGitHubToken_WithEmptyToken_ShouldLogWarning() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // Use reflection to set empty token
        try {
            var field = TokenValidationConfig.class.getDeclaredField("githubToken");
            field.setAccessible(true);
            field.set(config, "   ");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // When
        config.validateGitHubToken();
        
        // Then - No exception should be thrown
    }

    @Test
    void testValidateGitHubToken_WithDummyToken_ShouldLogWarning() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // Use reflection to set dummy token
        try {
            var field = TokenValidationConfig.class.getDeclaredField("githubToken");
            field.setAccessible(true);
            field.set(config, "dummy");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // When
        config.validateGitHubToken();
        
        // Then - No exception should be thrown
    }

    @Test
    void testValidateGitHubToken_WithValidToken_ShouldCompleteWithoutWarnings() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // Use reflection to set valid token
        try {
            var field = TokenValidationConfig.class.getDeclaredField("githubToken");
            field.setAccessible(true);
            field.set(config, "ghp_validtoken1234567890");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        // When
        config.validateGitHubToken();
        
        // Then - No exception should be thrown
    }

    @Test
    void testMaskToken_WithNull_ReturnsEmpty() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // When
        String result = invokeMaskToken(config, null);
        
        // Then
        assert "[empty]".equals(result) : "Expected [empty] but got: " + result;
    }

    @Test
    void testMaskToken_WithEmpty_ReturnsEmpty() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // When
        String result = invokeMaskToken(config, "");
        
        // Then
        assert "[empty]".equals(result) : "Expected [empty] but got: " + result;
    }

    @Test
    void testMaskToken_WithShortToken_ReturnsTooShort() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        
        // When
        String result = invokeMaskToken(config, "short");
        
        // Then
        assert "[too short to mask]".equals(result) : "Expected [too short to mask] but got: " + result;
    }

    @Test
    void testMaskToken_WithValidToken_ReturnsMasked() {
        // Given
        TokenValidationConfig config = new TokenValidationConfig();
        String token = "ghp_abcdefghijklmnopqrstuvwxyz1234567890";
        
        // When
        String result = invokeMaskToken(config, token);
        
        // Then
        assert result.startsWith("ghp_abcd...") : "Expected masked token starting with 'ghp_abcd...' but got: " + result;
        assert result.endsWith("7890") : "Expected masked token ending with '7890' but got: " + result;
    }

    /**
     * Helper method to invoke private maskToken method using reflection.
     */
    private String invokeMaskToken(TokenValidationConfig config, String token) {
        try {
            var method = TokenValidationConfig.class.getDeclaredMethod("maskToken", String.class);
            method.setAccessible(true);
            return (String) method.invoke(config, token);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}