package net.bandit.runes_of_power.item;

import net.bandit.runes_of_power.registry.EffectsRegistry;
import net.bandit.runes_of_power.registry.ItemRegistry;
import net.bandit.runes_of_power.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class ResisRune extends Item {

    private static final int MAX_LEVEL = 3;

    public ResisRune(Properties properties) {
        super(properties.durability(20));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!world.isClientSide && player.isShiftKeyDown()) {
            if (tryUpgradeRune(player, stack)) {
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
            }
        }

        if (!world.isClientSide) {
            int level = getRuneLevel(stack);

            if (!player.getCooldowns().isOnCooldown(this)) {
                int extraDuration = getExtraDurationForLevel(level);

                MobEffectInstance fireResEffect = player.getEffect(MobEffects.FIRE_RESISTANCE);
                if (fireResEffect != null) {
                    player.addEffect(new MobEffectInstance(
                            MobEffects.FIRE_RESISTANCE,
                            fireResEffect.getDuration() + extraDuration,
                            fireResEffect.getAmplifier()
                    ));
                } else {
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, extraDuration, 0));
                }

                var mobEffectRegistry = world.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                var lavaVisionHolder = mobEffectRegistry.getHolderOrThrow(EffectsRegistry.LAVA_VISION.getKey());

                MobEffectInstance lavaVisionEffect = player.getEffect(lavaVisionHolder);

                if (lavaVisionEffect != null) {
                    player.addEffect(new MobEffectInstance(
                            lavaVisionHolder,
                            lavaVisionEffect.getDuration() + extraDuration,
                            lavaVisionEffect.getAmplifier()
                    ));
                } else {
                    player.addEffect(new MobEffectInstance(
                            lavaVisionHolder,
                            extraDuration,
                            0
                    ));
                }
                player.clearFire();

                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.BOTTLE_FILL_DRAGONBREATH,
                        SoundSource.PLAYERS,
                        1.0F,
                        1.0F
                );

                player.getCooldowns().addCooldown(this, getCooldownForLevel(level));

                if (player instanceof ServerPlayer serverPlayer) {
                    stack.hurtAndBreak(
                            1,
                            serverPlayer.serverLevel(),
                            serverPlayer,
                            brokenItem -> serverPlayer.onEquippedItemBroken(
                                    brokenItem,
                                    LivingEntity.getSlotForHand(hand)
                            )
                    );
                }
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }

    private boolean tryUpgradeRune(Player player, ItemStack runeStack) {
        int currentLevel = getRuneLevel(runeStack);
        if (currentLevel >= MAX_LEVEL) {
            player.displayClientMessage(
                    Component.literal("This rune has reached its maximum power.")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
            return false;
        }

        ItemStack offhand = player.getOffhandItem();


        if (!offhand.is(ItemRegistry.FIRE_RESISTANCE_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("Another Inferno Rune is needed in your offhand to stoke the flames.")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return false;
        }

        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The heat bends away from you. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_RED),
                true
        );

        return true;
    }

    private int getExtraDurationForLevel(int level) {
        return switch (level) {
            case 0 -> 600;
            case 1 -> 900;
            case 2 -> 1200;
            case 3 -> 1600;
            default -> 600;
        };
    }

    private int getCooldownForLevel(int level) {
        return switch (level) {
            case 0 -> 400;
            case 1 -> 300;
            case 2 -> 200;
            case 3 -> 120;
            default -> 400;
        };
    }


    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.FIRE_RES_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.FIRE_RES_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("item.runes.fire_resistance_rune.tooltip")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.runes.fire_resistance_rune.lore_1")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.runes.fire_resistance_rune.lore_2")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);
        String[] roman = {"I", "II", "III", "IV"};

        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_GREEN));

        float durationSeconds = getExtraDurationForLevel(level) / 20.0F;
        float cdSeconds = getCooldownForLevel(level) / 20.0F;

        tooltip.add(Component.literal(
                        "Duration per use: " + durationSeconds + "s   Cooldown: " + cdSeconds + "s")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.runes.hold_shift"));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.runes.fire_resistance_rune.upgrade_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
