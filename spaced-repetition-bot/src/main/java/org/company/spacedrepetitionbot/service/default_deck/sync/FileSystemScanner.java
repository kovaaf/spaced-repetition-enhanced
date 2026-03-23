package org.company.spacedrepetitionbot.service.default_deck.sync;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Scanner that walks a directory tree and collects Markdown files.
 * <p>
 * Used during full synchronization to find all Markdown files that need to be processed.
 * </p>
 */
@Component
public class FileSystemScanner {
    /**
     * Recursively finds all Markdown files (ending with {@code .md}) under the given directory.
     *
     * @param directory the root directory to scan
     * @return a list of paths to Markdown files
     * @throws IOException if an I/O error occurs while walking the tree
     */
    public List<Path> findMarkdownFiles(Path directory) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> pathStream = Files.walk(directory)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isMarkdownFile)
                    .forEach(files::add);
        }
        return files;
    }

    private boolean isMarkdownFile(Path path) {
        return path.toString().toLowerCase().endsWith(".md");
    }
}
