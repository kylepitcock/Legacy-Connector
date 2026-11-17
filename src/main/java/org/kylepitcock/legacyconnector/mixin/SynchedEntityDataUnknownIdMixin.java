package org.kylepitcock.legacyconnector.mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A mod entity that extends a vanilla entity class without an entity type of its own (Integrated Dynamics' targetted item entity extends {@code ItemEntity} and
 * is spawned with the vanilla item type) is a plain vanilla entity on the client, while the server sends the extra data ids of the subclass. 1.20.1 ignored ids
 * the receiving entity does not have; 1.21.1 indexes an array and the client crashes ({@code ArrayIndexOutOfBoundsException} in {@code assignValues}).
 */
@Mixin(SynchedEntityData.class)
public abstract class SynchedEntityDataUnknownIdMixin {
    @Shadow
    @Final
    private SynchedEntityData.DataItem<?>[] itemsById;

    @ModifyVariable(method = "assignValues", at = @At("HEAD"), argsOnly = true, require = 0)
    private List<SynchedEntityData.DataValue<?>> legacyconnector$dropUnknownIds(List<SynchedEntityData.DataValue<?>> entries) {
        boolean ok = true;
        for (SynchedEntityData.DataValue<?> v : entries) {
            if (v.id() < 0 || v.id() >= itemsById.length || itemsById[v.id()] == null) {
                ok = false;
                break;
            }
        }
        if (ok) return entries;
        List<SynchedEntityData.DataValue<?>> known = new ArrayList<>(entries.size());
        for (SynchedEntityData.DataValue<?> v : entries) {
            if (v.id() >= 0 && v.id() < itemsById.length && itemsById[v.id()] != null) known.add(v);
        }
        return known;
    }
}
