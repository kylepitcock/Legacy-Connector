package legacyconnector.v1_12_2.net.minecraftforge.fml.client.registry;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.Render;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.entity.RenderManager;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;

/** Forge 1.12's entity renderer factory: called with the render manager when the renderers are made ({@link RenderingRegistry}). */
public interface IRenderFactory<T extends Entity> {
    Render<? super T> createRenderFor(RenderManager manager);
}
