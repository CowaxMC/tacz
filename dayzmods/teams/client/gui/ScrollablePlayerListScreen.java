package com.rustsayz.teams.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.PauseScreen;
import com.rustsayz.teams.packets.PlayersListResponsePacket;
import java.util.*;
import java.util.stream.Collectors;

public abstract class ScrollablePlayerListScreen extends Screen {
    protected final Screen parent;
    protected List<PlayersListResponsePacket.PlayerInfo> allPlayers = new ArrayList<>();
    protected List<PlayersListResponsePacket.PlayerInfo> filteredPlayers = new ArrayList<>();
    protected String searchQuery = "";
    protected EditBox searchBox;
    protected int scrollOffset = 0;
    protected int visibleRows = 10;
    protected int rowHeight = 20;
    protected int listWidth = 300;
    protected int listHeight = visibleRows * rowHeight;
    protected int listX, listY;
    protected int scrollBarX, scrollBarY, scrollBarWidth = 8, scrollBarHeight;
    protected boolean isDraggingScroll = false;
    protected int dragStartY = 0;
    
    protected ScrollablePlayerListScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        super.init();
        
        listX = (this.width - listWidth) / 2;
        listY = this.height / 4 + 20;
        listHeight = visibleRows * rowHeight;
        scrollBarX = listX + listWidth + 2;
        scrollBarY = listY;
        scrollBarHeight = listHeight;
        
        int searchBoxWidth = 200;
        int searchBoxHeight = 20;
        int searchBoxX = listX + (listWidth - searchBoxWidth) / 2;
        int searchBoxY = listY - 30;
        
        searchBox = new EditBox(Minecraft.getInstance().font, searchBoxX, searchBoxY, searchBoxWidth, searchBoxHeight, Component.literal("Поиск"));
        searchBox.setMaxLength(50);
        searchBox.setValue(searchQuery);
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);
        
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
        
        updateFilteredPlayers();
    }
    
    protected void onSearchChanged(String query) {
        searchQuery = query;
        updateFilteredPlayers();
        scrollOffset = 0;
    }
    
    protected void updateFilteredPlayers() {
        if (searchQuery.isEmpty()) {
            filteredPlayers = new ArrayList<>(allPlayers);
        } else {
            String lowerQuery = searchQuery.toLowerCase();
            filteredPlayers = allPlayers.stream()
                .filter(p -> p.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        }
    }
    
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        
        g.fill(listX - 2, listY - 2, listX + listWidth + 2, listY + listHeight + 2, 0xFF1A1A1A);
        g.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF2D2D2D);
        
        String title = getListTitle();
        int titleWidth = Minecraft.getInstance().font.width(title);
        g.drawString(Minecraft.getInstance().font, Component.literal(title), 
            listX + (listWidth - titleWidth) / 2, 35, 0xFFFFFF);
        
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
        
        super.render(g, mouseX, mouseY, partialTick);
    }
    
    protected abstract void renderRowExtra(GuiGraphics g, PlayersListResponsePacket.PlayerInfo player, 
                                          int x, int y, int mouseX, int mouseY);
    
    protected abstract String getListTitle();
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        int listMouseX = (int)mouseX;
        int listMouseY = (int)mouseY;
        
        if (listMouseX >= listX && listMouseX < listX + listWidth &&
            listMouseY >= listY && listMouseY < listY + listHeight) {
            int row = (listMouseY - listY) / rowHeight;
            if (row >= 0 && row < visibleRows && row + scrollOffset < filteredPlayers.size()) {
                PlayersListResponsePacket.PlayerInfo player = filteredPlayers.get(row + scrollOffset);
                if (onPlayerClick(player, listMouseX, listMouseY)) {
                    return true;
                }
            }
        }
        
        if (filteredPlayers.size() > visibleRows &&
            listMouseX >= scrollBarX && listMouseX < scrollBarX + scrollBarWidth &&
            listMouseY >= scrollBarY && listMouseY < scrollBarY + scrollBarHeight) {
            int maxScroll = filteredPlayers.size() - visibleRows;
            double scrollPercent = (listMouseY - scrollBarY) / (double)scrollBarHeight;
            scrollOffset = (int)(scrollPercent * maxScroll);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            isDraggingScroll = true;
            dragStartY = (int)mouseY;
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingScroll && button == 0) {
            int maxScroll = filteredPlayers.size() - visibleRows;
            double scrollPercent = (mouseY - scrollBarY) / (double)scrollBarHeight;
            scrollOffset = (int)(scrollPercent * maxScroll);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= listX && mouseX < listX + listWidth + scrollBarWidth &&
            mouseY >= listY && mouseY < listY + listHeight) {
            int maxScroll = filteredPlayers.size() - visibleRows;
            scrollOffset -= (int)delta;
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
    
    protected abstract boolean onPlayerClick(PlayersListResponsePacket.PlayerInfo player, int mouseX, int mouseY);
    
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
