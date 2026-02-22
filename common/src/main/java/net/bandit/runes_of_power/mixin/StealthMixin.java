package net.bandit.runes_of_power.mixin;

import net.bandit.runes_of_power.registry.EffectsRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class StealthMixin {

    @Inject(
            method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void runes$hidePlayerGameEvents(Holder<GameEvent> event,
                                            Vec3 pos,
                                            Context context,
                                            CallbackInfo ci) {
        Entity source = context.sourceEntity();
        if (!(source instanceof Player player)) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        var mobEffectRegistry = level.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
        var stealthHolder = mobEffectRegistry.getHolderOrThrow(EffectsRegistry.STEALTH.getKey());
        if (player.hasEffect(stealthHolder)) {
            ci.cancel();
        }
    }
}
