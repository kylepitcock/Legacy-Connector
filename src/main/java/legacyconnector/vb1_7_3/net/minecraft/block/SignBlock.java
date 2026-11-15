package legacyconnector.vb1_7_3.net.minecraft.block;

import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.SignBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;

/** Beta {@code BlockSign}: a standing or wall sign whose tile entity is {@code blockEntityClazz}. */
public class SignBlock extends BlockWithEntity {
    public Class<?> blockEntityClazz;
    public boolean standing;
    protected SignBlock(int id, Class<?> blockEntityClazz, boolean standing) {
        super(id, Material.WOOD);
        this.blockEntityClazz = blockEntityClazz;
        this.standing = standing;
    }
    @Override public BlockEntity createBlockEntity() {
        try { return (BlockEntity) blockEntityClazz.getDeclaredConstructor().newInstance(); }
        catch (ReflectiveOperationException | RuntimeException e) { return new SignBlockEntity(); }
    }
    /** Drawn by the sign tile entity renderer, not as a block. */
    @Override public int getRenderType() { return -1; }
    @Override public boolean isOpaque() { return false; }
    @Override public boolean isFullCube() { return false; }
    @Override public void updateBoundingBox(legacyconnector.vb1_7_3.net.minecraft.world.BlockView view, int x, int y, int z) {
        if (standing) { setBoundingBox(.25F, 0, .25F, .75F, 1, .75F); return; }
        float lo = .28125F, hi = .78125F, d = .125F;
        switch (view.getBlockMeta(x, y, z)) {
            case 2 -> setBoundingBox(0, lo, 1 - d, 1, hi, 1);
            case 3 -> setBoundingBox(0, lo, 0, 1, hi, d);
            case 4 -> setBoundingBox(1 - d, lo, 0, 1, hi, 1);
            case 5 -> setBoundingBox(0, lo, 0, d, hi, 1);
            default -> {}
        }
    }
}
