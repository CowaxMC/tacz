package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.teams.TeamManager;
import java.util.UUID;
import java.util.function.Supplier;

public class DeclineInvitationPacket {
    private UUID inviter;
    
    public DeclineInvitationPacket(UUID inviter) {
        this.inviter = inviter;
    }
    
    public static void encode(DeclineInvitationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.inviter);
    }
    
    public static DeclineInvitationPacket decode(FriendlyByteBuf buffer) {
        UUID inviter = buffer.readUUID();
        return new DeclineInvitationPacket(inviter);
    }
    
    public static void handle(DeclineInvitationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            
            UUID playerId = player.getUUID();
            UUID inviterId = packet.inviter;
            
            TeamManager manager = TeamManager.getInstance();
            manager.declineInvitation(playerId, inviterId);
        });
        context.setPacketHandled(true);
    }
    
    public UUID getInviter() {
        return inviter;
    }
}
