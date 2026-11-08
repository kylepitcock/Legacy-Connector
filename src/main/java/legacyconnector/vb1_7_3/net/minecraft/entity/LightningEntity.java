package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Vanilla weather entity identity, spawned through the native lightning engine. */
public class LightningEntity extends Entity {
    public LightningEntity(World world,double x,double y,double z){super(world);setPosition(x,y,z);}
}
