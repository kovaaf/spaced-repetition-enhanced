package org.company.spacedrepetitionbot.service.default_deck.utility;

import lombok.RequiredArgsConstructor;
import org.company.spacedrepetitionbot.config.properties.AppProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Utility service for handling file paths related to the Git repository.
 * <p>
 * Provides methods to obtain the repository root, relative paths,
 * and check whether a file is included or excluded based on configured source/exclude folders.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class PathService {
    private final AppProperties appProperties;

    /**
     * Returns the absolute path to the local Git repository.
     *
     * @return the repository root path
     */
    public Path getRepoAbsolutePath() {
        return Paths.get(appProperties.getDefaultDeck().getRepo().getPath()).toAbsolutePath();
    }

    /**
     * Converts an absolute file path to a path relative to the repository root.
     *
     * @param file the absolute file path
     * @return the relative path as a string
     */
    public String absolutePathToRelativePathString(Path file) {
        return getRepoAbsolutePath().relativize(file).toString();
    }

    /**
     * Checks whether a file path belongs to any of the source folders.
     *
     * @param filePath      the path to check (relative to repo root)
     * @param sourceFolders list of source folder names
     * @return {@code true} if the file is inside any source folder
     */
    public boolean isFileInSourceFolder(String filePath, List<String> sourceFolders) {
        if (sourceFolders == null || sourceFolders.isEmpty()) {
            return true;
        }
        String normalizedPath = normalizePath(filePath);
        return sourceFolders.stream()
                .anyMatch(folder -> normalizedPath.startsWith(normalizePath(folder) + "/") ||
                        normalizedPath.equals(normalizePath(folder)));
    }

    /**
     * Checks whether a file path should be included, based on source and exclude folders.
     *
     * @param filePath       the path to check (relative to repo root)
     * @param sourceFolders  list of source folders
     * @param excludeFolders list of exclude folders
     * @return {@code true} if the file is included
     */
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
