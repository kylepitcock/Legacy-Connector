package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerAbilitiesB173Test {
    static class Actor extends ClientPlayerEntity {
        int heals,waterCalls,nativeHeals;boolean replace=true,recursive;Actor nested;float multiplier=1;
        Actor(){super(null);health=5;maxHealth=20;}
    }
    public static class Abilities extends PlayerBase {
        public Abilities(ClientPlayerEntity player){super(player);}
        @Override public boolean heal(int amount){
            if(!(player instanceof Actor actor))return false;
            assertSame(player,PlayerApiB173.currentPlayer());actor.heals++;
            if(actor.recursive)PlayerAbilitiesB173.heal(actor,amount,()->actor.nativeHeals++);
            if(actor.nested!=null){var other=actor.nested;actor.nested=null;PlayerAbilitiesB173.heal(other,1,()->other.nativeHeals++);assertSame(actor,PlayerApiB173.currentPlayer());}
            if(actor.replace&&!actor.recursive)actor.health+=amount;return actor.replace;
        }
        @Override public boolean isInWater(boolean baseline){
            if(!(player instanceof Actor actor))return baseline;
            actor.waterCalls++;assertSame(actor,PlayerApiB173.currentPlayer());
            assertEquals(baseline,PlayerAbilitiesB173.water(actor,baseline),"Nested native/SDK predicate uses its supplied baseline");
            return false;
        }
        @Override public float getCurrentPlayerStrVsBlock(legacyconnector.vb1_7_3.net.minecraft.block.Block block,float baseline){
            assertSame(player,PlayerApiB173.currentPlayer());return player instanceof Actor actor?baseline*actor.multiplier:baseline;
        }
    }
    private static class NativeWaterPlayer extends net.minecraft.world.entity.player.Player {
        private NativeWaterPlayer(){super(null,net.minecraft.core.BlockPos.ZERO,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Water fixture"));}
        @Override public boolean isCreative(){return false;}
        @Override public boolean isSpectator(){return false;}
        @Override public boolean isInWater(){
            assertTrue(PlayerAbilitiesB173.active(this,PlayerAbilitiesB173.Hook.WATER),"View synchronization must establish the raw-query guard before reading native water");
            return PlayerAbilitiesB173.nativeWater(this,true);
        }
    }
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();PlayerApiB173.register(Abilities.class);}
    private static NativeWaterPlayer nativePlayer(boolean added) throws Exception {
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        var player=(NativeWaterPlayer)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),NativeWaterPlayer.class);
        var addedField=net.minecraft.world.entity.Entity.class.getDeclaredField("isAddedToLevel");addedField.setAccessible(true);addedField.setBoolean(player,added);
        return player;
    }
    @Test void genuineOverrideReplacesSdkHealAndFalseResultRunsNativeBaselineOnce(){
        var actor=new Actor();actor.heal(3);assertEquals(8,actor.health);assertEquals(1,actor.heals);
        actor.replace=false;actor.heal(2);assertEquals(10,actor.health);assertEquals(2,actor.heals);
    }
    @Test void recursiveSdkNativeHealAndDifferentActorsRemainScoped(){
        var actor=new Actor();actor.recursive=true;var second=new Actor();actor.nested=second;
        PlayerAbilitiesB173.heal(actor,2,()->fail("Replacing original hook must not run outer baseline"));
        assertEquals(1,actor.heals);assertEquals(1,actor.nativeHeals);assertEquals(1,second.heals);assertEquals(6,second.health);
        assertFalse(PlayerAbilitiesB173.active(actor,PlayerAbilitiesB173.Hook.HEAL));assertNull(PlayerApiB173.currentPlayer());
    }
    @Test void typedWaterPredicateDispatchesOnceAndDoesNotReadBaselineOutsideGuard(){
        var actor=new Actor();var reads=new java.util.concurrent.atomic.AtomicInteger();
        assertFalse(PlayerAbilitiesB173.water(actor,()->{
            assertTrue(PlayerAbilitiesB173.active(actor,PlayerAbilitiesB173.Hook.WATER));reads.incrementAndGet();return true;
        }));
        assertEquals(1,reads.get());assertEquals(1,actor.waterCalls);assertFalse(PlayerAbilitiesB173.active(actor,PlayerAbilitiesB173.Hook.WATER));
    }
    @Test void viewSynchronizationReadsRawNativeWaterWithoutConstructingOrRefreshingAView() throws Exception {
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        var nativePlayer=(NativeWaterPlayer)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),NativeWaterPlayer.class);
        assertTrue(PlayerAbilitiesB173.nativeWaterBaseline(nativePlayer));assertNull(PlayerViewB173.peek(nativePlayer));
        assertFalse(PlayerAbilitiesB173.active(nativePlayer,PlayerAbilitiesB173.Hook.WATER));
    }
    @Test void queryUsesCurrentNativeModifiedMiningBaselineAndDefaultImplementationsPreserveIt(){
        var actor=new Actor();actor.multiplier=2;
        assertEquals(15,PlayerApiB173.miningStrength(actor,null,7.5F));
        var ordinary=new ClientPlayerEntity(null){};assertEquals(7.5F,PlayerApiB173.miningStrength(ordinary,null,7.5F));
        assertTrue(PlayerAbilitiesB173.water(ordinary,true));assertFalse(PlayerAbilitiesB173.water(ordinary,false));
        assertFalse(PlayerApiB173.hasAbility("unregisteredHook"));
    }
    @Test void fractionalOrUnrepresentableNativeHealingKeepsModernBaseline(){
        assertTrue(PlayerAbilitiesB173.integralHeal(4));assertFalse(PlayerAbilitiesB173.integralHeal(.5F));
        assertFalse(PlayerAbilitiesB173.integralHeal(0));assertFalse(PlayerAbilitiesB173.integralHeal(Float.NaN));
        assertFalse(PlayerAbilitiesB173.integralHeal(Float.POSITIVE_INFINITY));assertFalse(PlayerAbilitiesB173.integralHeal(2147483648F));
    }
    @Test void waterTravelMembershipUsesItsOwnNativeBaselineAndPreservesPendingActor() throws Exception {
        var nativePlayer=nativePlayer(true);var actor=new Actor();actor.mcc$modern=nativePlayer;actor.health=13;
        PlayerApiB173.withPlayer(actor,()->{
            assertFalse(PlayerAbilitiesB173.travelWaterMembership(nativePlayer,true,true));
            assertEquals(1,actor.waterCalls);assertEquals(13,actor.health);
            assertNull(PlayerViewB173.peek(nativePlayer),"Active original view is reused without refreshing pending writes");
        });
        assertNull(PlayerApiB173.currentPlayer());assertFalse(PlayerAbilitiesB173.active(nativePlayer,PlayerAbilitiesB173.Hook.WATER));
        // In particular, entering fluid may have raw Entity.isInWater=false but membership=true.
        var ordinary=new ClientPlayerEntity(null){};ordinary.mcc$modern=nativePlayer;
        PlayerApiB173.withPlayer(ordinary,()->{
            assertTrue(PlayerAbilitiesB173.travelWaterMembership(nativePlayer,true,true),"An unchanged hook must retain membership's true baseline");
            assertFalse(PlayerAbilitiesB173.travelWaterMembership(nativePlayer,true,false));
        });
    }
    @Test void otherFluidsNonPlayersAndNotYetAddedPlayersKeepNativeMembership() throws Exception {
        var nativePlayer=nativePlayer(true);var actor=new Actor();actor.mcc$modern=nativePlayer;
        PlayerApiB173.withPlayer(actor,()->{
            assertTrue(PlayerAbilitiesB173.travelWaterMembership(nativePlayer,false,true));
            assertFalse(PlayerAbilitiesB173.travelWaterMembership(nativePlayer,false,false));
        });
        assertEquals(0,actor.waterCalls);
        assertTrue(PlayerAbilitiesB173.travelWaterMembership(null,true,true));
        assertFalse(PlayerAbilitiesB173.travelWaterMembership(null,true,false));
        assertTrue(PlayerAbilitiesB173.travelWaterMembership(nativePlayer(false),true,true));
    }
    @Test void nativeTravelHasBothIndependentFluidMembershipBranches() throws Exception {
        var count=new java.util.concurrent.atomic.AtomicInteger();
        try(var stream=net.minecraft.world.entity.LivingEntity.class.getResourceAsStream("LivingEntity.class")){
            assertNotNull(stream);
            new org.objectweb.asm.ClassReader(stream).accept(new org.objectweb.asm.ClassVisitor(9<<16){
                @Override public org.objectweb.asm.MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                    if(!name.equals("travel")||!descriptor.equals("(Lnet/minecraft/world/phys/Vec3;)V"))return null;
                    return new org.objectweb.asm.MethodVisitor(9<<16){
                        @Override public void visitMethodInsn(int opcode,String owner,String method,String desc,boolean itf){
                            if(owner.equals("net/minecraft/world/entity/LivingEntity")&&method.equals("isInFluidType")&&desc.equals("(Lnet/minecraft/world/level/material/FluidState;)Z"))count.incrementAndGet();
                        }
                    };
                }
            },org.objectweb.asm.ClassReader.SKIP_DEBUG|org.objectweb.asm.ClassReader.SKIP_FRAMES);
        }
        assertEquals(2,count.get(),"Both NeoForge travel fallbacks need the scoped expression adapter");
    }
}
