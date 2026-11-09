package legacyconnector.vb1_7_3.net.minecraft.client.util;

/** Original Beta minimum GUI area and ceil rounding. */
public class ScreenScaler {
    private int scaledWidth,scaledHeight;
    public double rawScaledWidth,rawScaledHeight;
    public int scaleFactor=1;
    public ScreenScaler(legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions options,int width,int height){
        int limit=options.guiScale==0?1000:options.guiScale;
        while(scaleFactor<limit&&width/(scaleFactor+1)>=320&&height/(scaleFactor+1)>=240)scaleFactor++;
        rawScaledWidth=(double)width/scaleFactor;rawScaledHeight=(double)height/scaleFactor;
        scaledWidth=(int)Math.ceil(rawScaledWidth);scaledHeight=(int)Math.ceil(rawScaledHeight);
    }
    public int getScaledWidth(){return scaledWidth;}
    public int getScaledHeight(){return scaledHeight;}
}
