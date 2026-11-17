package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.block.material.MapColor;
import legacyconnector.v1_7_10.net.minecraft.block.material.Material;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Glowstone: drops 2 to 4 dust (more with Fortune, at most 4) instead of itself. */
public class BlockGlowstone extends Block {
    public BlockGlowstone(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockGlowstone(Material material) {
        super(material);
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        if (mcc$vanilla != null) return super.quantityDroppedWithBonus(fortune, random);
        return Math.max(1, Math.min(4, quantityDropped(random) + random.nextInt(fortune + 1)));
    }

    @Override
    public int quantityDropped(Random random) {
        return mcc$vanilla != null ? super.quantityDropped(random) : 2 + random.nextInt(3);
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return mcc$vanilla != null ? super.getItemDropped(meta, random, fortune) : Items.glowstone_dust;
    }

    @Override
    public MapColor getMapColor(int meta) {
        return MapColor.sandColor;
    }
}
