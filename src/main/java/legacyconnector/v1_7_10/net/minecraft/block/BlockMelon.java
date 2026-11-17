package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.IIconRegister;
import legacyconnector.v1_7_10.net.minecraft.creativetab.CreativeTabs;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;

/** A melon (1.7.10 {@code BlockMelon}): gourd material, hardness 1; drops 3 to 7 slices (more with Fortune, at most 9). */
public class BlockMelon extends Block {
    private IIcon field_150201_a;

    public BlockMelon(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockMelon() {
        super(Material.gourd);
        setHardness(1.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(CreativeTabs.tabBlock);
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        if (field_150201_a == null) return super.getIcon(side, meta);
        return side != 1 && side != 0 ? blockIcon : field_150201_a;
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return mcc$vanilla != null ? super.getItemDropped(meta, random, fortune) : Items.melon;
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : 3 + random.nextInt(5);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (mcc$vanilla != null) return super.quantityDroppedWithBonus(fortune, random);
        return Math.min(9, quantityDropped(random) + random.nextInt(1 + fortune));
    }

    @Override
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(getTextureName() + "_side");
        field_150201_a = register.registerIcon(getTextureName() + "_top");
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.foliageColor;
    }
}
