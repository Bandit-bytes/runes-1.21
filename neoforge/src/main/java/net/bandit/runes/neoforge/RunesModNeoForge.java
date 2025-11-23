package net.bandit.runes.neoforge;

import dev.architectury.utils.EnvExecutor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.bandit.runes.RunesMod;

@Mod(RunesMod.MOD_ID)
public final class RunesModNeoForge {
    public RunesModNeoForge() {
        RunesMod.init();
        EnvExecutor.runInEnv(Dist.CLIENT, () -> RunesMod::initClient);
    }
}
