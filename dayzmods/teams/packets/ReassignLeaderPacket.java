package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.TeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.UUID;
import java.util.function.Supplier;

public class ReassignLeaderPacket {
    private UUID newLeader;
    
    public ReassignLeaderPacket(UUID newLeader) {
        this.newLeader = newLeader;
    }
    
    public static void encode(ReassignLeaderPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.newLeader);
    }
    
    public static ReassignLeaderPacket decode(FriendlyByteBuf buffer) {
        UUID newLeader = buffer.readUUID();
        return new ReassignLeaderPacket(newLeader);
    }
    
    public static void handle(ReassignLeaderPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            
            TeamManager manager = TeamManager.getInstance();
            var team = manager.getPlayerTeam(player.getUUID());
            
            if (team == null || !team.getLeader().equals(player.getUUID())) {
                player.sendSystemMessage(Component.literal("§cВы не являетесь лидером команды"));
                return;
            }
            
            if (manager.reassignLeader(player.getUUID(), packet.newLeader)) {
                player.sendSystemMessage(Component.literal("§aЛидер команды переназначен"));
                
                ServerPlayer newLeaderPlayer = player.getServer().getPlayerList().getPlayer(packet.newLeader);
                if (newLeaderPlayer != null) {
                    newLeaderPlayer.sendSystemMessage(Component.literal("§aВы стали лидером команды"));
                    
                    var updatedTeam = manager.getPlayerTeam(packet.newLeader);
                    if (updatedTeam != null) {
                        // Обновляем имена игроков в команде
                        manager.updateTeamPlayerNames(updatedTeam.getLeader(), player.getServer());
                        updatedTeam = manager.getPlayerTeam(packet.newLeader); // Получаем обновленную команду
                        
                        TeamUpdatePacket update = new TeamUpdatePacket(updatedTeam);
                        for (var memberId : updatedTeam.getMembers()) {
                            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                PacketHandler.sendToClient(member, update);
                            }
                        }
                    }
                }
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось переназначить лидера"));
            }
        });
        context.setPacketHandled(true);
    }
    
    public UUID getNewLeader() {
        return newLeader;
    }
}
