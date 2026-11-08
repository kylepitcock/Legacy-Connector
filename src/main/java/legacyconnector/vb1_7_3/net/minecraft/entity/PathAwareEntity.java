package legacyconnector.vb1_7_3.net.minecraft.entity;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Compatibility alias for the creature superclass; the canonical Beta API name is MobEntity. */
public abstract class PathAwareEntity extends MobEntity {
    public PathAwareEntity(World world){super(world);}
}
