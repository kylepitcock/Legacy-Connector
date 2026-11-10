package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173;
import org.kylepitcock.legacyconnector.vb1_7_3.InventoryControlDockB173.Edge;

/** Container-texture frames with an open shared edge where a native control docks. */
public final class InventoryControlFrameB173 {
    private static final int BORDER=4;
    private static final ClientGuiB173.Background VANILLA=new ClientGuiB173.Background(
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png"),0,0,176,166);
    private InventoryControlFrameB173() {}

    public static void draw(GuiGraphics graphics,ClientGuiB173.Background theme,InventoryControlDockB173.Placement placement,InventoryControlDockB173.Rect inventory) {
        var source=theme==null?VANILLA:theme;
        var box=placement.bounds();int x=box.x(),y=box.y(),w=box.width(),h=box.height();
        Edge shared=switch(placement.edge()) {
            case LEFT->Edge.RIGHT;case RIGHT->Edge.LEFT;case TOP->Edge.BOTTOM;case BOTTOM->Edge.TOP;case FREE->Edge.FREE;
        };
        int left=shared==Edge.LEFT?0:BORDER,top=shared==Edge.TOP?0:BORDER;
        int right=shared==Edge.RIGHT?0:BORDER,bottom=shared==Edge.BOTTOM?0:BORDER;
        piece(graphics,source,x+left,y+top,w-left-right,h-top-bottom,BORDER,BORDER,1,1);
        boolean topAligned=y==inventory.y(),bottomAligned=(long)y+h==(long)inventory.y()+inventory.height();
        boolean leftAligned=x==inventory.x(),rightAligned=(long)x+w==(long)inventory.x()+inventory.width();
        // Continue a shared outer edge through the parent's old corner instead of retaining its bevel.
        if(shared!=Edge.TOP){
            int start=shared==Edge.LEFT&&topAligned?0:BORDER,end=shared==Edge.RIGHT&&topAligned?0:BORDER;
            piece(graphics,source,x+start,y,w-start-end,BORDER,BORDER,0,source.width()-2*BORDER,BORDER);
        }
        if(shared!=Edge.BOTTOM){
            int start=shared==Edge.LEFT&&bottomAligned?0:BORDER,end=shared==Edge.RIGHT&&bottomAligned?0:BORDER;
            piece(graphics,source,x+start,y+h-BORDER,w-start-end,BORDER,BORDER,source.height()-BORDER,source.width()-2*BORDER,BORDER);
        }
        if(shared!=Edge.LEFT){
            int start=shared==Edge.TOP&&leftAligned?0:BORDER,end=shared==Edge.BOTTOM&&leftAligned?0:BORDER;
            piece(graphics,source,x,y+start,BORDER,h-start-end,0,BORDER,BORDER,source.height()-2*BORDER);
        }
        if(shared!=Edge.RIGHT){
            int start=shared==Edge.TOP&&rightAligned?0:BORDER,end=shared==Edge.BOTTOM&&rightAligned?0:BORDER;
            piece(graphics,source,x+w-BORDER,y+start,BORDER,h-start-end,source.width()-BORDER,BORDER,BORDER,source.height()-2*BORDER);
        }
        if(shared!=Edge.LEFT&&shared!=Edge.TOP)piece(graphics,source,x,y,BORDER,BORDER,0,0,BORDER,BORDER);
        if(shared!=Edge.RIGHT&&shared!=Edge.TOP)piece(graphics,source,x+w-BORDER,y,BORDER,BORDER,source.width()-BORDER,0,BORDER,BORDER);
        if(shared!=Edge.LEFT&&shared!=Edge.BOTTOM)piece(graphics,source,x,y+h-BORDER,BORDER,BORDER,0,source.height()-BORDER,BORDER,BORDER);
        if(shared!=Edge.RIGHT&&shared!=Edge.BOTTOM)piece(graphics,source,x+w-BORDER,y+h-BORDER,BORDER,BORDER,source.width()-BORDER,source.height()-BORDER,BORDER,BORDER);
        // At an inward corner, continue both edge bands instead of leaving the parent's border as a cap.
        if(shared==Edge.TOP){
            if(!leftAligned)join(graphics,source,x,y,Edge.LEFT,Edge.BOTTOM);
            if(!rightAligned)join(graphics,source,x+w-BORDER,y,Edge.RIGHT,Edge.BOTTOM);
        }else if(shared==Edge.BOTTOM){
            if(!leftAligned)join(graphics,source,x,y+h-BORDER,Edge.LEFT,Edge.TOP);
            if(!rightAligned)join(graphics,source,x+w-BORDER,y+h-BORDER,Edge.RIGHT,Edge.TOP);
        }else if(shared==Edge.LEFT){
            if(!topAligned)join(graphics,source,x,y,Edge.RIGHT,Edge.TOP);
            if(!bottomAligned)join(graphics,source,x,y+h-BORDER,Edge.RIGHT,Edge.BOTTOM);
        }else if(shared==Edge.RIGHT){
            if(!topAligned)join(graphics,source,x+w-BORDER,y,Edge.LEFT,Edge.TOP);
            if(!bottomAligned)join(graphics,source,x+w-BORDER,y+h-BORDER,Edge.LEFT,Edge.BOTTOM);
        }
    }

    private static void join(GuiGraphics graphics,ClientGuiB173.Background source,int x,int y,Edge horizontal,Edge vertical) {
        for(int row=0;row<BORDER;row++)for(int column=0;column<BORDER;column++){
            int across=horizontal==Edge.LEFT?column:BORDER-1-column;
            int down=vertical==Edge.TOP?row:BORDER-1-row;
            Edge edge=across>=down?horizontal:vertical;int depth=Math.max(across,down);
            int u=switch(edge){case LEFT->depth;case RIGHT->source.width()-1-depth;default->BORDER;};
            int v=switch(edge){case TOP->depth;case BOTTOM->source.height()-1-depth;default->BORDER;};
            piece(graphics,source,x+column,y+row,1,1,u,v,1,1);
        }
    }

    public static void drawSlot(GuiGraphics graphics,ClientGuiB173.Background theme,int itemX,int itemY,int originalSlotX,int originalSlotY) {
        var source=theme==null?VANILLA:theme;
        int u=theme==null?7:originalSlotX-1,v=theme==null?83:originalSlotY-1;
        if(u<0||v<0||u+18>source.width()||v+18>source.height()){source=VANILLA;u=7;v=83;}
        piece(graphics,source,itemX-1,itemY-1,18,18,u,v,18,18);
    }

    private static void piece(GuiGraphics graphics,ClientGuiB173.Background source,int x,int y,int width,int height,int u,int v,int sourceWidth,int sourceHeight) {
        if(width<=0||height<=0)return;
        // The original Gui.drawTexturedModalRect API uses a fixed 256-unit texture coordinate scale.
        graphics.blit(source.texture(),x,y,width,height,(float)(source.u()+u),(float)(source.v()+v),sourceWidth,sourceHeight,256,256);
    }
}
