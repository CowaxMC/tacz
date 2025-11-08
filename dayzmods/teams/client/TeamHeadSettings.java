package com.rustsayz.teams.client;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public class TeamHeadSettings {
    private static TeamHeadSettings instance;
    
    private float headSize = 1.0f; // Размер головы (0.2 - 2.0)
    private float headAlpha = 0.8f; // Прозрачность головы (0.0 - 1.0)
    private boolean headPositionInverted = true; // Инверсия расположения головы (включена по умолчанию)
    private boolean headMarkersEnabled = true; // Включены ли метки головы
    private int initialsColor = 0xFFFFFF; // Цвет первых 2-х букв имени (по умолчанию белый)
    private Path configFile;
    
    private TeamHeadSettings() {
    }
    
    public static TeamHeadSettings getInstance() {
        if (instance == null) {
            instance = new TeamHeadSettings();
        }
        return instance;
    }
    
    public void init() {
        if (configFile != null) {
            return; // Уже инициализировано
        }
        if (Minecraft.getInstance().gameDirectory != null) {
            configFile = Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("teams_mod")
                .resolve("head_settings.txt");
            loadSettings();
        }
    }
    
    public float getHeadSize() {
        return headSize;
    }
    
    public void setHeadSize(float size) {
        this.headSize = Math.max(0.2f, Math.min(2.0f, size));
        saveSettings();
    }
    
    public float getHeadAlpha() {
        return headAlpha;
    }
    
    public void setHeadAlpha(float alpha) {
        this.headAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
        saveSettings();
    }
    
    public boolean isHeadPositionInverted() {
        return headPositionInverted;
    }
    
    public void setHeadPositionInverted(boolean inverted) {
        this.headPositionInverted = inverted;
        saveSettings();
    }
    
    public boolean isHeadMarkersEnabled() {
        // Если настройки еще не инициализированы, возвращаем значение по умолчанию
        if (configFile == null && Minecraft.getInstance().gameDirectory != null) {
            init();
        }
        return headMarkersEnabled;
    }
    
    public void setHeadMarkersEnabled(boolean enabled) {
        this.headMarkersEnabled = enabled;
        saveSettings();
    }
    
    public int getInitialsColor() {
        return initialsColor;
    }
    
    public void setInitialsColor(int color) {
        this.initialsColor = color;
        saveSettings();
    }
    
    private void saveSettings() {
        if (configFile == null) return;
        
        try {
            Files.createDirectories(configFile.getParent());
            try (PrintWriter writer = new PrintWriter(new FileWriter(configFile.toFile()))) {
                writer.println("headSize=" + headSize);
                writer.println("headAlpha=" + headAlpha);
                writer.println("headPositionInverted=" + headPositionInverted);
                writer.println("headMarkersEnabled=" + headMarkersEnabled);
                writer.println("initialsColor=" + Integer.toHexString(initialsColor));
            }
        } catch (IOException e) {
            System.err.println("[TeamHeadSettings] Ошибка при сохранении настроек: " + e.getMessage());
        }
    }
    
    private void loadSettings() {
        if (configFile == null || !Files.exists(configFile)) {
            // Если файл не существует, используем значения по умолчанию
            return;
        }
        
        boolean foundMarkersEnabled = false;
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("headSize=")) {
                    try {
                        headSize = Float.parseFloat(line.substring(9));
                        headSize = Math.max(0.2f, Math.min(2.0f, headSize));
                    } catch (NumberFormatException e) {
                        // Игнорируем неверный формат
                    }
                } else if (line.startsWith("headAlpha=")) {
                    try {
                        headAlpha = Float.parseFloat(line.substring(10));
                        headAlpha = Math.max(0.0f, Math.min(1.0f, headAlpha));
                    } catch (NumberFormatException e) {
                        // Игнорируем неверный формат
                    }
                } else if (line.startsWith("headPositionInverted=")) {
                    try {
                        headPositionInverted = Boolean.parseBoolean(line.substring(21));
                    } catch (Exception e) {
                        // Игнорируем неверный формат
                    }
                } else if (line.startsWith("headMarkersEnabled=")) {
                    foundMarkersEnabled = true;
                    try {
                        headMarkersEnabled = Boolean.parseBoolean(line.substring(19));
                    } catch (Exception e) {
                        // Игнорируем неверный формат, используем значение по умолчанию
                        headMarkersEnabled = true;
                    }
                } else if (line.startsWith("initialsColor=")) {
                    try {
                        String colorStr = line.substring(14).trim();
                        // Пытаемся загрузить как hex
                        if (colorStr.startsWith("0x") || colorStr.startsWith("0X")) {
                            initialsColor = Integer.parseUnsignedInt(colorStr.substring(2), 16);
                        } else {
                            // Пытаемся загрузить как hex без префикса
                            initialsColor = Integer.parseUnsignedInt(colorStr, 16);
                        }
                    } catch (Exception e) {
                        // Игнорируем неверный формат, используем значение по умолчанию
                        initialsColor = 0xFFFFFF;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[TeamHeadSettings] Ошибка при загрузке настроек: " + e.getMessage());
        }
        
        // Если параметр headMarkersEnabled не найден в файле, используем значение по умолчанию (true)
        if (!foundMarkersEnabled) {
            headMarkersEnabled = true;
        }
    }
}

