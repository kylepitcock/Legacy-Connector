package legacyconnector.v1_7_10.net.minecraft.block;

import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Gravel: a falling block that drops flint one time in ten (Fortune improves the odds up to every time) and gravel otherwise. */
public class BlockGravel extends BlockFalling {
    public BlockGravel(org.kylepitcock.legacyconnector.v1_7_10.bridge.VanillaInfo info) {
        super(info);
    }

    public BlockGravel() {
        super();
    }

    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        if (mcc$vanilla != null) return super.getItemDropped(meta, random, fortune);
        if (fortune > 3) fortune = 3;
        return random.nextInt(10 - fortune * 3) == 0 ? Items.flint : Item.getItemFromBlock(this);
    }
}
