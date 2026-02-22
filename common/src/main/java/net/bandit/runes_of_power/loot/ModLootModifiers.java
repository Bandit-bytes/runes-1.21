package net.bandit.runes_of_power.loot;

import dev.architectury.event.events.common.LootEvent;
import net.bandit.runes_of_power.config.RunesConfig;
import net.bandit.runes_of_power.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModLootModifiers {

    private static ResourceKey<LootTable> createKey(String namespace, String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    private static final Set<ResourceKey<LootTable>> EASY_LOOT_TABLES = Set.of(
            createKey("minecraft", "chests/simple_dungeon"),
            createKey("minecraft", "chests/abandoned_mineshaft"),
            createKey("minecraft", "chests/village/village_temple")
    );

    private static final Set<ResourceKey<LootTable>> MEDIUM_LOOT_TABLES = Set.of(
            createKey("minecraft", "chests/jungle_temple"),
            createKey("minecraft", "chests/desert_pyramid"),
            createKey("minecraft", "chests/pillager_outpost"),
            createKey("minecraft", "chests/abandoned_mineshaft"),
            createKey("minecraft", "chests/simple_dungeon")
    );

    private static final Set<ResourceKey<LootTable>> HARD_LOOT_TABLES = Set.of(
            createKey("minecraft", "chests/stronghold_corridor"),
            createKey("minecraft", "chests/nether_bridge"),
            createKey("minecraft", "chests/bastion_treasure")
    );

    private static final Set<ResourceKey<LootTable>> ENDGAME_LOOT_TABLES = Set.of(
            createKey("minecraft", "chests/end_city_treasure"),
            createKey("minecraft", "chests/ancient_city"),
            createKey("minecraft", "chests/bastion_treasure")
    );

    public static void registerLootModifiers() {
        LootEvent.MODIFY_LOOT_TABLE.register((key, context, builtin) -> {
            if (!builtin) return;

            RunesConfig.ConfigData cfg = RunesConfig.configData;

            if (cfg.easyLootEnabled && EASY_LOOT_TABLES.contains(key)) {
                context.addPool(createEasyRunePool(cfg.easyLootDropChance));
            }

            if (cfg.mediumLootEnabled && MEDIUM_LOOT_TABLES.contains(key)) {
                context.addPool(createMediumRunePool(cfg.mediumLootDropChance));
            }

            if (cfg.hardLootEnabled && HARD_LOOT_TABLES.contains(key)) {
                context.addPool(createHardRunePool(cfg.hardLootDropChance));
            }

            if (cfg.endgameLootEnabled && ENDGAME_LOOT_TABLES.contains(key)) {
                context.addPool(createEndgameRunePool(cfg.endgameLootDropChance));
            }
        });
    }

    private static LootPool.Builder createEasyRunePool(float chance) {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(0.0F, 1.0F))
                .add(LootItem.lootTableItem(ItemRegistry.EMPTY_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.WATER_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BURROW_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.FIRE_RESISTANCE_RUNE.get()))
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }

    private static LootPool.Builder createMediumRunePool(float chance) {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(0.0F, 1.0F))
                .add(LootItem.lootTableItem(ItemRegistry.EMPTY_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.WATER_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.STORM_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BLOOD_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BLACK_OUT_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.FIRE_RESISTANCE_RUNE.get()))
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }

    private static LootPool.Builder createHardRunePool(float chance) {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(0.0F, 1.0F))
                .add(LootItem.lootTableItem(ItemRegistry.STEALTH_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BURROW_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.STORM_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BLOOD_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.TELEPORT_RUNE.get()))
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }

    private static LootPool.Builder createEndgameRunePool(float chance) {
        return LootPool.lootPool()
                .setRolls(UniformGenerator.between(0.0F, 1.0F))
                .add(LootItem.lootTableItem(ItemRegistry.CURE_INSOMNIA_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.STEALTH_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BURROW_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.STORM_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.BLOOD_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.CREATIVE_FLIGHT_RUNE.get()))
                .add(LootItem.lootTableItem(ItemRegistry.TELEPORT_RUNE.get()))
                .when(LootItemRandomChanceCondition.randomChance(chance));
    }
}
