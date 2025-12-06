package net.bandit.runes.events;

import dev.architectury.event.events.client.ClientTooltipEvent;
import net.bandit.runes.item.StormRune;
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

                    int socketLevel = StormRune.getSocketLevel(stack);
                    if (socketLevel < 0) {
                        return;
                    }

                    int displayTier = socketLevel + 1;

                    String romanTier = switch (displayTier) {
                        case 1 -> "I";
                        case 2 -> "II";
                        case 3 -> "III";
                        case 4 -> "IV";
                        default -> "?";
                    };

                    lines.add(Component.empty());

                    lines.add(
                            Component.literal("🔮 Socketed Rune: ")
                                    .withStyle(ChatFormatting.AQUA)
                                    .append(
                                            Component.literal("Storm (" + romanTier + ")")
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
        );
    }
}
