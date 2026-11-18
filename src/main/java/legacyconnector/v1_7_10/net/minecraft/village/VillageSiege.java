package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.Iterator;
import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.entity.EnumCreatureType;
import legacyconnector.v1_7_10.net.minecraft.entity.monster.EntityZombie;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.util.MathHelper;
import legacyconnector.v1_7_10.net.minecraft.util.Vec3;
import legacyconnector.v1_7_10.net.minecraft.world.SpawnerAnimals;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's zombie siege: at the first tick after midnight one night in ten, a village with at least 10 doors and 20 villagers
 * (and a player near it) is attacked by 20 zombies that appear around its edge, two ticks apart. Whoever drives it calls
 * {@link #tick} once per world tick; the modern game runs its own siege for the vanilla villages, so the shim does not tick this one.
 */
public class VillageSiege {
    private final World worldObj;
    private boolean field_75535_b;
    private int field_75536_c = -1;
    private int field_75533_d;
    private int field_75534_e;
    private Village theVillage;
    private int field_75532_g;
    private int field_75538_h;
    private int field_75539_i;

    public VillageSiege(World world) {
        this.worldObj = world;
    }

    public void tick() {
        if (worldObj.isDaytime()) {
            field_75536_c = 0;
        } else if (field_75536_c != 2) {
            if (field_75536_c == 0) {
                float angle = worldObj.getCelestialAngle(0.0F);
                if ((double) angle < 0.5D || (double) angle > 0.501D) return;
                field_75536_c = worldObj.rand.nextInt(10) == 0 ? 1 : 2;
                field_75535_b = false;
                if (field_75536_c == 2) return;
            }
            if (field_75536_c != -1) {
                if (!field_75535_b) {
                    if (!func_75529_b()) return;
                    field_75535_b = true;
                }
                if (field_75534_e > 0) {
                    --field_75534_e;
                } else {
                    field_75534_e = 2;
                    if (field_75533_d > 0) {
                        spawnZombie();
                        --field_75533_d;
                    } else {
                        field_75536_c = 2;
                    }
                }
            }
        }
    }

    /** Picks the village to besiege and the spot at its edge the zombies appear around. */
    private boolean func_75529_b() {
        List players = World.mcc$get$playerEntities(worldObj);
        Iterator it = players.iterator();
        while (it.hasNext()) {
            EntityPlayer player = (EntityPlayer) it.next();
            theVillage = World.mcc$get$villageCollectionObj(worldObj).findNearestVillage(
                    (int) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posX(player),
                    (int) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posY(player),
                    (int) legacyconnector.v1_7_10.net.minecraft.entity.Entity.mcc$get$posZ(player), 1);
            if (theVillage != null && theVillage.getNumVillageDoors() >= 10 && theVillage.getTicksSinceLastDoorAdding() >= 20
                    && theVillage.getNumVillagers() >= 20) {
                ChunkCoordinates center = theVillage.getCenter();
                float radius = (float) theVillage.getVillageRadius();
                boolean overlaps = false;
                for (int i = 0; i < 10; i++) {
                    field_75532_g = center.posX + (int) ((double) (MathHelper.cos(worldObj.rand.nextFloat() * (float) Math.PI * 2.0F) * radius) * 0.9D);
                    field_75538_h = center.posY;
                    field_75539_i = center.posZ + (int) ((double) (MathHelper.sin(worldObj.rand.nextFloat() * (float) Math.PI * 2.0F) * radius) * 0.9D);
                    overlaps = false;
                    for (Object o : World.mcc$get$villageCollectionObj(worldObj).getVillageList()) {
                        Village other = (Village) o;
                        if (other != theVillage && other.isInRange(field_75532_g, field_75538_h, field_75539_i)) {
                            overlaps = true;
                            break;
                        }
                    }
                    if (!overlaps) break;
                }
                if (overlaps) return false;
                if (func_75527_a(field_75532_g, field_75538_h, field_75539_i) != null) {
                    field_75534_e = 0;
                    field_75533_d = 20;
                    return true;
                }
            }
        }
        return false;
    }

    private boolean spawnZombie() {
        Vec3 spot = func_75527_a(field_75532_g, field_75538_h, field_75539_i);
        if (spot == null) return false;
        EntityZombie zombie = new EntityZombie(worldObj);
        zombie.onSpawnWithEgg(null);
        zombie.setVillager(false);
        zombie.setLocationAndAngles(spot.xCoord, spot.yCoord, spot.zCoord, worldObj.rand.nextFloat() * 360.0F, 0.0F);
        worldObj.spawnEntityInWorld(zombie);
        ChunkCoordinates center = theVillage.getCenter();
        zombie.setHomeArea(center.posX, center.posY, center.posZ, theVillage.getVillageRadius());
        return true;
    }

    /** A spot within 8 blocks of the point, inside the village, where a monster may spawn (ten tries). */
    private Vec3 func_75527_a(int x, int y, int z) {
        for (int i = 0; i < 10; i++) {
            int sx = x + worldObj.rand.nextInt(16) - 8;
            int sy = y + worldObj.rand.nextInt(6) - 3;
            int sz = z + worldObj.rand.nextInt(16) - 8;
            if (theVillage.isInRange(sx, sy, sz) && SpawnerAnimals.canCreatureTypeSpawnAtLocation(EnumCreatureType.monster, worldObj, sx, sy, sz)) {
                return Vec3.createVectorHelper((double) sx, (double) sy, (double) sz);
            }
        }
        return null;
    }
}
