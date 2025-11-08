package com.rustsayz.teams.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rustsayz.teams.TeamsMod;
import com.rustsayz.teams.client.gui.TeamHeadSettingsScreen;

@Mod.EventBusSubscriber(modid = TeamsMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class TeamHeadKeyHandler {
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            return;
        }
        
        if (TeamHeadKeyBindings.OPEN_SETTINGS.consumeClick()) {
            mc.setScreen(new TeamHeadSettingsScreen(null));
        }
    }
}

