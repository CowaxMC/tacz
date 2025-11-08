package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.TeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.UUID;
import java.util.function.Supplier;

public class AcceptInvitationPacket {
    private UUID inviter;
    
    public AcceptInvitationPacket(UUID inviter) {
        this.inviter = inviter;
    }
    
    public static void encode(AcceptInvitationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.inviter);
    }
    
    public static AcceptInvitationPacket decode(FriendlyByteBuf buffer) {
        UUID inviter = buffer.readUUID();
        return new AcceptInvitationPacket(inviter);
    }
    
    public static void handle(AcceptInvitationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.level() == null) {
                return;
            }
            
            UUID playerId = player.getUUID();
            UUID inviterId = packet.inviter;
            
            TeamManager manager = TeamManager.getInstance();
            if (manager.acceptInvitation(playerId, inviterId)) {
                player.sendSystemMessage(Component.literal("§aВы присоединились к команде!"));
                
                var team = manager.getPlayerTeam(playerId);
                if (team != null) {
                    // Обновляем имена игроков в команде
                    manager.updateTeamPlayerNames(team.getLeader(), player.getServer());
                    team = manager.getPlayerTeam(playerId); // Получаем обновленную команду
                    
                    for (UUID memberId : team.getMembers()) {
                        if (!memberId.equals(playerId)) {
                            ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                            if (member != null) {
                                member.sendSystemMessage(
                                    Component.literal("§a" + player.getDisplayName().getString() + " присоединился к команде!")
                                );
                            }
                        }
                    }
                }
                
                TeamUpdatePacket update = new TeamUpdatePacket(team);
                for (UUID memberId : team.getMembers()) {
                    ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        PacketHandler.sendToClient(member, update);
                    }
                }
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось принять приглашение"));
            }
        });
        context.setPacketHandled(true);
    }
    
    public UUID getInviter() {
        return inviter;
    }
}
