package net.bandit.runes.events;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.bandit.runes.item.StormRune;
import net.bandit.runes.registry.ItemRegistry;
import net.bandit.runes.registry.SoundsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
                ItemStack off = player.getOffhandItem();

                if (off.is(ItemRegistry.EMPTY_RUNE.get())) {

                    int socketLevel = StormRune.getSocketLevel(stack);
                    if (socketLevel >= 0) {
                        if (!world.isClientSide) {
                            off.shrink(1);

                            StormRune.clearSocketLevel(stack);

                            player.displayClientMessage(
                                    Component.literal("The rune has been extracted, but its power dissipates.")
                                            .withStyle(ChatFormatting.YELLOW),
                                    true
                            );

                            world.playSound(
                                    null,
                                    player.getX(), player.getY(), player.getZ(),
                                    SoundsRegistry.STORM_RUNE_USE.get(),
                                    SoundSource.PLAYERS,
                                    0.6F,
                                    0.8F
                            );
                        }

                        player.swing(hand, true);
                        return CompoundEventResult.interruptTrue(stack);
                    }
                }

                return CompoundEventResult.pass();
            }


            int socketLevel = StormRune.getSocketLevel(stack);
            if (socketLevel < 0) {
                return CompoundEventResult.pass();
            }

            // Handle cooldown
            if (!world.isClientSide && player.getCooldowns().isOnCooldown(stack.getItem())) {
                return CompoundEventResult.pass();
            }

            if (!world.isClientSide) {
                StormRune.castFromWeapon(world, player, hand, socketLevel);
                int cooldown = StormRune.getCooldownForLevel(socketLevel);
                player.getCooldowns().addCooldown(stack.getItem(), cooldown);
            } else {
                world.playLocalSound(
                        player.getX(), player.getY(), player.getZ(),
                        SoundsRegistry.STORM_RUNE_USE.get(),
                        SoundSource.PLAYERS,
                        0.4F, 0.9F, false
                );
            }

            player.swing(hand, true);
            return CompoundEventResult.interruptTrue(stack);
        });
    }
}
