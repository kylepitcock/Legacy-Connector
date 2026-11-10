package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.TitleScreen;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import org.junit.jupiter.api.Test;

class TitleScreenActivityB173Test {
    @Test void nativeMenuWorldAndSubmenuTransitionsKeepActivityScoped(){
        assertFalse(TitleScreen.mcc$menuActive(false,false,false)); // startup before a screen exists
        assertTrue(TitleScreen.mcc$menuActive(false,true,true)); // title
        assertTrue(TitleScreen.mcc$menuActive(false,true,false)); // options/multiplayer before a world
        assertFalse(TitleScreen.mcc$menuActive(true,false,false)); // gameplay
        assertFalse(TitleScreen.mcc$menuActive(true,true,false)); // pause/inventory
        assertTrue(TitleScreen.mcc$menuActive(false,true,true)); // return to title
    }
    @Test void dedicatedFallbackIsFalseAndInstalledSnapshotChangesAreLive(){
        TitleScreen.mcc$nativeActivity(null);assertFalse(TitleScreen.mcc$get$mmactive());
        var snapshot=new AtomicBoolean(true);
        try{TitleScreen.mcc$nativeActivity(snapshot::get);assertTrue(TitleScreen.mcc$get$mmactive());snapshot.set(false);assertFalse(TitleScreen.mcc$get$mmactive());}
        finally{TitleScreen.mcc$nativeActivity(null);}
        assertFalse(TitleScreen.mcc$get$mmactive());
    }
    @Test void originalGuiQueriesRecognizeTypedTitleIdentityWithoutTreatingEveryScreenAsTitle(){
        assertTrue(GuiQueriesB173.matches(TitleScreen.class,new TitleScreen()));
        assertTrue(GuiQueriesB173.matches(Screen.class,new TitleScreen()));
        assertFalse(GuiQueriesB173.matches(TitleScreen.class,new Screen()));
    }
}
