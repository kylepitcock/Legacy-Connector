package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import legacyconnector.vb1_7_3.net.minecraft.sound.BlockSoundGroup;

/**
 * Beta 1.7.3 {@code BlockFire}: its two tables by block id, in their original order (mods reach them as {@code BlockFire}'s fields 0 and 1
 * through ModLoader's reflection): how much a block encourages fire and how easily it catches. The bridge gives the mod blocks' values to
 * modern fire. Fire itself is the modern block.
 */
public class FireBlock extends Block {
    public int[] burnChances = new int[256];
    public int[] spreadChances = new int[256];

    protected FireBlock(int id, int textureId) {
        super(id, textureId, Material.FIRE);
        setTickRandomly(true);
    }

    @Override
    public void mcc$initVanilla(int id, int textureId, Material material, float hardness, BlockSoundGroup sound, String key) {
        super.mcc$initVanilla(id, textureId, material == null ? Material.FIRE : material, hardness, sound, key);
        burnChances = new int[256];
        spreadChances = new int[256];
        init();
    }

    /** The vanilla values: planks, logs, leaves, bookshelves, TNT, tall grass and wool. */
    @Override
    public void init() {
        registerFlammableBlock(5, 5, 20);
        registerFlammableBlock(17, 5, 5);
        registerFlammableBlock(18, 30, 60);
        registerFlammableBlock(47, 30, 20);
        registerFlammableBlock(46, 15, 100);
        registerFlammableBlock(31, 60, 100);
        registerFlammableBlock(35, 30, 60);
    }

    private void registerFlammableBlock(int id, int burn, int spread) {
        burnChances[id] = burn;
        spreadChances[id] = spread;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public boolean isFullCube() {
        return false;
    }

    @Override
    public int getRenderType() {
        return 3;
    }
}
