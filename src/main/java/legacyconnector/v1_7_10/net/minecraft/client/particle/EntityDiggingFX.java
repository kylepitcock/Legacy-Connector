package legacyconnector.v1_7_10.net.minecraft.client.particle;

import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/** A chip of a block's texture (breaking and hitting a block, landing, sprinting): a quarter of the side's icon. */
public class EntityDiggingFX extends EntityFX {
    private Block field_145784_a;
    private int side;

    public EntityDiggingFX(World world, double x, double y, double z, double vx, double vy, double vz, Block block, int meta) {
        this(world, x, y, z, vx, vy, vz, block, meta, world.rand.nextInt(6));
    }

    public EntityDiggingFX(World world, double x, double y, double z, double vx, double vy, double vz, Block block, int meta, int side) {
        super(world, x, y, z, vx, vy, vz);
        this.field_145784_a = block;
        try {
            setParticleIcon(block.getIcon(side, meta));
        } catch (RuntimeException | LinkageError e) {
            // a block whose icon lookup fails on this side/meta just draws without a texture piece
        }
        this.particleGravity = block.blockParticleGravity;
        this.particleRed = this.particleGreen = this.particleBlue = 0.6F;
        this.particleScale /= 2.0F;
        this.side = side;
    }

    /** Tints the particle with the block's colour at the position (grass, leaves); grass keeps its side colour. */
    public EntityDiggingFX applyColourMultiplier(int x, int y, int z) {
        if (field_145784_a == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass && side != 1) return this;
        int color = field_145784_a.colorMultiplier(mcc$world(), x, y, z);
        particleRed *= (color >> 16 & 255) / 255.0F;
        particleGreen *= (color >> 8 & 255) / 255.0F;
        particleBlue *= (color & 255) / 255.0F;
        return this;
    }

    /** Tints the particle with the block's item colour for the metadata. */
    public EntityDiggingFX applyRenderColor(int meta) {
        if (field_145784_a == legacyconnector.v1_7_10.net.minecraft.init.Blocks.grass) return this;
        int color = field_145784_a.getRenderColor(meta);
        particleRed *= (color >> 16 & 255) / 255.0F;
        particleGreen *= (color >> 8 & 255) / 255.0F;
        particleBlue *= (color & 255) / 255.0F;
        return this;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void renderParticle(Tessellator t, float partialTicks, float rx, float rxz, float rz, float ryz, float rxy) {
        mcc$renderIconPiece(t, partialTicks, rx, rxz, rz, ryz, rxy);
    }
}
