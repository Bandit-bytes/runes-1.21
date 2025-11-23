package net.bandit.runes.item;

import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.ModDataComponents;
import net.bandit.runes.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.LivingEntity;




import java.util.List;

public class BlackOutRune extends Item {

    private static final int MAX_LEVEL = 3;

    public BlackOutRune(Properties properties) {
        super(properties.durability(100));
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
            if (!player.getCooldowns().isOnCooldown(this)) {
                int level = getRuneLevel(stack);

                int cooldown = getCooldownForLevel(level);
                int effectDuration = getDisorientDurationForLevel(level);

                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, effectDuration, 0));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, effectDuration, 0));

                player.getServer().execute(() -> teleportPlayer(player));

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
            player.level().playLocalSound(
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundsRegistry.BLACK_OUT_RUNE_USE.get(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F,
                    false
            );
            playItemUseAnimation(player, stack);
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

        // Require another BlackOut Rune in offhand
        if (!offhand.is(ItemRegistry.BLACK_OUT_RUNE.get())) {
            player.displayClientMessage(
                    Component.literal("You need another Shadow Rune in your offhand to deepen this bond.")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return false;
        }

        offhand.shrink(1);

        int newLevel = currentLevel + 1;
        setRuneLevel(runeStack, newLevel);

        player.displayClientMessage(
                Component.literal("The shadows tighten around you. (Tier " + (newLevel + 1) + ")")
                        .withStyle(ChatFormatting.DARK_PURPLE),
                true
        );

        return true;
    }

    private int getCooldownForLevel(int level) {
        return switch (level) {
            case 0 -> 200; // 10s
            case 1 -> 160; // 8s
            case 2 -> 120; // 6s
            case 3 -> 80;  // 4s
            default -> 200;
        };
    }

    private int getDisorientDurationForLevel(int level) {
        return switch (level) {
            case 0 -> 140; // 7s
            case 1 -> 100; // 5s
            case 2 -> 60;  // 3s
            case 3 -> 40;  // 2s
            default -> 100;
        };
    }

    private void teleportPlayer(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        BlockPos bedLocation = serverPlayer.getRespawnPosition();
        ResourceKey<Level> respawnDimension = serverPlayer.getRespawnDimension();

        ServerLevel targetWorld = null;
        Vec3 respawnPos = null;

        // 1) Try bed + its dimension
        if (bedLocation != null) {
            targetWorld = serverPlayer.server.getLevel(respawnDimension);
            if (targetWorld != null) {
                respawnPos = findSafeFeetPos(targetWorld, bedLocation);
            }
        }

        // 2) Fallback: overworld spawn
        if (respawnPos == null) {
            targetWorld = serverPlayer.server.getLevel(Level.OVERWORLD);
            if (targetWorld != null) {
                BlockPos worldSpawn = targetWorld.getSharedSpawnPos();
                respawnPos = findSafeFeetPos(targetWorld, worldSpawn);
            }
        }

        if (respawnPos != null && targetWorld != null) {
            serverPlayer.teleportTo(
                    targetWorld,
                    respawnPos.x,
                    respawnPos.y,
                    respawnPos.z,
                    serverPlayer.getYRot(),
                    serverPlayer.getXRot()
            );
        }
    }

    /**
     * Very simple "try not to put you in a block" helper.
     */
    private Vec3 findSafeFeetPos(ServerLevel level, BlockPos basePos) {
        BlockPos.MutableBlockPos pos = basePos.mutable();

        // Move up until we find two air blocks for feet + head
        int maxY = level.getMaxBuildHeight() - 2;
        while (pos.getY() < maxY &&
                (!level.isEmptyBlock(pos) || !level.isEmptyBlock(pos.above()))) {
            pos.move(0, 1, 0);
        }

        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }


    private void playItemUseAnimation(Player player, ItemStack stack) {
        if (player.level().isClientSide) {
            Minecraft.getInstance().gameRenderer.displayItemActivation(stack);
        }
    }

    // === Data Component Level Helpers ===

    private int getRuneLevel(ItemStack stack) {
        int lvl = stack.getOrDefault(ModDataComponents.BLACK_OUT_LEVEL.get(), 0);
        return Mth.clamp(lvl, 0, MAX_LEVEL);
    }

    private void setRuneLevel(ItemStack stack, int level) {
        stack.set(ModDataComponents.BLACK_OUT_LEVEL.get(), Mth.clamp(level, 0, MAX_LEVEL));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag tooltipFlag) {
        tooltip.add(Component.translatable("item.runes.black_out_rune.tooltip")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.translatable("item.runes.black_out_rune.lore")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

        int level = getRuneLevel(stack);

        String[] roman = {"I", "II", "III", "IV"};
        tooltip.add(Component.literal("Tier: " + roman[Mth.clamp(level, 0, 3)])
                .withStyle(ChatFormatting.DARK_GREEN));

        float cdSeconds = getCooldownForLevel(level) / 20.0F;
        float durSeconds = getDisorientDurationForLevel(level) / 20.0F;

        tooltip.add(Component.literal(
                        "Cooldown: " + cdSeconds + "s   Disorientation: " + durSeconds + "s")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.translatable("item.runes.black_out_rune.upgrade_hint")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}
