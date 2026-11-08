package legacyconnector.vb1_7_3.net.minecraft.client.render.entity;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.model.BipedEntityModel;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;

/** Beta biped renderer's hand attachment transform; original subclasses retain their virtual render hooks. */
public class UndeadEntityRenderer extends LivingEntityRenderer {
    protected BipedEntityModel entityModel;
    public UndeadEntityRenderer(BipedEntityModel model,float radius){super(model,radius);entityModel=model;}
    @Override protected void renderMore(LivingEntity entity,float partial) {
        var stack=entity.getHeldItem();if(stack==null)return;
        GL11.glPushMatrix();
        try {
            entityModel.rightArm.transform(.0625F);GL11.glTranslatef(-.0625F,.4375F,.0625F);
            var block=stack.itemId>=0&&stack.itemId<256?Block.BLOCKS[stack.itemId]:null;
            int renderType=block==null?-1:block.getRenderType();
            if(renderType==0||renderType==13||renderType==10||renderType==11||renderType==16){
                GL11.glTranslatef(0,.1875F,-.3125F);GL11.glRotatef(20,1,0,0);GL11.glRotatef(45,0,1,0);GL11.glScalef(.375F,-.375F,.375F);
            }else if(stack.getItem()!=null&&stack.getItem().isHandheld()){
                GL11.glTranslatef(0,.1875F,0);GL11.glScalef(.625F,-.625F,.625F);GL11.glRotatef(-100,1,0,0);GL11.glRotatef(45,0,1,0);
            }else{
                GL11.glTranslatef(.25F,.1875F,-.1875F);GL11.glScalef(.375F,.375F,.375F);GL11.glRotatef(60,0,0,1);GL11.glRotatef(-90,1,0,0);GL11.glRotatef(20,0,0,1);
            }
            new legacyconnector.vb1_7_3.net.minecraft.client.render.item.HeldItemRenderer().renderItem(entity,stack);
        }finally{GL11.glPopMatrix();}
    }
}
