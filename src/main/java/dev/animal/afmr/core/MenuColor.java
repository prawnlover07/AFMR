package dev.animal.afmr.core;

import java.util.EnumMap;
import java.util.Map;

/** Menu color families retain their shade differences around a configurable base RGB. */
public enum MenuColor {
    BACKGROUND("Background",0x141B1B), PANELS("Panels",0x1D2727),
    CONTROLS("Buttons",0x283236), HOVER("Hover",0x3B4949),
    SELECTION("Selection",0x35594E), ACCENT("Highlights",0x8DE0BE),
    TEXT("Text",0xE1ECE6), MUTED_TEXT("Secondary text",0xA9BDB3),
    BORDERS("Borders",0x3C4D49), SCROLLBARS("Scrollbars",0x637B75);

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
    /** Original shades define relative channel offsets; default bases reproduce the exact existing palette. */
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
