package net.bandit.runes.item;

import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
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
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class CureRune extends Item {

    private static final int MAX_LEVEL = 3;

    public CureRune(Properties properties) {
        super(properties.durability(80));
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
                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));

                applyRestfulEffects(player, level);

                world.playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        SoundEvents.HONEY_DRINK,
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

    private void applyRestfulEffects(Player player, int level) {
        switch (level) {
            case 0 -> {
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            case 2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 0));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 0));
            }
            case 3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
            }
        }
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
        if (!offhand.is(ItemRegistry.CURE_INSOMNIA_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("Another Dreamweaver Rune is needed in your offhand to strengthen this charm.")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            return false;
        }

        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The rune settles your spirit more deeply. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true
        );

        return true;
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
        int lvl = stack.getOrDefault(ModDataComponents.CURE_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.CURE_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
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
        tooltip.add(Component.translatable("item.runes.cure_insomnia_rune.tooltip")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.runes.cure_insomnia_rune.lore")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);
        String[] roman = {"I", "II", "III", "IV"};

        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_GREEN));

        float cdSeconds = getCooldownForLevel(level) / 20.0F;

        tooltip.add(Component.literal("Cooldown: " + cdSeconds + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.cure_insomnia_rune.upgrade_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
