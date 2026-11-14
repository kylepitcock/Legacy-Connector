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
    @Override public boolean isOpaque() { return false; }
    @Override public boolean isFullCube() { return false; }
}
