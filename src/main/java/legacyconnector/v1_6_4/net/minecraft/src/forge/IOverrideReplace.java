package legacyconnector.v1_6_4.net.minecraft.src.forge;

public interface IOverrideReplace {
    boolean canReplaceBlock(legacyconnector.v1_7_10.net.minecraft.world.World world, int x, int y, int z, int blockId);

    boolean getReplacedSuccess();
}
