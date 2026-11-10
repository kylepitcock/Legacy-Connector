package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import legacyconnector.vb1_7_3.net.minecraft.item.ItemStack;
import legacyconnector.vb1_7_3.net.minecraft.src.IReach;
import legacyconnector.vb1_7_3.net.minecraft.src.IReachBlock;
import legacyconnector.vb1_7_3.net.minecraft.src.IReachEntity;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReachB173Test {
    @BeforeAll static void bootstrap(){net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();}
    private static IReach matching(int id,float value,AtomicInteger calls){return new IReach(){
        public boolean reachItemMatches(ItemStack stack){calls.incrementAndGet();return stack!=null&&stack.itemId==id;}
        public float getReach(ItemStack stack){return value;}
    };}
    private static AttributeInstance attribute(){return new AttributeInstance(Holder.direct(new RangedAttribute("test.reach",4.5,0,1024)),instance->{});}
    @Test void oldContractUsesHeldItemPredicateAndFirstRegistrationForBothChannels(){
        var registry=new ReachB173.Registry();var first=new AtomicInteger();var second=new AtomicInteger();
        registry.add(matching(257,10,first),ReachB173.Kind.BOTH);registry.add(matching(257,20,second),ReachB173.Kind.BOTH);
        var value=registry.resolve(new ItemStack(257,1,0));assertEquals(10F,value.block());assertEquals(10F,value.entity());
        assertEquals(1,first.get());assertEquals(0,second.get());
        var empty=registry.resolve(null);assertNull(empty.block());assertNull(empty.entity());
    }
    @Test void oldAndChannelRegistrationsShareOrderWithoutRunningOtherChannelQueries(){
        var registry=new ReachB173.Registry();var entityCalls=new AtomicInteger();var oldCalls=new AtomicInteger();
        registry.add(new IReachEntity(){public boolean reachEntityItemMatches(ItemStack stack){entityCalls.incrementAndGet();return true;}public float getReachEntity(ItemStack stack){return 7;}},ReachB173.Kind.ENTITY);
        registry.add(new IReachBlock(){public boolean reachBlockItemMatches(ItemStack stack){return true;}public float getReachBlock(ItemStack stack){return 8;}},ReachB173.Kind.BLOCK);
        registry.add(matching(257,10,oldCalls),ReachB173.Kind.BOTH);
        assertEquals(8F,registry.query(null,true));assertEquals(0,entityCalls.get());assertEquals(0,oldCalls.get());
        var desired=registry.resolve(new ItemStack(257,1,0));assertEquals(8F,desired.block());assertEquals(7F,desired.entity());assertEquals(0,oldCalls.get());
    }
    @Test void ownedContributionPreservesForeignBaseAdditionsAndMultipliersAndReleasesCleanly(){
        var attribute=attribute();attribute.setBaseValue(5.5);
        var id=ResourceLocation.fromNamespaceAndPath("othermod","reach");
        var foreign=new AttributeModifier(id,2,AttributeModifier.Operation.ADD_VALUE);attribute.addTransientModifier(foreign);
        var scale=new AttributeModifier(ResourceLocation.fromNamespaceAndPath("othermod","scale"),.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);attribute.addTransientModifier(scale);
        ReachB173.apply(attribute,10F,4.5);assertEquals((5.5+2+5.5)*1.5,attribute.getValue(),1E-9);
        var owned=attribute.getModifier(ReachB173.MODIFIER);ReachB173.apply(attribute,10F,4.5);assertSame(owned,attribute.getModifier(ReachB173.MODIFIER));
        ReachB173.apply(attribute,null,4.5);assertEquals((5.5+2)*1.5,attribute.getValue(),1E-9);
        assertEquals(5.5,attribute.getBaseValue());assertSame(foreign,attribute.getModifier(id));assertSame(scale,attribute.getModifier(scale.id()));
    }
    @Test void noHandlerAndInvalidValuesNeverInstallReducedBetaDefaults(){
        var attribute=attribute();ReachB173.apply(attribute,null,4.5);assertEquals(4.5,attribute.getValue());
        ReachB173.apply(attribute,10F,4.5);ReachB173.apply(attribute,Float.NaN,4.5);assertFalse(attribute.hasModifier(ReachB173.MODIFIER));assertEquals(4.5,attribute.getValue());
        ReachB173.apply(attribute,-1F,4.5);assertFalse(attribute.hasModifier(ReachB173.MODIFIER));
    }
}
