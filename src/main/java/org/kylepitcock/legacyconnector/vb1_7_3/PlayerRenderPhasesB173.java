package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;

/** Records an original render around its native base, without re-entering native renderer events. */
public final class PlayerRenderPhasesB173 {
    public record Plan(boolean nativeBase,List<Runnable> before,List<Runnable> after) {
        public void beforeBase(){before.forEach(Runnable::run);}
        public void afterBase(){after.forEach(Runnable::run);}
    }
    private static final class Recording {
        boolean base;
        final List<Runnable> before=new ArrayList<>(),after=new ArrayList<>();
    }
    private static final ThreadLocal<Recording> CURRENT=new ThreadLocal<>();
    private PlayerRenderPhasesB173(){}
    public static boolean active(){return CURRENT.get()!=null;}
    public static boolean nativeBase(){
        var recording=CURRENT.get();if(recording==null)return false;
        if(recording.base)throw new UnsupportedOperationException("An original player renderer requested its native base twice");
        // Applying a persistent pose in Pre would leak it if a later mod cancels the event.
        if(RendererContextB173.active()&&!RendererContextB173.matrix().equals(new Matrix4f()))
            throw new UnsupportedOperationException("Original player base transform cannot cross native render events");
        recording.base=true;return true;
    }
    public static void emit(Runnable command){
        var recording=CURRENT.get();if(recording==null)throw new IllegalStateException("No player render recording");
        (recording.base?recording.after:recording.before).add(command);
    }
    public static Plan capture(Runnable callback){
        var previous=CURRENT.get();var recording=new Recording();CURRENT.set(recording);
        try{callback.run();return new Plan(recording.base,List.copyOf(recording.before),List.copyOf(recording.after));}
        finally{if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
}
