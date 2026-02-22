package net.bandit.runes_of_power;


import net.bandit.runes_of_power.config.RunesConfig;
import net.bandit.runes_of_power.events.RuneCastEvents;
import net.bandit.runes_of_power.events.WeaponRuneTooltipHandler;
import net.bandit.runes_of_power.loot.ModLootModifiers;
import net.bandit.runes_of_power.registry.*;

public final class RunesMod {
    public static final String MOD_ID = "runes_of_power";

    public static void init() {
        ItemRegistry.register();
        EffectsRegistry.register();
        ModDataComponents.register();
        SoundsRegistry.registerSounds();
        TabRegistry.init();
        RuneCastEvents.register();
        RunesConfig.loadConfig();
        ModLootModifiers.registerLootModifiers();
    }

    public static void initClient() {
        WeaponRuneTooltipHandler.register();
    }
}
