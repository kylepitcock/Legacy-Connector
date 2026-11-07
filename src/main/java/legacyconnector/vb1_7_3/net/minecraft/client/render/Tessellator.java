package legacyconnector.vb1_7_3.net.minecraft.client.render;

import java.util.*;

/** Beta immediate-mode vertices captured per chunk worker, then baked into modern quads. */
public class Tessellator {
    public static final Tessellator INSTANCE=new Tessellator();
    public record Vertex(double x,double y,double z,double u,double v,int color){}
    private static final class Capture {
        final List<Vertex> vertices=new ArrayList<>();double u,v,x,y,z;int color=-1;
    }
    private static final ThreadLocal<Capture> CAPTURE=new ThreadLocal<>();
    public Tessellator(){}
    public Tessellator(int size){}
    public static void beginCapture(){CAPTURE.set(new Capture());}
    public static List<Vertex> endCapture(){var c=CAPTURE.get();CAPTURE.remove();return c==null?List.of():List.copyOf(c.vertices);}
    public void startQuads(){}
    public void start(int mode){if(mode!=7)throw new UnsupportedOperationException("Beta tessellation mode "+mode);}
    public void draw(){}
    public void texture(double u,double v){var c=CAPTURE.get();if(c!=null){c.u=u;c.v=v;}}
    public void vertex(double x,double y,double z,double u,double v){texture(u,v);vertex(x,y,z);}
    public void vertex(double x,double y,double z){var c=CAPTURE.get();if(c!=null)c.vertices.add(new Vertex(x+c.x,y+c.y,z+c.z,c.u,c.v,c.color));}
    public void color(float r,float g,float b){color(r,g,b,1);}
    public void color(float r,float g,float b,float a){color((int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255));}
    public void color(int r,int g,int b){color(r,g,b,255);}
    public void color(int r,int g,int b,int a){var c=CAPTURE.get();if(c!=null)c.color=(clamp(a)<<24)|(clamp(b)<<16)|(clamp(g)<<8)|clamp(r);}
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
    public void color(int rgb){color(rgb,255);}
    public void color(int rgb,int a){color((rgb>>16)&255,(rgb>>8)&255,rgb&255,a);}
    public void disableColor(){}
    public void normal(float x,float y,float z){}
    public void setOffset(double x,double y,double z){var c=CAPTURE.get();if(c!=null){c.x=x;c.y=y;c.z=z;}}
    public void translate(float x,float y,float z){var c=CAPTURE.get();if(c!=null){c.x+=x;c.y+=y;c.z+=z;}}
}
