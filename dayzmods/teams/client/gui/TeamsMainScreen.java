package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.PauseScreen;

public class TeamsMainScreen extends Screen {
    private final Screen parent;
    
    public TeamsMainScreen(Screen parent) {
        super(Component.literal("Команды"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int buttonWidth = 204;
        int buttonHeight = 20;
        int buttonX = (this.width - buttonWidth) / 2;
        int buttonY1 = this.height / 4 + 48;
        int buttonY2 = this.height / 4 + 72;
        
        this.addRenderableWidget(Button.builder(
            Component.literal("Invitations"),
            btn -> {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.setScreen(new InvitationsScreen(this));
                }
            }
        ).bounds(buttonX, buttonY1, buttonWidth, buttonHeight).build());
        
        this.addRenderableWidget(Button.builder(
            Component.literal("Invite"),
            btn -> {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.setScreen(new InvitePlayersScreen(this));
                }
            }
        ).bounds(buttonX, buttonY2, buttonWidth, buttonHeight).build());
        
        int backButtonWidth = 75;
        int backButtonHeight = 20;
        int backButtonX = 10;
        int backButtonY = 10;
        
        this.addRenderableWidget(Button.builder(
            Component.literal("Назад"),
            btn -> {
                if (minecraft != null) {
                    minecraft.setScreen(parent);
                }
            }
        ).bounds(backButtonX, backButtonY, backButtonWidth, backButtonHeight).build());
    }
    
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        super.render(g, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return parent instanceof PauseScreen;
    }
}
