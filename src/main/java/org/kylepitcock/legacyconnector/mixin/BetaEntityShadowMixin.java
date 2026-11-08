package org.kylepitcock.legacyconnector.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.kylepitcock.legacyconnector.vb1_7_3.client.ShadowOffsetB173;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderDispatcher.class)
public abstract class BetaEntityShadowMixin {
    @WrapOperation(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/entity/Entity;FFLnet/minecraft/world/level/LevelReader;F)V"))
    private void legacyconnector$betaShadow(PoseStack pose,MultiBufferSource buffers,Entity entity,float weight,float partial,LevelReader level,float radius,Operation<Void> original){
        ShadowOffsetB173.render(entity,pose,()->original.call(pose,buffers,entity,weight,partial,level,radius));
    }
    @ModifyExpressionValue(method="renderShadow",at=@At(value="INVOKE",target="Lnet/minecraft/util/Mth;lerp(DDD)D",ordinal=1))
    private static double legacyconnector$betaShadowSource(double y){return ShadowOffsetB173.sourceY(y);}
}
