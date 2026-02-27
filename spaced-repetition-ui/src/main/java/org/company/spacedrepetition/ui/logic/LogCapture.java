package org.company.spacedrepetition.ui.logic;

import javax.swing.SwingUtilities;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Intercepts an OutputStream and redirects newlines to a Consumer.
 * Used for capturing System.out/err and displaying in the UI.
 */
public class LogCapture extends OutputStream {
    private final PrintStream original;
    private final Consumer<String> logConsumer;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public LogCapture(PrintStream original, Consumer<String> logConsumer) {
        this.original = original;
        this.logConsumer = logConsumer;
    }

    @Override
    public void write(int b) throws IOException {
        if (original != null) {
            original.write(b);
        }
        buffer.write(b);
        if (b == '\n') {
            flushBuffer();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (original != null) {
            original.write(b, off, len);
        }
        buffer.write(b, off, len);
        
        // Search for newlines in the written bytes
        boolean hasNewline = false;
        for (int i = off; i < off + len; i++) {
            if (b[i] == '\n') {
                hasNewline = true;
                break;
            }
        }
        
        if (hasNewline) {
            flushBuffer();
        }
    }

    @Override
    public void flush() throws IOException {
        if (original != null) {
            original.flush();
        }
        flushBuffer();
    }

    private synchronized void flushBuffer() {
        if (buffer.size() == 0) {
            return;
        }
        
        String logLine = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();
        
        SwingUtilities.invokeLater(() -> logConsumer.accept(logLine));
    }
}
