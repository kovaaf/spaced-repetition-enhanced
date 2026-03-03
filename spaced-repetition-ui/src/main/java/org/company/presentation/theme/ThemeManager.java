package org.company.presentation.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class ThemeManager {
    private static final Map<String, Supplier<LookAndFeel>> themes = new LinkedHashMap<>();

    static {
        // Регистрируем доступные темы
        themes.put("Light", FlatLightLaf::new);
        themes.put("Dark", FlatDarkLaf::new);
        // Сюда можно легко добавить другие темы FlatLaf, например:
        // themes.put("IntelliJ", FlatIntelliJLaf::new);
        // themes.put("Darcula", FlatDarculaLaf::new);
    }

    public static String[] getAvailableThemeNames() {
        return themes.keySet().toArray(String[]::new);
    }

    public static void applyTheme(String themeName) {
        Supplier<LookAndFeel> supplier = themes.get(themeName);
        if (supplier == null) {
            log.warn("Unknown theme: {}, using default Light", themeName);
            supplier = themes.get("Light");
        }
        LookAndFeel laf = supplier.get();
        try {
            UIManager.setLookAndFeel(laf);
            // Обновляем все открытые окна
            for (Window window : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(window);
            }
            log.info("Applied theme: {}", themeName);
        } catch (Exception e) {
            log.error("Failed to apply theme: {}", themeName, e);
        }
    }
}