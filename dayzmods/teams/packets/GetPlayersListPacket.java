package com.rustsayz.teams.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import com.rustsayz.teams.PacketHandler;
import java.util.*;
import java.util.function.Supplier;

public class GetPlayersListPacket {
    public GetPlayersListPacket() {
    }
    
    public static void encode(GetPlayersListPacket packet, FriendlyByteBuf buffer) {
    }
    
    public static GetPlayersListPacket decode(FriendlyByteBuf buffer) {
        return new GetPlayersListPacket();
    }
    
    public static void handle(GetPlayersListPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null || player.getServer() == null) {
                return;
            }
            
            List<PlayersListResponsePacket.PlayerInfo> playerList = new ArrayList<>();
            for (ServerPlayer serverPlayer : player.getServer().getPlayerList().getPlayers()) {
                if (!serverPlayer.getUUID().equals(player.getUUID())) {
                    playerList.add(new PlayersListResponsePacket.PlayerInfo(
                        serverPlayer.getUUID(),
                        serverPlayer.getDisplayName().getString()
                    ));
                }
            }
            
            PlayersListResponsePacket response = new PlayersListResponsePacket(playerList);
            PacketHandler.sendToClient(player, response);
        });
        context.setPacketHandled(true);
    }
}
