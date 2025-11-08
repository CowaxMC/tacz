package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.packets.PlayersListResponsePacket;
import com.rustsayz.teams.packets.ReassignLeaderPacket;
import com.rustsayz.teams.PacketHandler;
import com.rustsayz.teams.client.ClientTeamManager;
import java.util.*;

public class ReassignLeaderScreen extends ScrollablePlayerListScreen {
    
    public ReassignLeaderScreen(Screen parent) {
        super(Component.literal("Переназначить лидера"), parent);
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
        ClientTeamManager manager = ClientTeamManager.getInstance();
        
        // Нельзя переназначить лидера на самого себя
        if (manager.getLeader() != null && player.getUuid().equals(manager.getLeader())) {
            return false;
        }
        
        // Отправляем пакет для переназначения лидера
        if (minecraft != null && minecraft.player != null) {
            ReassignLeaderPacket packet = new ReassignLeaderPacket(player.getUuid());
            PacketHandler.sendToServer(packet);
            
            // Закрываем экран
            if (minecraft != null) {
                minecraft.setScreen(parent);
            }
            return true;
        }
        return false;
    }
}

