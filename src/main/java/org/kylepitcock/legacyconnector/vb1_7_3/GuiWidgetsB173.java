package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.*;
import legacyconnector.vb1_7_3.net.minecraft.src.*;

/** Native drawing/input for GuiAPI's existing widget models; original callbacks own every setting change. */
public final class GuiWidgetsB173 {
    private final Widget root;
    private Widget focused,pressed;
    private int width,height,offset,maxOffset;
    public GuiWidgetsB173(Widget root){this.root=java.util.Objects.requireNonNull(root);}
    public void resize(int width,int height){this.width=width;this.height=height;layout();}
    private void layout(){maxOffset=0;place(root,0,0,width,height);int next=Math.min(offset,maxOffset);if(next!=offset){offset=next;layout();}}
    private static int preferred(Widget widget,int fallback){return widget.getPreferredWidth()>0?widget.getPreferredWidth():fallback;}
    private void place(Widget widget,int x,int y,int w,int h){
        widget.setPosition(x,y);widget.setSize(Math.max(1,w),Math.max(1,h));
        if(widget instanceof WidgetSimplewindow window){
            int top=Math.min(window.vtop,Math.max(20,h/4)),bottom=Math.min(window.vbot,Math.max(24,h/4));
            place(window.TitleWidget,x+4,y+8,w-8,20);place(window.buttonbar,x+Math.max(0,(w-200)/2),y+h-bottom,Math.min(200,w),20);
            place(window.mainwidget,x+Math.min(window.hpad,w/10),y+top,w-Math.min(window.hpad,w/10)*2,Math.max(20,h-top-bottom-4));
        }else if(widget instanceof WidgetClassicTwocolumn columns){
            int cell=Math.max(1,Math.min(columns.defaultwidth,(w-columns.split)/2));int row=Math.max(20,columns.defaultheight)+columns.defaultpad;
            int content=((columns.getNumChildren()+1)/2)*row;maxOffset=Math.max(maxOffset,Math.max(0,content-h));
            for(int i=0;i<widget.getNumChildren();i++){var child=widget.getChild(i);int cw=columns.overridewidth?cell:Math.min(cell,preferred(child,cell));int ch=columns.overrideheight?Math.max(20,columns.defaultheight):Math.max(20,child.getPreferredHeight());place(child,x+(i%2)*(cell+columns.split),y+(i/2)*row-offset,cw,ch);}
        }else if(widget instanceof WidgetSingleRow){
            int gap=4,count=widget.getNumChildren();if(count==0)return;int cell=Math.max(1,(w-gap*(count-1))/count);
            for(int i=0;i<count;i++)place(widget.getChild(i),x+i*(cell+gap),y,cell,h);
        }else if(widget instanceof WidgetSetting){for(int i=0;i<widget.getNumChildren();i++)place(widget.getChild(i),x,y,w,h);}
        else if(!(widget instanceof Button)&&!(widget instanceof WidgetSlider)&&!(widget instanceof Label)){
            int row=24;maxOffset=Math.max(maxOffset,Math.max(0,widget.getNumChildren()*row-h));
            for(int i=0;i<widget.getNumChildren();i++)place(widget.getChild(i),x,y+i*row-offset,w,20);
        }
    }
    public void render(int mouseX,int mouseY){
        render(root,mouseX,mouseY,0,height);
    }
    private void render(Widget widget,int mx,int my,int clipTop,int clipBottom){
        if(!widget.isVisible())return;
        int x=widget.getX(),y=widget.getY(),w=widget.getWidth(),h=widget.getHeight();
        boolean full=y>=clipTop&&y+h<=clipBottom;
        if(full&&(widget instanceof Button||widget instanceof WidgetSlider)){
            boolean hover=contains(widget,mx,my),enabled=enabled(widget);int color=!enabled?0xFF353535:pressed==widget?0xFF3B5264:hover?0xFF627C91:0xFF46505A;
            GuiDrawB173.fill(x,y,x+w,y+h,color);if(focused==widget){GuiDrawB173.fill(x,y,x+w,y+1,0xFFFFFFFF);GuiDrawB173.fill(x,y+h-1,x+w,y+h,0xFFFFFFFF);}
            String text;
            if(widget instanceof Button button){button.getModel().setHover(hover);text=button.getText();}
            else{var slider=(WidgetSlider)widget;float range=slider.mcc$max()-slider.mcc$min();float fraction=range==0?0:(slider.getValue()-slider.mcc$min())/range;int knob=x+Math.round(Math.max(0,w-8)*fraction);GuiDrawB173.fill(knob,y,knob+8,y+h,enabled?0xFF92A8BB:0xFF666666);text=slider.getFormat();}
            GuiDrawB173.text(text,x+Math.max(3,(w-GuiDrawB173.width(text))/2),y+(h-8)/2,enabled?0xFFFFFF:0x888888,true);
        }else if(full&&widget instanceof Label label)GuiDrawB173.text(label.getText(),x+(w-GuiDrawB173.width(label.getText()))/2,y+5,0xFFFFFF,true);
        int childTop=widget instanceof WidgetSimplewindow?clipTop:Math.max(clipTop,y),childBottom=widget instanceof WidgetSimplewindow?clipBottom:Math.min(clipBottom,y+h);
        for(int i=0;i<widget.getNumChildren();i++)render(widget.getChild(i),mx,my,childTop,childBottom);
    }
    private static boolean contains(Widget widget,int x,int y){return x>=widget.getX()&&y>=widget.getY()&&x<widget.getX()+widget.getWidth()&&y<widget.getY()+widget.getHeight();}
    private static boolean enabled(Widget widget){return widget.isEnabled()&&(!(widget instanceof Button button)||button.getModel().isEnabled());}
    private Widget hit(Widget widget,int x,int y){
        if(!widget.isVisible()||!enabled(widget)||!contains(widget,x,y))return null;
        for(int i=widget.getNumChildren()-1;i>=0;i--){Widget child=hit(widget.getChild(i),x,y);if(child!=null)return child;}
        return widget instanceof Button||widget instanceof WidgetSlider?widget:null;
    }
    public void press(int x,int y,int button){
        if(button!=0)return;focused=pressed=hit(root,x,y);if(pressed instanceof Button b){b.getModel().setArmed(true);b.getModel().setPressed(true);}
        if(pressed instanceof WidgetSlider slider)slide(slider,x);
    }
    public void drag(int x,int y,int button){if(button!=0)return;if(pressed instanceof Button b)b.getModel().setArmed(hit(root,x,y)==b);if(pressed instanceof WidgetSlider slider)slide(slider,x);}
    public void release(int x,int y,int button){if(button!=0)return;Widget previous=pressed;pressed=null;if(previous instanceof Button b){b.getModel().setArmed(hit(root,x,y)==b);b.getModel().setPressed(false);b.getModel().setArmed(false);if(hit(root,x,y)==b)GuiDrawB173.click();}}
    private static void slide(WidgetSlider slider,int x){float fraction=Math.max(0,Math.min(1,(x-slider.getX())/(float)Math.max(1,slider.getWidth()-1)));slider.setValue(slider.mcc$min()+fraction*(slider.mcc$max()-slider.mcc$min()));}
    public void scroll(double amount){offset=Math.max(0,Math.min(maxOffset,offset-(int)Math.round(amount*24)));layout();}
    private static void leaves(Widget widget,ArrayList<Widget> result){if(!widget.isVisible()||!enabled(widget))return;if(widget instanceof Button||widget instanceof WidgetSlider)result.add(widget);else for(int i=0;i<widget.getNumChildren();i++)leaves(widget.getChild(i),result);}
    public void key(int key){
        if(key==201){scroll(5);return;}if(key==209){scroll(-5);return;}
        if(key==15){var controls=new ArrayList<Widget>();leaves(root,controls);if(!controls.isEmpty()){focused=controls.get((controls.indexOf(focused)+1)%controls.size());ensureVisible(focused);}return;}
        if(focused instanceof Button button&&(key==28||key==57)){button.getModel().fireActionCallback();GuiDrawB173.click();}
        if(focused instanceof WidgetSlider slider){float step=1;if(slider.getParent() instanceof WidgetInt integer)step=Math.max(1,integer.value.step);else if(slider.getParent() instanceof WidgetFloat decimal)step=decimal.value.step>0?decimal.value.step:(slider.mcc$max()-slider.mcc$min())/100;
            if(key==203||key==208)slider.setValue(slider.getValue()-step);if(key==205||key==200)slider.setValue(slider.getValue()+step);if(key==199)slider.setValue(slider.mcc$min());if(key==207)slider.setValue(slider.mcc$max());}
    }
    private void ensureVisible(Widget widget){for(Widget parent=widget.getParent();parent!=null;parent=parent.getParent())if(parent instanceof WidgetClassicTwocolumn){if(widget.getY()<parent.getY())offset=Math.max(0,offset-(parent.getY()-widget.getY()));else if(widget.getY()+widget.getHeight()>parent.getY()+parent.getHeight())offset=Math.min(maxOffset,offset+widget.getY()+widget.getHeight()-parent.getY()-parent.getHeight());layout();return;}}
}
