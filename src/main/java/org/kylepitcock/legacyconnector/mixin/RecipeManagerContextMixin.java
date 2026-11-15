package org.kylepitcock.legacyconnector.mixin;

import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import com.google.gson.JsonElement;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.MigrateContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Recipes of 1.20.1 mods (custom serializers) read enchantments, potions and other data-driven entries through the Forge registries while the recipe
 * manager parses, before any server exists: the registries of this reload are remembered for them (see {@link MigrateContext}).
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerContextMixin {
    @Shadow @Final private HolderLookup.Provider registries;

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"), require = 0)
    private void legacyconnector$rememberRegistries(Map<ResourceLocation, JsonElement> object, ResourceManager manager, ProfilerFiller profiler, CallbackInfo ci) {
        MigrateContext.remember(registries);
    }
}
