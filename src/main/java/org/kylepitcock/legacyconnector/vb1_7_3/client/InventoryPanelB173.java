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
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173;
import org.kylepitcock.legacyconnector.vb1_7_3.StacksB173;
import org.lwjgl.glfw.GLFW;

/** Additive native-screen widgets; the native menu and all its slots remain untouched. */
public final class InventoryPanelB173 {
    public record Context(long token,int kind) {}
    public record CellGeometry(long token,String identity,int slot,int itemX,int itemY) {}
    private record Offer(String title,int[] slots,int[][] layout) {}
    private record Cell(String identity,int slot,int x,int y) {}
    private static final Map<Screen,Entry> ENTRIES=new WeakHashMap<>();
    private static WeakReference<Screen> previous=new WeakReference<>(null);
    private static long nextToken;
    private static boolean installed;
    private static final class Entry {
        final long token=++nextToken;
        final Map<String,Offer> offers=new LinkedHashMap<>();
        EquipmentWidget panel;
    }
    private InventoryPanelB173() {}

    public static void install() {
        if(installed)return;installed=true;
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Pre event)->OriginalInventoryScreenB173.clear(event.getScreen()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Closing event)->OriginalInventoryScreenB173.clear(event.getScreen()));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event)->{
            if(kind(event.getScreen())==0)return;
            Entry entry=ENTRIES.computeIfAbsent(event.getScreen(),ignored->new Entry());
            entry.panel=new EquipmentWidget((AbstractContainerScreen<?>)event.getScreen(),entry);
            event.addListener(entry.panel);
            for(var offer:entry.offers.entrySet())OriginalInventoryScreenB173.offer(event.getScreen(),offer.getKey(),offer.getValue().layout());
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event)->{
            OriginalInventoryScreenB173.prepare(event.getScreen());
            EquipmentWidget panel=panel(event.getScreen());if(panel!=null)panel.layout();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Post event)->{
            EquipmentWidget panel=panel(event.getScreen());
            if(panel!=null)panel.tooltip(event.getGuiGraphics(),event.getMouseX(),event.getMouseY());
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event)->{
            if(OriginalInventoryScreenB173.mousePressed(event.getScreen(),event.getMouseX(),event.getMouseY(),event.getButton())){event.setCanceled(true);return;}
            EquipmentWidget panel=panel(event.getScreen());if(panel==null)return;panel.layout();
            if(panel.isMouseOver(event.getMouseX(),event.getMouseY())){
                panel.consumedButton=event.getButton();event.getScreen().setFocused(null);panel.setFocused(false);
                panel.mouseClicked(event.getMouseX(),event.getMouseY(),event.getButton());event.setCanceled(true);
            }else if(PlayerInventoryClientB173.pending()&&(nativeSlot(event.getScreen(),event.getMouseX(),event.getMouseY())
                    ||blankDropArea(event.getScreen(),event.getMouseX(),event.getMouseY()))){panel.consumedButton=event.getButton();event.setCanceled(true);}
            else panel.nativeButton=event.getButton();
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonReleased.Pre event)->{
            if(OriginalInventoryScreenB173.mouseReleased(event.getScreen(),event.getMouseX(),event.getMouseY(),event.getButton())){event.setCanceled(true);return;}
            EquipmentWidget panel=panel(event.getScreen());if(panel==null)return;
            if(panel.consumedButton==event.getButton()){panel.consumedButton=-1;event.setCanceled(true);}
            else if(panel.nativeButton==event.getButton()&&panel.isMouseOver(event.getMouseX(),event.getMouseY())){
                panel.finishNativeRelease(event.getButton());event.setCanceled(true);
            }
            if(panel.nativeButton==event.getButton())panel.nativeButton=-1;
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseDragged.Pre event)->{
            if(OriginalInventoryScreenB173.mouseDragged(event.getScreen(),event.getMouseX(),event.getMouseY(),event.getMouseButton())){event.setCanceled(true);return;}
            EquipmentWidget panel=panel(event.getScreen());if(panel!=null&&panel.consumedButton==event.getMouseButton())event.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.MouseScrolled.Pre event)->{
            EquipmentWidget panel=panel(event.getScreen());
            if(panel!=null&&panel.isMouseOver(event.getMouseX(),event.getMouseY())){
                panel.scroll(event.getScrollDeltaY());event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.KeyPressed.Pre event)->{
            if(OriginalInventoryScreenB173.keyPressed(event.getScreen(),event.getKeyCode())){event.setCanceled(true);return;}
            EquipmentWidget panel=panel(event.getScreen());if(panel==null)return;
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
    /** Equipment UI belongs to survival inventory; Creative's personal-inventory tab follows Creative policy too. */
    private static int kind(Screen screen){return screen instanceof InventoryScreen?1:0;}
    private static EquipmentWidget panel(Screen screen){if(kind(screen)==0)return null;Entry entry=ENTRIES.get(screen);return entry==null?null:entry.panel;}
    /** Current visible item origins, shared with pointer-input QA and screen integrations. */
    public static List<CellGeometry> cellGeometry(Screen screen){
        EquipmentWidget panel=panel(screen);if(panel==null||!panel.visible)return List.of();
        var result=new ArrayList<CellGeometry>();
        for(Cell cell:panel.cells){int x=panel.cellX(cell),y=panel.cellY(cell);
            if(y>=panel.getY()+(panel.embedded?0:EquipmentWidget.EDGE)&&y+16<=panel.getY()+panel.getHeight()-(panel.embedded?0:EquipmentWidget.EDGE))
                result.add(new CellGeometry(panel.entry.token,cell.identity(),cell.slot(),x,y));
        }
        return List.copyOf(result);
    }
    public static Context currentContext(){
        Screen screen=Minecraft.getInstance().screen;if(kind(screen)==0)screen=null;
        Screen old=previous.get();
        if(old!=screen){Entry entry=ENTRIES.get(old);if(entry!=null)entry.offers.clear();OriginalInventoryScreenB173.clear(old);previous=new WeakReference<>(screen);}
        Entry entry=ENTRIES.get(screen);return entry==null?new Context(0,0):new Context(entry.token,kind(screen));
    }
    public static boolean matchesContext(long token){
        Minecraft mc=Minecraft.getInstance();Entry entry=ENTRIES.get(mc.screen);
        return kind(mc.screen)!=0&&entry!=null&&entry.token==token&&mc.player!=null
                &&mc.screen instanceof AbstractContainerScreen<?> screen&&screen.getMenu()==mc.player.containerMenu;
    }
    public static void reset(){OriginalInventoryScreenB173.clearAll();for(Entry entry:ENTRIES.values())entry.offers.clear();previous=new WeakReference<>(null);}
    public static void offer(long token,String identity,String title,int[] slots,int[][] layout){
        Minecraft mc=Minecraft.getInstance();Entry entry=ENTRIES.get(mc.screen);
        if(token==0||entry==null||entry.token!=token||kind(mc.screen)==0||identity==null||identity.length()>512
                ||slots==null||slots.length>512||layout==null||layout.length>512||!entry.offers.containsKey(identity)&&entry.offers.size()>=16)return;
        int[][] copy=new int[layout.length][];
        for(int i=0;i<layout.length;i++){if(layout[i]==null||layout[i].length!=4||Math.abs((long)layout[i][2])>2048||Math.abs((long)layout[i][3])>2048)return;copy[i]=layout[i].clone();}
        for(int slot:slots)if(slot<0||slot>=layout.length)return;
        entry.offers.put(identity,new Offer(title==null||title.isBlank()?"Equipment":title,slots.clone(),copy));
        OriginalInventoryScreenB173.offer(mc.screen,identity,copy);
    }
    private static boolean nativeSlot(Screen raw,double x,double y){
        if(!(raw instanceof AbstractContainerScreen<?> screen))return false;
        for(var slot:screen.getMenu().slots)if(slot.isActive()&&x>=screen.getGuiLeft()+slot.x-1&&x<screen.getGuiLeft()+slot.x+17
                &&y>=screen.getGuiTop()+slot.y-1&&y<screen.getGuiTop()+slot.y+17)return true;
        return false;
    }
    private static boolean blankDropArea(Screen raw,double x,double y){
        if(!(raw instanceof AbstractContainerScreen<?> screen))return false;
        if(x>=screen.getGuiLeft()&&x<screen.getGuiLeft()+screen.getXSize()
                &&y>=screen.getGuiTop()&&y<screen.getGuiTop()+screen.getYSize())return false;
        for(var child:screen.children())if(child instanceof AbstractWidget widget&&widget.visible&&widget.isMouseOver(x,y))return false;
        if(screen instanceof InventoryScreen inventory&&inventory.getRecipeBookComponent().isVisible()){
            int bookX=(screen.width-147)/2-(screen.width<379?0:86),bookY=(screen.height-166)/2;
            if(x>=bookX-30&&x<bookX+147&&y>=bookY&&y<bookY+166)return false;
        }
        return true;
    }

    static final class EquipmentWidget extends AbstractWidget {
        private static final int PADDING=8,EDGE=PADDING-1;
        private static final ResourceLocation FRAME=ResourceLocation.withDefaultNamespace("container/bundle/background");
        private static final ResourceLocation SLOT=ResourceLocation.withDefaultNamespace("container/slot");
        private final WeakReference<AbstractContainerScreen<?>> owner;
        private final Entry entry;
        private List<Cell> cells=List.of();
        private int offset,selected,minX,minY,contentHeight;
        private int consumedButton=-1,nativeButton=-1;
        private boolean embedded;
        EquipmentWidget(AbstractContainerScreen<?> screen,Entry entry){
            super(0,0,80,44,Component.literal("Equipment"));owner=new WeakReference<>(screen);this.entry=entry;visible=false;
        }
        private void layout(){
            AbstractContainerScreen<?> screen=owner.get();visible=false;
            if(screen==null||kind(screen)==0||Minecraft.getInstance().screen!=screen)return;
            var list=new ArrayList<Cell>();
            var seen=new HashSet<Integer>();
            for(var offer:entry.offers.entrySet())for(int slot:offer.getValue().slots()){
                if(list.size()>=512)break;
                if(seen.add(slot)){int[] position=offer.getValue().layout()[slot];list.add(new Cell(offer.getKey(),slot,position[2],position[3]));}
            }
            cells=List.copyOf(list);if(cells.isEmpty())return;
            setMessage(Component.literal(entry.offers.size()==1?entry.offers.values().iterator().next().title():"Equipment"));
            embedded=OriginalInventoryScreenB173.active(screen);
            if(embedded){
                if(screen instanceof InventoryScreen inventory&&screen.width<379&&inventory.getRecipeBookComponent().isVisible())return;
                setPosition(screen.getGuiLeft(),screen.getGuiTop());setWidth(screen.getXSize());setHeight(screen.getYSize());offset=0;visible=true;return;
            }
            minX=cells.stream().mapToInt(Cell::x).min().orElse(0);minY=cells.stream().mapToInt(Cell::y).min().orElse(0);
            int w=cells.stream().mapToInt(Cell::x).max().orElse(minX)-minX+16+2*PADDING;
            contentHeight=cells.stream().mapToInt(Cell::y).max().orElse(minY)-minY+18;
            if(w>screen.width-8||contentHeight>1024)return;
            {
                int h=Math.min(contentHeight+2*EDGE,screen.height-8);
                int y=Math.max(4,Math.min(screen.height-h-4,screen.getGuiTop()));
                for(int x:new int[]{screen.getGuiLeft()+screen.getXSize()+4,screen.getGuiLeft()-w-4}){
                    if(x<4||x+w>screen.width-4||y+h>screen.height-4||obstructed(screen,x,y,w,h))continue;
                    setPosition(x,y);setWidth(w);setHeight(h);visible=true;
                    offset=Math.min(offset,Math.max(0,contentHeight-(height-2*EDGE)));
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
            if(embedded&&!OriginalInventoryScreenB173.active(owner.get())){layout();if(!visible)return;}
            Minecraft mc=Minecraft.getInstance();
            var handler=mc.player==null?null:PlayerViewB173.of(mc.player).playerScreenHandler;
            if(!embedded)g.blitSprite(FRAME,getX(),getY(),width,height);
            g.enableScissor(getX(),getY()+(embedded?0:EDGE),getX()+width,getY()+height-(embedded?0:EDGE));
            for(int index=0;index<cells.size();index++){
                int x=cellX(cells.get(index))-1,y=cellY(cells.get(index))-1;
                if(!embedded)g.blitSprite(SLOT,x,y,18,18);
                ItemStack stack=stack(cells.get(index),handler);g.renderItem(stack,x+1,y+1);g.renderItemDecorations(mc.font,stack,x+1,y+1);
                if(index==cellAt(mouseX,mouseY))AbstractContainerScreen.renderSlotHighlight(g,x+1,y+1,0);
            }
            g.disableScissor();
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
            if(!visible||x<getX()||x>=getX()+width||y<getY()+(embedded?0:EDGE)||y>=getY()+height-(embedded?0:EDGE))return -1;
            for(int i=0;i<cells.size();i++)if(x>=cellX(cells.get(i))-1&&x<cellX(cells.get(i))+17&&y>=cellY(cells.get(i))-1&&y<cellY(cells.get(i))+17)return i;
            return -1;
        }
        private int cellX(Cell cell){return getX()+(embedded?cell.x():PADDING+cell.x()-minX);}
        private int cellY(Cell cell){return getY()+(embedded?cell.y():PADDING+cell.y()-minY-offset);}
        @Override public boolean isMouseOver(double x,double y){return visible&&(embedded?cellAt(x,y)>=0:super.isMouseOver(x,y));}
        private void finishNativeRelease(int button){
            var screen=owner.get();if(screen==null)return;
            for(int y=2;y<screen.getYSize()-2;y+=2)for(int x=2;x<screen.getXSize()-2;x+=2){
                double px=screen.getGuiLeft()+x,py=screen.getGuiTop()+y;
                if(nativeSlot(screen,px,py)||cellAt(px,py)>=0)continue;
                boolean widget=false;
                for(var child:screen.children())if(child instanceof AbstractWidget value&&value!=this&&value.visible&&value.isMouseOver(px,py))widget=true;
                if(!widget){screen.mouseReleased(px,py,button);return;}
            }
            screen.clearDraggingState();
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
        private void scroll(double amount){if(!visible||embedded)return;offset=Math.max(0,Math.min(Math.max(0,contentHeight-(height-2*EDGE)),offset+(amount<0?18:-18)));}
        @Override public boolean keyPressed(int key,int scan,int modifiers){
            if(!visible||!isFocused())return false;
            int next=selected;
            if(key==GLFW.GLFW_KEY_LEFT)next--;else if(key==GLFW.GLFW_KEY_RIGHT)next++;
            else if(key==GLFW.GLFW_KEY_UP)next--;else if(key==GLFW.GLFW_KEY_DOWN)next++;
            else if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER||key==GLFW.GLFW_KEY_SPACE){click(selected,0,Screen.hasShiftDown());return true;}
            else return false;
            selected=Math.max(0,Math.min(cells.size()-1,next));
            if(!embedded){int y=cells.get(selected).y()-minY;if(y<offset)offset=y;if(y+18>offset+height-2*EDGE)offset=y+18-height+2*EDGE;}
            return true;
        }
        private void tooltip(GuiGraphics g,int x,int y){
            Minecraft mc=Minecraft.getInstance();
            if(!visible||mc.player==null||!mc.player.inventoryMenu.getCarried().isEmpty())return;
            int index=cellAt(x,y);if(index<0)return;ItemStack stack=stack(cells.get(index));
            if(!stack.isEmpty())g.renderTooltip(mc.font,stack,x,y);
        }
        @Override protected void updateWidgetNarration(NarrationElementOutput narration){
            narration.add(NarratedElementType.TITLE,getMessage());
            if(!cells.isEmpty())narration.add(NarratedElementType.POSITION,Component.literal((selected+1)+" / "+cells.size()));
            narration.add(NarratedElementType.USAGE,Component.literal("Arrow keys select equipment. Enter picks up or places an item. Shift Enter moves it to inventory."));
        }
    }
}
