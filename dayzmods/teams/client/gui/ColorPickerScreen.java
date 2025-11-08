package com.rustsayz.teams.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.client.TeamHeadSettings;

public class ColorPickerScreen extends Screen {
    private final Screen parent;
    private final TeamHeadSettings settings;
    private static final int[] COLORS = {
        0xFFFFFF, // Белый
        0xFF0000, // Красный
        0x00FF00, // Зеленый
        0x0000FF, // Синий
        0xFFFF00, // Желтый
        0xFF00FF, // Пурпурный
        0x00FFFF, // Голубой
        0xFFA500, // Оранжевый
        0x800080, // Фиолетовый
        0xFFC0CB, // Розовый
        0xA52A2A, // Коричневый
        0x808080, // Серый
        0x000000, // Черный
        0xADD8E6, // Светло-голубой
        0x90EE90, // Светло-зеленый
        0xFFD700  // Золотой
    };
    
    public ColorPickerScreen(Screen parent) {
        super(Component.literal("Выбор цвета"));
        this.parent = parent;
        this.settings = TeamHeadSettings.getInstance();
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int buttonSize = 30;
        int spacing = 5;
        int buttonsPerRow = 4;
        int startX = centerX - (buttonsPerRow * (buttonSize + spacing)) / 2;
        int startY = centerY - 60;
        
        // Создаем кастомные цветные кнопки
        for (int i = 0; i < COLORS.length; i++) {
            int color = COLORS[i];
            int row = i / buttonsPerRow;
            int col = i % buttonsPerRow;
            int x = startX + col * (buttonSize + spacing);
            int y = startY + row * (buttonSize + spacing);
            
            int finalColor = color;
            ColorButton colorButton = new ColorButton(x, y, buttonSize, buttonSize, finalColor, btn -> {
                settings.setInitialsColor(finalColor);
                if (this.minecraft != null) {
                    this.minecraft.setScreen(parent);
                }
            });
            
            this.addRenderableWidget(colorButton);
        }
        
        // Кнопка "Назад"
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
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 30, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    // Кастомная кнопка с цветным фоном
    private static class ColorButton extends Button {
        private final int color;
        
        public ColorButton(int x, int y, int width, int height, int color, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.color = color;
        }
        
        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Рисуем цветной фон
            int bgColor = isHovered() ? 0xFF000000 | color : 0xFF000000 | color;
            graphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);
            
            // Рисуем рамку (светлее при наведении)
            int borderColor = isHovered() ? 0xFFFFFFFF : 0xFF808080;
            graphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
            graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);
            graphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
            graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        }
    }
}

