package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event;

/** Two mobs are about to make a child (cancel for none; the child can be replaced). Posted from the modern event. */
@Cancelable
public class BabyEntitySpawnEvent extends Event {
    private final EntityLiving parentA;
    private final EntityLiving parentB;
    private final EntityPlayer causedByPlayer;
    private EntityAgeable child;

    public BabyEntitySpawnEvent(EntityLiving parentA, EntityLiving parentB, EntityAgeable proposedChild) {
        this.parentA = parentA;
        this.parentB = parentB;
        EntityPlayer player = null;
        if (parentA instanceof EntityAnimal a) player = a.getLoveCause();
        if (player == null && parentB instanceof EntityAnimal b) player = b.getLoveCause();
        this.causedByPlayer = player;
        this.child = proposedChild;
    }

    public EntityLiving getParentA() {
        return parentA;
    }

    public EntityLiving getParentB() {
        return parentB;
    }

    public EntityPlayer getCausedByPlayer() {
        return causedByPlayer;
    }

    public EntityAgeable getChild() {
        return child;
    }

    public void setChild(EntityAgeable proposedChild) {
        this.child = proposedChild;
    }
}
