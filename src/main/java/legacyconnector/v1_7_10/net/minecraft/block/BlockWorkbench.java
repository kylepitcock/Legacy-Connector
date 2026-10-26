package legacyconnector.v1_7_10.net.minecraft.block;

import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaMenus1710;

/** The crafting table: a wooden block with top, front and side textures (planks below) that opens a 3x3 crafting grid. */
public class BlockWorkbench extends Block {
    /** Top texture. */
    private IIcon field_150035_a;
    /** Front texture. */
    private IIcon field_150034_b;

    public BlockWorkbench(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    protected BlockWorkbench() {
        super(Material.wood);
        setCreativeTab(legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs.tabDecorations);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150035_a == null && mcc$vanilla != null) return super.getIcon(side, meta);
        if (side == 1) return field_150035_a;
        if (side == 0) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.planks.getBlockTextureFromSide(side);
        return side != 2 && side != 4 ? blockIcon : field_150034_b;
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_side");
        field_150035_a = register.registerIcon(getTextureName() + "_top");
        field_150034_b = register.registerIcon(getTextureName() + "_front");
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return true;
        VanillaMenus1710.openWorkbench(player, world, x, y, z);
        return true;
    }
}
