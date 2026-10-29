package dev.animal.afmr.core;

/** Widget dimensions in unscaled GUI units; small viewports cap the requested scale. */
public record NowPlayingLayout(int width, int height, int iconSize, int textX, double scale) {
    public static float anchorX(Library.NowPlayingStyle style,Library.WidgetCorner corner,double width,int screenWidth){
        boolean right=corner.resolved(style).right();
        int margin=corner==Library.WidgetCorner.DEFAULT&&style!=Library.NowPlayingStyle.AFMR?0:8;
        return (float)Math.max(0,right?screenWidth-margin-width:margin);
    }
    public static float anchorY(Library.NowPlayingStyle style,Library.WidgetCorner corner,double width,double height,int screenWidth,int screenHeight){
        if(!corner.resolved(style).bottom())return corner==Library.WidgetCorner.DEFAULT&&style!=Library.NowPlayingStyle.AFMR?0:8;
        boolean besideHotbar=width+16<=(screenWidth-182)/2.0;
        return (float)Math.max(8,screenHeight-(besideHotbar?12:58)-height);
    }
    /** A 24 GUI-pixel/second round trip with a readable pause at either end. */
    public static double scrollOffset(int textWidth,int availableWidth,double age){
        int overflow=Math.max(0,textWidth-availableWidth);
        if(overflow==0||!Double.isFinite(age)||age<=0)return 0;
        double pause=1.5,travel=overflow/24.0,half=pause+travel;
        double phase=age%(half*2);
        if(phase<half)return Math.min(overflow,Math.max(0,phase-pause)*24);
        return overflow-Math.min(overflow,Math.max(0,phase-half-pause)*24);
    }

    public static NowPlayingLayout fit(int requestedWidth, double requestedScale, int rows, boolean artwork,
                                       boolean hybrid, int availableWidth, int availableHeight) {
        int width=Math.clamp(requestedWidth,140,600);
        int icon=artwork?(hybrid?24:40):0;
        int height=Math.max(hybrid?30:28,Math.max(icon,rows==0?0:rows*14-5)+14);
        double scale=Math.min(Math.clamp(requestedScale,0.5,2),
            Math.min(Math.max(1,availableWidth)/(double)width,Math.max(1,availableHeight)/(double)height));
        return new NowPlayingLayout(width,height,icon,artwork?icon+15:7,scale);
    }
}
