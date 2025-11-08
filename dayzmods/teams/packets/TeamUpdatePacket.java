package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.Team;
import java.util.*;
import java.util.function.Supplier;

public class TeamUpdatePacket {
    private UUID leader;
    private Set<UUID> members;
    private Map<UUID, String> playerNames; // Имена игроков
    
    public TeamUpdatePacket(Team team) {
        if (team != null) {
            this.leader = team.getLeader();
            this.members = team.getMembers();
            this.playerNames = team.getPlayerNames();
        } else {
            this.leader = null;
            this.members = Collections.emptySet();
            this.playerNames = Collections.emptyMap();
        }
    }
    
    public TeamUpdatePacket(UUID leader, Set<UUID> members) {
        this.leader = leader;
        this.members = members != null ? new HashSet<>(members) : Collections.emptySet();
        this.playerNames = Collections.emptyMap();
    }
    
    public TeamUpdatePacket(UUID leader, Set<UUID> members, Map<UUID, String> playerNames) {
        this.leader = leader;
        this.members = members != null ? new HashSet<>(members) : Collections.emptySet();
        this.playerNames = playerNames != null ? new HashMap<>(playerNames) : Collections.emptyMap();
    }
    
    public static void encode(TeamUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.leader != null);
        if (packet.leader != null) {
            buffer.writeUUID(packet.leader);
            buffer.writeInt(packet.members.size());
            for (UUID member : packet.members) {
                buffer.writeUUID(member);
            }
            // Кодируем имена игроков
            buffer.writeInt(packet.playerNames.size());
            for (Map.Entry<UUID, String> entry : packet.playerNames.entrySet()) {
                buffer.writeUUID(entry.getKey());
                buffer.writeUtf(entry.getValue() != null ? entry.getValue() : "", 32767);
            }
        }
    }
    
    public static TeamUpdatePacket decode(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            UUID leader = buffer.readUUID();
            int memberCount = buffer.readInt();
            Set<UUID> members = new HashSet<>();
            for (int i = 0; i < memberCount; i++) {
                members.add(buffer.readUUID());
            }
            // Декодируем имена игроков
            int nameCount = buffer.readInt();
            Map<UUID, String> playerNames = new HashMap<>();
            for (int i = 0; i < nameCount; i++) {
                UUID playerId = buffer.readUUID();
                String name = buffer.readUtf(32767);
                if (!name.isEmpty()) {
                    playerNames.put(playerId, name);
                }
            }
            return new TeamUpdatePacket(leader, members, playerNames);
        }
        return new TeamUpdatePacket(null, Collections.emptySet(), Collections.emptyMap());
    }
    
    public static void handle(TeamUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.rustsayz.teams.client.ClientTeamManager.getInstance().updateTeam(packet);
        });
        context.setPacketHandled(true);
    }
    
    public UUID getLeader() {
        return leader;
    }
    
    public Set<UUID> getMembers() {
        return members;
    }
    
    public Map<UUID, String> getPlayerNames() {
        return playerNames != null ? new HashMap<>(playerNames) : Collections.emptyMap();
    }
}
