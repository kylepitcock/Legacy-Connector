package org.kylepitcock.legacyconnector.v1_12_2.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Supplier;
import legacyconnector.v1_12_2.net.minecraftforge.fluids.Fluid;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.FluidBridge1122;

/**
 * The client side of 1.12.2 fluids (see {@link FluidBridge1122}): each fluid type draws the legacy fluid's still and flowing textures (added
 * to the block atlas, as Forge 1.12 stitched every fluid's textures) tinted with its colour; a fluid placed in the world renders in its block's
 * legacy render layer; the generated buckets use NeoForge's fluid container model (the vanilla bucket with the fluid's texture and colour).
 */
public final class Fluids1122 {
    private Fluids1122() {}

    public static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        FluidBridge1122.fluidTypes().forEach((type, legacy) -> event.registerFluidType(new Extensions(legacy), type));
    }

    /** The render layer of each fluid placed in the world: its block's legacy layer (translucent by default, as Forge's fluid blocks). */
    public static void setRenderLayers() {
        for (Object[] placed : FluidBridge1122.placedFluids()) {
            Fluid legacy = (Fluid) placed[0];
            RenderType type = RenderType.translucent();
            try {
                var block = legacy == null ? null : legacy.getBlock();
                var layer = block == null ? null : block.getRenderLayer();
                if (layer != null) {
                    type = switch (layer.name()) {
                        case "SOLID" -> RenderType.solid();
                        case "CUTOUT" -> RenderType.cutout();
                        case "CUTOUT_MIPPED" -> RenderType.cutoutMipped();
                        default -> RenderType.translucent();
                    };
                }
            } catch (RuntimeException | LinkageError e) {
                // translucent
            }
            ItemBlockRenderTypes.setRenderLayer((net.minecraft.world.level.material.Fluid) placed[1], type);
            ItemBlockRenderTypes.setRenderLayer((net.minecraft.world.level.material.Fluid) placed[2], type);
        }
    }

    /** The bucket tint: the fluid's colour on the fluid layer. */
    public static void onItemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event) {
        var buckets = FluidBridge1122.generatedBuckets().keySet().toArray(new net.minecraft.world.item.Item[0]);
        if (buckets.length > 0) event.register(new net.neoforged.neoforge.client.model.DynamicFluidContainerModel.Colors(), buckets);
    }

    /** Adds the fluids' textures to the block atlas and writes the generated buckets' item models. */
    public static void contribute(Map<String, Supplier<byte[]>> out) {
        FluidBridge1122.fluidTypes().values().forEach(legacy -> {
            for (var texture : new Object[] {legacy.getStill(), legacy.getFlowing(), legacy.getOverlay()}) {
                if (texture != null) Sprites1122.register(texture.toString());
            }
        });
        FluidBridge1122.generatedBuckets().forEach((item, fluid) -> {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
            com.google.gson.JsonObject model = new com.google.gson.JsonObject();
            model.addProperty("parent", "neoforge:item/bucket");
            model.addProperty("loader", "neoforge:fluid_container");
            model.addProperty("fluid", fluidId.toString());
            byte[] bytes = model.toString().getBytes(StandardCharsets.UTF_8);
            out.put("assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json", () -> bytes);
        });
    }

    private record Extensions(Fluid legacy) implements IClientFluidTypeExtensions {
        private static ResourceLocation sprite(Object texture) {
            return texture == null ? null : Sprites1122.modernName(texture.toString());
        }

        /** A colour without alpha (0xRRGGBB) is opaque: an alpha of 0 would hide the fluid. */
        private static int opaque(int color) {
            return (color >>> 24) == 0 ? color | 0xFF000000 : color;
        }

        @Override
        public ResourceLocation getStillTexture() {
            ResourceLocation still = sprite(legacy.getStill());
            return still != null ? still : ResourceLocation.withDefaultNamespace("block/water_still");
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            ResourceLocation flowing = sprite(legacy.getFlowing());
            return flowing != null ? flowing : getStillTexture();
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return sprite(legacy.getOverlay());
        }

        @Override
        public int getTintColor() {
            try {
                return opaque(legacy.getColor());
            } catch (RuntimeException | LinkageError e) {
                return 0xFFFFFFFF;
            }
        }

        @Override
        public int getTintColor(FluidStack stack) {
            try {
                return opaque(legacy.getColor(new legacyconnector.v1_12_2.net.minecraftforge.fluids.FluidStack(legacy, Math.max(1, stack.getAmount()))));
            } catch (RuntimeException | LinkageError e) {
                return getTintColor();
            }
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return getTintColor();
        }
    }
}
