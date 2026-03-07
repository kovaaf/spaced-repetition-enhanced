package org.company.spacedrepetitionbot.service.default_deck;

import org.springframework.stereotype.Service;

@Service
public class RepoUrlNormalizer {
    private static final String HTTPS_PREFIX = "https://github.com/";
    private static final String SSH_PREFIX = "git@github.com:";
    private static final String GIT_SUFFIX = ".git";

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
