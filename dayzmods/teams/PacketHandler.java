package com.rustsayz.teams;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import com.rustsayz.teams.packets.*;
import java.util.Optional;

public class PacketHandler {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        ResourceLocation.fromNamespaceAndPath("teams_mod", "main"),
        () -> PROTOCOL,
        PROTOCOL::equals,
        PROTOCOL::equals
    );
    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, InvitePlayerPacket.class,
            InvitePlayerPacket::encode, InvitePlayerPacket::decode, InvitePlayerPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, AcceptInvitationPacket.class,
            AcceptInvitationPacket::encode, AcceptInvitationPacket::decode, AcceptInvitationPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, DeclineInvitationPacket.class,
            DeclineInvitationPacket::encode, DeclineInvitationPacket::decode, DeclineInvitationPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, InvitationNotificationPacket.class,
            InvitationNotificationPacket::encode, InvitationNotificationPacket::decode, InvitationNotificationPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        INSTANCE.registerMessage(id++, TeamUpdatePacket.class,
            TeamUpdatePacket::encode, TeamUpdatePacket::decode, TeamUpdatePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        INSTANCE.registerMessage(id++, GetPlayersListPacket.class,
            GetPlayersListPacket::encode, GetPlayersListPacket::decode, GetPlayersListPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, PlayersListResponsePacket.class,
            PlayersListResponsePacket::encode, PlayersListResponsePacket::decode, PlayersListResponsePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        INSTANCE.registerMessage(id++, GetInvitationsPacket.class,
            GetInvitationsPacket::encode, GetInvitationsPacket::decode, GetInvitationsPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, InvitationsListResponsePacket.class,
            InvitationsListResponsePacket::encode, InvitationsListResponsePacket::decode, InvitationsListResponsePacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        INSTANCE.registerMessage(id++, DisbandTeamPacket.class,
            DisbandTeamPacket::encode, DisbandTeamPacket::decode, DisbandTeamPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, ReassignLeaderPacket.class,
            ReassignLeaderPacket::encode, ReassignLeaderPacket::decode, ReassignLeaderPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER));
        
        INSTANCE.registerMessage(id++, TeamMemberPositionPacket.class,
            TeamMemberPositionPacket::encode, TeamMemberPositionPacket::decode, TeamMemberPositionPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        
        INSTANCE.registerMessage(id++, PlayerSkinDataPacket.class,
            PlayerSkinDataPacket::encode, PlayerSkinDataPacket::decode, PlayerSkinDataPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToClient(ServerPlayer player, Object packet) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(Object packet) {
        INSTANCE.sendToServer(packet);
    }
}
