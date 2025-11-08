package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

public class PlayersListResponsePacket {
    private List<PlayerInfo> players;
    
    public PlayersListResponsePacket(List<PlayerInfo> players) {
        this.players = players != null ? new ArrayList<>(players) : new ArrayList<>();
    }
    
    public static void encode(PlayersListResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.players.size());
        for (PlayerInfo player : packet.players) {
            buffer.writeUUID(player.getUuid());
            buffer.writeUtf(player.getName());
        }
    }
    
    public static PlayersListResponsePacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readInt();
        List<PlayerInfo> players = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            players.add(new PlayerInfo(buffer.readUUID(), buffer.readUtf()));
        }
        return new PlayersListResponsePacket(players);
    }
    
    public static void handle(PlayersListResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.rustsayz.teams.client.ClientTeamManager.getInstance().updatePlayersList(packet.getPlayers());
        });
        context.setPacketHandled(true);
    }
    
    public List<PlayerInfo> getPlayers() {
        return players;
    }
    
    public static class PlayerInfo {
        private UUID uuid;
        private String name;
        
        public PlayerInfo(UUID uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
        
        public UUID getUuid() {
            return uuid;
        }
        
        public String getName() {
            return name;
        }
    }
}
