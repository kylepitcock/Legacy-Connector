package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.function.Supplier;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;

/** Original instance checks, with native client screens resolved by the client adapter. */
public final class GuiQueriesB173 {
    private static volatile Supplier<Object> client;
    private GuiQueriesB173(){}
    public static void installClient(Supplier<Object> current){client=current;}
    public static boolean matches(Class<?> type,Object screen){return type==null?screen==null:type.isInstance(screen);}
    public static boolean isOpen(Class<?> type){
        Object screen=Minecraft.mcc$instance().currentScreen;
        // A concrete original GUI in the actor scope wins over presentation snapshots.
        if(screen!=null&&screen.getClass()!=Screen.class)return matches(type,screen);
        var nativeClient=client;if(nativeClient!=null)return matches(type,nativeClient.get());
        return matches(type,screen);
    }
}
