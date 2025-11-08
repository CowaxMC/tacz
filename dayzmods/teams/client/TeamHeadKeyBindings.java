package com.rustsayz.teams.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.rustsayz.teams.TeamsMod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TeamsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TeamHeadKeyBindings {
    
    public static final KeyMapping OPEN_SETTINGS = new KeyMapping(
        "key.teams_mod.open_head_settings",
        GLFW.GLFW_KEY_RIGHT_CONTROL,
        "key.categories.teams_mod"
    );
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SETTINGS);
    }
}

