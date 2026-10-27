package legacyconnector.v1_12_2.net.minecraftforge.client.event;

import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;

/** One first-person hand (and its item) is about to be drawn; cancelling hides it (handlers draw their own). */
@legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Cancelable
public class RenderSpecificHandEvent extends legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler.Event {
    private final EnumHand hand;
    private final float partialTicks;
    private final float interpolatedPitch;
    private final float swingProgress;
    private final float equipProgress;
    private final ItemStack stack;

    public RenderSpecificHandEvent(EnumHand hand, float partialTicks, float interpolatedPitch, float swingProgress, float equipProgress, ItemStack stack) {
        this.hand = hand;
        this.partialTicks = partialTicks;
        this.interpolatedPitch = interpolatedPitch;
        this.swingProgress = swingProgress;
        this.equipProgress = equipProgress;
        this.stack = stack;
    }

    public EnumHand getHand() {
        return hand;
    }

    public float getPartialTicks() {
        return partialTicks;
    }

    public float getInterpolatedPitch() {
        return interpolatedPitch;
    }

    public float getSwingProgress() {
        return swingProgress;
    }

    public float getEquipProgress() {
        return equipProgress;
    }

    public ItemStack getItemStack() {
        return stack;
    }
}
