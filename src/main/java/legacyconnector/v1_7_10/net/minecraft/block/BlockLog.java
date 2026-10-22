package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/** Vanilla 1.7.10 log base: wood pillar; subclasses fill the side/top icon arrays in registerBlockIcons. */
public abstract class BlockLog extends BlockRotatedPillar {
    protected IIcon[] field_150167_a;
    protected IIcon[] field_150166_b;

    public BlockLog(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockLog() {
        super(Material.wood);
        this.setHardness(2.0F);
        this.setStepSound(soundTypeWood);
    }

    @Override
    protected IIcon getSideIcon(int variant) {
        return field_150167_a == null || field_150167_a.length == 0 ? blockIcon : field_150167_a[variant % field_150167_a.length];
    }

    @Override
    protected IIcon getTopIcon(int variant) {
        return field_150166_b == null || field_150166_b.length == 0 ? field_150164_N : field_150166_b[variant % field_150166_b.length];
    }

    public static int func_150165_c(int meta) {
        return meta & 3;
    }

    @Override
    public boolean isWood(IBlockAccess world, int x, int y, int z) {
        return true;
    }

    public boolean canSustainLeaves(IBlockAccess world, int x, int y, int z) {
        return true;
    }
}
