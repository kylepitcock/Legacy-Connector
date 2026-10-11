package mccompat.v1_7_10.net.minecraft.block;

import mccompat.v1_7_10.net.minecraft.block.material.Material;
import mccompat.v1_7_10.net.minecraft.util.IIcon;
import mccompat.v1_7_10.net.minecraft.world.World;

/**
 * Vanilla 1.7.10 pillar (logs, hay, quartz pillars): metadata bits 2-3 are the axis (0 = Y, 4 = X, 8 = Z), the
 * low bits the variant. Faces along the axis use the top icon.
 */
public abstract class BlockRotatedPillar extends Block {
    protected IIcon field_150164_N;

    public BlockRotatedPillar(org.kylepitcock.mccompat.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockRotatedPillar(Material material) {
        super(material);
    }

    @Override
    public int getRenderType() {
        return 31;
    }

    @Override
    public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
        int variant = meta & 3;
        int axis = switch (side) {
            case 2, 3 -> 8;
            case 4, 5 -> 4;
            default -> 0;
        };
        return variant | axis;
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        int axis = meta & 12;
        int variant = meta & 3;
        boolean top = axis == 0 && (side == 0 || side == 1) || axis == 4 && (side == 4 || side == 5) || axis == 8 && (side == 2 || side == 3);
        return top ? getTopIcon(variant) : getSideIcon(variant);
    }

    protected abstract IIcon getSideIcon(int variant);

    protected IIcon getTopIcon(int variant) {
        return field_150164_N;
    }

    @Override
    public int damageDropped(int meta) {
        return meta & 3;
    }

    public int func_150162_k(int meta) {
        return meta & 3;
    }
}
