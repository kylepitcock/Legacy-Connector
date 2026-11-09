package legacyconnector.v1_12_2.net.minecraft.entity;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExistingEntityAttributes1122Test {
    @BeforeAll static void bootstrap()throws Exception{
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    /** Same virtual constructor attribute defaults as a vanilla 1.12 animal, without requiring a world. */
    private static final class Defaults extends EntityLivingBase {
        static int initialized;
        Defaults(){super((World)null);}
        @Override public void applyEntityAttributes(){
            initialized++;super.applyEntityAttributes();
            getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10);
            getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(.2);
        }
    }
    private static net.minecraft.world.entity.animal.Cow nativeCow()throws Exception{
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var cow=(net.minecraft.world.entity.animal.Cow)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(net.minecraft.world.entity.animal.Cow.class);
        var attributes=net.minecraft.world.entity.LivingEntity.class.getDeclaredField("attributes");attributes.setAccessible(true);
        attributes.set(cow,new AttributeMap(AttributeSupplier.builder()
                .add(Attributes.MAX_HEALTH,10).add(Attributes.MOVEMENT_SPEED,.2).build()));
        return cow;
    }
    @SuppressWarnings("unchecked")
    @Test void existingNativeViewKeepsForeignAttributesAndLaterExplicitWritesStayLive()throws Exception{
        var cow=nativeCow();var max=cow.getAttribute(Attributes.MAX_HEALTH);var speed=cow.getAttribute(Attributes.MOVEMENT_SPEED);
        max.setBaseValue(100);speed.setBaseValue(.37);
        var foreign=new AttributeModifier(ResourceLocation.parse("fixture:health"),5,AttributeModifier.Operation.ADD_VALUE);max.addTransientModifier(foreign);
        var field=org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.class.getDeclaredField("VIEW_BINDING");field.setAccessible(true);
        var binding=(ThreadLocal<net.minecraft.world.entity.Entity>)field.get(null);var previous=binding.get();Defaults.initialized=0;
        try{
            binding.set(cow);var view=new Defaults();
            assertSame(cow,view.mcc$entity);assertTrue(view.mcc$existingNativeView());assertEquals(0,Defaults.initialized);
            assertEquals(100,max.getBaseValue());assertEquals(105,max.getValue());assertEquals(.37,speed.getBaseValue());assertEquals(foreign,max.getModifier(foreign.id()));
            view.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(140);
            assertEquals(140,max.getBaseValue());assertEquals(145,max.getValue());assertEquals(foreign,max.getModifier(foreign.id()));
        }finally{if(previous==null)binding.remove();else binding.set(previous);}
    }
    @Test void newlyConstructedLegacyEntityStillAppliesItsVirtualAttributeDefaults(){
        Defaults.initialized=0;var entity=new Defaults();assertFalse(entity.mcc$existingNativeView());assertEquals(1,Defaults.initialized);
        assertEquals(10,entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue());
        assertEquals(.2,entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue());
    }
}
