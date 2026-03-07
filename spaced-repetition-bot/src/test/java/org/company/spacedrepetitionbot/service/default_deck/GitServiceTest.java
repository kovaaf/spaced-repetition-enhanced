package org.company.spacedrepetitionbot.service.default_deck;

import org.company.spacedrepetitionbot.config.AppProperties;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


/**
 * Unit tests for GitService.
 * Tests Git authentication scenarios with mocked JGit operations.
 */
@ExtendWith(MockitoExtension.class)
class GitServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.DefaultDeckConfig defaultDeckConfig;

    @Mock
    private AppProperties.DefaultDeckConfig.RepoConfig repoConfig;

    @Mock
    private UsernamePasswordCredentialsProvider credentials;

    @InjectMocks
    private GitService gitService;

    private Git invokeGetGitInstance() throws Exception {
        Method method = GitService.class.getDeclaredMethod("getGitInstance");
        method.setAccessible(true);
        try {
            return (Git) method.invoke(gitService);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e;
            }
        }
    }

    @Test
    void getGitInstance_WhenRepositoryExists_ShouldOpenAndFetchSuccessfully() throws Exception {
        // Given
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getRepo()).thenReturn(repoConfig);
        when(repoConfig.getPath()).thenReturn("/tmp/test-repo");
        when(repoConfig.getUrl()).thenReturn("https://github.com/test/repo.git");
        
        // Create a temporary directory that exists
        Path tempDir = Files.createTempDirectory("test-repo");
        try {
            when(repoConfig.getPath()).thenReturn(tempDir.toString());
            File repoDir = tempDir.toFile();
            
            try (MockedStatic<Git> gitMockedStatic = mockStatic(Git.class)) {
                Git mockGit = mock(Git.class);
                FetchCommand mockFetchCommand = mock(FetchCommand.class);
                
                gitMockedStatic.when(() -> Git.open(repoDir)).thenReturn(mockGit);
                when(mockGit.fetch()).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setCredentialsProvider(credentials)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setRemoveDeletedRefs(true)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.call()).thenReturn(null);
                
                // When
                Git result = gitService.getGitInstance();
                
                // Then
                assertNotNull(result);
                assertEquals(mockGit, result);
                verify(mockFetchCommand).call();
                gitMockedStatic.verify(() -> Git.open(repoDir));
            }
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void getGitInstance_WhenRepositoryExistsAndInvalidCredentials_ShouldThrowTransportException() throws Exception {
        // Given
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getRepo()).thenReturn(repoConfig);
        when(repoConfig.getPath()).thenReturn("/tmp/test-repo");
        when(repoConfig.getUrl()).thenReturn("https://github.com/test/repo.git");
        
        // Create a temporary directory that exists
        Path tempDir = Files.createTempDirectory("test-repo");
        try {
            when(repoConfig.getPath()).thenReturn(tempDir.toString());
            File repoDir = tempDir.toFile();
            
            try (MockedStatic<Git> gitMockedStatic = mockStatic(Git.class)) {
                Git mockGit = mock(Git.class);
                FetchCommand mockFetchCommand = mock(FetchCommand.class);
                
                gitMockedStatic.when(() -> Git.open(repoDir)).thenReturn(mockGit);
                when(mockGit.fetch()).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setCredentialsProvider(credentials)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setRemoveDeletedRefs(true)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.call()).thenThrow(new TransportException("not authorized"));
                // Mock cloneRepository for fallback after fetch fails
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                gitMockedStatic.when(() -> Git.cloneRepository()).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setCredentialsProvider(any(UsernamePasswordCredentialsProvider.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenThrow(new TransportException("not authorized"));
                
                // When / Then
                assertThrows(TransportException.class, () -> gitService.getGitInstance());
                gitMockedStatic.verify(() -> Git.open(repoDir));
            }
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void getGitInstance_WhenRepositoryNotExists_ShouldCloneRepository() throws Exception {
        // Given
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getRepo()).thenReturn(repoConfig);
        when(repoConfig.getPath()).thenReturn("/tmp/test-repo");
        when(repoConfig.getUrl()).thenReturn("https://github.com/test/repo.git");
        
        // Use a path that does NOT exist
        Path nonExistentDir = Path.of("/tmp/nonexistent-repo-" + System.currentTimeMillis());
        when(repoConfig.getPath()).thenReturn(nonExistentDir.toString());
        File repoDir = nonExistentDir.toFile();
        
        try (MockedStatic<Git> gitMockedStatic = mockStatic(Git.class)) {
            Git mockGit = mock(Git.class);
            CloneCommand mockCloneCommand = mock(CloneCommand.class);
            
            // Mock cloneRepository
            gitMockedStatic.when(() -> Git.cloneRepository()).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.setCredentialsProvider(any(UsernamePasswordCredentialsProvider.class))).thenReturn(mockCloneCommand);
            when(mockCloneCommand.call()).thenReturn(mockGit);
            
            // When
            Git result = gitService.getGitInstance();
            
            // Then
            assertNotNull(result);
            assertEquals(mockGit, result);
            gitMockedStatic.verify(() -> Git.cloneRepository());
            verify(mockCloneCommand).call();
            // Git.open should NOT be called because directory doesn't exist
            gitMockedStatic.verify(() -> Git.open(repoDir), never());
        }
    }

    @Test
    void getGitInstance_WhenNetworkError_ShouldThrowTransportException() throws Exception {
        // Given
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getRepo()).thenReturn(repoConfig);
        when(repoConfig.getPath()).thenReturn("/tmp/test-repo");
        when(repoConfig.getUrl()).thenReturn("https://github.com/test/repo.git");
        
        // Create a temporary directory that exists
        Path tempDir = Files.createTempDirectory("test-repo");
        try {
            when(repoConfig.getPath()).thenReturn(tempDir.toString());
            File repoDir = tempDir.toFile();
            
            try (MockedStatic<Git> gitMockedStatic = mockStatic(Git.class)) {
                Git mockGit = mock(Git.class);
                FetchCommand mockFetchCommand = mock(FetchCommand.class);
                
                gitMockedStatic.when(() -> Git.open(repoDir)).thenReturn(mockGit);
                when(mockGit.fetch()).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setCredentialsProvider(credentials)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setRemoveDeletedRefs(true)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.call()).thenThrow(new TransportException("Connection refused"));
                // Mock cloneRepository for fallback after fetch fails
                CloneCommand mockCloneCommand = mock(CloneCommand.class);
                gitMockedStatic.when(() -> Git.cloneRepository()).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setURI(anyString())).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setDirectory(any(File.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.setCredentialsProvider(any(UsernamePasswordCredentialsProvider.class))).thenReturn(mockCloneCommand);
                when(mockCloneCommand.call()).thenThrow(new TransportException("Connection refused"));
                
                // When / Then
                assertThrows(TransportException.class, () -> gitService.getGitInstance());
                gitMockedStatic.verify(() -> Git.open(repoDir));
            }
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void getGitInstanceWithHardReset_ShouldResetToOriginBranch() throws Exception {
        // Given
        when(appProperties.getDefaultDeck()).thenReturn(defaultDeckConfig);
        when(defaultDeckConfig.getRepo()).thenReturn(repoConfig);
        when(repoConfig.getPath()).thenReturn("/tmp/test-repo");
        when(repoConfig.getUrl()).thenReturn("https://github.com/test/repo.git");
        when(repoConfig.getBranch()).thenReturn("main");
        
        // Create a temporary directory that exists
        Path tempDir = Files.createTempDirectory("test-repo");
        try {
            when(repoConfig.getPath()).thenReturn(tempDir.toString());
            File repoDir = tempDir.toFile();
            
            try (MockedStatic<Git> gitMockedStatic = mockStatic(Git.class)) {
                Git mockGit = mock(Git.class);
                FetchCommand mockFetchCommand = mock(FetchCommand.class);
                ResetCommand mockResetCommand = mock(ResetCommand.class);
                
                gitMockedStatic.when(() -> Git.open(repoDir)).thenReturn(mockGit);
                when(mockGit.fetch()).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setCredentialsProvider(credentials)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.setRemoveDeletedRefs(true)).thenReturn(mockFetchCommand);
                when(mockFetchCommand.call()).thenReturn(null);
                when(mockGit.reset()).thenReturn(mockResetCommand);
                when(mockResetCommand.setMode(ResetCommand.ResetType.HARD)).thenReturn(mockResetCommand);
                when(mockResetCommand.setRef("origin/main")).thenReturn(mockResetCommand);
                when(mockResetCommand.call()).thenReturn(null);
                
                // When
                Git result = gitService.getGitInstanceWithHardReset();
                
                // Then
                assertNotNull(result);
                verify(mockResetCommand).call();
            }
        } finally {
            Files.deleteIfExists(tempDir);
        }
    }

    @Test
    void getLatestCommit_ShouldReturnCommitHash() throws IOException {
        // Given
        Git mockGit = mock(Git.class);
        Repository mockRepository = mock(Repository.class);
        org.eclipse.jgit.lib.ObjectId mockObjectId = mock(org.eclipse.jgit.lib.ObjectId.class);
        org.eclipse.jgit.revwalk.RevCommit mockCommit = mock(org.eclipse.jgit.revwalk.RevCommit.class);
        when(mockGit.getRepository()).thenReturn(mockRepository);
        when(mockRepository.resolve(org.eclipse.jgit.lib.Constants.HEAD)).thenReturn(mockObjectId);
        when(mockCommit.getName()).thenReturn("abc123def456");
        // Mock RevWalk constructor using MockedConstruction
        try (MockedConstruction<RevWalk> mockedConstruction = mockConstruction(RevWalk.class,
                (mockRevWalk, context) -> {
                    when(mockRevWalk.parseCommit(mockObjectId)).thenReturn(mockCommit);
                })) {
            // When
            String commitHash = gitService.getLatestCommit(mockGit);
            
            // Then
            assertNotNull(commitHash);
            assertEquals("abc123def456", commitHash);
            assertEquals(1, mockedConstruction.constructed().size());
        }
}
}