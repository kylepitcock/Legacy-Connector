package legacyconnector.v1_12_2.net.minecraftforge.event.entity.living;

import legacyconnector.v1_12_2.net.minecraft.entity.passive.EntityAnimal;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable;

/** A player is taming an animal (cancel to make the attempt fail). */
@Cancelable
public class AnimalTameEvent extends LivingEvent {
    private final EntityAnimal animal;
    private final EntityPlayer tamer;

    public AnimalTameEvent(EntityAnimal animal, EntityPlayer tamer) {
        super(animal);
        this.animal = animal;
        this.tamer = tamer;
    }

    public EntityAnimal getAnimal() {
        return animal;
    }

    public EntityPlayer getTamer() {
        return tamer;
    }
}
