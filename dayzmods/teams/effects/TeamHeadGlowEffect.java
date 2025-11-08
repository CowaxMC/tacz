package com.rustsayz.teams.effects;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class TeamHeadGlowEffect extends MobEffect {
    
    public TeamHeadGlowEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x00FF00); // Зеленый цвет
    }
    
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return true; // Эффект активен все время
    }
    
    @Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        // Эффект не имеет реальных изменений, только визуальный для свечения головы
    }
    
    @Override
    public boolean isInstantenous() {
        return false;
    }
}

