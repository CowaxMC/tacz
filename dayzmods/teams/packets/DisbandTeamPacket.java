package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.TeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.function.Supplier;

public class DisbandTeamPacket {
    public DisbandTeamPacket() {
    }
    
    public static void encode(DisbandTeamPacket packet, FriendlyByteBuf buffer) {
    }
    
    public static DisbandTeamPacket decode(FriendlyByteBuf buffer) {
        return new DisbandTeamPacket();
    }
    
    public static void handle(DisbandTeamPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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
            
            if (manager.disbandTeam(player.getUUID())) {
                // Отправляем уведомление всем участникам команды
                for (var memberId : team.getMembers()) {
                    ServerPlayer member = player.getServer().getPlayerList().getPlayer(memberId);
                    if (member != null) {
                        if (memberId.equals(player.getUUID())) {
                            member.sendSystemMessage(Component.literal("§aКоманда расформирована"));
                        } else {
                            member.sendSystemMessage(Component.literal("§eКоманда расформирована"));
                        }
                        // Отправляем обновление команды (null означает, что команды больше нет)
                        PacketHandler.sendToClient(member, new TeamUpdatePacket(null, null));
                    }
                }
            } else {
                player.sendSystemMessage(Component.literal("§cНе удалось расформировать команду"));
            }
        });
        context.setPacketHandled(true);
    }
}
