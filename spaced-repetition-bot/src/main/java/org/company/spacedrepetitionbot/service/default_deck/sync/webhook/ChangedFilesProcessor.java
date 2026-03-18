package org.company.spacedrepetitionbot.service.default_deck.sync.webhook;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.company.spacedrepetitionbot.dto.WebhookPayload;
import org.company.spacedrepetitionbot.service.default_deck.utility.PathService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts the list of changed files from a GitHub webhook payload and filters them
 * according to the configured source and exclude folders.
 */
@Service
@RequiredArgsConstructor
public class ChangedFilesProcessor {
    private static final String MD_FILE_EXTENSION = ".md";

    private final PathService pathService;

    /**
     * Returns a filtered list of changed Markdown files from the webhook payload.
     *
     * @param payload    the webhook payload
     * @param deckConfig the configuration of the default deck (source/exclude folders)
     * @return list of relative file paths that have changed and are relevant
     */
    public List<String> getChangedFiles(WebhookPayload payload, AppProperties.DefaultDeckConfig deckConfig) {
        List<String> changedFiles = extractAllChangedFiles(payload);
        List<String> sourceFolders = deckConfig.getRepo().getSourceFolders();
        List<String> excludeFolders = deckConfig.getRepo().getExcludeFolders();

        return changedFiles.stream()
                .filter(file -> file.endsWith(MD_FILE_EXTENSION))
                .filter(file -> pathService.isFileIncluded(file, sourceFolders, excludeFolders))
                .collect(Collectors.toList());
    }

    private List<String> extractAllChangedFiles(WebhookPayload payload) {
        List<String> files = new ArrayList<>();

        if (payload.commits() != null) {
            for (WebhookPayload.Commit commit : payload.commits()) {
                if (commit.added() != null) {
                    files.addAll(commit.added());
                }
                if (commit.modified() != null) {
                    files.addAll(commit.modified());
                }
                if (commit.removed() != null) {
                    files.addAll(commit.removed());
                }
            }
        }

        return files;
    }

    //    private boolean isInSourceFolder(String filePath, List<String> sourceFolders) {
    //        if (sourceFolders == null || sourceFolders.isEmpty()) {
    //            return true;
    //        }
    //        return sourceFolders.stream().anyMatch(folder ->
    //                filePath.startsWith(folder + "/") || filePath.equals(folder)
    //        );
    //    }
}
