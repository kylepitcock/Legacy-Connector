package legacyconnector.v1_12_2.net.minecraft.block.properties;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import java.util.Collection;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

public class PropertyDirection extends PropertyEnum<EnumFacing> {
    protected PropertyDirection(String name, Collection<EnumFacing> values) {
        super(name, EnumFacing.class, values);
    }

    public static PropertyDirection create(String name) {
        return create(name, com.google.common.base.Predicates.alwaysTrue());
    }

    public static PropertyDirection create(String name, Predicate<EnumFacing> filter) {
        return create(name, Lists.newArrayList(com.google.common.collect.Collections2.filter(Lists.newArrayList(EnumFacing.values()), filter)));
    }

    public static PropertyDirection create(String name, Collection<EnumFacing> values) {
        return new PropertyDirection(name, values);
    }

    /** Vanilla classes whose FACING holds the four horizontal sides only (BlockHorizontal and the blocks built like it in 1.12). */
    private static final java.util.Set<String> HORIZONTAL = java.util.Set.of("BlockHorizontal", "BlockChest", "BlockFurnace", "BlockEnderChest", "BlockBed",
            "BlockDoor", "BlockFenceGate", "BlockLadder", "BlockTrapDoor", "BlockStairs", "BlockCocoa", "BlockTripWireHook", "BlockWallSign", "BlockAnvil",
            "BlockPumpkin", "BlockRedstoneDiode", "BlockRedstoneRepeater", "BlockRedstoneComparator", "BlockBanner", "BlockBanner$BlockBannerHanging",
            "BlockEndPortalFrame", "BlockGlazedTerracotta");

    /** Vanilla FACING constants with the sides the vanilla property allows (torches never face down, hoppers never up). */
    public static PropertyDirection mcc$synthDefault(String key) {
        String name = key.substring(key.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        String owner = key.substring(0, Math.max(0, key.lastIndexOf('.')));
        owner = owner.substring(owner.lastIndexOf('.') + 1);
        if (HORIZONTAL.contains(owner)) return create(name, EnumFacing.Plane.HORIZONTAL);
        if (owner.equals("BlockTorch") || owner.equals("BlockRedstoneTorch")) return create(name, f -> f != EnumFacing.DOWN);
        if (owner.equals("BlockHopper")) return create(name, f -> f != EnumFacing.UP);
        return create(name);
    }
}
