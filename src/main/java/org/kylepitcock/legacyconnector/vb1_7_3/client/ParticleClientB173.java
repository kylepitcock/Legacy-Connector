package org.kylepitcock.legacyconnector.vb1_7_3.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Native particle lifetime/collision integration drawing each original billboard callback and its selected legacy sprite. */
public final class ParticleClientB173 {
    private ParticleClientB173(){}
    private static final java.util.Set<String> REJECTED=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static void reject(legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle original,String reason){if(REJECTED.add(original.getClass().getName()+":"+reason))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3/Particles").warn("Original particle {} not drawn: {}",original.getClass().getName(),reason);}
    private static ResourceLocation texture(legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle original){
        int group=original.getGroup(),index=original.textureId;
        if(group==0)return index>=0&&index<=7?ResourceLocation.withDefaultNamespace("generic_"+index):null;
        var png=AssetsB173.particleSprite(group,index);if(png==null)return null;
        String path=png.getPath();return ResourceLocation.fromNamespaceAndPath(png.getNamespace(),path.substring("textures/".length(),path.length()-4));
    }
    private static TextureAtlasSprite admittedSprite(legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle original){
        var texture=texture(original);if(texture==null){reject(original,"unsupported sheet/cell "+original.getGroup()+":"+original.textureId);return null;}
        var sprite=Minecraft.getInstance().getTextureAtlas(original.getGroup()==0?TextureAtlas.LOCATION_PARTICLES:TextureAtlas.LOCATION_BLOCKS).apply(texture);
        if(sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())){reject(original,"sprite was not stitched: "+texture);return null;}return sprite;
    }
    public static void install(){ParticlesB173.installClient(original->{var mc=Minecraft.getInstance();if(mc.level!=null&&admittedSprite(original)!=null)mc.particleEngine.add(new Host(mc.level,original));});}
    private static final ParticleRenderType BLOCKS=new SheetType(TextureAtlas.LOCATION_BLOCKS);
    private static final ParticleRenderType PARTICLES=new SheetType(TextureAtlas.LOCATION_PARTICLES);
    private record SheetType(ResourceLocation atlas) implements ParticleRenderType {
        @Override public BufferBuilder begin(Tesselator t,TextureManager textures){RenderSystem.depthMask(true);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,atlas);return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}
    }
    private static final class Host extends net.minecraft.client.particle.Particle implements ParticleHostB173 {
        private final legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle original;
        Host(ClientLevel level,legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle original){super(level,original.x,original.y-original.standingEyeHeight,original.z);this.original=original;original.world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);original.mcc$particleHost=this;setSize(original.width,original.height);push();}
        private void push(){setSize(original.width,original.height);setPos(original.x,original.y-original.standingEyeHeight,original.z);xd=original.velocityX;yd=original.velocityY;zd=original.velocityZ;onGround=original.onGround;}
        private void pull(){original.setPosition(x,y+original.standingEyeHeight,z);original.velocityX=xd;original.velocityY=yd;original.velocityZ=zd;original.onGround=onGround;}
        @Override public void tick(){original.tick();push();if(original.dead||admittedSprite(original)==null)remove();}
        @Override public void moveOriginal(legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle particle,double x,double y,double z){
            push();
            var wanted=new net.minecraft.world.phys.Vec3(x,y,z);
            var resolved=particle.noClip?wanted:net.minecraft.world.entity.Entity.collideBoundingBox(null,wanted,getBoundingBox(),level,java.util.List.of());
            setBoundingBox(getBoundingBox().move(resolved));setLocationFromBoundingbox();
            onGround=y!=resolved.y&&y<0;
            if(x!=resolved.x)xd=0;if(y!=resolved.y)yd=0;if(z!=resolved.z)zd=0;
            particle.horizontalCollision=x!=resolved.x||z!=resolved.z;particle.verticalCollision=y!=resolved.y;particle.hasCollided=particle.horizontalCollision||particle.verticalCollision;
            pull();
        }
        @Override public ParticleRenderType getRenderType(){return original.getGroup()==0?PARTICLES:BLOCKS;}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){
            int index=original.textureId;
            TextureAtlasSprite sprite=admittedSprite(original);if(sprite==null){remove();return;}
            var pos=camera.getPosition();legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle.xOffset=pos.x;legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle.yOffset=pos.y;legacyconnector.vb1_7_3.net.minecraft.client.particle.Particle.zOffset=pos.z;
            double yaw=Math.toRadians(camera.getYRot()),pitch=Math.toRadians(camera.getXRot());float rx=(float)Math.cos(yaw),rz=(float)Math.sin(yaw),rxz=(float)Math.cos(pitch),ryz=(float)(-rz*Math.sin(pitch)),rxy=(float)(rx*Math.sin(pitch));
            legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.beginCapture();
            java.util.List<legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.Vertex> vertices;
            try{original.render(legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.INSTANCE,partial,rx,rxz,rz,ryz,rxy);}finally{vertices=legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.endCapture();}
            if(vertices.size()%4!=0)throw new IllegalStateException("Original particle emitted incomplete quad");
            for(var v:vertices){int color=v.color();float u=(float)(v.u()*16-(index%16)),vv=(float)(v.v()*16-(index/16));buffer.addVertex((float)v.x(),(float)v.y(),(float)v.z()).setUv(sprite.getU(u),sprite.getV(vv)).setColor(color&255,(color>>8)&255,(color>>16)&255,(color>>>24)&255).setLight(0xF000F0);}
        }
    }
}
