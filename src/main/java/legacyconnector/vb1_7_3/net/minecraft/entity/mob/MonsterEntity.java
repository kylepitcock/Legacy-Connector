package legacyconnector.vb1_7_3.net.minecraft.entity.mob;

import legacyconnector.vb1_7_3.net.minecraft.entity.PathAwareEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Typed hostile view preserves Beta instanceof and target writes; custom hostile hosting remains gated. */
public abstract class MonsterEntity extends PathAwareEntity {
    public MonsterEntity(World world){super(world);}
}
