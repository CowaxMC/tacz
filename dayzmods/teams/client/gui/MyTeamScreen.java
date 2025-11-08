package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.packets.PlayersListResponsePacket;
import com.rustsayz.teams.packets.DisbandTeamPacket;
import com.rustsayz.teams.packets.ReassignLeaderPacket;
import com.rustsayz.teams.packets.InvitePlayerPacket;
import com.rustsayz.teams.PacketHandler;
import com.rustsayz.teams.client.ClientTeamManager;
import com.rustsayz.teams.packets.TeamUpdatePacket;
import java.util.*;

public class MyTeamScreen extends ScrollablePlayerListScreen {
    private List<Button> actionButtons = new ArrayList<>();
    
    public MyTeamScreen(Screen parent) {
        super(Component.literal("Моя команда"), parent);
    }
    
    @Override
    protected void init() {
        super.init();
        
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
        
        refreshTeamData();
    }
    
    public void refreshTeamData() {
        ClientTeamManager manager = ClientTeamManager.getInstance();
        if (manager.hasTeam()) {
            Set<UUID> memberIds = manager.getMembers();
            allPlayers.clear();
            
            Minecraft mc = Minecraft.getInstance();
            for (UUID memberId : memberIds) {
                String name = null;
                
                // Сначала пытаемся получить имя из сохраненных имен
                name = manager.getPlayerName(memberId);
                
                // Если имя не найдено в сохраненных, пытаемся получить из онлайн игроков
                if (name == null && mc != null && mc.level != null) {
                    var player = mc.level.getPlayerByUUID(memberId);
                    if (player != null) {
                        name = player.getDisplayName().getString();
                    } else if (mc.getConnection() != null && mc.getConnection().getOnlinePlayers() != null) {
                        var onlinePlayer = mc.getConnection().getOnlinePlayers().stream()
                            .filter(p -> p.getProfile().getId().equals(memberId))
                            .findFirst()
                            .orElse(null);
                        if (onlinePlayer != null) {
                            name = onlinePlayer.getProfile().getName();
                        }
                    }
                }
                
                // Если имя все еще не найдено, используем сохраненное имя из карты имен
                if (name == null) {
                    Map<UUID, String> savedNames = manager.getPlayerNames();
                    name = savedNames.get(memberId);
                }
                
                // Если имя все еще не найдено, используем UUID (но это не должно происходить)
                if (name == null) {
                    name = "Unknown Player";
                }
                
                allPlayers.add(new PlayersListResponsePacket.PlayerInfo(memberId, name));
            }
            updateFilteredPlayers();
            
            // Удаляем кнопки лидера, если они есть
            for (Button button : actionButtons) {
                this.removeWidget(button);
            }
            actionButtons.clear();
            
            // Создаем кнопки лидера только если текущий игрок является лидером
            if (manager.getLeader() != null && mc != null && mc.player != null && manager.getLeader().equals(mc.player.getUUID())) {
                createActionButtons();
            }
        }
    }
    
    private void createActionButtons() {
        // Удаляем старые кнопки перед созданием новых
        for (Button button : actionButtons) {
            this.removeWidget(button);
        }
        actionButtons.clear();
        
        int buttonWidth = 150;
        int buttonHeight = 20;
        int buttonSpacing = 5;
        int centerY = listY + listHeight / 2;
        
        int leftX = listX - buttonWidth - 10; // Сдвинуто еще правее на 10 пикселей
        int rightX = listX + listWidth + 10; // Сдвинуто левее на 10 пикселей
        
        actionButtons.add(Button.builder(
            Component.literal("Расформировать команду"),
            btn -> {
                PacketHandler.sendToServer(new DisbandTeamPacket());
                // Возвращаемся на предыдущий экран
                if (minecraft != null) {
                    minecraft.setScreen(parent);
                }
            }
        ).bounds(leftX, centerY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight).build());
        
        actionButtons.add(Button.builder(
            Component.literal("Настроить команду"),
            btn -> {
            }
        ).bounds(leftX, centerY, buttonWidth, buttonHeight).build());
        
        actionButtons.add(Button.builder(
            Component.literal("Переназначить лидера"),
            btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new ReassignLeaderScreen(this));
                }
            }
        ).bounds(rightX, centerY - buttonHeight - buttonSpacing, buttonWidth, buttonHeight).build());
        
        actionButtons.add(Button.builder(
            Component.literal("Пригласить в команду"),
            btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new InvitePlayersScreen(this));
                }
            }
        ).bounds(rightX, centerY, buttonWidth, buttonHeight).build());
        
        for (Button button : actionButtons) {
            this.addRenderableWidget(button);
        }
    }
    
    @Override
    protected String getListTitle() {
        return "Участники команды";
    }
    
    @Override
    protected void renderRowExtra(GuiGraphics g, PlayersListResponsePacket.PlayerInfo player, 
                                  int x, int y, int mouseX, int mouseY) {
        ClientTeamManager manager = ClientTeamManager.getInstance();
        if (manager.getLeader() != null && player.getUuid().equals(manager.getLeader())) {
            g.drawString(Minecraft.getInstance().font, Component.literal("(Лидер)"),
                x + listWidth - 60, y + (rowHeight - 9) / 2, 0xFFFFD700);
        }
    }
    
    @Override
    protected boolean onPlayerClick(PlayersListResponsePacket.PlayerInfo player, int mouseX, int mouseY) {
        return false;
    }
}
