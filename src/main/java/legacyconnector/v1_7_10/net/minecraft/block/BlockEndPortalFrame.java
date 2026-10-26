package legacyconnector.v1_7_10.net.minecraft.block;

/** The end portal frame: metadata bits 0-1 direction, 4 the eye of ender; RenderBlocks draws it (render type 26). */
public class BlockEndPortalFrame extends Block {
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon iconEndPortalFrameTop, iconEndPortalFrameEye;

    public BlockEndPortalFrame(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockEndPortalFrame() {
        super(legacyconnector.v1_7_10.net.minecraft.block.material.Material.rock);
    }

    @Override
    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIcon(int side, int meta) {
        if (side == 1) return iconEndPortalFrameTop != null ? iconEndPortalFrameTop : mcc$vanillaIcon("endframe_top");
        if (side == 0) return legacyconnector.v1_7_10.net.minecraft.init.Blocks.end_stone.getBlockTextureFromSide(side);
        return blockIcon != null ? blockIcon : mcc$vanillaIcon("endframe_side");
    }

    @Override
    public void registerBlockIcons(legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_side");
        iconEndPortalFrameTop = register.registerIcon(getTextureName() + "_top");
        iconEndPortalFrameEye = register.registerIcon(getTextureName() + "_eye");
    }

    public legacyconnector.v1_7_10.net.minecraft.util.IIcon getIconEndPortalFrameEye() {
        return iconEndPortalFrameEye != null ? iconEndPortalFrameEye : mcc$vanillaIcon("endframe_eye");
    }

    public static boolean isEnderEyeInserted(int meta) {
        return (meta & 4) != 0;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 26;
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 0.8125F, 1.0F);
    }
}
