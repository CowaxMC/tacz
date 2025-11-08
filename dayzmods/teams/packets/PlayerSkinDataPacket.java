package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.client.ClientPlayerSkinCache;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerSkinDataPacket {
    private final UUID playerId;
    private final String playerName;
    private final boolean remove;
    
    public PlayerSkinDataPacket(UUID playerId, String playerName, boolean remove) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.remove = remove;
    }
    
    public static void encode(PlayerSkinDataPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.playerId);
        buffer.writeUtf(packet.playerName != null ? packet.playerName : "");
        buffer.writeBoolean(packet.remove);
    }
    
    public static PlayerSkinDataPacket decode(FriendlyByteBuf buffer) {
        UUID playerId = buffer.readUUID();
        String playerName = buffer.readUtf();
        boolean remove = buffer.readBoolean();
        return new PlayerSkinDataPacket(playerId, playerName.isEmpty() ? null : playerName, remove);
    }
    
    public static void handle(PlayerSkinDataPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.remove) {
                // Удаляем скин из кэша
                ClientPlayerSkinCache.getInstance().removeSkin(packet.playerId);
            } else {
                // Загружаем скин через SkinManager
                ClientPlayerSkinCache.getInstance().loadSkinFromServer(packet.playerId, packet.playerName);
            }
        });
        context.setPacketHandled(true);
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public boolean isRemove() {
        return remove;
    }
}

