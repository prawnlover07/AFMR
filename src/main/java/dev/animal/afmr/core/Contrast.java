package dev.animal.afmr.core;

/** WCAG contrast for sRGB colors; alpha composition uses an explicit opaque reference. */
public final class Contrast {
    public static final int WIDGET_LIGHT=0xF0F7F3,WIDGET_DARK=0x101010,REFERENCE=0x808080;
    private Contrast(){}
    public static double luminance(int rgb){
        return .2126*linear((rgb>>16)&255)+.7152*linear((rgb>>8)&255)+.0722*linear(rgb&255);
    }
    private static double linear(int channel){double value=channel/255.0;return value<=.04045?value/12.92:Math.pow((value+.055)/1.055,2.4);}
    public static double ratio(int first,int second){
        double a=luminance(first),b=luminance(second);return (Math.max(a,b)+.05)/(Math.min(a,b)+.05);
    }
    public static int composite(int rgb,double opacity,int reference){
        double alpha=Double.isFinite(opacity)?Math.clamp(opacity,0,1):0;int result=0;
        for(int shift=0;shift<=16;shift+=8)result|=(int)Math.round(((rgb>>shift)&255)*alpha+((reference>>shift)&255)*(1-alpha))<<shift;
        return result;
    }
    public static int widgetText(int background){return ratio(WIDGET_LIGHT,background)>=ratio(WIDGET_DARK,background)?WIDGET_LIGHT:WIDGET_DARK;}
}
