package com.rustsayz.teams.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import com.rustsayz.teams.TeamsMod;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = 
        DeferredRegister.create(Registries.MOB_EFFECT, TeamsMod.MODID);
    
    public static final RegistryObject<MobEffect> TEAM_HEAD_GLOW = MOB_EFFECTS.register(
        "team_head_glow", 
        TeamHeadGlowEffect::new
    );
    
    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}

