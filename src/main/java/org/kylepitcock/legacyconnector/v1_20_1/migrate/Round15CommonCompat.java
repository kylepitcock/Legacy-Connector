package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

/** Round 15, common/server side: small Minecraft members of 1.20.1 that 1.21.1 changed (rows in mc-calls.tsv and mc-fields.tsv). */
public final class Round15CommonCompat {
    private Round15CommonCompat() {
    }

    /** {@code new ResourceLocation("ns:path")} (the constructor is private since 1.21). */
    public static ResourceLocation newResourceLocation(String id) {
        return ResourceLocation.parse(id);
    }

    public static MobEffectsPredicate mobEffectsAny() {
        return new MobEffectsPredicate(Map.of());
    }

    public static DamageSourcePredicate damageSourceAny() {
        return new DamageSourcePredicate(List.of(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static EntityFlagsPredicate entityFlagsAny() {
        return new EntityFlagsPredicate(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());
    }

    /** {@code MinMaxBounds.getMin()}: the bound or null (an Optional since 1.20.5). */
    public static Number boundsMin(MinMaxBounds<?> bounds) {
        return bounds.min().orElse(null);
    }

    public static Number boundsMax(MinMaxBounds<?> bounds) {
        return bounds.max().orElse(null);
    }

    public static AdvancementType advancementTypeByName(String name) {
        for (AdvancementType type : AdvancementType.values()) {
            if (type.getSerializedName().equals(name)) return type;
        }
        throw new IllegalArgumentException("Unknown frame type '" + name + "'");
    }

    /** {@code new ChunkStorage(Path, DataFixer, boolean)}: 1.21 wants the storage info (level name, dimension, type). */
    public static ChunkStorage newChunkStorage(Path folder, DataFixer fixer, boolean sync) {
        return new ChunkStorage(new RegionStorageInfo("legacy", Level.OVERWORLD, "chunk"), folder, fixer, sync);
    }

    /** {@code ResourceLocationArgument.getRecipe} returned the recipe; 1.21 answers its holder. */
    public static Recipe<?> argumentRecipe(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ResourceLocationArgument.getRecipe(context, name).value();
    }

    /** {@code ScoreHolderArgument.getName} answered the holder's name; 1.21 answers the holder. */
    public static String scoreHolderName(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return ScoreHolderArgument.getName(context, name).getScoreboardName();
    }

    /** {@code DispenserBlock.getDispenseMethod(ItemStack)}: 1.21 takes the level too; the registry lookup is the same. */
    public static DispenseItemBehavior dispenseMethod(DispenserBlock block, ItemStack stack) {
        DispenseItemBehavior behavior = DispenserBlock.DISPENSER_REGISTRY.get(stack.getItem());
        return behavior != null ? behavior : new DefaultDispenseItemBehavior();
    }

    /** {@code AbstractMinecart.createMinecart(Level, x, y, z, Type)}: 1.21 takes the source stack and player (none here); a client level creates nothing. */
    public static AbstractMinecart createMinecart(Level level, double x, double y, double z, AbstractMinecart.Type type) {
        if (!(level instanceof ServerLevel server)) return null;
        return AbstractMinecart.createMinecart(server, x, y, z, type, ItemStack.EMPTY, null);
    }
}
