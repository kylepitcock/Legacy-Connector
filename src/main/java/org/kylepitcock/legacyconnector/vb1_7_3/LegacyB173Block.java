package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The modern block of a b1.7.3 mod block. The legacy 4-bit metadata is one property {@link #META} (as for 1.12.2); hardness, resistance,
 * light, sound, opacity and collision come from the legacy block when it is registered, its bounds (set in its constructor) are the shape,
 * and its drops are asked from the legacy block ({@code getDroppedItemId/Count/Meta}), as b1.7.3's {@code dropStacks} did.
 */
public class LegacyB173Block extends net.minecraft.world.level.block.Block {
    public static final IntegerProperty META = IntegerProperty.create("meta", 0, 15);
    private static final Random RANDOM = new Random();

    private final Block legacy;
    private final VoxelShape shape;
    private int failures;

    public LegacyB173Block(Block legacy, Properties properties) {
        super(properties);
        this.legacy = legacy;
        this.shape = shapeOf(legacy);
        registerDefaultState(stateDefinition.any().setValue(META, 0));
    }

    private static VoxelShape shapeOf(Block legacy) {
        double x0 = legacy.minX, y0 = legacy.minY, z0 = legacy.minZ, x1 = legacy.maxX, y1 = legacy.maxY, z1 = legacy.maxZ;
        if (x0 <= 0 && y0 <= 0 && z0 <= 0 && x1 >= 1 && y1 >= 1 && z1 >= 1) return Shapes.block();
        if (x1 <= x0 || y1 <= y0 || z1 <= z0) return Shapes.block();
        return Shapes.box(Math.max(0, x0), Math.max(0, y0), Math.max(0, z0), Math.min(1, x1), Math.min(1, y1), Math.min(1, z1));
    }

    public Block legacy() {
        return legacy;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(META);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int meta = state.getValue(META);
        List<ItemStack> out = new ArrayList<>(1);
        try {
            int count = legacy.getDroppedItemCount(RANDOM);
            for (int i = 0; i < count; i++) {
                int id = legacy.getDroppedItemId(meta, RANDOM);
                if (id <= 0) continue;
                ItemStack stack = StacksB173.toModern(id, 1, legacy.getDroppedItemMeta(meta));
                if (stack.isEmpty()) continue;
                boolean merged = false;
                for (ItemStack s : out) {
                    if (ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                        s.grow(1);
                        merged = true;
                        break;
                    }
                }
                if (!merged) out.add(stack);
            }
        } catch (RuntimeException | LinkageError e) {
            if (failures++ < 3) {
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Drops of {} failed: {}", legacy, e.toString());
            }
        }
        return out;
    }
}
