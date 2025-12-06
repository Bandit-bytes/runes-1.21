package net.bandit.runes.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class EmptyRune extends Item {

    public EmptyRune(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack,
                                TooltipContext context,
                                List<Component> tooltip,
                                TooltipFlag flag) {

        tooltip.add(Component.literal("★ Rune Extraction Catalyst")
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(Component.literal("Used to remove socketed runes from weapons.")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.literal("Consumes this item and destroys the socketed rune.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }
}
