package org.kylepitcock.legacyconnector.v1_12_2.bridge;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.ModelLoader;
import org.kylepitcock.legacyconnector.core.synth.Synth;

/**
 * 1.12.2 client registrations whose types only the synthesizer provides (custom model loaders, entity render factories, resource
 * metadata section serializers) and item model variants: kept in order, as Forge kept them, for the client support to come.
 */
public final class ClientRegistrations1122 {
    private static final List<Object> MODEL_LOADERS = new ArrayList<>();
    /** Entity class -> its IRenderFactory (or, through the deprecated overload, its Render). */
    private static final Map<Class<?>, Object> ENTITY_RENDERERS = new LinkedHashMap<>();
    /** Metadata section serializer -> the section class it reads. */
    private static final Map<Object, Class<?>> METADATA_SERIALIZERS = new LinkedHashMap<>();

    private ClientRegistrations1122() {}

    public static void register() {
        Synth.addStaticMethod("net.minecraftforge.client.model.ModelLoaderRegistry.registerLoader", args -> {
            if (args.length > 0 && args[0] != null) {
                synchronized (MODEL_LOADERS) {
                    if (!MODEL_LOADERS.contains(args[0])) MODEL_LOADERS.add(args[0]);
                }
            }
            return null;
        });
        Synth.addStaticMethod("net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler", args -> {
            if (args.length > 1 && args[0] instanceof Class<?> c && args[1] != null) {
                synchronized (ENTITY_RENDERERS) {
                    ENTITY_RENDERERS.put(c, args[1]);
                }
            }
            return null;
        });
        Synth.addInstanceMethod("net.minecraft.client.resources.data.MetadataSerializer.registerMetadataSectionType", (self, args) -> {
            if (args.length > 1 && args[0] != null && args[1] instanceof Class<?> c) {
                synchronized (METADATA_SERIALIZERS) {
                    METADATA_SERIALIZERS.put(args[0], c);
                }
            }
            return null;
        });
        Synth.StaticMethod variants = args -> {
            if (args.length > 1 && args[0] instanceof Item item && args[1] instanceof ResourceLocation[] names) ModelLoader.registerItemVariants(item, names);
            return null;
        };
        Synth.addStaticMethod("net.minecraft.client.renderer.block.model.ModelBakery.registerItemVariants", variants);
        // a tile entity renderer: drawn by the modern renderer of the hosting block entity (Client1122)
        Synth.addStaticMethod("net.minecraftforge.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer", args -> {
            if (args.length > 1 && args[0] instanceof Class<?> c && args[1] instanceof legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer<?> r
                    && legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity.class.isAssignableFrom(c)) {
                @SuppressWarnings("unchecked")
                Class<? extends legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity> te = (Class<? extends legacyconnector.v1_12_2.net.minecraft.tileentity.TileEntity>) c;
                legacyconnector.v1_12_2.net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance.mcc$register(te, r);
            }
            return null;
        });
    }

    /** ModelLoaderRegistry.registerLoader of the shim's class. */
    public static void addModelLoader(Object loader) {
        synchronized (MODEL_LOADERS) {
            if (!MODEL_LOADERS.contains(loader)) MODEL_LOADERS.add(loader);
        }
    }

    public static List<Object> modelLoaders() {
        synchronized (MODEL_LOADERS) {
            return List.copyOf(MODEL_LOADERS);
        }
    }

    public static Map<Class<?>, Object> entityRenderers() {
        synchronized (ENTITY_RENDERERS) {
            return new LinkedHashMap<>(ENTITY_RENDERERS);
        }
    }

    /** How many entity renderers are registered (cheap; RenderManager creates new ones when it grows). */
    public static int entityRendererCount() {
        synchronized (ENTITY_RENDERERS) {
            return ENTITY_RENDERERS.size();
        }
    }

    public static Map<Object, Class<?>> metadataSerializers() {
        synchronized (METADATA_SERIALIZERS) {
            return new LinkedHashMap<>(METADATA_SERIALIZERS);
        }
    }
}
