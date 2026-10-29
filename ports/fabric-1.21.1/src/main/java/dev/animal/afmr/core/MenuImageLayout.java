package dev.animal.afmr.core;

/** Image placement in GUI coordinates; fill crops the source instead of drawing offscreen. */
public record MenuImageLayout(int x,int y,int width,int height,int u,int v,int sourceWidth,int sourceHeight) {
    public enum Fit { FILL, FIT, STRETCH }

    public static MenuImageLayout calculate(int imageWidth,int imageHeight,int width,int height,Fit fit){
        if(imageWidth<1||imageHeight<1||width<1||height<1)throw new IllegalArgumentException("Positive image and viewport dimensions required");
        if(fit==Fit.FILL){
            double scale=Math.max((double)width/imageWidth,(double)height/imageHeight);
            int sw=Math.max(1,Math.min(imageWidth,(int)Math.round(width/scale)));
            int sh=Math.max(1,Math.min(imageHeight,(int)Math.round(height/scale)));
            return new MenuImageLayout(0,0,width,height,(imageWidth-sw)/2,(imageHeight-sh)/2,sw,sh);
        }
        if(fit==Fit.FIT){
            double scale=Math.min((double)width/imageWidth,(double)height/imageHeight);
            int w=Math.max(1,Math.min(width,(int)Math.round(imageWidth*scale)));
            int h=Math.max(1,Math.min(height,(int)Math.round(imageHeight*scale)));
            return new MenuImageLayout((width-w)/2,(height-h)/2,w,h,0,0,imageWidth,imageHeight);
        }
        return new MenuImageLayout(0,0,width,height,0,0,imageWidth,imageHeight);
    }
}
