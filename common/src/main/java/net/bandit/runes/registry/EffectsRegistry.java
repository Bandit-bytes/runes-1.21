package net.bandit.runes.registry;

import net.bandit.runes.RunesMod;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.registries.Registries;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.effect.MobEffectCategory;

public class EffectsRegistry {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = DeferredRegister.create(RunesMod.MOD_ID, Registries.MOB_EFFECT);

    public static final RegistrySupplier<MobEffect> LAVA_VISION =
            MOB_EFFECTS.register("lava_vision", () ->
                    new MobEffect(MobEffectCategory.BENEFICIAL,0xFF6A00) {});

    public static final RegistrySupplier<MobEffect> CREATIVE_FLIGHT =
            MOB_EFFECTS.register("creative_flight", () ->
                    new MobEffect(MobEffectCategory.BENEFICIAL,0xFFD700) {});

    public static final RegistrySupplier<MobEffect> STEALTH =
            MOB_EFFECTS.register("stealth_effect", () ->
                    new MobEffect(MobEffectCategory.BENEFICIAL,0x555555) {});

    public static void register() {
        MOB_EFFECTS.register();
    }
}
