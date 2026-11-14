package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;

/**
 * Helpers of 1.20.1 calls that 1.21.1 dropped or changed (ATM 9 gap round three, 2026-11-13): the client model baking signatures, the Architectury 13
 * networking calls and fluid stacks, the layer-event renderer lookup. Architectury is no compile dependency: its objects arrive as {@code Object}.
 */
public final class Round3Compat {
    private Round3Compat() {}

    // ----- client model and render API

    /** {@code EntityRenderersEvent.AddLayers.getRenderer(type)}: 1.20.1 promised a {@code LivingEntityRenderer}; 1.21.1 returns a generic {@code EntityRenderer}. */
    public static LivingEntityRenderer<?, ?> addLayersRenderer(EntityRenderersEvent.AddLayers event, EntityType<?> type) {
        EntityRenderer<?> renderer = event.getRenderer(type);
        return renderer instanceof LivingEntityRenderer<?, ?> living ? living : null;
    }

    /** The animation section of a sprite as a resource metadata (the 1.20.1 {@code SpriteContents} constructor took the section itself). */
    public static ResourceMetadata spriteMeta(AnimationMetadataSection animation) {
        if (animation == null) return ResourceMetadata.EMPTY;
        return new ResourceMetadata() {
            @Override
            @SuppressWarnings("unchecked")
            public <T> Optional<T> getSection(MetadataSectionSerializer<T> serializer) {
                return serializer == AnimationMetadataSection.SERIALIZER ? Optional.of((T) animation) : Optional.empty();
            }
        };
    }

    /** A sprite that only carries the contents of one frame: 1.20.1's item element helpers took the {@code SpriteContents}, 1.21.1's take the atlas sprite. */
    private static final class ContentsSprite extends TextureAtlasSprite {
        ContentsSprite(SpriteContents contents) {
            super(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"), contents, contents.width(), contents.height(), 0, 0);
        }
    }

    /** {@code UnbakedGeometryHelper.createUnbakedItemElements(layer, contents)}. */
    public static List<BlockElement> itemElements(int layer, SpriteContents contents) {
        return UnbakedGeometryHelper.createUnbakedItemElements(layer, new ContentsSprite(contents));
    }

    /** {@code UnbakedGeometryHelper.createUnbakedItemMaskElements(layer, contents)}. */
    public static List<BlockElement> itemMaskElements(int layer, SpriteContents contents) {
        return UnbakedGeometryHelper.createUnbakedItemMaskElements(layer, new ContentsSprite(contents));
    }

    // ----- Architectury 13

    /**
     * The buffer of an Architectury packet: 13 hands out and takes a {@code RegistryFriendlyByteBuf}; a 1.20.1 mod writes plain data into a
     * {@code FriendlyByteBuf}, whose bytes are moved into one.
     */
    public static RegistryFriendlyByteBuf registryBuffer(FriendlyByteBuf buf) {
        if (buf instanceof RegistryFriendlyByteBuf r) return r;
        RegistryFriendlyByteBuf out = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(Math.max(16, buf.readableBytes())), MigrateContext.registries());
        out.writeBytes(buf, buf.readerIndex(), buf.readableBytes());
        return out;
    }

    /** {@code FluidStack.setTag(tag)}: the NBT is the custom data component of the stack now. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void fluidSetTag(Object stack, CompoundTag tag) {
        try {
            if (tag == null || tag.isEmpty()) {
                stack.getClass().getMethod("remove", net.minecraft.core.component.DataComponentType.class).invoke(stack, DataComponents.CUSTOM_DATA);
            } else {
                stack.getClass().getMethod("set", net.minecraft.core.component.DataComponentType.class, Object.class).invoke(stack, DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Architectury FluidStack components are not available", e);
        }
    }

    /** {@code FluidStack.write(CompoundTag)}: writes the stack into the tag and returns it (13 writes into a fresh tag and returns that). */
    public static CompoundTag fluidWrite(Object stack, CompoundTag into) {
        try {
            var m = stack.getClass().getMethod("write", net.minecraft.core.HolderLookup.Provider.class, Tag.class);
            Tag written = (Tag) m.invoke(stack, MigrateContext.provider(), new CompoundTag());
            if (into == null) into = new CompoundTag();
            if (written instanceof CompoundTag c) into.merge(c);
            return into;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Architectury FluidStack.write is not available", e);
        }
    }
}
