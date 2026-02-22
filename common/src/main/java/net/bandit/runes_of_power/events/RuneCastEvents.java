package net.bandit.runes_of_power.events;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.bandit.runes_of_power.item.StormRune;
import net.bandit.runes_of_power.item.BloodRune;
import net.bandit.runes_of_power.registry.ItemRegistry;
import net.bandit.runes_of_power.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RuneCastEvents {

    public static void register() {

        InteractionEvent.RIGHT_CLICK_ITEM.register((Player player, InteractionHand hand) -> {
            Level world = player.level();
            ItemStack stack = player.getItemInHand(hand);

            boolean sneaking = player.isShiftKeyDown();

            if (sneaking) {
                ItemStack offhand = player.getOffhandItem();

                if (offhand.is(ItemRegistry.EMPTY_RUNE.get())) {

                    int stormSocket = StormRune.getSocketLevel(stack);
                    int bloodSocket = BloodRune.getSocketLevel(stack);

                    if (stormSocket >= 0 || bloodSocket >= 0) {

                        if (!world.isClientSide) {

                            offhand.shrink(1);

                            SoundEvent extractionSound = null;

                            if (stormSocket >= 0) {
                                StormRune.clearSocketLevel(stack);
                                extractionSound = SoundsRegistry.STORM_RUNE_USE.get();
                            }
                            if (bloodSocket >= 0) {
                                BloodRune.clearSocketLevel(stack);
                                extractionSound = SoundsRegistry.BLOOD_RUNE_USE.get();
                            }

                            player.displayClientMessage(
                                    Component.literal("The rune has been extracted, but its power dissipates.")
                                            .withStyle(ChatFormatting.YELLOW),
                                    true
                            );

                            if (extractionSound != null) {
                                world.playSound(
                                        null,
                                        player.getX(), player.getY(), player.getZ(),
                                        extractionSound,
                                        SoundSource.PLAYERS,
                                        0.6F,
                                        0.8F
                                );
                            }
                        }

                        player.swing(hand, true);
                        return CompoundEventResult.interruptTrue(stack);
                    }
                }

                return CompoundEventResult.pass();
            }

            int stormLevel = StormRune.getSocketLevel(stack);
            int bloodLevel = BloodRune.getSocketLevel(stack);

            if (stormLevel < 0 && bloodLevel < 0) {
                return CompoundEventResult.pass();
            }

            if (!world.isClientSide && player.getCooldowns().isOnCooldown(stack.getItem())) {
                return CompoundEventResult.pass();
            }

            SoundEvent castSound = null;
            int maxCooldown = 0;

            if (!world.isClientSide) {

                if (stormLevel >= 0) {
                    StormRune.castFromWeapon(world, player, hand, stormLevel);
                    castSound = SoundsRegistry.STORM_RUNE_USE.get();
                    maxCooldown = Math.max(maxCooldown, StormRune.getCooldownForLevel(stormLevel));
                }

                if (bloodLevel >= 0) {
                    BloodRune.castFromWeapon(world, player, hand, bloodLevel);
                    castSound = SoundsRegistry.BLOOD_RUNE_USE.get();
                    maxCooldown = Math.max(maxCooldown, BloodRune.getCooldownForLevel(bloodLevel));
                }

                if (maxCooldown > 0) {
                    player.getCooldowns().addCooldown(stack.getItem(), maxCooldown);
                }

            } else {
                if (stormLevel >= 0) {
                    castSound = SoundsRegistry.STORM_RUNE_USE.get();
                } else if (bloodLevel >= 0) {
                    castSound = SoundsRegistry.BLOOD_RUNE_USE.get();
                }

                if (castSound != null) {
                    world.playLocalSound(
                            player.getX(), player.getY(), player.getZ(),
                            castSound,
                            SoundSource.PLAYERS,
                            0.4F, 0.9F, false
                    );
                }
            }

            player.swing(hand, true);
            return CompoundEventResult.interruptTrue(stack);
        });
    }
}
