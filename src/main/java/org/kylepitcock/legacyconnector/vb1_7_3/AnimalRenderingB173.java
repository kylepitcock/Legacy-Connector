package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Shared original-renderer dispatch; both native drawing and server callback proofs use this path. */
public final class AnimalRenderingB173 {
    private AnimalRenderingB173(){}
    public static void draw(LegacyB173Animal entity,float partial,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink){
        try(var readOnly=EntityViewsB173.readOnly()){
            var view=entity.prepareRenderView(partial);var info=RenderersB173.info(view.getClass());
            if(info==null)throw new IllegalStateException("No original renderer for "+view.getClass().getName());
            try(var scope=RendererContextB173.open(path->MobTexturesB173.resolve(info.mod(),path),sink)){
                info.renderer().render(view,0,view.standingEyeHeight,0,Mth.rotLerp(partial,entity.yRotO,entity.getYRot()),partial);
            }
        }
    }
}
