package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_20_1.LegacyItemStackNbt1201;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Compatibility surface for 1.20.1 mods and KubeJS 6 scripts which expect
 * ItemStack.hasNBT() and the JavaBean property ItemStack.nbt.
 *
 * <p>On Rhino-backed scripts, hasNBT() remains a normal method call while
 * getNbt()/setNbt() provide the legacy {@code stack.nbt} property. Native
 * 1.21.1 KubeJS APIs are not changed.</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackLegacyNbtMixin {
    @Unique
    public boolean hasNBT() {
        return LegacyItemStackNbt1201.hasNBT((ItemStack) (Object) this);
    }

    @Unique
    public CompoundTag getNbt() {
        return LegacyItemStackNbt1201.getNbt((ItemStack) (Object) this);
    }

    @Unique
    public void setNbt(CompoundTag nbt) {
        LegacyItemStackNbt1201.setNbt((ItemStack) (Object) this, nbt);
    }
}
