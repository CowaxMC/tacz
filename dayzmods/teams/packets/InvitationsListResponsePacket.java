package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;

public class InvitationsListResponsePacket {
    private List<PlayersListResponsePacket.PlayerInfo> invitations;
    
    public InvitationsListResponsePacket(List<PlayersListResponsePacket.PlayerInfo> invitations) {
        this.invitations = invitations != null ? new ArrayList<>(invitations) : new ArrayList<>();
    }
    
    public static void encode(InvitationsListResponsePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.invitations.size());
        for (PlayersListResponsePacket.PlayerInfo invitation : packet.invitations) {
            buffer.writeUUID(invitation.getUuid());
            buffer.writeUtf(invitation.getName());
        }
    }
    
    public static InvitationsListResponsePacket decode(FriendlyByteBuf buffer) {
        int count = buffer.readInt();
        List<PlayersListResponsePacket.PlayerInfo> invitations = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            invitations.add(new PlayersListResponsePacket.PlayerInfo(buffer.readUUID(), buffer.readUtf()));
        }
        return new InvitationsListResponsePacket(invitations);
    }
    
    public static void handle(InvitationsListResponsePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            com.rustsayz.teams.client.ClientTeamManager.getInstance().updateInvitationsList(packet.getInvitations());
        });
        context.setPacketHandled(true);
    }
    
    public List<PlayersListResponsePacket.PlayerInfo> getInvitations() {
        return invitations;
    }
}
