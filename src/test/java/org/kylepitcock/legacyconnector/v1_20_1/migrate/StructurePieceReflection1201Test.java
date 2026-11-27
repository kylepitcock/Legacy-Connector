package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.compat.jdk.ReflectionCompat;

public class StructurePieceReflection1201Test {
    @Test
    @SuppressWarnings("deprecation") // The access override can be checked without a live StructurePiece receiver.
    void legacyChestLookupFindsAccessibleModernOverload() throws NoSuchMethodException {
        Method method = Round7Compat.findMethod(StructurePiece.class, "createChest", ServerLevelAccessor.class,
                BoundingBox.class, RandomSource.class, BlockPos.class, ResourceLocation.class, BlockState.class);

        assertEquals(StructurePiece.class.getDeclaredMethod("createChest", ServerLevelAccessor.class,
                BoundingBox.class, RandomSource.class, BlockPos.class, ResourceKey.class, BlockState.class), method);
        assertTrue(method.isAccessible());
    }

    @Test
    void legacyJigsawLookupFindsStaticCompatibilityBridge() throws NoSuchMethodException {
        Class<?>[] params = {RandomState.class, int.class, boolean.class, ChunkGenerator.class, StructureTemplateManager.class,
                LevelHeightAccessor.class, RandomSource.class, Registry.class, PoolElementStructurePiece.class, List.class, VoxelShape.class};

        Method method = Round7Compat.findMethod(JigsawPlacement.class, "addPieces", params);

        assertEquals(Round7Compat.class.getMethod("jigsawAddPieces", params), method);
        assertTrue(method.canAccess(null));
    }

    @Test
    void invokeAdaptsLootTableIdsWithoutMutatingTheArgumentArray() throws ReflectiveOperationException {
        Method method = StructurePieceReflection1201Test.class.getMethod("lootTable", ResourceKey.class);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("test", "chests/example");
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, id);
        Object[] arguments = {id};

        assertEquals(key, ReflectionCompat.invoke(method, null, arguments));
        assertSame(id, arguments[0]);
        assertSame(key, ReflectionCompat.invoke(method, null, key));
    }

    @Test
    void invokePreservesOtherRegistryKeysAndNullArgumentArrays() throws ReflectiveOperationException {
        Method dimension = StructurePieceReflection1201Test.class.getMethod("dimension", ResourceKey.class);
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionCompat.invoke(dimension, null, Level.OVERWORLD.location()));
        assertSame(Level.OVERWORLD, ReflectionCompat.invoke(dimension, null, Level.OVERWORLD));

        Method noArguments = StructurePieceReflection1201Test.class.getMethod("noArguments");
        assertEquals("ok", ReflectionCompat.invoke(noArguments, null, (Object[]) null));
    }

    public static ResourceKey<LootTable> lootTable(ResourceKey<LootTable> key) {
        return key;
    }

    public static ResourceKey<Level> dimension(ResourceKey<Level> key) {
        return key;
    }

    public static String noArguments() {
        return "ok";
    }
}