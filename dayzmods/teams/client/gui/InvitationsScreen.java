package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.packets.GetInvitationsPacket;
import com.rustsayz.teams.packets.AcceptInvitationPacket;
import com.rustsayz.teams.packets.DeclineInvitationPacket;
import com.rustsayz.teams.packets.PlayersListResponsePacket;
import com.rustsayz.teams.client.ClientTeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.ArrayList;
import java.util.List;

public class InvitationsScreen extends ScrollablePlayerListScreen {
    private static final int ACCEPT_BUTTON_SIZE = 12;
    private static final int DECLINE_BUTTON_SIZE = 12;
    
    public InvitationsScreen(Screen parent) {
        super(Component.literal("Invitations"), parent);
    }
    
    @Override
    protected void init() {
        super.init();
        
        PacketHandler.sendToServer(new GetInvitationsPacket());
        
        ClientTeamManager manager = ClientTeamManager.getInstance();
        allPlayers = manager.getInvitationsList();
        updateFilteredPlayers();
    }
    
    @Override
    protected String getListTitle() {
        return "Invitations";
    }
    
    @Override
    protected void renderRowExtra(GuiGraphics g, PlayersListResponsePacket.PlayerInfo player, 
                                  int x, int y, int mouseX, int mouseY) {
        int buttonY = y + (rowHeight - ACCEPT_BUTTON_SIZE) / 2;
        int acceptX = x + listWidth - DECLINE_BUTTON_SIZE - ACCEPT_BUTTON_SIZE - 4;
        int declineX = x + listWidth - DECLINE_BUTTON_SIZE - 2;
        
        int acceptColor = 0xFF00FF00;
        if (mouseX >= acceptX && mouseX < acceptX + ACCEPT_BUTTON_SIZE &&
            mouseY >= buttonY && mouseY < buttonY + ACCEPT_BUTTON_SIZE) {
            acceptColor = 0xFF00AA00;
        }
        g.fill(acceptX, buttonY, acceptX + ACCEPT_BUTTON_SIZE, buttonY + ACCEPT_BUTTON_SIZE, acceptColor);
        int textY = buttonY + (ACCEPT_BUTTON_SIZE - 9) / 2;
        g.drawString(Minecraft.getInstance().font, Component.literal("✓"), acceptX + 2, textY, 0x000000);
        
        int declineColor = 0xFFFF0000;
        if (mouseX >= declineX && mouseX < declineX + DECLINE_BUTTON_SIZE &&
            mouseY >= buttonY && mouseY < buttonY + DECLINE_BUTTON_SIZE) {
            declineColor = 0xFFAA0000;
        }
        g.fill(declineX, buttonY, declineX + DECLINE_BUTTON_SIZE, buttonY + DECLINE_BUTTON_SIZE, declineColor);
        g.drawString(Minecraft.getInstance().font, Component.literal("✗"), declineX + 2, textY, 0x000000);
    }
    
    @Override
    protected boolean onPlayerClick(PlayersListResponsePacket.PlayerInfo player, int mouseX, int mouseY) {
        int row = (mouseY - listY) / rowHeight;
        if (row < 0 || row >= visibleRows || row + scrollOffset >= filteredPlayers.size()) {
            return false;
        }
        
        int buttonY = listY + row * rowHeight + (rowHeight - ACCEPT_BUTTON_SIZE) / 2;
        int acceptX = listX + listWidth - DECLINE_BUTTON_SIZE - ACCEPT_BUTTON_SIZE - 4;
        int declineX = listX + listWidth - DECLINE_BUTTON_SIZE - 2;
        
        if (mouseX >= acceptX && mouseX < acceptX + ACCEPT_BUTTON_SIZE &&
            mouseY >= buttonY && mouseY < buttonY + ACCEPT_BUTTON_SIZE) {
            AcceptInvitationPacket packet = new AcceptInvitationPacket(player.getUuid());
            PacketHandler.sendToServer(packet);
            onClose();
            return true;
        }
        
        if (mouseX >= declineX && mouseX < declineX + DECLINE_BUTTON_SIZE &&
            mouseY >= buttonY && mouseY < buttonY + DECLINE_BUTTON_SIZE) {
            DeclineInvitationPacket packet = new DeclineInvitationPacket(player.getUuid());
            PacketHandler.sendToServer(packet);
            allPlayers.removeIf(p -> p.getUuid().equals(player.getUuid()));
            updateFilteredPlayers();
            return true;
        }
        
        return false;
    }
    
    public void refreshInvitationsList() {
        ClientTeamManager manager = ClientTeamManager.getInstance();
        allPlayers = manager.getInvitationsList();
        updateFilteredPlayers();
    }
}
