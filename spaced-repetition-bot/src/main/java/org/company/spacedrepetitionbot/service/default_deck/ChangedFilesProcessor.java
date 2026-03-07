package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.company.spacedrepetitionbot.dto.WebhookPayload;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChangedFilesProcessor {
    private static final String MD_FILE_EXTENSION = ".md";

    private final PathService pathService;

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
