package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.PacketHandler;
import com.rustsayz.teams.teams.TeamManager;
import java.util.*;
import java.util.function.Supplier;

public class GetInvitationsPacket {
    public GetInvitationsPacket() {
    }
    
    public static void encode(GetInvitationsPacket packet, FriendlyByteBuf buffer) {
    }
    
    public static GetInvitationsPacket decode(FriendlyByteBuf buffer) {
        return new GetInvitationsPacket();
    }
    
    public static void handle(GetInvitationsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            
            TeamManager manager = TeamManager.getInstance();
            Set<UUID> inviterIds = manager.getInvitations(player.getUUID());
            
            List<PlayersListResponsePacket.PlayerInfo> invitations = new ArrayList<>();
            for (UUID inviterId : inviterIds) {
                ServerPlayer inviter = player.getServer().getPlayerList().getPlayer(inviterId);
                if (inviter != null) {
                    invitations.add(new PlayersListResponsePacket.PlayerInfo(
                        inviterId,
                        inviter.getDisplayName().getString()
                    ));
                }
            }
            
            InvitationsListResponsePacket response = new InvitationsListResponsePacket(invitations);
            PacketHandler.sendToClient(player, response);
        });
        context.setPacketHandled(true);
    }
}
