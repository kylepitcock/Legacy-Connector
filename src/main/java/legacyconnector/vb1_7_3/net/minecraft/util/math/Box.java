package legacyconnector.vb1_7_3.net.minecraft.util.math;
import legacyconnector.vb1_7_3.net.minecraft.util.hit.HitResult;
import net.minecraft.world.phys.AABB;
public class Box {
    public double minX, minY, minZ, maxX, maxY, maxZ;
    public Box(double a,double b,double c,double d,double e,double f) { set(a,b,c,d,e,f); }
    public static Box create(double a,double b,double c,double d,double e,double f) { return new Box(a,b,c,d,e,f); }
    public static Box createCached(double a,double b,double c,double d,double e,double f) { return create(a,b,c,d,e,f); }
    public Box set(double a,double b,double c,double d,double e,double f) { minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;return this; }
    public AABB mcc$box() { return new AABB(minX,minY,minZ,maxX,maxY,maxZ); }
    private static Box of(AABB b) { return create(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ); }
    public Box stretch(double x,double y,double z) { return of(mcc$box().expandTowards(x,y,z)); }
    public Box expand(double x,double y,double z) { return of(mcc$box().inflate(x,y,z)); }
    public Box offset(double x,double y,double z) { return of(mcc$box().move(x,y,z)); }
    public Box translate(double x,double y,double z) { return set(minX+x,minY+y,minZ+z,maxX+x,maxY+y,maxZ+z); }
    public boolean intersects(Box b) { return mcc$box().intersects(b.mcc$box()); }
    public boolean contains(Vec3d p) { return mcc$box().contains(p.x,p.y,p.z); }
    public Box copy() { return of(mcc$box()); }
    public double getAverageSideLength(){return ((maxX-minX)+(maxY-minY)+(maxZ-minZ))/3.0;}
    public HitResult raycast(Vec3d from, Vec3d to) {
        var hit=mcc$box().clip(new net.minecraft.world.phys.Vec3(from.x,from.y,from.z), new net.minecraft.world.phys.Vec3(to.x,to.y,to.z));
        return hit.map(p -> new HitResult(0,0,0,0,Vec3d.create(p.x,p.y,p.z))).orElse(null);
    }
}
