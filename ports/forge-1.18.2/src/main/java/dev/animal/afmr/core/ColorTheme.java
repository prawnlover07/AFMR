package dev.animal.afmr.core;

import java.util.*;

/** Shared appearance snapshots. Music, images, fonts and widget layout are outside a theme. */
public final class ColorTheme {
    public static final String GRAPHITE="graphite";
    public static final int MAX_SAVED=128,MAX_NAME=80;
    public String id="",name="";
    public Map<MenuColor,Integer> menuColors=MenuColor.defaults();
    public double menuOpacity=.8,widgetOpacity=.92;
    public int widgetBackground;

    public static ColorTheme graphite(){var theme=new ColorTheme();theme.id=GRAPHITE;theme.name="Graphite";return theme;}
    public static ColorTheme capture(String name,Library.Settings settings){
        var theme=new ColorTheme();theme.id=UUID.randomUUID().toString();theme.name=validName(name);
        theme.menuColors=MenuColor.normalize(settings.menuColors);theme.menuOpacity=settings.menuBackgroundOpacity;
        theme.widgetBackground=settings.widgetBackgroundColor;theme.widgetOpacity=settings.widgetBackgroundOpacity;return theme;
    }
    public ColorTheme copy(){
        var next=new ColorTheme();next.id=id;next.name=name;next.menuColors=MenuColor.normalize(menuColors);
        next.menuOpacity=menuOpacity;next.widgetBackground=widgetBackground;next.widgetOpacity=widgetOpacity;return next;
    }
    public void apply(Library.Settings settings){
        settings.menuColors=MenuColor.normalize(menuColors);settings.menuBackgroundOpacity=menuOpacity;
        settings.widgetBackgroundColor=widgetBackground;settings.widgetBackgroundOpacity=widgetOpacity;
    }
    public boolean matches(Library.Settings settings){return menuColors.equals(settings.menuColors)
        &&menuOpacity==settings.menuBackgroundOpacity&&widgetBackground==settings.widgetBackgroundColor&&widgetOpacity==settings.widgetBackgroundOpacity;}
    public boolean matches(ColorTheme other){return menuColors.equals(other.menuColors)&&menuOpacity==other.menuOpacity&&widgetBackground==other.widgetBackground&&widgetOpacity==other.widgetOpacity;}
    public static String validName(String value){
        if(value==null)throw new IllegalArgumentException("Enter a theme name.");String name=value.strip();
        if(name.isEmpty()||name.length()>MAX_NAME||name.codePoints().anyMatch(Character::isISOControl))throw new IllegalArgumentException("Use a theme name of 1–80 characters.");
        return name;
    }
    public static boolean validOpacity(double value){return Double.isFinite(value)&&value>=0&&value<=1;}
    public static ColorTheme find(Library.Settings settings,String id){
        if(GRAPHITE.equals(id))return graphite();for(var theme:settings.savedThemes)if(theme.id.equals(id))return theme;return null;
    }
    public static String uniqueName(Library.Settings settings,String requested,String except){
        String base=validName(requested),name=base;
        for(int suffix=2;;suffix++){
            boolean used=name.equalsIgnoreCase("Graphite");
            for(var theme:settings.savedThemes)if(!theme.id.equals(except)&&theme.name.equalsIgnoreCase(name)){used=true;break;}
            if(!used)return name;String tail=" ("+suffix+")";name=base.substring(0,Math.min(base.length(),MAX_NAME-tail.length()))+tail;
        }
    }
    public static List<ColorTheme> copySaved(List<ColorTheme> source){
        var result=new ArrayList<ColorTheme>();if(source==null)return result;var ids=new HashSet<String>();
        for(var theme:source){
            if(result.size()==MAX_SAVED)break;if(theme==null||theme.id==null||theme.id.isBlank()||theme.id.length()>80||theme.id.equals(GRAPHITE)||!ids.add(theme.id))continue;
            try{var next=theme.copy();next.name=validName(theme.name);if(!validOpacity(next.menuOpacity)||!validOpacity(next.widgetOpacity)||next.widgetBackground<0||next.widgetBackground>0xFFFFFF)continue;result.add(next);}
            catch(IllegalArgumentException ignored){}
        }return result;
    }
}
