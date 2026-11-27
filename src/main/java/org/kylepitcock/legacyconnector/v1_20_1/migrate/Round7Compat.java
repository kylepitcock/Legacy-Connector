package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.20.1 members that 1.21.1 dropped or reshaped, ATM 9 gap round 7 (common side; the client side is {@link Round7ClientCompat}). Rows in {@code mc-calls.tsv}.
 */
public final class Round7Compat {
    private Round7Compat() {
    }

    /** {@code ObfuscationReflectionHelper.remapName(domain, name)}: names are Mojang names at run time, nothing to remap. */
    public static String remapName(Object domain, String name) {
        return name;
    }

    /**
     * {@code ObfuscationReflectionHelper.findMethod(clazz, name, params...)}: the static {@code PotionBrewing.addMix(Potion, Item, Potion)} (a mod's way
     * past the protected 1.20.1 method; Blueprint) is answered with the brewing facade's method, which registers the mix on the 1.21.1 builder.
     * The legacy {@code StructurePiece.createChest} overload resolves to the modern overload that accepts a loot table key.
     */
    public static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Class<?>... params) {
        if (clazz == JigsawPlacement.class && "addPieces".equals(name) && params != null && params.length == 11
                && params[0] == RandomState.class && params[1] == int.class && params[2] == boolean.class
                && params[3] == ChunkGenerator.class && params[4] == StructureTemplateManager.class
                && params[5] == LevelHeightAccessor.class && params[6] == RandomSource.class && params[7] == Registry.class
                && params[8] == PoolElementStructurePiece.class && params[9] == List.class && params[10] == VoxelShape.class) {
            try {
                // MiscCompat also declares client-only methods, which reflection would resolve on a dedicated server.
                return Round7Compat.class.getMethod("jigsawAddPieces", params);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        if (clazz == StructurePiece.class && "createChest".equals(name) && params != null && params.length == 6
                && params[0] == ServerLevelAccessor.class && params[1] == BoundingBox.class && params[2] == RandomSource.class
                && params[3] == BlockPos.class && params[4] == ResourceLocation.class && params[5] == BlockState.class) {
            try {
                java.lang.reflect.Method method = StructurePiece.class.getDeclaredMethod("createChest", ServerLevelAccessor.class, BoundingBox.class,
                        RandomSource.class, BlockPos.class, ResourceKey.class, BlockState.class);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        if (clazz == net.minecraft.world.item.alchemy.PotionBrewing.class && params.length == 3
                && params[0] == net.minecraft.world.item.alchemy.Potion.class && params[1] == net.minecraft.world.item.Item.class
                && params[2] == net.minecraft.world.item.alchemy.Potion.class) {
            try {
                return LegacyBrewing.class.getMethod("addPotionMix", net.minecraft.world.item.alchemy.Potion.class,
                        net.minecraft.world.item.Item.class, net.minecraft.world.item.alchemy.Potion.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(e);
            }
        }
        return net.neoforged.fml.util.ObfuscationReflectionHelper.findMethod(clazz, name, params);
    }

    public static void jigsawAddPieces(RandomState randomState, int maxDepth, boolean useExpansionHack,
            ChunkGenerator generator, StructureTemplateManager templates, LevelHeightAccessor heightAccessor,
            RandomSource random, Registry<StructureTemplatePool> pools, PoolElementStructurePiece piece,
            List<PoolElementStructurePiece> pieces, VoxelShape shape) {
        MiscCompat.jigsawAddPieces(randomState, maxDepth, useExpansionHack, generator, templates, heightAccessor,
                random, pools, piece, pieces, shape);
    }

    /** {@code DisplayInfo.getFrame()}: the frame type is the advancement type now. */
    public static AdvancementType displayFrame(DisplayInfo info) {
        return info.getType();
    }

    /** {@code DisplayInfo.getBackground()}: the background is an optional texture in a record accessor now (null when absent, as before). */
    public static net.minecraft.resources.ResourceLocation displayBackground(DisplayInfo info) {
        return info.getBackground().orElse(null);
    }

    /** {@code ShieldItem.getColor(stack)}: the base colour is a data component now (null without a banner pattern, as before). */
    public static DyeColor shieldColor(ItemStack stack) {
        return stack.get(DataComponents.BASE_COLOR);
    }
}
