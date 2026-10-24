package legacyconnector.v1_7_10.net.minecraft.world;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;

/** Result handle of an explosion; the modern explosion already happened when this object is returned. */
public class Explosion {
    public boolean isFlaming;
    public boolean isSmoking = true;
    public double explosionX;
    public double explosionY;
    public double explosionZ;
    public Entity exploder;
    public float explosionSize;
    public List affectedBlockPositions = new ArrayList();
    private final World mcc$world;

    public Explosion(World world, Entity exploder, double x, double y, double z, float size) {
        this.mcc$world = world;
        this.exploder = exploder;
        this.explosionX = x;
        this.explosionY = y;
        this.explosionZ = z;
        this.explosionSize = size;
    }

    public void doExplosionA() {
    }

    public void doExplosionB(boolean spawnParticles) {
    }

    public legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase getExplosivePlacedBy() {
        return exploder instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase living ? living : null;
    }
}
