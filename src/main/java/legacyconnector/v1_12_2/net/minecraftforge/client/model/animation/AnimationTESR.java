package legacyconnector.v1_12_2.net.minecraftforge.client.model.animation;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity;
import legacyconnector.v1_12_2.net.minecraftforge.common.animation.Event;
import legacyconnector.v1_12_2.net.minecraftforge.common.animation.IEventHandler;

/**
 * Forge 1.12.2 renderer of blocks animated by an animation state machine. Legacy blocks keep their model in the chunk mesh (their
 * static pose; the 1.12 animated render type is not used), so this renderer adds nothing: drawing the model again here would draw it
 * twice. Subclasses still receive {@link #handleEvents} calls they make themselves.
 */
public class AnimationTESR<T extends TileEntity> extends FastTESR<T> implements IEventHandler<T> {
    public AnimationTESR() {
    }

    @Override
    public void renderTileEntityFast(T te, double x, double y, double z, float partialTick, int breakStage, float partial, BufferBuilder renderer) {
    }

    @Override
    public void handleEvents(T te, float time, Iterable<Event> pastEvents) {
    }
}
