package com.rustsayz.teams;

import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.rustsayz.teams.commands.TeamsCommands;
import com.rustsayz.teams.client.TeamMemberOverlay;
import com.rustsayz.teams.effects.ModEffects;

@Mod(TeamsMod.MODID)
public class TeamsMod {
    public static final String MODID = "teams_mod";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public TeamsMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        
        PacketHandler.register();
        ModEffects.register(modEventBus);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("Teams Mod loaded!");
    }
    
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TeamsCommands.register(event.getDispatcher());
    }
    
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("team_members", TeamMemberOverlay.INSTANCE);
        }
        
        @SubscribeEvent
        public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
            // Инициализируем настройки при загрузке клиента
            com.rustsayz.teams.client.TeamHeadSettings.getInstance().init();
        }
    }
}
