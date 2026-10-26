package legacyconnector.v1_7_10.net.minecraft.block;

/** The brewing stand: three bottle arms around a rod on a base plate; RenderBlocks draws it (render type 25). */
public class BlockBrewingStand extends BlockContainer {
    public java.util.Random rand = new java.util.Random();
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon iconBrewingStandBase;

    public BlockBrewingStand(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockBrewingStand() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.iron);
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        super.registerBlockIcons(register);
        iconBrewingStandBase = register.registerIcon(getTextureName() + "_base");
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        return blockIcon != null ? blockIcon : mcc$vanillaIcon("brewing_stand");
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIconBrewingStandBase() {
        return iconBrewingStandBase != null ? iconBrewingStandBase : mcc$vanillaIcon("brewing_stand_base");
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 25;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.125F, 1.0F);
    }
}
