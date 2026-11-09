package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173;
import org.kylepitcock.legacyconnector.vb1_7_3.StacksB173;
import org.lwjgl.glfw.GLFW;

/** Additive native-screen widgets; the native menu and all its slots remain untouched. */
public final class InventoryPanelB173 {
    public record Context(long token,int kind) {}
    private record Offer(String title,int[] slots) {}
    private record Cell(String identity,String title,int slot) {}
    private static final Map<Screen,Entry> ENTRIES=new WeakHashMap<>();
    private static WeakReference<Screen> previous=new WeakReference<>(null);
    private static long nextToken;
    private static boolean installed;
    private static final class Entry {
        final long token=++nextToken;
        final Map<String,Offer> offers=new LinkedHashMap<>();
        Panel panel;
    }
    private InventoryPanelB173() {}

    public static void install() {
        if(installed)return;installed=true;
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event)->{
            if(kind(event.getScreen())==0)return;
            Entry entry=ENTRIES.computeIfAbsent(event.getScreen(),ignored->new Entry());
            entry.panel=new Panel((AbstractContainerScreen<?>)event.getScreen(),entry);
            event.addListener(entry.panel);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event)->{
            Panel panel=panel(event.getScreen());if(panel!=null)panel.layout();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event)->{
            Panel panel=panel(event.getScreen());
            if(panel!=null)panel.tooltip(event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event)->{
            Panel panel=panel(event.getScreen());if(panel==null)return;panel.layout();
            if(panel.isMouseOver(event.getMouseX(),event.getMouseY())){
                panel.pressed=true;event.getScreen().setFocused(panel);panel.setFocused(true);
                panel.mouseClicked(event.getMouseX(),event.getMouseY(),event.getButton());event.setCanceled(true);
            }else if(PlayerInventoryClientB173.pending()&&(nativeSlot(event.getScreen(),event.getMouseX(),event.getMouseY())
                    ||blankDropArea(event.getScreen(),event.getMouseX(),event.getMouseY())))event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre event)->{
            Panel panel=panel(event.getScreen());
            if(panel!=null&&panel.pressed){panel.pressed=false;event.setCanceled(true);}
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseDragged.Pre event)->{
            Panel panel=panel(event.getScreen());if(panel!=null&&panel.pressed)event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre event)->{
            Panel panel=panel(event.getScreen());
            if(panel!=null&&panel.isMouseOver(event.getMouseX(),event.getMouseY())){
                panel.scroll(event.getScrollDeltaY());event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre event)->{
            Panel panel=panel(event.getScreen());if(panel==null)return;
            Minecraft mc=Minecraft.getInstance();
            double x=mc.mouseHandler.xpos()*mc.getWindow().getGuiScaledWidth()/mc.getWindow().getScreenWidth();
            double y=mc.mouseHandler.ypos()*mc.getWindow().getGuiScaledHeight()/mc.getWindow().getScreenHeight();
            if(PlayerInventoryClientB173.pending()&&nativeSlot(event.getScreen(),x,y)){
                boolean action=mc.options.keyDrop.matches(event.getKeyCode(),event.getScanCode())
                        ||mc.options.keySwapOffhand.matches(event.getKeyCode(),event.getScanCode());
                for(var key:mc.options.keyHotbarSlots)action|=key.matches(event.getKeyCode(),event.getScanCode());
                if(action)event.setCanceled(true);
            }
        });
    }
    private static int kind(Screen screen){return screen instanceof InventoryScreen?1:screen instanceof CreativeModeInventoryScreen?2:0;}
    private static Panel panel(Screen screen){Entry entry=ENTRIES.get(screen);return entry==null?null:entry.panel;}
    public static Context currentContext(){
        Screen screen=Minecraft.getInstance().screen;if(kind(screen)==0)screen=null;
        Screen old=previous.get();
        if(old!=screen){Entry entry=ENTRIES.get(old);if(entry!=null)entry.offers.clear();previous=new WeakReference<>(screen);}
        Entry entry=ENTRIES.get(screen);return entry==null?new Context(0,0):new Context(entry.token,kind(screen));
    }
    public static boolean matchesContext(long token){
        Minecraft mc=Minecraft.getInstance();Entry entry=ENTRIES.get(mc.screen);
        return kind(mc.screen)!=0&&entry!=null&&entry.token==token&&mc.player!=null
                &&mc.screen instanceof AbstractContainerScreen<?> screen&&screen.getMenu()==mc.player.containerMenu;
    }
    public static void reset(){for(Entry entry:ENTRIES.values())entry.offers.clear();previous=new WeakReference<>(null);}
    public static void offer(long token,String identity,String title,int[] slots){
        Minecraft mc=Minecraft.getInstance();Entry entry=ENTRIES.get(mc.screen);
        if(token==0||entry==null||entry.token!=token||kind(mc.screen)==0||identity==null||identity.length()>512
                ||slots==null||slots.length>512||!entry.offers.containsKey(identity)&&entry.offers.size()>=16)return;
        for(int slot:slots)if(slot<0||slot>=512)return;
        entry.offers.put(identity,new Offer(title==null||title.isBlank()?"Equipment":title,slots.clone()));
    }
    private static boolean nativeSlot(Screen raw,double x,double y){
        if(!(raw instanceof AbstractContainerScreen<?> screen))return false;
        for(var slot:screen.getMenu().slots)if(slot.isActive()&&x>=screen.getGuiLeft()+slot.x-1&&x<screen.getGuiLeft()+slot.x+17
                &&y>=screen.getGuiTop()+slot.y-1&&y<screen.getGuiTop()+slot.y+17)return true;
        return false;
    }
    private static boolean blankDropArea(Screen raw,double x,double y){
        if(!(raw instanceof AbstractContainerScreen<?> screen))return false;
        // Creative category tabs are native controls outside the container rectangle, not widget children.
        int margin=screen instanceof CreativeModeInventoryScreen?32:0;
        if(x>=screen.getGuiLeft()&&x<screen.getGuiLeft()+screen.getXSize()
                &&y>=screen.getGuiTop()-margin&&y<screen.getGuiTop()+screen.getYSize()+margin)return false;
        for(var child:screen.children())if(child instanceof AbstractWidget widget&&widget.visible&&widget.isMouseOver(x,y))return false;
        if(screen instanceof InventoryScreen inventory&&inventory.getRecipeBookComponent().isVisible()){
            int bookX=(screen.width-147)/2-(screen.width<379?0:86),bookY=(screen.height-166)/2;
            if(x>=bookX-30&&x<bookX+147&&y>=bookY&&y<bookY+166)return false;
        }
        return true;
    }

    private static final class Panel extends AbstractWidget {
        private final WeakReference<AbstractContainerScreen<?>> owner;
        private final Entry entry;
        private List<Cell> cells=List.of();
        private int columns=4,rows=1,offset,selected;
        private boolean pressed;
        Panel(AbstractContainerScreen<?> screen,Entry entry){
            super(0,0,80,44,Component.literal("Equipment"));owner=new WeakReference<>(screen);this.entry=entry;visible=false;
        }
        private void layout(){
            AbstractContainerScreen<?> screen=owner.get();visible=false;
            if(screen==null||Minecraft.getInstance().screen!=screen)return;
            var list=new ArrayList<Cell>();
            var seen=new HashSet<Integer>();
            for(var offer:entry.offers.entrySet())for(int slot:offer.getValue().slots()){
                if(list.size()>=512)break;
                if(seen.add(slot))list.add(new Cell(offer.getKey(),offer.getValue().title(),slot));
            }
            cells=List.copyOf(list);if(cells.isEmpty())return;
            setMessage(Component.literal(entry.offers.size()==1?entry.offers.values().iterator().next().title():"Equipment"));
            for(int cols:new int[]{4,2}){
                int w=8+cols*18;
                int maxRows=Math.min(8,Math.max(1,(screen.height-44)/18));
                int r=Math.min(maxRows,(cells.size()+cols-1)/cols),h=30+r*18;
                int y=Math.max(4,Math.min(screen.height-h-4,screen.getGuiTop()));
                for(int x:new int[]{screen.getGuiLeft()+screen.getXSize()+4,screen.getGuiLeft()-w-4}){
                    if(x<4||x+w>screen.width-4||y+h>screen.height-4||obstructed(screen,x,y,w,h))continue;
                    columns=cols;rows=r;setPosition(x,y);setWidth(w);setHeight(h);visible=true;
                    offset=Math.min(offset,Math.max(0,(cells.size()-1)/columns-rows+1));
                    selected=Math.min(selected,cells.size()-1);return;
                }
            }
        }
        private boolean obstructed(AbstractContainerScreen<?> screen,int x,int y,int w,int h){
            if(screen instanceof InventoryScreen inventory&&inventory.getRecipeBookComponent().isVisible()){
                if(screen.width<379||overlap(x,y,w,h,screen.getGuiLeft()-147,screen.getGuiTop(),147,166))return true;
            }
            Minecraft mc=Minecraft.getInstance();
            if(mc.player!=null&&!mc.player.getActiveEffects().isEmpty()
                    &&overlap(x,y,w,h,screen.getGuiLeft()+screen.getXSize()+2,screen.getGuiTop(),120,166))return true;
            for(var child:screen.children())if(child instanceof AbstractWidget widget&&widget!=this&&widget.visible
                    &&overlap(x,y,w,h,widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight()))return true;
            return false;
        }
        private static boolean overlap(int x,int y,int w,int h,int a,int b,int c,int d){return x<a+c&&x+w>a&&y<b+d&&y+h>b;}
        @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
            Minecraft mc=Minecraft.getInstance();
            var handler=mc.player==null?null:PlayerViewB173.of(mc.player).playerScreenHandler;
            g.fill(getX(),getY(),getX()+width,getY()+height,0xFFC6C6C6);g.renderOutline(getX(),getY(),width,height,0xFF555555);
            String title=PlayerInventoryClientB173.pending()?"…":mc.font.plainSubstrByWidth(getMessage().getString(),width-8);
            g.drawString(mc.font,title,getX()+4,getY()+5,0x404040,false);
            for(int i=0;i<rows*columns;i++){
                int index=offset*columns+i;if(index>=cells.size())break;
                int x=getX()+4+i%columns*18,y=getY()+17+i/columns*18;
                g.fill(x,y,x+18,y+18,0xFF373737);g.fill(x+1,y+1,x+18,y+18,0xFFFFFFFF);g.fill(x+1,y+1,x+17,y+17,0xFF8B8B8B);
                ItemStack stack=stack(cells.get(index),handler);g.renderItem(stack,x+1,y+1);g.renderItemDecorations(mc.font,stack,x+1,y+1);
                if(index==cellAt(mouseX,mouseY)||isFocused()&&index==selected)g.renderOutline(x,y,18,18,0xFFFFFFFF);
            }
            if(cells.size()>rows*columns)g.drawString(mc.font,"↕",getX()+width-10,getY()+height-10,0x404040,false);
        }
        private ItemStack stack(Cell cell){
            Minecraft mc=Minecraft.getInstance();if(mc.player==null)return ItemStack.EMPTY;
            var handler=PlayerViewB173.of(mc.player).playerScreenHandler;
            return stack(cell,handler);
        }
        private ItemStack stack(Cell cell,legacyconnector.vb1_7_3.net.minecraft.screen.ScreenHandler handler){
            return handler==null||cell.slot()>=handler.slots.size()?ItemStack.EMPTY:StacksB173.toModern(handler.slots.get(cell.slot()).getStack());
        }
        private int cellAt(double x,double y){
            int localX=(int)x-getX()-4,localY=(int)y-getY()-17;
            if(localX<0||localY<0||localX>=columns*18||localY>=rows*18)return -1;
            int index=offset*columns+localY/18*columns+localX/18;return index<cells.size()?index:-1;
        }
        @Override public boolean mouseClicked(double x,double y,int button){
            if(!visible||!isMouseOver(x,y))return false;
            int index=cellAt(x,y);
            if(index>=0&&(button==0||button==1)){selected=index;click(index,button,Screen.hasShiftDown());}
            return true;
        }
        private void click(int index,int button,boolean quick){
            if(PlayerInventoryClientB173.pending()||index<0||index>=cells.size())return;
            Cell cell=cells.get(index);PlayerInventoryClientB173.click(entry.token,cell.identity(),cell.slot(),button,quick);
        }
        private void scroll(double amount){if(!visible)return;offset=Math.max(0,Math.min(Math.max(0,(cells.size()-1)/columns-rows+1),offset+(amount<0?1:-1)));}
        @Override public boolean keyPressed(int key,int scan,int modifiers){
            if(!visible||!isFocused())return false;
            int next=selected;
            if(key==GLFW.GLFW_KEY_LEFT)next--;else if(key==GLFW.GLFW_KEY_RIGHT)next++;
            else if(key==GLFW.GLFW_KEY_UP)next-=columns;else if(key==GLFW.GLFW_KEY_DOWN)next+=columns;
            else if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE){click(selected,0,Screen.hasShiftDown());return true;}
            else return false;
            selected=Math.max(0,Math.min(cells.size()-1,next));
            if(selected<offset*columns)offset=selected/columns;
            if(selected>=(offset+rows)*columns)offset=selected/columns-rows+1;
            return true;
        }
        private void tooltip(GuiGraphics g,int x,int y){
            Minecraft mc=Minecraft.getInstance();
            if(!visible||mc.player==null||!mc.player.inventoryMenu.getCarried().isEmpty())return;
            int index=cellAt(x,y);if(index<0)return;ItemStack stack=stack(cells.get(index));
            if(stack.isEmpty())g.renderTooltip(mc.font,Component.literal(cells.get(index).title()),x,y);
            else g.renderTooltip(mc.font,stack,x,y);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput narration){
            narration.add(NarratedElementType.TITLE,getMessage());
            if(!cells.isEmpty())narration.add(NarratedElementType.POSITION,Component.literal((selected+1)+" / "+cells.size()));
            narration.add(NarratedElementType.USAGE,Component.literal("Arrow keys select equipment. Enter picks up or places an item. Shift Enter moves it to inventory."));
        }
    }
}
