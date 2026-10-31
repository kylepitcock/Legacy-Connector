package org.kylepitcock.legacyconnector.mixin;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.InsertableLinkedOpenCustomHashSet;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A stack already in a creative tab is skipped instead of failing the whole tab build (which stops the server). Every subtype item
 * of a legacy block is a {@code BlockItem} of the same modern block, so modern code that makes one variant per item of a tag from
 * {@code BlockItem.getBlock().defaultBlockState()} makes equal stacks: EvilCraft's display stand per {@code minecraft:planks} item
 * ("Itemstack 1 evilcraft:display_stand already exists in the tab's list") with Witchery's and Thaumcraft's planks.
 */
@Mixin(value = BuildCreativeModeTabContentsEvent.class, remap = false)
public abstract class CreativeTabDuplicateMixin {
    private static final Logger LEGACYCONNECTOR$LOG = LoggerFactory.getLogger("Legacy Connector");
    private static final Set<String> LEGACYCONNECTOR$REPORTED = ConcurrentHashMap.newKeySet();

    @Inject(method = "assertNewEntryDoesNotAlreadyExists", at = @At("HEAD"), cancellable = true, require = 0)
    private void legacyconnector$skipDuplicate(InsertableLinkedOpenCustomHashSet<ItemStack> entries, ItemStack stack, CallbackInfo ci) {
        if (!entries.contains(stack)) return;
        ci.cancel();
        String key = ((BuildCreativeModeTabContentsEvent) (Object) this).getTabKey().location() + " " + BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (LEGACYCONNECTOR$REPORTED.add(key)) {
            LEGACYCONNECTOR$LOG.warn("Creative tab {}: a duplicate of {} was skipped (the mod added an equal stack twice)", key.split(" ")[0], key.split(" ")[1]);
        }
    }
}
