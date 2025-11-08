package com.rustsayz.teams.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.function.Supplier;

public class InvitationNotificationPacket {
    private UUID inviter;
    
    public InvitationNotificationPacket(UUID inviter) {
        this.inviter = inviter;
    }
    
    public static void encode(InvitationNotificationPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.inviter);
    }
    
    public static InvitationNotificationPacket decode(FriendlyByteBuf buffer) {
        UUID inviter = buffer.readUUID();
        return new InvitationNotificationPacket(inviter);
    }
    
    public static void handle(InvitationNotificationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                mc.player.sendSystemMessage(
                    Component.literal("§aВы получили приглашение в команду!")
                );
                
                try {
                    String soundPath = "C:\\Users\\User\\Downloads\\short-ringing-signal.mp3";
                    Path soundFile = Paths.get(soundPath);
                    
                    if (Files.exists(soundFile)) {
                        try {
                            mc.getSoundManager().play(
                                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                                    1.0f,
                                    1.0f
                                )
                            );
                        } catch (Exception e) {
                            System.err.println("[TeamsMod] Ошибка воспроизведения звука: " + e.getMessage());
                        }
                    } else {
                        mc.getSoundManager().play(
                            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                                1.0f,
                                1.0f
                            )
                        );
                    }
                } catch (Exception e) {
                    System.err.println("[TeamsMod] Ошибка при обработке звука: " + e.getMessage());
                }
            }
        });
        context.setPacketHandled(true);
    }
    
    public UUID getInviter() {
        return inviter;
    }
}
