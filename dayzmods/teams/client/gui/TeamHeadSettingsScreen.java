package com.rustsayz.teams.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.client.TeamHeadSettings;
import java.text.DecimalFormat;

public class TeamHeadSettingsScreen extends Screen {
    private final Screen parent;
    private final TeamHeadSettings settings;
    private AbstractSliderButton sizeSlider;
    private AbstractSliderButton alphaSlider;
    private Button toggleMarkersButton;
    private ColorPickerButton colorButton;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    
    public TeamHeadSettingsScreen(Screen parent) {
        super(Component.literal("Настройки голов команды"));
        this.parent = parent;
        this.settings = TeamHeadSettings.getInstance();
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // Ползунок размера головы
        float normalizedSize = (settings.getHeadSize() - 0.2f) / 1.8f; // Нормализуем от 0.2 до 2.0 -> 0.0 до 1.0
        this.sizeSlider = new AbstractSliderButton(
            centerX - 150, centerY - 40, 300, 20,
            Component.literal("Размер головы: " + DECIMAL_FORMAT.format(settings.getHeadSize())),
            normalizedSize
        ) {
            @Override
            protected void updateMessage() {
                float value = (float) this.value * 1.8f + 0.2f; // Преобразуем обратно
                settings.setHeadSize(value);
                this.setMessage(Component.literal("Размер головы: " + DECIMAL_FORMAT.format(value)));
            }
            
            @Override
            protected void applyValue() {
                float value = (float) this.value * 1.8f + 0.2f;
                settings.setHeadSize(value);
            }
        };
        this.addRenderableWidget(sizeSlider);
        
        // Ползунок прозрачности
        this.alphaSlider = new AbstractSliderButton(
            centerX - 150, centerY, 300, 20,
            Component.literal("Прозрачность: " + DECIMAL_FORMAT.format(settings.getHeadAlpha() * 100) + "%"),
            settings.getHeadAlpha()
        ) {
            @Override
            protected void updateMessage() {
                float value = (float) this.value;
                settings.setHeadAlpha(value);
                this.setMessage(Component.literal("Прозрачность: " + DECIMAL_FORMAT.format(value * 100) + "%"));
            }
            
            @Override
            protected void applyValue() {
                settings.setHeadAlpha((float) this.value);
            }
        };
        this.addRenderableWidget(alphaSlider);
        
        // Кнопка переключения меток головы
        this.toggleMarkersButton = Button.builder(
            Component.literal(settings.isHeadMarkersEnabled() ? "Метки голов: Вкл" : "Метки голов: Выкл"),
            btn -> {
                settings.setHeadMarkersEnabled(!settings.isHeadMarkersEnabled());
                this.toggleMarkersButton.setMessage(Component.literal(settings.isHeadMarkersEnabled() ? "Метки голов: Вкл" : "Метки голов: Выкл"));
            }
        ).bounds(centerX - 150, centerY + 50, 300, 20).build();
        this.addRenderableWidget(toggleMarkersButton);
        
        // Кнопка выбора цвета первых 2-х букв имени
        this.colorButton = new ColorPickerButton(
            centerX - 150, centerY + 80, 300, 20,
            settings.getInitialsColor(),
            btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ColorPickerScreen(this));
                }
            }
        );
        this.addRenderableWidget(colorButton);
        
        // Кнопка "Назад" (слева сверху)
        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(parent);
                }
            }
        ).bounds(10, 10, 60, 20).build());
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Обновляем цвет на кнопке, если он изменился
        if (colorButton != null) {
            colorButton.updateColor(settings.getInitialsColor());
        }
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    // Кастомная кнопка с отображением текущего цвета
    private static class ColorPickerButton extends Button {
        private int currentColor;
        
        public ColorPickerButton(int x, int y, int width, int height, int color, OnPress onPress) {
            super(x, y, width, height, Component.literal("Цвет 2-x первый букв имени"), onPress, DEFAULT_NARRATION);
            this.currentColor = color;
        }
        
        public void updateColor(int color) {
            this.currentColor = color;
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Рисуем стандартный фон кнопки
            int bgColor = isHovered() ? 0xFF404040 : 0xFF2C2C2C;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            
            // Рисуем рамку
            int borderColor = isHovered() ? 0xFFFFFFFF : 0xFF808080;
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
            
            // Рисуем текст
            var font = net.minecraft.client.Minecraft.getInstance().font;
            int textWidth = font.width(getMessage());
            int textX = getX() + (width - textWidth) / 2 - 15; // Смещаем влево, чтобы освободить место для цветного квадрата
            int textY = getY() + (height - font.lineHeight) / 2;
            graphics.drawString(font, getMessage(), textX, textY, 0xFFFFFF, false);
            
            // Рисуем цветной квадрат справа от текста
            int colorSquareSize = 14;
            int colorSquareX = getX() + width - colorSquareSize - 5;
            int colorSquareY = getY() + (height - colorSquareSize) / 2;
            
            // Рисуем цветной квадрат
            graphics.fill(colorSquareX, colorSquareY, colorSquareX + colorSquareSize, colorSquareY + colorSquareSize, 0xFF000000 | currentColor);
            
            // Рисуем рамку вокруг цветного квадрата
            graphics.fill(colorSquareX - 1, colorSquareY - 1, colorSquareX + colorSquareSize + 1, colorSquareY, 0xFF000000);
            graphics.fill(colorSquareX - 1, colorSquareY + colorSquareSize, colorSquareX + colorSquareSize + 1, colorSquareY + colorSquareSize + 1, 0xFF000000);
            graphics.fill(colorSquareX - 1, colorSquareY - 1, colorSquareX, colorSquareY + colorSquareSize + 1, 0xFF000000);
            graphics.fill(colorSquareX + colorSquareSize, colorSquareY - 1, colorSquareX + colorSquareSize + 1, colorSquareY + colorSquareSize + 1, 0xFF000000);
        }
    }
}

