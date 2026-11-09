package legacyconnector.vb1_7_3.net.minecraft.client;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class MinecraftPlayerContextTest {
    private record Actor(String level,boolean alive,double distance){}
    private static Actor select(List<Actor> loaded,List<Actor> registered){
        return Minecraft.mcc$contextPlayer(loaded,()->registered,actor->actor.level.equals("overworld"),Actor::distance);
    }
    @Test void trulyEmptyWorldHasNoInventedActor(){assertNull(select(List.of(),List.of()));}
    @Test void nearestLoadedPlayerRetainsPriorityWithoutConsultingRegistry(){
        var near=new Actor("overworld",true,4);var far=new Actor("overworld",true,100);
        assertSame(near,Minecraft.mcc$contextPlayer(List.of(far,near),()->{throw new AssertionError("fallback must be lazy");},actor->true,Actor::distance));
        assertSame(near,select(List.of(near),List.of(new Actor("overworld",false,0))));
    }
    @Test void connectedDeadActorSurvivesMissingLevelMembership(){
        var dead=new Actor("overworld",false,4);assertSame(dead,select(List.of(),List.of(dead)));
        var farther=new Actor("overworld",false,16);assertSame(dead,select(List.of(),List.of(farther,dead)));
    }
    @Test void anotherDimensionCannotSupplyFallback(){assertNull(select(List.of(),List.of(new Actor("nether",false,0))));}
    @Test void LogoutDoesNotRetainPreviousFallback(){
        var actor=new Actor("overworld",false,1);assertSame(actor,select(List.of(),List.of(actor)));
        assertNull(select(List.of(),List.of()),"lookup must consult current registration, not a retained player cache");
    }
}
