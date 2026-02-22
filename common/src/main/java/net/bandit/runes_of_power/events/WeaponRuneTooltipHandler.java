package net.bandit.runes_of_power.events;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.bandit.runes_of_power.item.StormRune;
import net.bandit.runes_of_power.item.BloodRune;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class WeaponRuneTooltipHandler {

    public static void register() {

        ClientTooltipEvent.ITEM.register(
                (ItemStack stack,
                 List<Component> lines,
                 Item.TooltipContext tooltipContext,
                 TooltipFlag flag) -> {

                    int stormLevel = StormRune.getSocketLevel(stack);
                    int bloodLevel = BloodRune.getSocketLevel(stack);

                    if (stormLevel < 0 && bloodLevel < 0) {
                        return;
                    }

                    lines.add(Component.empty());
                    java.util.function.IntFunction<String> roman = tier -> switch (tier) {
                        case 1 -> "I";
                        case 2 -> "II";
                        case 3 -> "III";
                        case 4 -> "IV";
                        default -> "?";
                    };

                    if (stormLevel >= 0) {
                        int displayTier = stormLevel + 1;
                        lines.add(
                                Component.literal("🔮 Socketed Rune: ")
                                        .withStyle(ChatFormatting.AQUA)
                                        .append(
                                                Component.literal("Storm (" + roman.apply(displayTier) + ")")
                                                        .withStyle(ChatFormatting.DARK_PURPLE)
                                        )
                        );

                        lines.add(
                                Component.literal("Right-click to cast Chain Lightning")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withColor(ChatFormatting.GRAY)
                                                        .withItalic(true)
                                        )
                        );
                    }

                    if (bloodLevel >= 0) {
                        int displayTier = bloodLevel + 1;
                        lines.add(
                                Component.literal("🔮 Socketed Rune: ")
                                        .withStyle(ChatFormatting.AQUA)
                                        .append(
                                                Component.literal("Blood (" + roman.apply(displayTier) + ")")
                                                        .withStyle(ChatFormatting.DARK_RED)
                                        )
                        );

                        lines.add(
                                Component.literal("Right-click to drain nearby foes")
                                        .setStyle(
                                                Style.EMPTY
                                                        .withColor(ChatFormatting.GRAY)
                                                        .withItalic(true)
                                        )
                        );
                    }
                }
        );
    }
}
