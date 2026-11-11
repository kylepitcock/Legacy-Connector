package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlayerViewLifetimeB173Test {
    @BeforeAll static void bootstrap()throws Exception {NativeStackComponentsB173Test.bootstrap();}
    private static <T>T blank(Class<T> type)throws Exception {
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)field.get(null)).allocateInstance(type));
    }
    private static void field(Class<?> owner,Object target,String name,Object value)throws Exception {
        var field=owner.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    private static Level level(boolean client)throws Exception {
        var level=blank(ServerLevel.class);field(Level.class,level,"isClientSide",client);return level;
    }
    private static Player player(Level level,boolean removed)throws Exception {
        var player=blank(ServerPlayer.class);field(Entity.class,player,"level",level);
        if(removed)field(Entity.class,player,"removalReason",Entity.RemovalReason.DISCARDED);
        return player;
    }
    @SuppressWarnings("unchecked")
    @Test void removedAndOldClientViewsAreReleasedWithoutTouchingLiveUntrackedOrServerIdentities()throws Exception {
        var field=PlayerViewB173.class.getDeclaredField("VIEWS");field.setAccessible(true);
        var cache=(Map<Player,PlayerViewB173>)field.get(null);
        var current=level(true);var old=level(true);var server=level(false);
        Player live=player(current,false),removed=player(current,true),oldPlayer=player(old,false),authority=player(server,false);
        var fixtures=new java.util.ArrayList<Player>();
        try{
            for(var player:new Player[]{live,removed,oldPlayer,authority}){
                var view=blank(PlayerViewB173.class);field(PlayerViewB173.class,view,"player",player);
                view.mcc$modern=player;view.velocityY=-.25;view.fallDistance=7;view.health=13;
                cache.put(player,view);fixtures.add(player);
            }
            var liveView=PlayerViewB173.peek(live);var serverView=PlayerViewB173.peek(authority);
            PlayerViewB173.releaseDepartedClientViews(current);
            assertNull(PlayerViewB173.peek(removed));assertNull(PlayerViewB173.peek(oldPlayer));
            assertSame(liveView,PlayerViewB173.peek(live));assertSame(serverView,PlayerViewB173.peek(authority));
            assertEquals(-.25,liveView.velocityY);assertEquals(7,liveView.fallDistance);assertEquals(13,liveView.health);
            assertEquals(0,live.tickCount);assertEquals(0,authority.tickCount);
            PlayerViewB173.releaseDepartedClientViews(null);
            assertNull(PlayerViewB173.peek(live));assertSame(serverView,PlayerViewB173.peek(authority));
        }finally{for(var player:fixtures)PlayerViewB173.release(player);}
    }
}
