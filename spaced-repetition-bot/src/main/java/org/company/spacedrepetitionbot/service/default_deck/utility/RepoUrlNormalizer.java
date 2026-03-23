package org.company.spacedrepetitionbot.service.default_deck.utility;

import org.springframework.stereotype.Service;

/**
 * Normalizes GitHub repository URLs to a consistent format (owner/repo).
 * <p>
 * Handles HTTPS, SSH, and optional {@code .git} suffix.
 * </p>
 */
@Service
public class RepoUrlNormalizer {
    private static final String HTTPS_PREFIX = "https://github.com/";
    private static final String SSH_PREFIX = "git@github.com:";
    private static final String GIT_SUFFIX = ".git";

    /**
     * Normalizes a GitHub repository URL.
     *
     * @param url the raw URL (e.g., {@code https://github.com/owner/repo.git})
     * @return the repository name in the form {@code owner/repo}
     */
    public String normalize(String url) {
        if (url == null) {
            return "";
        }

        if (url.startsWith(HTTPS_PREFIX)) {
            return url.substring(HTTPS_PREFIX.length()).replace(GIT_SUFFIX, "");
        }
        if (url.startsWith(SSH_PREFIX)) {
            return url.substring(SSH_PREFIX.length()).replace(GIT_SUFFIX, "");
        }
        return url;
    }
}
