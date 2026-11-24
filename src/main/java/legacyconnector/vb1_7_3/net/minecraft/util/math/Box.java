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
    /** Beta's {@code func_1177_b}: the box shrunk by the amounts on each side (a copy; an amount larger than half the box leaves it unchanged on that axis). */
    public Box contract(double x,double y,double z) { return of(mcc$box().inflate(-x,-y,-z)); }
    /** Beta's {@code setBB}: takes over the corners of another box. */
    public void clone(Box other) { set(other.minX,other.minY,other.minZ,other.maxX,other.maxY,other.maxZ); }
    /** Beta's {@code calculateXOffset}: how far this box can move along X (by {@code d}) before it touches the other. */
    public double getXOffset(Box other,double d) {
        if(other.maxY<=minY||other.minY>=maxY||other.maxZ<=minZ||other.minZ>=maxZ)return d;
        if(d>0.0&&other.maxX<=minX){double e=minX-other.maxX;if(e<d)d=e;}
        if(d<0.0&&other.minX>=maxX){double e=maxX-other.minX;if(e>d)d=e;}
        return d;
    }
    public double getYOffset(Box other,double d) {
        if(other.maxX<=minX||other.minX>=maxX||other.maxZ<=minZ||other.minZ>=maxZ)return d;
        if(d>0.0&&other.maxY<=minY){double e=minY-other.maxY;if(e<d)d=e;}
        if(d<0.0&&other.minY>=maxY){double e=maxY-other.minY;if(e>d)d=e;}
        return d;
    }
    public double getZOffset(Box other,double d) {
        if(other.maxX<=minX||other.minX>=maxX||other.maxY<=minY||other.minY>=maxY)return d;
        if(d>0.0&&other.maxZ<=minZ){double e=minZ-other.maxZ;if(e<d)d=e;}
        if(d<0.0&&other.minZ>=maxZ){double e=maxZ-other.minZ;if(e>d)d=e;}
        return d;
    }
    /** Beta pooled its boxes; ours are plain objects, so clearing the pool has nothing to do. */
    public static void clearCache() {}
    public static void resetCacheCount() {}
    /** Beta tests all six faces, including exits when the segment starts inside the box. */
    public HitResult raycast(Vec3d from, Vec3d to) {
        Vec3d[] candidates={from.interpolateByX(to,minX),from.interpolateByX(to,maxX),
                from.interpolateByY(to,minY),from.interpolateByY(to,maxY),
                from.interpolateByZ(to,minZ),from.interpolateByZ(to,maxZ)};
        int[] sides={4,5,0,1,2,3};
        Vec3d nearest=null;int side=-1;
        for(int i=0;i<candidates.length;i++){
            Vec3d point=candidates[i];if(point==null)continue;
            boolean onFace=switch(i/2){
                case 0 -> point.y>=minY&&point.y<=maxY&&point.z>=minZ&&point.z<=maxZ;
                case 1 -> point.x>=minX&&point.x<=maxX&&point.z>=minZ&&point.z<=maxZ;
                default -> point.x>=minX&&point.x<=maxX&&point.y>=minY&&point.y<=maxY;
            };
            // Strictly closer preserves original X, then Y, then Z precedence at edges/corners.
            if(onFace&&(nearest==null||from.squaredDistanceTo(point)<from.squaredDistanceTo(nearest))){nearest=point;side=sides[i];}
        }
        return nearest==null?null:new HitResult(0,0,0,side,nearest);
    }
}
