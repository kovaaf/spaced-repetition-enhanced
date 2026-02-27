package org.company.spacedrepetitionbot.service.default_deck;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class FileSystemScanner {
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
