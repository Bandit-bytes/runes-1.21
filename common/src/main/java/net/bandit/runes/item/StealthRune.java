package net.bandit.runes.item;

import net.bandit.runes.registry.EffectsRegistry;
import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

public class StealthRune extends Item {

    private static final int MAX_LEVEL = 3;

    public StealthRune(Properties properties) {
        super(properties.durability(10));
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
                int duration = getDurationForLevel(level);

                var mobEffectRegistry = world.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.MOB_EFFECT);
                var stealthHolder = mobEffectRegistry.getHolderOrThrow(EffectsRegistry.STEALTH.getKey());

                MobEffectInstance existing = player.getEffect(stealthHolder);
                if (existing != null) {
                    player.addEffect(new MobEffectInstance(
                            stealthHolder,
                            existing.getDuration() + duration,
                            existing.getAmplifier(),
                            false,
                            true
                    ));
                } else {
                    player.addEffect(new MobEffectInstance(
                            stealthHolder,
                            duration,
                            0,
                            false,
                            true
                    ));
                }

                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.BOTTLE_EMPTY,
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
                            broken -> serverPlayer.onEquippedItemBroken(
                                    broken,
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

        if (!offhand.is(ItemRegistry.STEALTH_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("Another Veil Rune is needed in your offhand to deepen the shadows.")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    true
            );
            return false;
        }

        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The shadows cling closer. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true
        );

        return true;
    }

    // Duration scaling (ticks)
    private int getDurationForLevel(int level) {
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

    // === Data Components ===

    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.STEALTH_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.STEALTH_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        tooltip.add(Component.translatable("item.runes.stealth_rune.tooltip")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.runes.stealth_rune.lore")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);

        String[] roman = {"I", "II", "III", "IV"};
        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_GREEN));

        float durationSeconds = getDurationForLevel(level) / 20.0F;
        float cdSeconds = getCooldownForLevel(level) / 20.0F;

        tooltip.add(Component.literal(
                        "Duration: " + durationSeconds + "s   Cooldown: " + cdSeconds + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.stealth_rune.upgrade_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
