package net.bandit.runes.item;

import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.bandit.runes.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class StormRune extends Item {

    private static final int MAX_LEVEL = 3;

    public StormRune(Properties properties) {
        super(properties.durability(100));
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
            if (!player.getCooldowns().isOnCooldown(this)) {
                int level = getRuneLevel(stack);

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));

                if (world instanceof ServerLevel serverLevel) {
                    castChainLightning(serverLevel, player, level);
                }

                int cooldown = getCooldownForLevel(level);
                player.getCooldowns().addCooldown(this, cooldown);

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
        } else {
            world.playLocalSound(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundsRegistry.STORM_RUNE_USE.get(),
                    SoundSource.PLAYERS,
                    0.4F,
                    0.9F,
                    false
            );
            playItemUseAnimation(player, hand);
        }


        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }


    private void castChainLightning(ServerLevel world, Player player, int level) {
        int maxTargets = getMaxTargetsForLevel(level);
        float damage = getDamageForLevel(level);
        double range = getRangeForLevel(level);

        List<LivingEntity> candidates = world.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(range),
                entity -> entity.isAlive()
                        && entity != player
                        && !entity.isAlliedTo(player)
        );

        if (candidates.isEmpty()) {
            return;
        }

        List<LivingEntity> chain = new ArrayList<>();
        LivingEntity current = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (current == null) return;

        chain.add(current);
        candidates.remove(current);

        while (chain.size() < maxTargets && !candidates.isEmpty()) {
            LivingEntity last = chain.get(chain.size() - 1);
            LivingEntity next = candidates.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(last)))
                    .orElse(null);
            if (next == null) break;
            chain.add(next);
            candidates.remove(next);
        }

        LivingEntity previous = player;

        for (LivingEntity target : chain) {
            target.hurt(world.damageSources().magic(), damage);
            spawnLightningArc(world, previous, target);

            world.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundsRegistry.STORM_RUNE_USE.get(),
                    SoundSource.PLAYERS,
                    0.7F,
                    1.2F
            );
            world.gameEvent(GameEvent.LIGHTNING_STRIKE, BlockPos.containing(target.position()), GameEvent.Context.of(player));

            previous = target;
        }
    }

    private void spawnLightningArc(ServerLevel world, LivingEntity from, LivingEntity to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() + to.getBbHeight() * 0.5 - (from.getY() + from.getBbHeight() * 0.5);
        double dz = to.getZ() - from.getZ();
        int steps = 12;

        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            double px = from.getX() + dx * t;
            double py = from.getY() + from.getBbHeight() * 0.5 + dy * t;
            double pz = from.getZ() + dz * t;

            world.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    px, py, pz,
                    1,
                    0.02, 0.02, 0.02,
                    0.01
            );
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

        if (!offhand.is(ItemRegistry.STORM_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("You need a special catalyst in your offhand to empower this rune.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("Storms answer your call. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.AQUA),
                true
        );

        return true;
    }


    private int getMaxTargetsForLevel(int level) {
        return switch (level) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            default -> 2;
        };
    }

    private float getDamageForLevel(int level) {
        return switch (level) {
            case 0 -> 4.0F;
            case 1 -> 6.0F;
            case 2 -> 8.0F;
            case 3 -> 10.0F;
            default -> 4.0F;
        };
    }

    private int getCooldownForLevel(int level) {
        // ticks
        return switch (level) {
            case 0 -> 140;
            case 1 -> 100;
            case 2 -> 60;
            case 3 -> 20;
            default -> 240;
        };
    }

    private double getRangeForLevel(int level) {
        return switch (level) {
            case 0 -> 8.0;
            case 1 -> 10.0;
            case 2 -> 12.0;
            case 3 -> 14.0;
            default -> 8.0;
        };
    }

    private void playItemUseAnimation(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            player.swing(hand, true);
        }
    }

    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.STORM_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.STORM_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }


    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag tooltipFlag) {

        tooltip.add(Component.translatable("item.runes.storm_rune.tooltip")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.runes.storm_rune.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);
        String[] roman = {"I", "II", "III", "IV"};

        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.literal(
                        "Targets: " + getMaxTargetsForLevel(level) +
                                "   Damage: " + (int) getDamageForLevel(level) / 2 + "❤" +
                                "   Cooldown: " + (getCooldownForLevel(level) / 20.0F) + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.storm_rune.upgrade_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
