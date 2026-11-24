package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

/** Failure-class round, client side: members of 1.20.1 that 1.21.1 moved or dropped (rows in mc-calls.tsv). */
public final class Round16ClientCompat {
    private Round16ClientCompat() {}

    /** {@code SpriteLoader.loadSprite(id, resource)} (static in 1.20.1): the sprite loader of 1.21.1 is an instance of {@link SpriteResourceLoader}. */
    public static SpriteContents loadSprite(ResourceLocation id, Resource resource) {
        return SpriteResourceLoader.create(List.of(AnimationMetadataSection.SERIALIZER)).loadSprite(id, resource);
    }
}
