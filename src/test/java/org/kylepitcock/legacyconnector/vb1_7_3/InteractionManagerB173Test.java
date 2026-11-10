package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InteractionManagerB173Test {
    static class CallbackMinecraft extends Minecraft {}
    static class NativePlayer extends net.minecraft.world.entity.player.Player {
        boolean creative,spectator;double reach;
        private NativePlayer(){super(null,net.minecraft.core.BlockPos.ZERO,0,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Fixture"));}
        @Override public boolean isCreative(){return creative;}
        @Override public boolean isSpectator(){return spectator;}
        @Override public double blockInteractionRange(){return reach;}
    }
    @BeforeAll static void bootstrap() throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.List.of(),java.util.Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static NativePlayer nativePlayer(double reach) throws Exception {
        // Query-only host: allocate without constructing an unrelated native world or connection.
        var unsafe=Class.forName("sun.misc.Unsafe");var field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
        var value=(NativePlayer)unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),NativePlayer.class);value.reach=reach;return value;
    }
    private static void actor(Minecraft minecraft,NativePlayer nativePlayer){
        var actor=new ClientPlayerEntity(null){};actor.mcc$modern=nativePlayer;Minecraft.mcc$set$player(minecraft,actor);
    }
    @Test void newCallbackMinecraftAlwaysHasControllerWithOrdinaryBetaFallbacks(){
        var minecraft=new CallbackMinecraft();assertNotNull(minecraft.interactionManager);
        assertTrue(minecraft.interactionManager.canBeRendered());assertEquals(4,minecraft.interactionManager.getReachDistance());
    }
    @Test void hudVisibilityAndReachFollowNativeActorGameModeAndAttribute() throws Exception {
        var minecraft=new CallbackMinecraft();var nativePlayer=nativePlayer(7.25);actor(minecraft,nativePlayer);
        assertTrue(minecraft.interactionManager.canBeRendered());assertEquals(7.25F,minecraft.interactionManager.getReachDistance());
        nativePlayer.creative=true;assertFalse(minecraft.interactionManager.canBeRendered());
        nativePlayer.creative=false;nativePlayer.spectator=true;assertFalse(minecraft.interactionManager.canBeRendered());
    }
    @Test void retainedControllerUsesCurrentCallbackActorWithoutBorrowingAnotherSession() throws Exception {
        var first=new CallbackMinecraft();var second=new CallbackMinecraft();actor(first,nativePlayer(4.5));
        var other=nativePlayer(8);other.creative=true;actor(second,other);var retained=first.interactionManager;
        assertTrue(retained.canBeRendered());assertEquals(4.5F,retained.getReachDistance());
        Minecraft.mcc$scope(second,()->{assertFalse(retained.canBeRendered());assertEquals(8,retained.getReachDistance());});
        assertTrue(retained.canBeRendered());assertEquals(4.5F,retained.getReachDistance());
    }
}
