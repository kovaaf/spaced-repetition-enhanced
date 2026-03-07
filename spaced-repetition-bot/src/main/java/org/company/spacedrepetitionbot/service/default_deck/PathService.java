package org.company.spacedrepetitionbot.service.default_deck;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.config.AppProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PathService {
    private final AppProperties appProperties;

    public Path getRepoPath() {
        return Paths.get(appProperties.getDefaultDeck().getRepo().getPath()).toAbsolutePath();
    }

    public String getRelativePath(Path file) {
        return getRepoPath().relativize(file).toString();
    }

    public boolean isFileInSourceFolder(String filePath, List<String> sourceFolders) {
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            return true;
        }
        String normalizedPath = normalizePath(filePath);
        return sourceFolders.stream()
                .anyMatch(folder -> normalizedPath.startsWith(normalizePath(folder) + "/") ||
                        normalizedPath.equals(normalizePath(folder)));
    }

    public boolean isFileIncluded(String filePath, List<String> sourceFolders, List<String> excludeFolders) {
        String normalizedPath = normalizePath(filePath);
        boolean inSource = isFileInSourceFolder(normalizedPath, sourceFolders);

        if (!inSource) {
            return false;
        }
        if (excludeFolders == null || excludeFolders.isEmpty()) {
            return true;
        }

        return excludeFolders.stream().noneMatch(exFolder -> {
            String normalizedExFolder = normalizePath(exFolder);

            // 1. Проверка полного пути
            if (normalizedPath.startsWith(normalizedExFolder + "/") || normalizedPath.equals(normalizedExFolder)) {
                return true;
            }

            // 2. Проверка всех сегментов пути, а не только последнего
            String[] pathSegments = normalizedPath.split("/");
            for (String segment : pathSegments) {
                if (segment.equals(normalizedExFolder)) {
                    return true;
                }
            }

            return false;
        });
    }

    private String normalizePath(String path) {
        return path.replace('\\', '/').toLowerCase();
    }
}
