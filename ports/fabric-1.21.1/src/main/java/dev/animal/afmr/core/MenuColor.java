package dev.animal.afmr.core;

import java.util.EnumMap;
import java.util.Map;

/** Menu color families retain their shade differences around a configurable base RGB. */
public enum MenuColor {
    BACKGROUND("Background",0x17191D), PANELS("Panels",0x202329),
    CONTROLS("Buttons",0x2C3038), HOVER("Hover",0x3B424D),
    SELECTION("Selection",0x304963), ACCENT("Highlights",0x90C6FF),
    TEXT("Text",0xEBEEF3), MUTED_TEXT("Secondary text",0xABB5C2),
    BORDERS("Borders",0x3B424C), SCROLLBARS("Scrollbars",0x66778B);

    public final String label;
    public final int defaultRgb;
    MenuColor(String label,int rgb){this.label=label;defaultRgb=rgb;}
    public static EnumMap<MenuColor,Integer> defaults(){
        var colors=new EnumMap<MenuColor,Integer>(MenuColor.class);
        for(var role:values())colors.put(role,role.defaultRgb);
        return colors;
    }
    public static EnumMap<MenuColor,Integer> normalize(Map<MenuColor,Integer> source){
        var colors=defaults();
        if(source!=null)for(var role:values()){
            Integer rgb=source.get(role);if(rgb!=null&&rgb>=0&&rgb<=0xFFFFFF)colors.put(role,rgb);
        }
        return colors;
    }
    public int rgb(Map<MenuColor,Integer> colors){
        Integer rgb=colors==null?null:colors.get(this);return rgb==null?defaultRgb:rgb;
    }
    /** Graphite shades define relative channel offsets around each configurable family base. */
    public int argb(Map<MenuColor,Integer> colors,int original,double opacity){
        int chosen=rgb(colors),rgb=0;
        for(int shift=0;shift<=16;shift+=8){
            int channel=((chosen>>shift)&255)+((original>>shift)&255)-((defaultRgb>>shift)&255);
            rgb|=Math.max(0,Math.min(255,channel))<<shift;
        }
        int alpha=(int)Math.round(Math.max(0,Math.min(1,Double.isFinite(opacity)?opacity:0.80))*255);
        return alpha<<24|rgb;
    }
    public static int parse(String text){
        String hex=text.strip();if(hex.startsWith("#"))hex=hex.substring(1);
        if(hex.length()!=6)throw new IllegalArgumentException("Use six hex digits");
        for(int i=0;i<hex.length();i++)if(Character.digit(hex.charAt(i),16)<0||hex.charAt(i)>127)throw new IllegalArgumentException("Use six hex digits");
        return Integer.parseInt(hex,16);
    }
}
