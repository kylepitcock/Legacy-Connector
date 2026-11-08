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
    @Test void nativeWidgetScreenDrawsAndDispatchesOriginalInput(){
        var drawn=new java.util.ArrayList<String>();
        var sink=(GuiDrawB173.Sink)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{GuiDrawB173.Sink.class},(proxy,method,args)->{
            if(method.getName().equals("text"))drawn.add((String)args[0]);
            if(method.getReturnType()==int.class)return method.getName().equals("width")?((String)args[0]).length()*6:1;
            return null;
        });
        var flag=new SettingBoolean("enabled",true);var control=new WidgetBoolean(flag,"Enabled");
        var number=new SettingInt("number",4,0,2,20);var numeric=new WidgetInt(number,"Number");
        var columns=new WidgetClassicTwocolumn(control,numeric);var window=new WidgetSimplewindow(columns,"Settings");var ui=new GuiWidgetsB173(window);
        GuiDrawB173.install(sink);
        try{
            ui.resize(320,240);ui.render(-1,-1);assertTrue(drawn.containsAll(java.util.List.of("Settings","Enabled: true","Number: 4","Back")));
            int x=control.b.getX()+5,y=control.b.getY()+5;ui.press(x,y,0);assertTrue(flag.get(""));ui.release(x,y,0);assertFalse(flag.get(""));
            control.b.getModel().setEnabled(false);ui.press(x,y,0);ui.release(x,y,0);assertFalse(flag.get(""));control.b.getModel().setEnabled(true);
            ui.press(numeric.s.getX(),numeric.s.getY()+5,0);ui.drag(numeric.s.getX()+numeric.s.getWidth()-1,numeric.s.getY()+5,0);ui.release(numeric.s.getX()+numeric.s.getWidth()-1,numeric.s.getY()+5,0);assertEquals(20,number.get(""));
            ui.key(203);assertEquals(18,number.get(""));ui.key(199);assertEquals(0,number.get(""));
            ui.resize(240,180);assertTrue(numeric.s.getX()+numeric.s.getWidth()<=240);assertTrue(control.b.getWidth()>0);
        }finally{GuiDrawB173.install(null);WidgetSetting.all.remove(control);WidgetSetting.all.remove(numeric);}
    }
    @Test void overflowingSettingsRemainReachableByWheelAndKeyboard(){
        var columns=new WidgetClassicTwocolumn();int[] selected={-1};
        for(int i=0;i<40;i++){int index=i;var button=new Button("Entry "+i);button.addCallback(()->selected[0]=index);columns.add(button);}
        var ui=new GuiWidgetsB173(new WidgetSimplewindow(columns,"Many settings"));ui.resize(320,180);
        var last=columns.getChild(39);assertTrue(last.getY()>180);ui.scroll(-100);assertTrue(last.getY()+last.getHeight()<=columns.getY()+columns.getHeight());
        ui.press(last.getX()+3,last.getY()+3,0);ui.release(last.getX()+3,last.getY()+3,0);assertEquals(39,selected[0]);
        ui.key(15);ui.key(28); // Tab reaches the always-visible Back button without selecting a hidden row.
        assertEquals(39,selected[0]);
    }
    @Test void controlSkinsUseMinecraftTexturesAndKeepTheirBordersWithinEachState(){
        var textures=new java.util.ArrayList<String>();var quads=new java.util.ArrayList<int[]>();int[] fills={0};
        var sink=(GuiDrawB173.Sink)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{GuiDrawB173.Sink.class},(proxy,method,args)->{
            if(method.getName().equals("textureId"))textures.add((String)args[0]);
            if(method.getName().equals("blit"))quads.add(java.util.Arrays.stream(args).mapToInt(value->(Integer)value).toArray());
            if(method.getName().equals("fill"))fills[0]++;
            if(method.getReturnType()==int.class)return method.getName().equals("width")?((String)args[0]).length()*6:1;
            return null;
        });
        var button=new Button("Config");var ui=new GuiWidgetsB173(button);ui.resize(321,31);
        java.util.function.IntConsumer check=bank->{
            assertFalse(quads.isEmpty());assertTrue(textures.contains("/gui/gui.png"));assertEquals(0,fills[0]);
            boolean[][] covered=new boolean[31][321];
            for(var q:quads){
                assertTrue(q[2]>=0&&q[2]+q[4]<=200&&q[3]>=bank&&q[3]+q[5]<=bank+20,"texture sampling stays in the Minecraft state");
                assertTrue(q[0]>=0&&q[1]>=0&&q[0]+q[4]<=321&&q[1]+q[5]<=31,"skin stays inside the control");
                for(int y=q[1];y<q[1]+q[5];y++)for(int x=q[0];x<q[0]+q[4];x++){assertFalse(covered[y][x],"skin patches do not overlap");covered[y][x]=true;}
            }
            for(var row:covered)for(boolean pixel:row)assertTrue(pixel,"skin has no gaps at a nonstandard control size");
            quads.clear();textures.clear();
        };
        GuiDrawB173.withSink(sink,()->{
            ui.render(-1,-1);check.accept(66);
            ui.render(1,1);check.accept(86);
            button.getModel().setEnabled(false);ui.render(1,1);check.accept(46);
            var number=new WidgetInt(new SettingInt("skin",0,0,1,100),"Value");
            try{
                var sliderUi=new GuiWidgetsB173(number);sliderUi.resize(321,20);
                for(int value:new int[]{0,100}){
                    quads.clear();number.s.setValue(value);sliderUi.render(-1,-1);
                    var thumb=quads.stream().filter(q->q[3]>=66&&q[3]+q[5]<=86).toList();
                    assertFalse(thumb.isEmpty(),"slider thumb uses the enabled Minecraft skin");
                    int left=value==0?0:313;
                    assertEquals(left,thumb.stream().mapToInt(q->q[0]).min().orElseThrow());
                    assertEquals(left+8,thumb.stream().mapToInt(q->q[0]+q[4]).max().orElseThrow());
                    assertTrue(quads.stream().anyMatch(q->q[3]>=46&&q[3]+q[5]<=66),"slider keeps its recessed Minecraft track");
                }
            }finally{WidgetSetting.all.remove(number);}
        });
    }
}
