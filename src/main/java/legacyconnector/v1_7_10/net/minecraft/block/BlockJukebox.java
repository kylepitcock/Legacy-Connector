package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaOps;

/** The jukebox (metadata 1 while it holds a disc). The vanilla jukebox keeps its disc in the modern jukebox block entity. */
public class BlockJukebox extends BlockContainer {
    public BlockJukebox(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockJukebox() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.wood);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    /** {@code func_149926_b}: puts a disc into the jukebox at the position (a copy of the stack). */
    public void func_149926_b(World world, int x, int y, int z, ItemStack record) {
        mcc$insert(world, x, y, z, record);
    }

    /** {@code func_149925_e}: ejects the jukebox's disc onto the ground. */
    public void func_149925_e(World world, int x, int y, int z) {
        var level = VanillaOps.server(world);
        if (level != null && level.getBlockEntity(VanillaOps.pos(x, y, z)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
            jukebox.popOutTheItem();
        }
    }

    /** Puts a copy of the disc into the modern jukebox at the position (its block state then shows a disc). */
    public static void mcc$insert(World world, int x, int y, int z, ItemStack record) {
        if (world == null || world.isRemote || record == null) return;
        var level = VanillaOps.server(world);
        if (level == null) return;
        if (level.getBlockEntity(VanillaOps.pos(x, y, z)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
            var copy = record.mcc$modern().copyWithCount(1);
            jukebox.setTheItem(copy);
        }
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        var level = VanillaOps.live(world);
        if (level != null && level.getBlockEntity(VanillaOps.pos(x, y, z)) instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
            return jukebox.getComparatorOutput();
        }
        return 0;
    }
}
