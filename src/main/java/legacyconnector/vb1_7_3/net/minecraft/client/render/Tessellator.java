package legacyconnector.vb1_7_3.net.minecraft.client.render;

import java.util.*;
import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/** Beta immediate-mode vertices captured per chunk worker, then baked into modern quads. */
public class Tessellator {
    public static final Tessellator INSTANCE=new Tessellator();
    /** A captured vertex; {@code sheet} is the texture a Forge renderer bound for it ({@code MinecraftForgeClient.bindTexture}), null for the block's own. */
    public record Vertex(double x,double y,double z,double u,double v,int color,String sheet){
        public Vertex(double x,double y,double z,double u,double v,int color){this(x,y,z,u,v,color,null);}
    }
    private static final class Capture {
        final List<Vertex> vertices=new ArrayList<>();
        final List<RendererContextB173.Normal> normals=new ArrayList<>();
        int mode=7;double u,v,x,y,z;int color=-1;float nx,ny,nz;boolean hasNormals;String sheet;boolean untextured;
    }
    private static final ThreadLocal<Capture> CAPTURE=new ThreadLocal<>();
    public Tessellator(){}
    public Tessellator(int size){}
    public static void beginCapture(){CAPTURE.set(new Capture());}
    /** Forge's texture switch while a world block is captured: the following vertices use that sheet ({@code /terrain.png} after an unbind). */
    public static void mcc$sheet(String sheet){var c=CAPTURE.get();if(c!=null)c.sheet=sheet;}
    /** The sheet name of vertices drawn with texturing off ({@code glDisable(GL_TEXTURE_2D)}: a plain coloured face). */
    public static final String UNTEXTURED="#untextured";
    public static void mcc$textured(boolean textured){var c=CAPTURE.get();if(c!=null)c.untextured=!textured;}
    public static List<Vertex> endCapture(){var c=CAPTURE.get();CAPTURE.remove();return c==null?List.of():List.copyOf(c.vertices);}
    public void startQuads(){start(7);}
    public void start(int mode){if(mode!=7&&mode!=5&&mode!=1&&mode!=2&&mode!=3)throw new UnsupportedOperationException("Beta tessellation mode "+mode);if(RendererContextB173.active()||GuiDrawB173.drawing()){var c=new Capture();c.mode=mode;CAPTURE.set(c);}}
    private static RendererContextB173.Vertex vertex(Vertex v,RendererContextB173.Normal n){int color=v.color();int argb=(color&0xff00ff00)|((color&255)<<16)|((color>>16)&255);return new RendererContextB173.Vertex(v.x(),v.y(),v.z(),v.u(),v.v(),argb,n.x(),n.y(),n.z());}
    public void draw(){
        // a world block renderer that flushes mid-block (Forge's second pass) still adds to the same chunk geometry
        if(org.kylepitcock.legacyconnector.vb1_7_3.BlockRenderB173.capturing())return;
        if(RendererContextB173.active()){
        var c=CAPTURE.get();CAPTURE.remove();if(c==null)return;
        // Without a normal array, OpenGL uses the current normal when the buffered batch is drawn.
        // Tessellator.normal instead enables its normal array and records each vertex's explicit normal.
        var currentNormal=RendererContextB173.normal();var recorded=new ArrayList<RendererContextB173.Vertex>(c.vertices.size());
        for(int i=0;i<c.vertices.size();i++)recorded.add(vertex(c.vertices.get(i),c.hasNormals?c.normals.get(i):currentNormal));
        if(c.mode==5){
            for(int i=2;i<c.vertices.size();i++){
                int a=(i%2==0)?i-2:i-1,b=(i%2==0)?i-1:i-2;
                var end=recorded.get(i);
                RendererContextB173.quad(List.of(recorded.get(a),recorded.get(b),end,end));
            }return;
        }
        if(c.mode!=7){
            int n=c.vertices.size(),step=c.mode==1?2:1;
            for(int i=0;i+1<n;i+=step)RendererContextB173.line(recorded.get(i),recorded.get(i+1));
            if(c.mode==2&&n>1)RendererContextB173.line(recorded.get(n-1),recorded.getFirst());
            return;
        }
        if(c.vertices.size()%4!=0)throw new IllegalStateException("Incomplete Beta quad");
        for(int i=0;i<c.vertices.size();i+=4){var vertices=new ArrayList<RendererContextB173.Vertex>();for(var v:recorded.subList(i,i+4))vertices.add(v);RendererContextB173.quad(vertices);}
    }else{
        // A legacy GUI drawing with immediate-mode quads (a loading screen's dirt tiles and progress bar).
        var c=CAPTURE.get();CAPTURE.remove();if(c==null||c.mode!=7||c.vertices.size()%4!=0)return;
        for(int i=0;i<c.vertices.size();i+=4){
            double[] x=new double[4],y=new double[4],u=new double[4],v=new double[4];int[] argb=new int[4];
            for(int k=0;k<4;k++){var vx=c.vertices.get(i+k);x[k]=vx.x();y[k]=vx.y();u[k]=vx.u();v[k]=vx.v();int color=vx.color();argb[k]=(color&0xff00ff00)|((color&255)<<16)|((color>>16)&255);}
            GuiDrawB173.quad(x,y,u,v,argb);
        }
    }}
    public void texture(double u,double v){var c=CAPTURE.get();if(c!=null){c.u=u;c.v=v;}}
    public void vertex(double x,double y,double z,double u,double v){texture(u,v);vertex(x,y,z);}
    public void vertex(double x,double y,double z){var c=CAPTURE.get();if(c!=null){c.vertices.add(new Vertex(x+c.x,y+c.y,z+c.z,c.u,c.v,c.color,c.untextured?UNTEXTURED:c.sheet));if(RendererContextB173.active())c.normals.add(new RendererContextB173.Normal(c.nx,c.ny,c.nz));}}
    public void color(float r,float g,float b){color(r,g,b,1);}
    public void color(float r,float g,float b,float a){color((int)(r*255),(int)(g*255),(int)(b*255),(int)(a*255));}
    public void color(int r,int g,int b){color(r,g,b,255);}
    public void color(int r,int g,int b,int a){var c=CAPTURE.get();if(c!=null)c.color=(clamp(a)<<24)|(clamp(b)<<16)|(clamp(g)<<8)|clamp(r);}
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
    public void color(int rgb){color(rgb,255);}
    public void color(int rgb,int a){color((rgb>>16)&255,(rgb>>8)&255,rgb&255,a);}
    public void disableColor(){}
    public void normal(float x,float y,float z){var c=CAPTURE.get();if(c!=null){c.hasNormals=true;c.nx=x;c.ny=y;c.nz=z;}}
    public void setOffset(double x,double y,double z){var c=CAPTURE.get();if(c!=null){c.x=x;c.y=y;c.z=z;}}
    public void translate(float x,float y,float z){var c=CAPTURE.get();if(c!=null){c.x+=x;c.y+=y;c.z+=z;}}
}
