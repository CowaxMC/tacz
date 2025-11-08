package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.TeamManager;
import com.rustsayz.teams.PacketHandler;
import java.util.UUID;
import java.util.function.Supplier;

public class InvitePlayerPacket {
    private UUID targetPlayer;
    
    public InvitePlayerPacket(UUID targetPlayer) {
        this.targetPlayer = targetPlayer;
    }
    
    public static void encode(InvitePlayerPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetPlayer);
    }
    
    public static InvitePlayerPacket decode(FriendlyByteBuf buffer) {
        UUID targetPlayer = buffer.readUUID();
        return new InvitePlayerPacket(targetPlayer);
    }
    
    public static void handle(InvitePlayerPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null || sender.level() == null) {
                return;
            }
            
            UUID senderId = sender.getUUID();
            UUID targetId = packet.targetPlayer;
            
            TeamManager manager = TeamManager.getInstance();
            if (manager.sendInvitation(senderId, targetId)) {
                sender.sendSystemMessage(Component.literal("§aПриглашение отправлено"));
                
                ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetId);
                if (target != null) {
                    InvitationNotificationPacket notification = new InvitationNotificationPacket(senderId);
                    PacketHandler.sendToClient(target, notification);
                }
            } else {
                sender.sendSystemMessage(Component.literal("§cНе удалось отправить приглашение"));
            }
        });
        context.setPacketHandled(true);
    }
    
    public UUID getTargetPlayer() {
        return targetPlayer;
    }
}
