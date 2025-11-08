package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

public class TeamMemberPositionPacket {
    private Map<UUID, Position> positions;
    
    public static class Position {
        private double x;
        private double y;
        private double z;
        
        public Position(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
    }
    
    public TeamMemberPositionPacket(Map<UUID, Position> positions) {
        this.positions = positions != null ? new HashMap<>(positions) : new HashMap<>();
    }
    
    public static void encode(TeamMemberPositionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.positions.size());
        for (Map.Entry<UUID, Position> entry : packet.positions.entrySet()) {
            buffer.writeUUID(entry.getKey());
            Position pos = entry.getValue();
            buffer.writeDouble(pos.x);
            buffer.writeDouble(pos.y);
            buffer.writeDouble(pos.z);
        }
    }
    
    public static TeamMemberPositionPacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readInt();
        Map<UUID, Position> positions = new HashMap<>();
        for (int i = 0; i < count; i++) {
            UUID uuid = buffer.readUUID();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            positions.put(uuid, new Position(x, y, z));
        }
        return new TeamMemberPositionPacket(positions);
    }
    
    public static void handle(TeamMemberPositionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.rustsayz.teams.client.ClientTeamManager.getInstance().updateMemberPositions(packet.positions);
        });
        context.setPacketHandled(true);
    }
    
    public Map<UUID, Position> getPositions() {
        return new HashMap<>(positions);
    }
}

