package legacyconnector.v1_7_10.net.minecraft.entity.monster;

import legacyconnector.v1_7_10.net.minecraft.world.World;

/** 1.7.10 cave spider: a small spider. */
public class EntityCaveSpider extends EntitySpider {
    public EntityCaveSpider(World world) {
        super(world);
        setSize(0.7F, 0.5F);
    }
}
