package net.bandit.runes.item;

import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.bandit.runes.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BloodRune extends Item {

    private static final int MAX_LEVEL = 3;

    public BloodRune(Properties properties) {
        super(properties.durability(56));
    }


    public static void castFromWeapon(Level world, Player player, InteractionHand hand, int socketLevel) {
        if (!(world instanceof ServerLevel server)) return;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, false, false));
        BloodRune.castLifeDrain(server, player, socketLevel);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack   = player.getItemInHand(hand);
        ItemStack offhand = player.getOffhandItem();

        boolean offhandIsRune   = offhand.is(ItemRegistry.BLOOD_RUNE.get());
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

            Integer existingObj = offhand.get(ModDataComponents.BLOOD_SOCKET_LEVEL.get());
            int existingLevel = (existingObj == null || existingObj <= 0) ? -1 : existingObj;

            if (existingLevel < 0) {
                setSocketLevel(offhand, runeLevel);

                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Blood Rune etched into your weapon. (Tier " + (runeLevel + 1) + ")")
                                    .withStyle(ChatFormatting.DARK_RED),
                            true
                    );
                }
            } else if (runeLevel > existingLevel) {
                setSocketLevel(offhand, runeLevel);

                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("The Blood Rune in your weapon grows hungrier. (Tier " + (runeLevel + 1) + ")")
                                    .withStyle(ChatFormatting.DARK_RED),
                            true
                    );
                }
            } else {
                if (!world.isClientSide) {
                    player.displayClientMessage(
                            Component.literal("This weapon already holds an equal or stronger Blood Rune.")
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
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false));

                if (world instanceof ServerLevel serverLevel) {
                    castLifeDrain(serverLevel, player, level);
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
                    SoundsRegistry.BLOOD_RUNE_USE.get(),
                    SoundSource.PLAYERS,
                    0.4F,
                    0.9F,
                    false
            );
            playItemUseAnimation(player, hand);
        }

        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
    }

    public static void castLifeDrain(ServerLevel world, Player player, int level) {
        double range            = getRangeForLevel(level);
        float damagePerTarget   = getDamageForLevel(level);
        float healFraction      = getHealFractionForLevel(level);
        int maxTargets          = getMaxTargetsForLevel(level);

        List<LivingEntity> candidates = world.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(player.blockPosition()).inflate(range),
                entity -> entity.isAlive()
                        && entity != player
                        && !entity.isAlliedTo(player)
        );

        if (candidates.isEmpty()) return;
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(player)));
        List<LivingEntity> targets = new ArrayList<>(candidates.subList(0, Math.min(maxTargets, candidates.size())));

        float totalDamage = 0f;

        for (LivingEntity target : targets) {
            DamageSource source = world.damageSources().indirectMagic(player, player);
            boolean hurt = target.hurt(source, damagePerTarget);
            if (hurt) {
                totalDamage += damagePerTarget;
            }

            spawnDrainParticles(world, player, target);

            world.playSound(
                    null,
                    target.getX(),
                    target.getY(),
                    target.getZ(),
                    SoundsRegistry.BLOOD_RUNE_USE.get(),
                    SoundSource.PLAYERS,
                    0.7F,
                    0.9F + world.random.nextFloat() * 0.2F
            );

            world.gameEvent(GameEvent.ENTITY_DAMAGE, BlockPos.containing(target.position()), GameEvent.Context.of(player));
        }

        if (totalDamage > 0) {
            float healAmount = totalDamage * healFraction;
            player.heal(healAmount);

            world.sendParticles(
                    ParticleTypes.HEART,
                    player.getX(),
                    player.getY() + player.getBbHeight() * 0.5,
                    player.getZ(),
                    6,
                    0.3, 0.3, 0.3,
                    0.01
            );
        }
    }

    private static void spawnDrainParticles(ServerLevel world, LivingEntity from, LivingEntity to) {
        double dx = from.getX() - to.getX();
        double dy = (from.getY() + from.getBbHeight() * 0.5) - (to.getY() + to.getBbHeight() * 0.5);
        double dz = from.getZ() - to.getZ();
        int steps = 10;

        for (int i = 0; i <= steps; i++) {
            double t  = i / (double) steps;
            double px = to.getX() + dx * t;
            double py = to.getY() + to.getBbHeight() * 0.5 + dy * t;
            double pz = to.getZ() + dz * t;

            world.sendParticles(
                    ParticleTypes.DAMAGE_INDICATOR,
                    px, py, pz,
                    1,
                    0.02, 0.02, 0.02,
                    0.0
            );
        }
    }

    private boolean tryUpgradeRune(Player player, ItemStack runeStack, ItemStack catalystStack) {
        int currentLevel = getRuneLevel(runeStack);
        if (currentLevel >= MAX_LEVEL) {
            player.displayClientMessage(
                    Component.literal("This rune has reached its maximum hunger.")
                            .withStyle(ChatFormatting.DARK_RED),
                    true
            );
            return false;
        }

        if (!catalystStack.is(ItemRegistry.BLOOD_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("You need another Blood Rune in your offhand to empower this rune.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return false;
        }

        catalystStack.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The rune drinks deeper. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_RED),
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
            case 0 -> 3.0F;
            case 1 -> 5.0F;
            case 2 -> 7.0F;
            case 3 -> 9.0F;
            default -> 3.0F;
        };
    }

    private static float getHealFractionForLevel(int level) {
        return switch (level) {
            case 0 -> 0.30F;
            case 1 -> 0.35F;
            case 2 -> 0.40F;
            case 3 -> 0.50F;
            default -> 0.30F;
        };
    }

    public static int getCooldownForLevel(int level) {
        return switch (level) {
            case 0 -> 160;
            case 1 -> 120;
            case 2 -> 80;
            case 3 -> 40;
            default -> 200;
        };
    }

    private static double getRangeForLevel(int level) {
        return switch (level) {
            case 0 -> 6.0;
            case 1 -> 8.0;
            case 2 -> 10.0;
            case 3 -> 12.0;
            default -> 6.0;
        };
    }


    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.BLOOD_RUNE_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.BLOOD_RUNE_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    public static int getSocketLevel(ItemStack stack) {
        Integer lvl = stack.get(ModDataComponents.BLOOD_SOCKET_LEVEL.get());
        if (lvl == null) return -1;
        return Mth.clamp(lvl, -1, MAX_LEVEL);
    }

    public static void setSocketLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.BLOOD_SOCKET_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    public static void clearSocketLevel(ItemStack stack) {
        stack.remove(ModDataComponents.BLOOD_SOCKET_LEVEL.get());
    }


    private void playItemUseAnimation(Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            player.swing(hand, true);
        }
    }

    private boolean isWeapon(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem;
        // or: return stack.is(ModTags.CAN_HOLD_RUNES);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag tooltipFlag) {

        tooltip.add(Component.translatable("item.runes.blood_rune.tooltip")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.runes.blood_rune.lore")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);
        String[] roman = {"I", "II", "III", "IV"};

        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_PURPLE));

        tooltip.add(Component.literal(
                        "Targets: " + getMaxTargetsForLevel(level) +
                                "   Drain: " + (int) getDamageForLevel(level) / 2 + "❤" +
                                "   Cooldown: " + (getCooldownForLevel(level) / 20.0F) + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.hold_shift"));
        if (Screen.hasShiftDown()) {
            tooltip.add(Component.translatable("item.runes.blood_rune.upgrade_hint")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("item.runes.blood_rune.upgrade_hint2")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            tooltip.add(Component.translatable("item.runes.blood_rune.upgrade_hint3")
                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
