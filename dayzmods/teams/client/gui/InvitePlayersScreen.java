package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.rustsayz.teams.packets.GetPlayersListPacket;
import com.rustsayz.teams.packets.InvitePlayerPacket;
import com.rustsayz.teams.packets.PlayersListResponsePacket;
import com.rustsayz.teams.client.ClientTeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.ArrayList;
import java.util.List;

public class InvitePlayersScreen extends ScrollablePlayerListScreen {
    
    public InvitePlayersScreen(Screen parent) {
        super(Component.literal("Invite"), parent);
    }
    
    @Override
    protected void init() {
        super.init();
        
        PacketHandler.sendToServer(new GetPlayersListPacket());
        
        ClientTeamManager manager = ClientTeamManager.getInstance();
        allPlayers = manager.getPlayersList();
        updateFilteredPlayers();
    }
    
    @Override
    protected String getListTitle() {
        return "Players";
    }
    
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        
        g.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0xFF1A1A1A);
        g.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF2D2D2D);
        
        String title = getListTitle();
        int titleX = listX + (listWidth - Minecraft.getInstance().font.width(title)) / 2;
        int titleY = 35;
        
        g.pose().pushPose();
        g.pose().translate(titleX, titleY, 0);
        g.pose().scale(1.2f, 1.2f, 1.0f);
        g.drawString(Minecraft.getInstance().font, Component.literal(title), 0, 0, 0xFFFFFF);
        g.pose().popPose();
        
        int maxScroll = Math.max(0, filteredPlayers.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        
        for (int i = 0; i < visibleRows && i + scrollOffset < filteredPlayers.size(); i++) {
            int y = listY + i * rowHeight;
            PlayersListResponsePacket.PlayerInfo player = filteredPlayers.get(i + scrollOffset);
            
            int rowColor = (i % 2 == 0) ? 0xFF2D2D2D : 0xFF252525;
            if (mouseX >= listX && mouseX < listX + listWidth && 
                mouseY >= y && mouseY < y + rowHeight) {
                rowColor = 0xFF404040;
            }
            g.fill(listX, y, listX + listWidth, y + rowHeight, rowColor);
            
            g.drawString(Minecraft.getInstance().font, Component.literal(player.getName()),
                listX + 5, y + (rowHeight - 9) / 2, 0xFFFFFF);
            
            renderRowExtra(g, player, listX, y, mouseX, mouseY);
        }
        
        if (filteredPlayers.size() > visibleRows) {
            int scrollBarMaxHeight = listHeight;
            int scrollBarThumbHeight = (int)((double)visibleRows / filteredPlayers.size() * scrollBarMaxHeight);
            int scrollBarThumbY = scrollBarY + (int)((double)scrollOffset / maxScroll * (scrollBarMaxHeight - scrollBarThumbHeight));
            
            g.fill(scrollBarX, scrollBarY, scrollBarX + scrollBarWidth, scrollBarY + scrollBarHeight, 0xFF1A1A1A);
            g.fill(scrollBarX, scrollBarThumbY, scrollBarX + scrollBarWidth, 
                scrollBarThumbY + scrollBarThumbHeight, 0xFF808080);
            g.vLine(scrollBarX, scrollBarY, scrollBarY + scrollBarHeight, 0xFF000000);
            g.vLine(scrollBarX + scrollBarWidth, scrollBarY, scrollBarY + scrollBarHeight, 0xFF000000);
        }
        
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(g, mouseX, mouseY, partialTick);
        }
    }
    
    @Override
    protected void renderRowExtra(GuiGraphics g, PlayersListResponsePacket.PlayerInfo player, 
                                  int x, int y, int mouseX, int mouseY) {
    }
    
    @Override
    protected boolean onPlayerClick(PlayersListResponsePacket.PlayerInfo player, int mouseX, int mouseY) {
        if (minecraft != null && minecraft.player != null) {
            InvitePlayerPacket packet = new InvitePlayerPacket(player.getUuid());
            PacketHandler.sendToServer(packet);
            onClose();
            return true;
        }
        return false;
    }
    
    public void refreshPlayersList() {
        ClientTeamManager manager = ClientTeamManager.getInstance();
        allPlayers = manager.getPlayersList();
        updateFilteredPlayers();
    }
}
