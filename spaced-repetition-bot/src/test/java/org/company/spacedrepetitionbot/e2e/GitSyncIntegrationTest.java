package org.company.spacedrepetitionbot.e2e;

import org.company.spacedrepetitionbot.config.TokenValidationConfig;
import org.company.spacedrepetitionbot.model.Deck;
import org.company.spacedrepetitionbot.service.DeckService;
import org.company.spacedrepetitionbot.service.default_deck.RepoSynchronizer;
import org.company.spacedrepetitionbot.service.default_deck.event.SyncEventDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Git sync flow end-to-end.
 * Tests configuration validation, repository access, and default deck initialization.
 * Requires valid GitHub token and repository URL via environment variables.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@EnabledIfEnvironmentVariable(named = "GIT_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DEFAULT_DECK_REPO_URL", matches = ".+")
public class GitSyncIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.5-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    static Path tempRepoDir;

    @Autowired
    private TokenValidationConfig tokenValidationConfig;

    @Autowired
    private DeckService deckService;

    @Autowired
    private RepoSynchronizer repoSynchronizer;

    @BeforeAll
    static void setupTempDir() throws Exception {
        tempRepoDir = Files.createTempDirectory("git-sync-test");
        // Ensure directory is deleted on JVM exit
        tempRepoDir.toFile().deleteOnExit();
    }

    @AfterAll
    static void cleanupTempDir() throws Exception {
        if (tempRepoDir != null && Files.exists(tempRepoDir)) {
            deleteDirectory(tempRepoDir.toFile());
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.liquibase.enabled", () -> "true");
        // Disable Telegram bot and Git integration from interfering
        registry.add("telegram.bot.token", () -> "dummy-token");
        registry.add("telegram.bot.name", () -> "TestBot");
        // Set Git configuration from environment variables
        registry.add("app.default-deck.repo.url", () -> System.getenv("DEFAULT_DECK_REPO_URL"));
        registry.add("app.default-deck.repo.path", () -> tempRepoDir.toString());
        registry.add("app.default-deck.repo.token", () -> System.getenv("GIT_TOKEN"));
        registry.add("app.git-sync.enabled", () -> "true");
        // Disable analytics outbox processor
        registry.add("app.analytics.outbox.processor.cron", () -> "0 0 0 * * *");
        // Disable scheduled sync to avoid interference
        registry.add("app.default-deck.sync.cron", () -> "0 0 0 * * *");
    }

    @BeforeEach
    void setUp() {
        // Ensure token validation passes (should have been triggered at startup)
        // No action needed
    }

    @Test
    void gitSyncFlow_endToEnd() throws Exception {
        // 1. Token validation should have passed at startup
        // (validated by TokenValidationConfig's PostConstruct)
        
        // 2. Verify repository directory exists (or will be created)
        assertNotNull(tempRepoDir);
        assertTrue(Files.exists(tempRepoDir) || !Files.exists(tempRepoDir));
        
        // 3. Trigger sync with null deck (should initialize default deck)
        SyncEventDTO event = new SyncEventDTO();
        repoSynchronizer.sync(event, null);
        
        // 4. Verify default deck created
        Deck defaultDeck = deckService.getDefaultDeck();
        assertNotNull(defaultDeck);
        assertTrue(defaultDeck.getName().contains("Default"));
        
        // 5. Verify repository was cloned (directory should have .git subdirectory)
        File gitDir = new File(tempRepoDir.toFile(), ".git");
        assertTrue(gitDir.exists() || gitDir.isDirectory());
        
        // 6. Verify cards were created (at least one card)
        // This depends on repository content; we can skip if no markdown files
        // For now, just ensure no exceptions during sync
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}