package net.bandit.runes_of_power.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.bandit.runes_of_power.registry.EffectsRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class LavaFogMixin {

    @Inject(method = "setupFog", at = @At("HEAD"), cancellable = true)
    private static void runes$setupLavaFog(Camera camera,
                                           FogRenderer.FogMode fogMode,
                                           float viewDistance,
                                           boolean thickFog,
                                           float partialTicks,
                                           CallbackInfo ci) {
        Entity entity = camera.getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }

        if (!entity.isEyeInFluid(FluidTags.LAVA)) {
            return;
        }

        boolean hasLavaVision = player.getActiveEffects().stream()
                .map(MobEffectInstance::getEffect)
                .map(Holder::value)
                .anyMatch(effect ->
                        effect != null &&
                                EffectsRegistry.LAVA_VISION.isPresent() &&
                                effect == EffectsRegistry.LAVA_VISION.get()
                );


        if (!hasLavaVision) {
            return;
        }

        float startFogDistance = 0.0F;
        float endFogDistance   = viewDistance;

        RenderSystem.setShaderFogStart(startFogDistance);
        RenderSystem.setShaderFogEnd(endFogDistance);

        RenderSystem.setShaderFogColor(1.0F, 0.5F, 0.3F);
        ci.cancel();
    }
}
