package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.model.ModelPart;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.EntityModel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Named original-mod fixtures; no production selection depends on these class names. */
public final class ModelSelfTestB173 {
    private ModelSelfTestB173() {}
    public static void register(IEventBus game){
        if(Boolean.getBoolean("legacyconnector.entitySelfTest"))game.addListener((ServerStartedEvent event)->run());
    }
    private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("B1.7.3 MODEL TEST: "+detail);}
    private static void run(){
        for(var entity:ContentB173.entities()){
            String model;int expected;
            switch(entity.type().getSimpleName()){
                case "CREEPSEntityHippo" -> {model="CREEPSModelHippo";expected=16;}
                case "EntityBunny" -> {model="ModelBunny";expected=11;}
                default -> {continue;}
            }
            try{
                String name=entity.type().getPackageName()+"."+model;
                EntityModel original=(EntityModel)Class.forName(name,true,entity.type().getClassLoader()).getConstructor().newInstance();
                List<ModelPart> parts=new ArrayList<>();
                for(var field:original.getClass().getFields())if(ModelPart.class.isAssignableFrom(field.getType()))parts.add((ModelPart)field.get(original));
                require(parts.size()==expected,model+" original part count "+parts.size());
                require(parts.stream().allMatch(p->p!=null&&p.mcc$cuboids().size()==1),model+" original cuboids");
                var idle=new ArrayList<ModelGeometryB173.Draw>();var moving=new ArrayList<ModelGeometryB173.Draw>();
                ModelGeometryB173.capture(idle::add,()->original.render(0,0,0,0,0,.0625F));
                ModelGeometryB173.capture(moving::add,()->original.render(1,.8F,2,30,15,.0625F));
                require(idle.size()==expected&&moving.size()==expected,model+" original render emitted every part");
                boolean changed=false;
                for(int i=0;i<idle.size();i++){
                    changed|=!idle.get(i).transform().equals(moving.get(i).transform(),.00001F);
                    var box=moving.get(i).cuboids().getFirst();
                    var faces=ModelGeometryB173.vertices(box);require(faces.size()==6,model+" six textured faces");
                    for(var face:faces)for(var v:face)require(Float.isFinite(v.x())&&Float.isFinite(v.y())&&Float.isFinite(v.z())&&Float.isFinite(v.u())&&Float.isFinite(v.v()),model+" finite geometry and UVs");
                }
                require(changed,model+" original animation changes transforms");
                org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173ModelTest").info("B1.7.3 MODEL TEST: original {} constructor, {} cuboids, textured faces, animation and draw capture passed (headless model foundation; no custom renderer/AI integration)",model,expected);
            }catch(ReflectiveOperationException exception){throw new IllegalStateException("B1.7.3 MODEL TEST: original "+model+" failed",exception);}
        }
    }
}
