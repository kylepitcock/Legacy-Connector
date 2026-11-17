package org.kylepitcock.legacyconnector.mixin;

import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.AnimalFood1201;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.20.1 mods add crops to the food of animals by writing the static food fields ({@code Chicken.FOOD_ITEMS = ...}); 1.21.1 reads item tags. The layer
 * remembers what a mod wrote ({@link AnimalFood1201}) and {@code isFood} answers from it. A no-op while no mod wrote a food set.
 */
@Mixin({Chicken.class, Pig.class, Cat.class, Ocelot.class, Strider.class, AbstractHorse.class})
public abstract class AnimalFoodHookMixin {
    @Inject(method = "isFood(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), cancellable = true, require = 0)
    private void legacyconnector$writtenFood(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        boolean vanilla = cir.getReturnValueZ();
        boolean answer = AnimalFood1201.isFood(this, stack, vanilla);
        if (answer != vanilla) cir.setReturnValue(answer);
    }
}
