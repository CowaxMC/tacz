package com.rustsayz.teams.client;

import com.rustsayz.teams.TeamsMod;
import com.rustsayz.teams.client.gui.MyTeamScreen;
import com.rustsayz.teams.client.gui.TeamsMainScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TeamsMod.MODID, value = Dist.CLIENT)
public class PauseMenuHandler {

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int buttonWidth = 204;
        int buttonHeight = 20;
        int screenWidth = event.getScreen().width;
        int screenHeight = event.getScreen().height;

        int buttonX = (screenWidth - buttonWidth) / 2;
        // Кнопка "Сохранить и выйти" находится на screenHeight / 4 + 120
        // Расстояние между кнопками в стандартном меню: buttonHeight + 4 = 24
        // Кнопка "Команды" должна быть на том же расстоянии от "Сохранить и выйти"
        int teamsButtonY = screenHeight / 4 + 120 + buttonHeight + 4;

        Button teamsButton = Button.builder(
            Component.literal("Команды"),
            btn -> { 
                if (mc.player != null) {
                    mc.setScreen(new TeamsMainScreen(event.getScreen()));
                }
            }
        ).bounds(buttonX, teamsButtonY, buttonWidth, buttonHeight).build();

        event.addListener(teamsButton);
        
        ClientTeamManager teamManager = ClientTeamManager.getInstance();
        if (teamManager.hasTeam()) {
            int myTeamButtonY = teamsButtonY + buttonHeight + 4;
            Button myTeamButton = Button.builder(
                Component.literal("Моя команда"),
                btn -> { 
                    if (mc.player != null) {
                        mc.setScreen(new MyTeamScreen(event.getScreen()));
                    }
                }
            ).bounds(buttonX, myTeamButtonY, buttonWidth, buttonHeight).build();
            
            event.addListener(myTeamButton);
        }
    }
}
