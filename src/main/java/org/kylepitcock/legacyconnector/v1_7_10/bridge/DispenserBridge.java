package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import legacyconnector.v1_7_10.net.minecraft.dispenser.IBehaviorDispenseItem;
import legacyconnector.v1_7_10.net.minecraft.dispenser.IBlockSource;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.world.level.block.DispenserBlock;

/** Legacy dispenser behaviours ({@code BlockDispenser.dispenseBehaviorRegistry.putObject}) become modern ones. */
public final class DispenserBridge {
    private DispenserBridge() {}

    public static void register(Object key, Object behavior) {
        if (!(key instanceof Item item) || !(behavior instanceof IBehaviorDispenseItem legacy)) return;
        DispenseItemBehavior modern = (source, stack) -> {
            ItemStack legacyStack = ItemStack.mcc$wrap(stack);
            ItemStack rest = legacy.dispense(new Source(source), legacyStack);
            return rest == null ? net.minecraft.world.item.ItemStack.EMPTY : ItemStack.mcc$toModern(rest);
        };
        if (item.mcc$vanilla != null && item.mcc$vanilla.modern() instanceof net.minecraft.world.item.Item vanilla) {
            DispenserBlock.registerBehavior(vanilla, modern);
        }
        for (var variant : item.mcc$variants.values()) DispenserBlock.registerBehavior(variant, modern);
    }

    /** The legacy view of the dispenser a behaviour runs for. */
    private record Source(BlockSource source) implements IBlockSource {
        @Override
        public double getX() {
            return source.pos().getX() + 0.5D;
        }

        @Override
        public double getY() {
            return source.pos().getY() + 0.5D;
        }

        @Override
        public double getZ() {
            return source.pos().getZ() + 0.5D;
        }

        @Override
        public int getXInt() {
            return source.pos().getX();
        }

        @Override
        public int getYInt() {
            return source.pos().getY();
        }

        @Override
        public int getZInt() {
            return source.pos().getZ();
        }

        @Override
        public int getBlockMetadata() {
            var state = source.state();
            return state.hasProperty(DispenserBlock.FACING) ? state.getValue(DispenserBlock.FACING).get3DDataValue() : BlockBridge.legacyMeta(state);
        }

        @Override
        public TileEntity getBlockTileEntity() {
            return getWorld().getTileEntity(getXInt(), getYInt(), getZInt());
        }

        @Override
        public World getWorld() {
            return World.mcc$wrap(source.level());
        }
    }
}
