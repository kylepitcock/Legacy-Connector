package org.kylepitcock.legacyconnector.mixin.compat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PACK COMPAT, performance (HBM's Nuclear Tech Mod port, mod id "hbm"; not legacy code; remove when HBM fixes it).
 * {@code RecipesCommon.TagStack.getDisplayStacks()} lists the items of a tag by walking the WHOLE item registry,
 * building an ItemStack per item and testing it against the tag, once per TagStack instance (every recipe builds its own
 * instances, and an empty tag is never cached, so it rescans on every call). Its JEI plugin calls it for thousands of
 * ingredients while "Registering recipes", so the cost is (tag stacks) x (registry size); a pack with tens of thousands of
 * items (plus the legacy ones this mod adds) spent about a minute there. The walk now starts from the tag's own members
 * (an O(tag size) lookup) in registry order, so the loop body and the result stay identical.
 * {@code compat.hbm.fastTagStacks=false} turns it off.
 */
@Pseudo
@Mixin(targets = "com.hbm.inventory.RecipesCommon$TagStack", remap = false)
public abstract class HbmTagStackDisplayMixin {
    @Shadow(remap = false)
    public TagKey<Item> tag;

    @Redirect(method = "getDisplayStacks()Ljava/util/List;", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/DefaultedRegistry;iterator()Ljava/util/Iterator;", remap = false))
    private Iterator<Item> legacyconnector$tagMembersOnly(DefaultedRegistry<Item> registry) {
        if (!org.kylepitcock.legacyconnector.core.LegacyConfig.hbmFastTagStacks()) return registry.iterator();
        List<Item> members = new ArrayList<>();
        for (Holder<Item> holder : registry.getTagOrEmpty(this.tag)) members.add(holder.value());
        members.sort(Comparator.comparingInt(registry::getId));
        return members.iterator();
    }
}
