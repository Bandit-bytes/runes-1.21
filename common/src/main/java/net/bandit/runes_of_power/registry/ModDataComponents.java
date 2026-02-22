package net.bandit.runes_of_power.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.bandit.runes_of_power.RunesMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(RunesMod.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Integer>> BLACK_OUT_LEVEL =
            DATA_COMPONENTS.register("black_out_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> BREATH_LEVEL =
            DATA_COMPONENTS.register("breath_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> BURROW_LEVEL =
            DATA_COMPONENTS.register("burrow_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> CURE_LEVEL =
            DATA_COMPONENTS.register("cure_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> FLIGHT_LEVEL =
            DATA_COMPONENTS.register("flight_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> FIRE_RES_LEVEL =
            DATA_COMPONENTS.register("fire_res_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> STEALTH_LEVEL =
            DATA_COMPONENTS.register("stealth_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> TELEPORT_LEVEL =
            DATA_COMPONENTS.register("teleport_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<ResourceKey<Level>>> TELEPORT_DESTINATION =
            DATA_COMPONENTS.register("teleport_destination", () ->
                    DataComponentType.<ResourceKey<Level>>builder()
                            .persistent(ResourceKey.codec(Registries.DIMENSION))
                            .networkSynchronized(StreamCodec.of(
                                    (buf, value) -> buf.writeResourceLocation(value.location()),
                                    buf -> ResourceKey.create(
                                            Registries.DIMENSION,
                                            buf.readResourceLocation()
                                    )
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> STORM_RUNE_LEVEL =
            DATA_COMPONENTS.register("storm_rune_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> STORM_SOCKET_LEVEL =
            DATA_COMPONENTS.register("storm_socket_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> BLOOD_RUNE_LEVEL =
            DATA_COMPONENTS.register("blood_rune_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );
    public static final RegistrySupplier<DataComponentType<Integer>> BLOOD_SOCKET_LEVEL =
            DATA_COMPONENTS.register("blood_socket_level", () ->
                    DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(StreamCodec.of(
                                    RegistryFriendlyByteBuf::writeVarInt,
                                    RegistryFriendlyByteBuf::readVarInt
                            ))
                            .build()
            );

    public static void register() {
        DATA_COMPONENTS.register();
    }
}
