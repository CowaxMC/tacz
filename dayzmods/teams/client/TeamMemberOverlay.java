package com.rustsayz.teams.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import com.rustsayz.teams.client.ClientTeamManager;
import com.rustsayz.teams.client.TeamHeadSettings;
import com.rustsayz.teams.client.ClientPlayerSkinCache;
import com.rustsayz.teams.packets.TeamMemberPositionPacket;
import java.util.*;

public class TeamMemberOverlay implements IGuiOverlay {
    
    public static final TeamMemberOverlay INSTANCE = new TeamMemberOverlay();
    private static final int ICON_SIZE = 16;
    private static final int EDGE_MARGIN = 20;
    
    private TeamMemberOverlay() {
    }
    
    @Override
    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        
        if (localPlayer == null || mc.level == null) {
            return;
        }
        
        ClientTeamManager manager = ClientTeamManager.getInstance();
        if (!manager.hasTeam()) {
            return;
        }
        
        // Проверяем, включены ли метки головы
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        // Инициализируем настройки, если еще не инициализированы
        if (Minecraft.getInstance().gameDirectory != null) {
            settings.init();
        }
        if (!settings.isHeadMarkersEnabled()) {
            return;
        }
        
        Set<UUID> teamMembers = manager.getMembers();
        Map<UUID, com.rustsayz.teams.packets.TeamMemberPositionPacket.Position> memberPositions = manager.getAllMemberPositions();
        
        // Обрабатываем всех игроков команды, включая тех, кто вне зоны прорисовки
        for (UUID memberId : teamMembers) {
            if (memberId.equals(localPlayer.getUUID())) {
                continue; // Пропускаем самого себя
            }
            
            Player teamMember = mc.level.getPlayerByUUID(memberId);
            com.rustsayz.teams.packets.TeamMemberPositionPacket.Position position = memberPositions.get(memberId);
            
            // Если игрок не в загруженных чанках, но есть его позиция с сервера
            if (teamMember == null && position != null) {
                // Отображаем иконку используя позицию с сервера
                renderTeamMemberIconFromPosition(graphics, localPlayer, memberId, position, screenWidth, screenHeight, partialTick);
                continue;
            }
            
            // Если игрок в загруженных чанках
            if (teamMember != null) {
                // Всегда отображаем иконку для игроков команды, если они не видны напрямую
                // Или если они находятся на расстоянии более 32 блоков
                double distance = localPlayer.distanceTo(teamMember);
                if (distance > 32.0 || !isPlayerVisible(mc, localPlayer, teamMember)) {
                    // Игрок далеко или не виден, отображаем иконку
                    renderTeamMemberIcon(graphics, localPlayer, teamMember, screenWidth, screenHeight, partialTick);
                }
            }
        }
    }
    
    private boolean isPlayerVisible(Minecraft mc, LocalPlayer localPlayer, Player teamMember) {
        // Проверяем, находится ли игрок в поле зрения камеры
        // Используем упрощенную проверку: если игрок в радиусе видимости и не заблокирован стенами
        
        double distance = localPlayer.distanceTo(teamMember);
        // Если игрок слишком далеко, считаем его скрытым
        if (distance > 128.0) {
            return false;
        }
        
        // Проверяем, находится ли игрок в поле зрения камеры
        // Вычисляем угол между направлением взгляда и направлением к игроку
        double dx = teamMember.getX() - localPlayer.getX();
        double dz = teamMember.getZ() - localPlayer.getZ();
        double distance2D = Math.sqrt(dx * dx + dz * dz);
        
        if (distance2D < 0.1) {
            return true; // Игрок очень близко
        }
        
        // Вычисляем угол направления взгляда
        double yaw = Math.toRadians(localPlayer.getYRot());
        double lookX = -Math.sin(yaw);
        double lookZ = Math.cos(yaw);
        
        // Вычисляем направление к игроку
        double toPlayerX = dx / distance2D;
        double toPlayerZ = dz / distance2D;
        
        // Вычисляем угол между направлением взгляда и направлением к игроку
        double dot = lookX * toPlayerX + lookZ * toPlayerZ;
        double angle = Math.acos(Mth.clamp(dot, -1.0, 1.0));
        
        // Если угол больше 90 градусов (игрок за спиной или сбоку), считаем его скрытым
        // Но если он очень близко, все равно показываем
        if (angle > Math.PI / 2 && distance > 16.0) {
            return false;
        }
        
        // Проверяем, виден ли игрок через стены (используем простую проверку расстояния)
        // В реальности нужно использовать raycast, но для простоты используем расстояние
        return distance <= 32.0 || teamMember.hasEffect(MobEffects.GLOWING);
    }
    
    private void renderTeamMemberIcon(GuiGraphics graphics, LocalPlayer localPlayer, Player teamMember, 
                                      int screenWidth, int screenHeight, float partialTick) {
        // Вычисляем направление к игроку
        double dx = teamMember.getX() - localPlayer.getX();
        double dy = teamMember.getY() - localPlayer.getY();
        double dz = teamMember.getZ() - localPlayer.getZ();
        
        // Вычисляем угол относительно направления взгляда игрока
        double yaw = Math.toRadians(localPlayer.getYRot() + partialTick * (localPlayer.getYRot() - localPlayer.yRotO));
        double pitch = Math.toRadians(localPlayer.getXRot() + partialTick * (localPlayer.getXRot() - localPlayer.xRotO));
        
        // Преобразуем координаты в систему координат камеры
        double forward = -Math.sin(yaw) * dx + Math.cos(yaw) * dz;
        double right = Math.cos(yaw) * dx + Math.sin(yaw) * dz;
        double up = -Math.sin(pitch) * Math.sqrt(dx * dx + dz * dz) + Math.cos(pitch) * dy;
        
        // Применяем инверсию расположения если включена (только право/лево)
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        if (settings.isHeadPositionInverted()) {
            right = -right; // Инвертируем только горизонтальную составляющую
        }
        
        // Вычисляем угол на экране
        double angle = Math.atan2(right, forward);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Вычисляем позицию иконки на краю экрана
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int radius = Math.min(screenWidth, screenHeight) / 2 - EDGE_MARGIN;
        
        int iconX = centerX + (int)(Math.sin(angle) * radius) - ICON_SIZE / 2;
        int iconY = centerY - (int)(Math.cos(angle) * radius) - ICON_SIZE / 2;
        
        // Ограничиваем позицию в пределах экрана
        iconX = Mth.clamp(iconX, EDGE_MARGIN, screenWidth - EDGE_MARGIN - ICON_SIZE);
        iconY = Mth.clamp(iconY, EDGE_MARGIN, screenHeight - EDGE_MARGIN - ICON_SIZE);
        
        // Рисуем иконку головы игрока
        renderPlayerHeadIcon(graphics, teamMember, iconX, iconY, distance);
    }
    
    private void renderPlayerHeadIcon(GuiGraphics graphics, Player player, int x, int y, double distance) {
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        float headSize = settings.getHeadSize();
        float headAlpha = settings.getHeadAlpha();
        
        // Вычисляем размер иконки с учетом настроек
        int actualIconSize = (int)(ICON_SIZE * headSize);
        
        // Рисуем фон иконки
        int bgAlpha = (int)(128 * headAlpha);
        graphics.fill(x - 2, y - 2, x + actualIconSize + 2, y + actualIconSize + 2, (bgAlpha << 24) | 0x000000);
        
        // Рисуем рамку
        int borderAlpha = (int)(255 * headAlpha);
        int borderColor = (borderAlpha << 24) | 0x00FF00; // Зеленая рамка
        graphics.fill(x - 2, y - 2, x + actualIconSize + 2, y - 1, borderColor);
        graphics.fill(x - 2, y + actualIconSize + 1, x + actualIconSize + 2, y + actualIconSize + 2, borderColor);
        graphics.fill(x - 2, y - 2, x - 1, y + actualIconSize + 2, borderColor);
        graphics.fill(x + actualIconSize + 1, y - 2, x + actualIconSize + 2, y + actualIconSize + 2, borderColor);
        
        // Рисуем иконку головы игрока (используем скин)
        ResourceLocation skinTexture = null;
        
        if (player instanceof AbstractClientPlayer clientPlayer) {
            try {
                skinTexture = clientPlayer.getSkinTextureLocation();
            } catch (Exception e) {
                // Игнорируем ошибки
            }
        }
        
        if (skinTexture != null) {
            try {
                // Убеждаемся, что текстура загружена
                var textureManager = Minecraft.getInstance().getTextureManager();
                // Загружаем текстуру если нужно
                textureManager.getTexture(skinTexture, null);
                
                // Устанавливаем альфу для прозрачности
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, headAlpha);
                
                // Рисуем голову из скина с учетом размера
                graphics.blit(skinTexture, x, y, actualIconSize, actualIconSize, 8, 8, 8, 8, 64, 64);
                // Рисуем слой шлема/аксессуаров
                graphics.blit(skinTexture, x, y, actualIconSize, actualIconSize, 40, 8, 8, 8, 64, 64);
                
                // Восстанавливаем цвет
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                // Если произошла ошибка при рисовании, показываем первые 2 буквы имени
                ClientTeamManager manager = ClientTeamManager.getInstance();
                renderPlayerInitials(graphics, manager, player.getUUID(), x, y, actualIconSize, headAlpha);
            }
        } else {
            // Если не удалось получить текстуру скина, показываем первые 2 буквы имени
            ClientTeamManager manager = ClientTeamManager.getInstance();
            renderPlayerInitials(graphics, manager, player.getUUID(), x, y, actualIconSize, headAlpha);
        }
        
        // Отображаем расстояние
        String distanceStr = String.format("%.0fm", distance);
        int textX = x + actualIconSize / 2 - Minecraft.getInstance().font.width(distanceStr) / 2;
        int textY = y + actualIconSize + 2;
        int textAlpha = (int)(255 * headAlpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        graphics.drawString(Minecraft.getInstance().font, distanceStr, textX, textY, textColor, true);
    }
    
    private void renderTeamMemberIconFromPosition(GuiGraphics graphics, LocalPlayer localPlayer, UUID memberId,
                                                   TeamMemberPositionPacket.Position position, 
                                                   int screenWidth, int screenHeight, float partialTick) {
        // Вычисляем направление к игроку используя позицию с сервера
        double dx = position.getX() - localPlayer.getX();
        double dy = position.getY() - localPlayer.getY();
        double dz = position.getZ() - localPlayer.getZ();
        
        // Вычисляем угол относительно направления взгляда игрока
        double yaw = Math.toRadians(localPlayer.getYRot() + partialTick * (localPlayer.getYRot() - localPlayer.yRotO));
        double pitch = Math.toRadians(localPlayer.getXRot() + partialTick * (localPlayer.getXRot() - localPlayer.xRotO));
        
        // Преобразуем координаты в систему координат камеры
        double forward = -Math.sin(yaw) * dx + Math.cos(yaw) * dz;
        double right = Math.cos(yaw) * dx + Math.sin(yaw) * dz;
        double up = -Math.sin(pitch) * Math.sqrt(dx * dx + dz * dz) + Math.cos(pitch) * dy;
        
        // Применяем инверсию расположения если включена (только право/лево)
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        if (settings.isHeadPositionInverted()) {
            right = -right; // Инвертируем только горизонтальную составляющую
        }
        
        // Вычисляем угол на экране
        double angle = Math.atan2(right, forward);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Вычисляем позицию иконки на краю экрана
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int radius = Math.min(screenWidth, screenHeight) / 2 - EDGE_MARGIN;
        
        int iconX = centerX + (int)(Math.sin(angle) * radius) - ICON_SIZE / 2;
        int iconY = centerY - (int)(Math.cos(angle) * radius) - ICON_SIZE / 2;
        
        // Ограничиваем позицию в пределах экрана
        iconX = Mth.clamp(iconX, EDGE_MARGIN, screenWidth - EDGE_MARGIN - ICON_SIZE);
        iconY = Mth.clamp(iconY, EDGE_MARGIN, screenHeight - EDGE_MARGIN - ICON_SIZE);
        
        // Рисуем иконку головы игрока используя сохраненное имя
        renderPlayerHeadIconFromUUID(graphics, memberId, iconX, iconY, distance);
    }
    
    private void renderPlayerHeadIconFromUUID(GuiGraphics graphics, UUID playerId, int x, int y, double distance) {
        ClientTeamManager manager = ClientTeamManager.getInstance();
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        float headSize = settings.getHeadSize();
        float headAlpha = settings.getHeadAlpha();
        
        // Вычисляем размер иконки с учетом настроек
        int actualIconSize = (int)(ICON_SIZE * headSize);
        
        // Рисуем фон иконки
        int bgAlpha = (int)(128 * headAlpha);
        graphics.fill(x - 2, y - 2, x + actualIconSize + 2, y + actualIconSize + 2, (bgAlpha << 24) | 0x000000);
        
        // Рисуем рамку
        int borderAlpha = (int)(255 * headAlpha);
        int borderColor = (borderAlpha << 24) | 0x00FF00; // Зеленая рамка
        graphics.fill(x - 2, y - 2, x + actualIconSize + 2, y - 1, borderColor);
        graphics.fill(x - 2, y + actualIconSize + 1, x + actualIconSize + 2, y + actualIconSize + 2, borderColor);
        graphics.fill(x - 2, y - 2, x - 1, y + actualIconSize + 2, borderColor);
        graphics.fill(x + actualIconSize + 1, y - 2, x + actualIconSize + 2, y + actualIconSize + 2, borderColor);
        
        // Проверяем, находится ли игрок в загруженных чанках
        Minecraft mc = Minecraft.getInstance();
        boolean playerInLoadedChunks = false;
        ResourceLocation skinTexture = null;
        
        if (mc.level != null) {
            Player player = mc.level.getPlayerByUUID(playerId);
            if (player instanceof AbstractClientPlayer clientPlayer) {
                playerInLoadedChunks = true;
                ClientPlayerSkinCache skinCache = ClientPlayerSkinCache.getInstance();
                skinTexture = skinCache.getOrCacheSkin(playerId, clientPlayer);
            }
        }
        
        if (playerInLoadedChunks && skinTexture != null) {
            // Игрок в загруженных чанках - рисуем иконку скина
            try {
                // Убеждаемся, что текстура загружена
                var textureManager = mc.getTextureManager();
                // Загружаем текстуру если нужно
                textureManager.getTexture(skinTexture, null);
                
                // Устанавливаем альфу для прозрачности
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, headAlpha);
                
                // Рисуем голову из скина с учетом размера
                graphics.blit(skinTexture, x, y, actualIconSize, actualIconSize, 8, 8, 8, 8, 64, 64);
                // Рисуем слой шлема/аксессуаров
                graphics.blit(skinTexture, x, y, actualIconSize, actualIconSize, 40, 8, 8, 8, 64, 64);
                
                // Восстанавливаем цвет
                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                RenderSystem.disableBlend();
            } catch (Exception e) {
                // Если произошла ошибка при рисовании, показываем первые 2 буквы имени
                renderPlayerInitials(graphics, manager, playerId, x, y, actualIconSize, headAlpha);
            }
        } else {
            // Игрок вне загруженных чанков - показываем первые 2 буквы имени
            renderPlayerInitials(graphics, manager, playerId, x, y, actualIconSize, headAlpha);
        }
        
        // Отображаем расстояние
        String distanceStr = String.format("%.0fm", distance);
        int textX = x + actualIconSize / 2 - Minecraft.getInstance().font.width(distanceStr) / 2;
        int textY = y + actualIconSize + 2;
        int textAlpha = (int)(255 * headAlpha);
        int textColor = (textAlpha << 24) | 0xFFFFFF;
        graphics.drawString(Minecraft.getInstance().font, distanceStr, textX, textY, textColor, true);
    }
    
    private void renderPlayerInitials(GuiGraphics graphics, ClientTeamManager manager, UUID playerId, int x, int y, int size, float alpha) {
        // Получаем имя игрока
        String playerName = manager.getPlayerName(playerId);
        if (playerName == null || playerName.isEmpty()) {
            // Если имя недоступно, рисуем простой квадрат
            int grayAlpha = (int)(128 * alpha);
            graphics.fill(x, y, x + size, y + size, (grayAlpha << 24) | 0x808080);
            return;
        }
        
        // Получаем первые 2 буквы имени (в верхнем регистре)
        String initials = playerName.length() >= 2 
            ? playerName.substring(0, 2).toUpperCase() 
            : playerName.toUpperCase();
        
        // Рисуем фон для букв
        int bgAlpha = (int)(200 * alpha);
        graphics.fill(x, y, x + size, y + size, (bgAlpha << 24) | 0x333333);
        
        // Рисуем буквы по центру с выбранным цветом
        TeamHeadSettings settings = TeamHeadSettings.getInstance();
        int colorRGB = settings.getInitialsColor();
        int textAlpha = (int)(255 * alpha);
        int textColor = (textAlpha << 24) | colorRGB;
        int textX = x + size / 2 - Minecraft.getInstance().font.width(initials) / 2;
        int textY = y + size / 2 - Minecraft.getInstance().font.lineHeight / 2;
        graphics.drawString(Minecraft.getInstance().font, initials, textX, textY, textColor, false);
    }
}

