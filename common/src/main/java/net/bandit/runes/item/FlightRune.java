package net.bandit.runes.item;

import net.bandit.runes.registry.EffectsRegistry;
import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class FlightRune extends Item {

    private static final int MAX_LEVEL = 3;

    public FlightRune(Properties properties) {
        super(properties.durability(24));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Sneak-use to upgrade
        if (!world.isClientSide && player.isShiftKeyDown()) {
            if (tryUpgradeRune(player, stack)) {
                return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
            }
        }

        if (!world.isClientSide) {
            int level = getRuneLevel(stack);

            if (!player.getCooldowns().isOnCooldown(this)) {

                int duration = getDurationForLevel(level);

                var mobEffectRegistry = world.registryAccess().registryOrThrow(Registries.MOB_EFFECT);
                var flightHolder = mobEffectRegistry.getHolderOrThrow(EffectsRegistry.CREATIVE_FLIGHT.getKey());

                player.addEffect(new MobEffectInstance(flightHolder, duration, 0));


                // Sound
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

                // Cooldown + durability
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

        // Require another Flight Rune in offhand
        if (!offhand.is(ItemRegistry.CREATIVE_FLIGHT_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("Another Rune of Ascension is needed in your offhand to heighten your flight.")
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return false;
        }

        // Consume one upgrade item
        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The winds answer more eagerly. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_AQUA),
                true
        );

        return true;
    }

    private int getDurationForLevel(int level) {
        return switch (level) {
            case 0 -> 6000;
            case 1 -> 8400;
            case 2 -> 10800;
            case 3 -> 14400;
            default -> 6000;
        };
    }

    private int getCooldownForLevel(int level) {
        return switch (level) {
            case 0 -> 1200;
            case 1 -> 900;
            case 2 -> 600;
            case 3 -> 400;
            default -> 1200;
        };
    }


    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.FLIGHT_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.FLIGHT_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
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
        tooltip.add(Component.translatable("item.runes.creative_flight_rune.tooltip")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("item.runes.creative_flight_rune.lore_1")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.runes.creative_flight_rune.lore_2")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);
        String[] roman = {"I", "II", "III", "IV"};

        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_GREEN));

        float durationSeconds = getDurationForLevel(level) / 20.0F;
        float cdSeconds = getCooldownForLevel(level) / 20.0F;

        tooltip.add(Component.literal("Duration: " + durationSeconds + "s   Cooldown: " + cdSeconds + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.creative_flight_rune.upgrade_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
