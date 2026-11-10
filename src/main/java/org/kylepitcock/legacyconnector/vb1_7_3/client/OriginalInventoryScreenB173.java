package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerInventoryLayoutB173;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerMenuContextB173;
import org.kylepitcock.legacyconnector.vb1_7_3.PlayerViewB173;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Anchor;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Placement;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Rect;
import org.lwjgl.glfw.GLFW;

/** Scoped original inventory artwork and coordinates on the native inventory screen/menu. */
public final class OriginalInventoryScreenB173 {
    private static final Map<InventoryScreen,OriginalInventoryScreenB173> ACTIVE=new WeakHashMap<>();
    private static final java.lang.reflect.Field BUTTON_SPRITES=net.neoforged.fml.util.ObfuscationReflectionHelper.findField(ImageButton.class,"sprites");
    private final WeakReference<InventoryScreen> owner;
    private final HandledScreen gui;
    private final PreviewMinecraft minecraft=new PreviewMinecraft();
    private final List<Move> moves=new ArrayList<>();
    private final AbstractWidget book;
    private final Slot offhand;
    private int bookBeforeX,bookBeforeY,bookAppliedX,bookAppliedY;
    private boolean bookMoved,failed,rendered;
    private final Control handControl=new Control("offhand"),bookControl=new Control("recipeBook");
    private ClientGuiB173.Background theme;
    private Drag drag;
    private int releaseMask;
    private static final int CONTROL_SIZE=32,ITEM_INSET=(CONTROL_SIZE-16)/2;
    public record ControlGeometry(String id,Rect bounds,InventoryControlDockB173.Edge edge) {}
    private static final class Control {
        final String id;Anchor anchor;Placement placement;
        Control(String id){this.id=id;anchor=InventoryControlPositionsB173.get(id);}
    }
    private static final class Drag {
        final Control control;final double startX,startY,grabX,grabY;
        Rect proposed;boolean moved;
        Drag(Control control,double x,double y){this.control=control;startX=x;startY=y;grabX=x-control.placement.bounds().x();grabY=y-control.placement.bounds().y();proposed=control.placement.bounds();}
    }
    private static final class PreviewMinecraft extends legacyconnector.vb1_7_3.net.minecraft.client.Minecraft {}
    private static final class Move {
        final Slot slot;final int beforeX,beforeY;int appliedX,appliedY;
        Move(Slot slot){this.slot=slot;beforeX=appliedX=slot.x;beforeY=appliedY=slot.y;}
        void apply(int x,int y){slot.x=appliedX=x;slot.y=appliedY=y;}
        boolean unchanged(){return slot.x==appliedX&&slot.y==appliedY;}
        void restore(){if(unchanged()){slot.x=beforeX;slot.y=beforeY;}}
    }
    private OriginalInventoryScreenB173(InventoryScreen screen,HandledScreen gui){
        owner=new WeakReference<>(screen);this.gui=gui;
        var mc=Minecraft.getInstance();var view=PlayerViewB173.of(mc.player);
        var mapping=PlayerInventoryLayoutB173.nativeSlots(mc.player.inventoryMenu,view);
        if(mapping.size()!=45||gui.mcc$backgroundWidth()!=screen.getXSize()||gui.mcc$backgroundHeight()!=screen.getYSize())
            throw new IllegalArgumentException("Original inventory layout does not match native inventory bounds/storage");
        for(var position:mapping)moves.add(new Move(position.slot()));
        var hands=mc.player.inventoryMenu.slots.stream().filter(slot->slot.container==mc.player.getInventory()&&slot.getContainerSlot()==40).toList();
        if(hands.size()!=1)throw new IllegalArgumentException("Ambiguous native offhand storage");
        offhand=hands.getFirst();moves.add(new Move(offhand));
        AbstractWidget found=null;
        for(var child:screen.children())if(child instanceof ImageButton button)try{
            if(BUTTON_SPRITES.get(button).equals(RecipeBookComponent.RECIPE_BUTTON_SPRITES)){if(found!=null)throw new IllegalArgumentException("Ambiguous recipe book controls");found=button;}
        }catch(IllegalAccessException error){throw new IllegalStateException(error);}
        book=found;
        if(book!=null){bookBeforeX=bookAppliedX=book.getX();bookBeforeY=bookAppliedY=book.getY();}
        minecraft.world=view.world;legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(minecraft,view);
        try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(view.world,view)){
            gui.init(minecraft,screen.width,screen.height);
        }
    }
    public static void offer(Screen screen,String identity,int[][] layout){
        if(screen.getClass()!=InventoryScreen.class||ACTIVE.containsKey((InventoryScreen)screen))return;
        var mc=Minecraft.getInstance();if(mc.player==null)return;
        try{
            var view=PlayerViewB173.of(mc.player);
            HandledScreen gui=PlayerMenuContextB173.reconstructClient(view,identity,layout);
            if(gui!=null)ACTIVE.put((InventoryScreen)screen,new OriginalInventoryScreenB173((InventoryScreen)screen,gui));
        }catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Original inventory UI could not attach; retaining native inventory",error);}
    }
    public static boolean active(Screen screen){var value=ACTIVE.get(screen);return value!=null&&!value.failed;}
    public static boolean usingOriginal(Screen screen){return active(screen);}
    public static boolean renderOriginal(InventoryScreen screen,GuiGraphics graphics,float partial,int mouseX,int mouseY){return render(screen,graphics,partial,mouseX,mouseY);}
    public static boolean containsModernControl(Screen screen,double x,double y){return screen instanceof InventoryScreen inventory&&controlsContain(inventory,x,y);}
    public static void clear(Screen screen){var value=ACTIVE.remove(screen);if(value!=null)value.restore();}
    public static void clearAll(){for(var value:ACTIVE.values())value.restore();ACTIVE.clear();}
    public static void prepare(Screen screen){var value=ACTIVE.get(screen);if(value!=null)value.prepare();}
    public static boolean hideLabels(InventoryScreen screen){var value=ACTIVE.get(screen);return value!=null&&!value.failed&&value.rendered;}
    public static boolean render(InventoryScreen screen,GuiGraphics graphics,float partial,int mouseX,int mouseY){
        var value=ACTIVE.get(screen);return value!=null&&value.draw(graphics,partial,mouseX,mouseY);
    }
    public static boolean controlsContain(InventoryScreen screen,double x,double y){
        var value=ACTIVE.get(screen);return value!=null&&!value.failed&&value.controlAt(x,y)!=null;
    }
    public static List<ControlGeometry> controlGeometry(Screen screen){
        var value=ACTIVE.get(screen);if(value==null||value.failed)return List.of();
        var result=new ArrayList<ControlGeometry>();
        for(Control control:new Control[]{value.handControl,value.bookControl})if(control.placement!=null){
            result.add(new ControlGeometry(control.id,control.placement.bounds(),control.placement.edge()));
        }
        return List.copyOf(result);
    }
    public static boolean mousePressed(Screen screen,double x,double y,int button){var value=ACTIVE.get(screen);return value!=null&&value.press(x,y,button);}
    public static boolean mouseDragged(Screen screen,double x,double y,int button){var value=ACTIVE.get(screen);return value!=null&&value.drag(x,y,button);}
    public static boolean mouseReleased(Screen screen,double x,double y,int button){var value=ACTIVE.get(screen);return value!=null&&value.release(x,y,button);}
    public static boolean keyPressed(Screen screen,int key){
        var value=ACTIVE.get(screen);if(value==null||value.drag==null)return false;
        if(key==GLFW.GLFW_KEY_ESCAPE){value.cancelDrag();value.prepare();}
        return true;
    }
    private void prepare(){
        rendered=false;if(failed)return;
        InventoryScreen screen=owner.get();var mc=Minecraft.getInstance();
        if(screen==null||mc.player==null||screen.getMenu()!=mc.player.inventoryMenu){fail();return;}
        for(Move move:moves)if(!move.unchanged()){fail();return;}
        if(screen.width<379&&screen.getRecipeBookComponent().isVisible()){cancelDrag();restore();handControl.placement=bookControl.placement=null;return;}
        if(bookMoved&&book!=null&&(book.getX()!=bookAppliedX||book.getY()!=bookAppliedY)){
            if(book.getX()==screen.getGuiLeft()+104&&book.getY()==screen.height/2-22){bookBeforeX=book.getX();bookBeforeY=book.getY();bookMoved=false;}
            else {fail();return;}
        }
        var mapping=PlayerInventoryLayoutB173.nativeSlots(mc.player.inventoryMenu,PlayerViewB173.of(mc.player));
        if(mapping.size()!=45){fail();return;}
        for(var child:screen.children())if(child instanceof AbstractWidget widget&&widget!=book&&widget.visible
                &&!(widget instanceof InventoryPanelB173.EquipmentWidget))for(var slot:gui.container.slots)
            if(overlap(screen.getGuiLeft()+slot.x-1,screen.getGuiTop()+slot.y-1,18,18,widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight())){fail();return;}
        for(int i=0;i<45;i++){
            var position=mapping.get(i);if(position.slot()!=moves.get(i).slot){fail();return;}
            moves.get(i).apply(position.x(),position.y());
        }
        if(!resolveControls(screen)){fail();return;}
        applyControls(screen);
    }
    private Rect inventory(InventoryScreen screen){return new Rect(screen.getGuiLeft(),screen.getGuiTop(),screen.getXSize(),screen.getYSize());}
    private List<Rect> obstacles(InventoryScreen screen,Control forControl){
        var result=new ArrayList<Rect>();
        for(var slot:screen.getMenu().slots)if(slot!=offhand&&slot.isActive())result.add(new Rect(screen.getGuiLeft()+slot.x-1,screen.getGuiTop()+slot.y-1,18,18));
        for(var child:screen.children())if(child instanceof AbstractWidget widget&&widget!=book&&widget.visible&&!(widget instanceof InventoryPanelB173.EquipmentWidget))
            result.add(new Rect(widget.getX(),widget.getY(),widget.getWidth(),widget.getHeight()));
        if(screen.getRecipeBookComponent().isVisible()){
            int bookX=(screen.width-147)/2-(screen.width<379?0:86),bookY=(screen.height-166)/2;
            result.add(new Rect(bookX-30,bookY,177,166));
        }
        if(!Minecraft.getInstance().player.getActiveEffects().isEmpty())result.add(new Rect(screen.getGuiLeft()+screen.getXSize()+2,screen.getGuiTop(),120,166));
        Control other=forControl==handControl?bookControl:handControl;
        if(other.placement!=null&&(other!=bookControl||book!=null))result.add(other.placement.bounds());
        return result;
    }
    private boolean resolveControls(InventoryScreen screen){
        Control first=drag!=null&&drag.control==handControl?bookControl:handControl;
        Control second=first==handControl?bookControl:handControl;
        if((first!=bookControl||book!=null)&&(drag==null||first.placement==null)){
            first.placement=InventoryControlDockB173.resolve(first.anchor,inventory(screen),CONTROL_SIZE,screen.width,screen.height,obstacles(screen,first));if(first.placement==null)return false;
        }
        if(second!=bookControl||book!=null){
            Anchor anchor=second.anchor;
            if(drag!=null&&drag.control==second&&drag.moved){
                anchor=InventoryControlDockB173.drop(drag.proposed,inventory(screen),CONTROL_SIZE,screen.width,screen.height,obstacles(screen,second));
                if(anchor==null){cancelDrag();anchor=second.anchor;}
            }
            second.placement=InventoryControlDockB173.resolve(anchor,inventory(screen),CONTROL_SIZE,screen.width,screen.height,obstacles(screen,second));
            if(second.placement==null)return false;
        }
        return true;
    }
    private void applyControls(InventoryScreen screen){
        Rect hand=handControl.placement.bounds();moves.get(45).apply(hand.x()+ITEM_INSET-screen.getGuiLeft(),hand.y()+ITEM_INSET-screen.getGuiTop());
        if(book!=null){Rect bounds=bookControl.placement.bounds();book.setPosition(bookAppliedX=bounds.x()+(CONTROL_SIZE-book.getWidth())/2,bookAppliedY=bounds.y()+(CONTROL_SIZE-book.getHeight())/2);bookMoved=true;}
    }
    private Control controlAt(double x,double y){
        for(Control control:new Control[]{handControl,bookControl})if(control.placement!=null&&(control!=bookControl||book!=null)){
            Rect r=control.placement.bounds();if(inside(x,y,r.x(),r.y(),r.width(),r.height()))return control;
        }
        return null;
    }
    private boolean center(Control control,double x,double y){
        Rect r=control.placement.bounds();return control==handControl?inside(x,y,r.x()+ITEM_INSET,r.y()+ITEM_INSET,16,16)
                :inside(x,y,r.x()+(CONTROL_SIZE-book.getWidth())/2,r.y()+(CONTROL_SIZE-book.getHeight())/2,book.getWidth(),book.getHeight());
    }
    private boolean press(double x,double y,int button){
        if(failed)return false;
        if(drag!=null){releaseMask|=1<<button;return true;}
        if(button!=0||PlayerInventoryClientB173.pending()||!Minecraft.getInstance().player.inventoryMenu.getCarried().isEmpty())return false;
        Control control=controlAt(x,y);if(control==null)return false;
        if(center(control,x,y)&&(control==bookControl||offhand.hasItem()))return false;
        drag=new Drag(control,x,y);releaseMask|=1;return true;
    }
    private boolean drag(double x,double y,int button){
        if(drag==null)return false;
        if(PlayerInventoryClientB173.pending()||!Minecraft.getInstance().player.inventoryMenu.getCarried().isEmpty()){cancelDrag();prepare();return true;}
        if(button==0){
            if(Math.hypot(x-drag.startX,y-drag.startY)>=3)drag.moved=true;
            if(drag.moved)drag.proposed=new Rect((int)Math.round(x-drag.grabX),(int)Math.round(y-drag.grabY),CONTROL_SIZE,CONTROL_SIZE);
            prepare();
        }
        return true;
    }
    private boolean release(double x,double y,int button){
        boolean captured=(releaseMask&(1<<button))!=0;
        if(button==0&&drag!=null){
            Drag completed=drag;drag=null;
            if(Math.hypot(x-completed.startX,y-completed.startY)>=3){completed.moved=true;completed.proposed=new Rect((int)Math.round(x-completed.grabX),(int)Math.round(y-completed.grabY),CONTROL_SIZE,CONTROL_SIZE);}
            InventoryScreen screen=owner.get();
            if(completed.moved&&screen!=null&&!failed&&!PlayerInventoryClientB173.pending()&&Minecraft.getInstance().player.inventoryMenu.getCarried().isEmpty()){
                Anchor anchor=InventoryControlDockB173.drop(completed.proposed,inventory(screen),CONTROL_SIZE,screen.width,screen.height,obstacles(screen,completed.control));
                if(anchor!=null){completed.control.anchor=anchor;InventoryControlPositionsB173.save(completed.control.id,anchor);}
            }
            prepare();captured=true;
        }
        releaseMask&=~(1<<button);return captured;
    }
    private void cancelDrag(){drag=null;}
    private boolean draw(GuiGraphics g,float partial,int mouseX,int mouseY){
        InventoryScreen screen=owner.get();if(screen==null||failed)return false;
        if(screen.width<379&&screen.getRecipeBookComponent().isVisible())return false;
        prepare();if(failed)return false;
        int centerX=(screen.width-gui.mcc$backgroundWidth())/2,centerY=(screen.height-gui.mcc$backgroundHeight())/2;
        int dx=screen.getGuiLeft()-centerX,dy=screen.getGuiTop()-centerY;
        var view=PlayerViewB173.of(Minecraft.getInstance().player);
        gui.width=screen.width;gui.height=screen.height;minecraft.world=view.world;
        legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$set$player(minecraft,view);
        // A separate pose stack contains even an original renderer that fails before balancing GL push/pop.
        GuiGraphics originalGraphics=new GuiGraphics(Minecraft.getInstance(),g.bufferSource());
        originalGraphics.pose().last().pose().set(g.pose().last().pose());
        originalGraphics.pose().last().normal().set(g.pose().last().normal());
        g.flush();g.enableScissor(screen.getGuiLeft(),screen.getGuiTop(),screen.getGuiLeft()+screen.getXSize(),screen.getGuiTop()+screen.getYSize());
        try{
            originalGraphics.pose().translate(dx,dy,0);ClientGuiB173.begin(originalGraphics,centerX,centerY,gui.mcc$backgroundWidth(),gui.mcc$backgroundHeight());
            try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(view.world,view)){
                legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$scope(minecraft,()->gui.render(mouseX-dx,mouseY-dy,partial));
                theme=ClientGuiB173.background();
            }finally{ClientGuiB173.end();}
            rendered=true;
        }catch(RuntimeException|LinkageError error){
            fail();org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn("Original inventory drawing failed; restoring native inventory",error);
        }finally{originalGraphics.flush();g.disableScissor();}
        if(!rendered)return false;
        for(Control control:new Control[]{handControl,bookControl})if(control.placement!=null&&(control!=bookControl||book!=null))InventoryControlFrameB173.draw(g,theme,control.placement,inventory(screen));
        var source=gui.container.slots.stream().filter(slot->slot.inventory==view.inventory&&slot.index>=0&&slot.index<36).findFirst().orElse(null);
        InventoryControlFrameB173.drawSlot(g,theme,screen.getGuiLeft()+offhand.x,screen.getGuiTop()+offhand.y,source==null?-1:source.x,source==null?-1:source.y);
        return true;
    }
    private void fail(){failed=true;rendered=false;restore();}
    private void restore(){cancelDrag();for(Move move:moves){move.restore();move.appliedX=move.slot.x;move.appliedY=move.slot.y;}
        if(bookMoved&&book!=null&&book.getX()==bookAppliedX&&book.getY()==bookAppliedY)book.setPosition(bookBeforeX,bookBeforeY);bookMoved=false;
    }
    private static boolean inside(double x,double y,int a,int b,int w,int h){return x>=a&&x<a+w&&y>=b&&y<b+h;}
    private static boolean overlap(int x,int y,int w,int h,int a,int b,int c,int d){return x<a+c&&x+w>a&&y<b+d&&y+h>b;}
}
