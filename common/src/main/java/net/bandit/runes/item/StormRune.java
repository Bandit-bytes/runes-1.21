package net.bandit.runes.item;

import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.bandit.runes.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
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

    public static void castFromWeapon(Level world, Player player, InteractionHand hand, int socketLevel) {
        if (!(world instanceof ServerLevel server)) return;

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, false, false));
        StormRune.castChainLightning(server, player, socketLevel);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack   = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();

        boolean offhandIsRune   = offhand.is(ItemRegistry.STORM_RUNE.get());
        boolean offhandIsWeapon = isWeapon(offhand);

        if (!world.isClientSide
                && hand == InteractionHand.MAIN_HAND
                && player.isShiftKeyDown()
                && offhandIsRune) {

            boolean upgraded = tryUpgradeRune(player, stack, offhand);
            return upgraded
                    ? InteractionResultHolder.sidedSuccess(stack, world.isClientSide())
                    : InteractionResultHolder.fail(stack);
        }

        if (offhandIsWeapon) {
            int runeLevel = getRuneLevel(stack);


            Integer existingObj = offhand.get(ModDataComponents.STORM_SOCKET_LEVEL.get());
            int existingLevel = (existingObj == null || existingObj <= 0) ? -1 : existingObj;

            if (existingLevel < 0) {
                setSocketLevel(offhand, runeLevel);

                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Storm Rune socketed into your weapon! (Tier " + (runeLevel + 1) + ")")
                                    .withStyle(ChatFormatting.AQUA),
                            true
                    );
                }
            } else if (runeLevel > existingLevel) {
                setSocketLevel(offhand, runeLevel);

                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("The Storm Rune in your weapon grows stronger! (Tier " + (runeLevel + 1) + ")")
                                    .withStyle(ChatFormatting.AQUA),
                            true
                    );
                }
            } else {
                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("This weapon already holds an equal or stronger Storm Rune.")
                                    .withStyle(ChatFormatting.RED),
                            true
                    );
                }
                return InteractionResultHolder.fail(stack);
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            return InteractionResultHolder.success(stack);
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


    public static void castChainLightning(ServerLevel world, Player player, int level) {
        int maxTargets = getMaxTargetsForLevel(level);
        float damage   = getDamageForLevel(level);
        double range   = getRangeForLevel(level);

        List<LivingEntity> candidates = world.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(range),
                entity -> entity.isAlive()
                        && entity != player
                        && !entity.isAlliedTo(player)
        );

        if (candidates.isEmpty()) return;

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
        var damageSource = world.damageSources().indirectMagic(player, player);

        for (LivingEntity target : chain) {
            target.hurt(damageSource, damage);

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


    private static void spawnLightningArc(ServerLevel world, LivingEntity from, LivingEntity to) {
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

    private boolean tryUpgradeRune(Player player, ItemStack runeStack, ItemStack catalystStack) {
        int currentLevel = getRuneLevel(runeStack);
        if (currentLevel >= MAX_LEVEL) {
            player.displayClientMessage(
                    Component.literal("This rune has reached its maximum power.")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
            return false;
        }

        if (!catalystStack.is(ItemRegistry.STORM_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("You need another Storm Rune in your offhand to empower this rune.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        catalystStack.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("Storms answer your call. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.AQUA),
                true
        );

        return true;
    }


    private static int getMaxTargetsForLevel(int level) {
        return switch (level) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 4;
            case 3 -> 5;
            default -> 2;
        };
    }

    private static float getDamageForLevel(int level) {
        return switch (level) {
            case 0 -> 4.0F;
            case 1 -> 6.0F;
            case 2 -> 8.0F;
            case 3 -> 10.0F;
            default -> 4.0F;
        };
    }

    public static int getCooldownForLevel(int level) {
        return switch (level) {
            case 0 -> 140;
            case 1 -> 100;
            case 2 -> 60;
            case 3 -> 20;
            default -> 240;
        };
    }

    private static double getRangeForLevel(int level) {
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
        int lvl = stack.getOrDefault(ModDataComponents.STORM_RUNE_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.STORM_RUNE_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    public static int getSocketLevel(ItemStack stack) {
        Integer lvl = stack.get(ModDataComponents.STORM_SOCKET_LEVEL.get());
        if (lvl == null) return -1;
        return Mth.clamp(lvl, -1, MAX_LEVEL);
    }

    public static void setSocketLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.STORM_SOCKET_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
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

        tooltip.add(Component.translatable("item.runes.hold_shift"));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.runes.storm_rune.upgrade_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("item.runes.storm_rune.upgrade_hint2")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    private boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem;
        // or: return stack.is(ModTags.CAN_HOLD_RUNES);
    }
    public static void clearSocketLevel(ItemStack stack) {
        stack.remove(ModDataComponents.STORM_SOCKET_LEVEL.get());

    }
}
