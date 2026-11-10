package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.ClientPlayerEntity;
import legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerAPI;
import legacyconnector.vb1_7_3.net.minecraft.src.PlayerBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

class PlayerApiB173Test {
    private Set<Class<?>> registry;
    private List<Class<?>> previous;
    private static final List<String> CALLS=new ArrayList<>();
    private static final class Player extends ClientPlayerEntity {Player(){super(null);}}
    public static class First extends PlayerBase {
        public First(ClientPlayerEntity player){super(player);assertSame(player,PlayerApiB173.currentPlayer());CALLS.add("first constructor");}
        @Override public void playerInit(){CALLS.add("first init");}
        @Override public boolean onLivingUpdate(){assertSame(player,PlayerApiB173.currentPlayer());CALLS.add("first tick");return true;}
        @Override public boolean writeEntityToNBT(NbtCompound tag){tag.putInt("owned",37);return false;}
    }
    public static class Second extends PlayerBase {
        public Second(ClientPlayerEntity player){super(player);CALLS.add("second constructor");}
        @Override public void playerInit(){CALLS.add("second init");}
        @Override public boolean onLivingUpdate(){CALLS.add("second tick");return false;}
    }
    public static class Broken extends PlayerBase {
        static int attempts;
        public Broken(ClientPlayerEntity player){super(player);attempts++;throw new IllegalStateException("fixture constructor failure");}
    }
    public static class BrokenTick extends PlayerBase {
        public BrokenTick(ClientPlayerEntity player){super(player);}
        @Override public boolean onLivingUpdate(){throw new IllegalStateException("fixture tick failure");}
    }
    @BeforeEach @SuppressWarnings("unchecked") void isolateRegistry()throws Exception{
        var field=PlayerApiB173.class.getDeclaredField("REGISTERED");field.setAccessible(true);
        registry=(Set<Class<?>>)field.get(null);previous=List.copyOf(registry);registry.clear();CALLS.clear();Broken.attempts=0;
    }
    @AfterEach void restoreRegistry(){registry.clear();registry.addAll(previous);}
    @Test void originalRegistrationConstructsEachBaseOncePerPlayerInOrder(){
        PlayerAPI.RegisterPlayerBase(First.class);PlayerAPI.RegisterPlayerBase(Second.class);PlayerAPI.RegisterPlayerBase(First.class);
        var player=new Player();PlayerApiB173.ensure(player);PlayerApiB173.ensure(player);
        assertEquals(List.of("first constructor","first init","second constructor","second init"),CALLS);
        assertSame(PlayerApiB173.bases(player).getFirst(),PlayerAPI.getPlayerBase(player,First.class));
        assertSame(player,PlayerAPI.getPlayerBase(player,First.class).player);
        assertTrue(PlayerApiB173.tick(player));
        assertEquals(List.of("first tick","second tick"),CALLS.subList(4,6));
        var other=new Player();assertNotSame(PlayerAPI.getPlayerBase(player,First.class),PlayerAPI.getPlayerBase(other,First.class));
        assertNull(PlayerApiB173.currentPlayer());
    }
    @Test void failedBaseAndFailedTickDoNotPreventOtherOriginalCallbacks(){
        PlayerAPI.RegisterPlayerBase(Broken.class);PlayerAPI.RegisterPlayerBase(BrokenTick.class);PlayerAPI.RegisterPlayerBase(Second.class);
        var player=new Player();PlayerApiB173.ensure(player);PlayerApiB173.ensure(player);
        assertEquals(1,Broken.attempts);assertNull(PlayerAPI.getPlayerBase(player,Broken.class));
        long failures=PlayerApiB173.callbackFailures(player);assertDoesNotThrow(()->PlayerApiB173.tick(player));
        assertEquals(failures+1,PlayerApiB173.callbackFailures(player));assertTrue(CALLS.contains("second tick"));assertNull(PlayerApiB173.currentPlayer());
    }
    @Test void originalSuppliedNbtAndNestedActorScopesRemainOwned(){
        PlayerAPI.RegisterPlayerBase(First.class);var player=new Player();var other=new Player();var tag=new NbtCompound();
        PlayerApiB173.write(player,tag);assertEquals(37,tag.getInt("owned"));
        PlayerApiB173.withPlayer(player,()->{
            assertSame(player,PlayerApiB173.currentPlayer());
            assertThrows(IllegalStateException.class,()->PlayerApiB173.withPlayer(other,(Runnable)()->{throw new IllegalStateException("fixture");}));
            assertSame(player,PlayerApiB173.currentPlayer());
        });assertNull(PlayerApiB173.currentPlayer());
    }
    @Test void spStyleAdditionalFilesAreSeparatedByNativePlayerIdentity(@TempDir Path root)throws Exception{
        UUID first=UUID.randomUUID(),second=UUID.randomUUID();
        Path a=PlayerApiB173.playerStorageDirectory(root,first).toPath(),b=PlayerApiB173.playerStorageDirectory(root,second).toPath();
        java.nio.file.Files.writeString(a.resolve("mod-data.dat"),"first actor");
        assertNotEquals(a,b);assertTrue(a.startsWith(root));assertTrue(b.startsWith(root));
        assertFalse(java.nio.file.Files.exists(b.resolve("mod-data.dat")));
        assertEquals(a,PlayerApiB173.playerStorageDirectory(root,first).toPath());
        assertFalse(java.nio.file.Files.exists(root.resolve("mod-data.dat")));
    }
    @Test void existingContextLookupNeverCreatesAPlayerAndRestoresNestedActors(){
        var player=new Player();var other=new Player();
        assertNull(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
        try(var outer=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(null,player)){
            assertSame(player,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
            player.health=7;
            try(var inner=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(null,other)){
                assertSame(other,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
            }
            assertSame(player,legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
            assertEquals(7,player.health);
        }
        assertNull(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$currentPlayerContext());
    }
    @Test void unnamedRegistrationCannotClaimUnrelatedCallerStorage(){
        Assumptions.assumeFalse(First.class.getModule().isNamed());
        PlayerAPI.RegisterPlayerBase(First.class);
        assertFalse(PlayerApiB173.registeredCaller());
    }
}
