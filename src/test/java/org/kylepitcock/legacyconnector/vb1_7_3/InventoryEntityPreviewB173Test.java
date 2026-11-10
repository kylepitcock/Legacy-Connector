package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import legacyconnector.vb1_7_3.net.minecraft.client.render.entity.EntityRenderDispatcher;
import legacyconnector.vb1_7_3.net.minecraft.client.render.platform.Lighting;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;
import org.junit.jupiter.api.Test;

class InventoryEntityPreviewB173Test {
    @Test void originalInventoryPreviewLinksAndRetainsItsPoseAtTheGuiBoundary() throws Exception {
        assertSame(EntityRenderDispatcher.INSTANCE,EntityRenderDispatcher.class.getField("INSTANCE").get(null));
        var actor=new Entity(null) {};
        var calls=new ArrayList<String>();
        var sink=(GuiDrawB173.Sink)Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{GuiDrawB173.Sink.class},
                (proxy,method,args)->{
                    calls.add(method.getName());
                    if(method.getName().equals("entity")){
                        assertSame(actor,args[0]);assertEquals(1F,args[5]);
                    }
                    if(method.getName().equals("rotate"))assertEquals(180F,args[0]);
                    return null;
                });
        GuiDrawB173.withSink(sink,()->{
            GL11.glPushMatrix();GL11.glTranslatef(33,75,50);GL11.glScalef(-30,30,30);
            GL11.glRotatef(180,0,0,1);Lighting.turnOn();
            EntityRenderDispatcher.INSTANCE.yaw=180;
            EntityRenderDispatcher.INSTANCE.render(actor,0,0,0,0,1);
            Lighting.turnOff();GL11.glPopMatrix();
        });
        assertEquals(List.of("push","translate","scale","rotate","lighting","entity","lighting","pop"),calls);
    }
}
