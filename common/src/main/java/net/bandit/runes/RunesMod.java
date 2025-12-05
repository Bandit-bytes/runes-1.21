package net.bandit.runes;


import net.bandit.runes.config.RunesConfig;
import net.bandit.runes.loot.ModLootModifiers;
import net.bandit.runes.registry.*;

public final class RunesMod {
    public static final String MOD_ID = "runes";

    public static void init() {
        ItemRegistry.register();
        EffectsRegistry.register();
        ModDataComponents.register();
        SoundsRegistry.registerSounds();
        TabRegistry.init();
        RunesConfig.loadConfig();
        ModLootModifiers.registerLootModifiers();
    }

    public static void initClient() {
    }
}
