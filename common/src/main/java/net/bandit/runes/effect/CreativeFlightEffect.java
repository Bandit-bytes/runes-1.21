package net.bandit.runes.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class CreativeFlightEffect extends MobEffect {
    public CreativeFlightEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        if (entity.level().isClientSide()) {
            return false;
        }

        var abilities = player.getAbilities();

        if (!abilities.mayfly) {
            abilities.mayfly = true;
            player.onUpdateAbilities();
        }

        return true;
    }
}
