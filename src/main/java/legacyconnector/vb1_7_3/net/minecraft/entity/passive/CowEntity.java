package legacyconnector.vb1_7_3.net.minecraft.entity.passive;

import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Beta animal identity for native entity views and original instanceof decisions. */
public class CowEntity extends AnimalEntity {
    public CowEntity(World world){super(world);texture="/mob/cow.png";setBoundingBoxSpacing(.9F,1.3F);}
}
