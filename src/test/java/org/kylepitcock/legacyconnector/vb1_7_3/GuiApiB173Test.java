package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import legacyconnector.vb1_7_3.net.minecraft.src.*;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.*;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.model.*;

class GuiApiB173Test {
    private static int total;
    private static void add(int n){total+=n;}
    @Test void typedDefaultsAndIndependentContexts(){
        var s=new SettingInt("count",7,100,5,0);
        assertEquals(0,s.min);assertEquals(100,s.max);assertEquals(7,((Setting<?>)s).get());
        s.set(13,"");s.set(28,"world");
        assertEquals(15,s.get(""));assertEquals(30,s.get("world"));assertEquals(15,s.get("absent"));
        s.fromString("19","world");assertEquals(19,s.get("world"));
        s.copyContext("world","copy");s.set(45,"world");assertEquals(19,s.get("copy"));
        s.reset("world");assertEquals(5,s.get("world"));
        var f=new SettingFloat("strength",1f,0f,.25f,10f);f.set(1.62f,"");assertEquals(1.5f,f.get(""));
        var m=new SettingMulti("mode",1,"a","b");m.fromString("a","");assertEquals(0,m.get(""));m.next("");assertEquals("b",m.getLabel(""));
    }
    @Test void callbackHierarchyAndRemoval(){
        total=0;Runnable callback=new ModAction(GuiApiB173Test.class,"add",int.class).setDefaultArguments(4);
        ButtonModel model=new SimpleButtonModel();Button button=new Button(model);
        assertInstanceOf(TextWidget.class,button);button.addCallback(callback);
        model.setArmed(true);model.setPressed(true);model.setPressed(false);assertEquals(4,total);
        button.removeCallback(callback);model.fireActionCallback();assertEquals(4,total);
        var failure=new ModAction(GuiApiB173Test.class,"fail");assertThrows(RuntimeException.class,failure::run);
    }
    private static void fail(){throw new IllegalStateException("original callback failure");}
    @Test void widgetOwnershipAndSettingControls(){
        var a=new Widget();var b=new Widget();var child=new Widget();a.add(child);b.add(child);
        assertEquals(0,a.getNumChildren());assertSame(b,child.getParent());assertThrows(IllegalArgumentException.class,()->child.add(b));
        var flag=new SettingBoolean("enabled",true);var control=new WidgetBoolean(flag,"Enabled");
        try{control.b.getModel().fireActionCallback();assertFalse(flag.get(""));assertEquals("Enabled: false",control.b.getText());}
        finally{WidgetSetting.all.remove(control);}
    }
}
