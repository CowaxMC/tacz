package com.rustsayz.teams.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.resources.ResourceLocation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кэш текстур скинов игроков для отображения голов даже в непрогруженных чанках
 */
public class ClientPlayerSkinCache {
    private static final ClientPlayerSkinCache INSTANCE = new ClientPlayerSkinCache();
    private final Map<UUID, ResourceLocation> cachedSkinLocations = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> loadingSkins = new ConcurrentHashMap<>();
    
    public static ClientPlayerSkinCache getInstance() {
        return INSTANCE;
    }
    
    /**
     * Получает или кэширует текстуру скина игрока
     * @param playerId UUID игрока
     * @param player Игрок (может быть null, если не в загруженных чанках)
     * @return ResourceLocation текстуры скина или null если недоступен
     */
    public ResourceLocation getOrCacheSkin(UUID playerId, AbstractClientPlayer player) {
        // Если игрок доступен, пытаемся получить и кэшировать его текстуру
        if (player != null) {
            try {
                ResourceLocation skinLocation = player.getSkinTextureLocation();
                // Кэшируем текстуру
                cachedSkinLocations.put(playerId, skinLocation);
                return skinLocation;
            } catch (Exception e) {
                // Игнорируем ошибки при получении текстуры
            }
        }
        
        // Если игрок недоступен, но текстура была закэширована ранее, возвращаем её
        ResourceLocation cachedSkin = cachedSkinLocations.get(playerId);
        if (cachedSkin != null) {
            // Проверяем, что кэшированная текстура всё ещё доступна
            try {
                var textureManager = Minecraft.getInstance().getTextureManager();
                var texture = textureManager.getTexture(cachedSkin);
                if (texture != null) {
                    return cachedSkin;
                }
                // Если текстура не загружена, пытаемся загрузить её
                textureManager.getTexture(cachedSkin, null);
                // Возвращаем кэшированную текстуру даже если она еще не загружена
                // (она загрузится асинхронно)
                return cachedSkin;
            } catch (Exception e) {
                // Игнорируем ошибки, но не удаляем из кэша сразу
                // Возможно, текстура еще загружается
            }
        }
        
        // Если текстура не найдена и игрок не в загруженных чанках, пытаемся загрузить через SkinManager
        if (player == null && !loadingSkins.containsKey(playerId) && cachedSkin == null) {
            loadSkinFromSkinManager(playerId);
        }
        
        // Возвращаем кэшированную текстуру даже если она еще не загружена
        return cachedSkin;
    }
    
    /**
     * Загружает скин игрока через SkinManager по UUID (вызывается с сервера)
     */
    public void loadSkinFromServer(UUID playerId, String playerName) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSkinManager() == null) {
            return;
        }
        
        // Проверяем, не загружается ли уже скин для этого игрока
        if (loadingSkins.putIfAbsent(playerId, true) != null) {
            return; // Уже загружается
        }
        
        try {
            // Создаем GameProfile для игрока
            GameProfile profile = new GameProfile(playerId, playerName);
            
            // Загружаем скин через SkinManager
            SkinManager skinManager = mc.getSkinManager();
            skinManager.registerSkins(profile, (type, location, texture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN && location != null) {
                    // Кэшируем текстуру скина
                    cachedSkinLocations.put(playerId, location);
                    loadingSkins.remove(playerId);
                } else if (type == MinecraftProfileTexture.Type.SKIN) {
                    // Если location null, все равно удаляем из loadingSkins
                    loadingSkins.remove(playerId);
                }
            }, true);
        } catch (Exception e) {
            // Игнорируем ошибки при загрузке
            loadingSkins.remove(playerId);
        }
    }
    
    /**
     * Загружает скин игрока через SkinManager по UUID (локальный метод)
     */
    private void loadSkinFromSkinManager(UUID playerId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSkinManager() == null) {
            return;
        }
        
        // Проверяем, не загружается ли уже скин для этого игрока
        if (loadingSkins.putIfAbsent(playerId, true) != null) {
            return; // Уже загружается
        }
        
        try {
            // Получаем имя игрока из ClientTeamManager если доступно
            String playerName = null;
            try {
                ClientTeamManager teamManager = ClientTeamManager.getInstance();
                playerName = teamManager.getPlayerName(playerId);
            } catch (Exception e) {
                // Игнорируем ошибки
            }
            
            // Создаем GameProfile для игрока
            GameProfile profile = new GameProfile(playerId, playerName);
            
            // Загружаем скин через SkinManager
            SkinManager skinManager = mc.getSkinManager();
            skinManager.registerSkins(profile, (type, location, texture) -> {
                if (type == MinecraftProfileTexture.Type.SKIN && location != null) {
                    // Кэшируем текстуру скина
                    cachedSkinLocations.put(playerId, location);
                    loadingSkins.remove(playerId);
                } else if (type == MinecraftProfileTexture.Type.SKIN) {
                    // Если location null, все равно удаляем из loadingSkins
                    loadingSkins.remove(playerId);
                }
            }, true);
        } catch (Exception e) {
            // Игнорируем ошибки при загрузке
            loadingSkins.remove(playerId);
        }
    }
    
    /**
     * Очищает кэш для игрока (когда он выходит из игры)
     */
    public void removeSkin(UUID playerId) {
        cachedSkinLocations.remove(playerId);
        loadingSkins.remove(playerId);
    }
    
    /**
     * Очищает весь кэш
     */
    public void clearCache() {
        cachedSkinLocations.clear();
    }
}

