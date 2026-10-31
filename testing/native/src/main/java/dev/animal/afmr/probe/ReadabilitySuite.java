package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Contrast;
import dev.animal.afmr.core.Library.NowPlayingStyle;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.ui.LibraryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.sdl.SDLVideo;
import java.nio.file.*;
import java.util.*;

/** Whole-pixel workspace scale, HUD text colour and theme warning; only the disposable workspace profile is changed. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class ReadabilitySuite {
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static LibraryScreen editor;
    private static final List<Runnable> steps=new ArrayList<>();
    private static final Path results=Path.of("../../results/readability-results.txt");
    private static int startup,ticks,index,passed,failed,originalGui,originalWidth,originalHeight;
    private static boolean started,finished;
    private static Object field(Object object,String name){
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}catch(Exception ex){throw new RuntimeException(ex);}throw new IllegalArgumentException(name);
    }
    private static Screen open(String name,Object...args){
        try{var type=Class.forName("dev.animal.afmr.ui."+name);for(var c:type.getDeclaredConstructors())if(c.getParameterCount()==args.length){c.setAccessible(true);return (Screen)c.newInstance(args);}}catch(Exception ex){throw new RuntimeException(ex);}
        throw new IllegalArgumentException(name);
    }
    private static void log(String text){try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}System.out.println("AFMR READABILITY "+text);}
    private static void check(String label,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+label);}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"readability-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,text->{});}
    private static void gui(int scale){mc.options.guiScale().set(scale);mc.resizeGui();}
    private static void window(int width,int height){SDLVideo.SDL_RestoreWindow(mc.getWindow().handle());SDLVideo.SDL_SetWindowSize(mc.getWindow().handle(),width,height);}
    private static String warning(){return (String)field(mc.gui.screen(),"contrastWarning");}
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.readability")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.level==null||mc.player==null||++startup<80)return;Files.createDirectories(results.toAbsolutePath().getParent());Files.writeString(results,"");prepare();started=true;return;}
            if(++ticks%10!=0)return;
            if(index==steps.size()){finish();return;}
            steps.get(index++).run();
        }catch(Throwable error){failed++;log("FAIL "+error);error.printStackTrace();shot("failure");finish();}
    }
    private static void finish(){
        finished=true;rt.themes().cancelPreview();rt.library().settings.widgetBackgroundColor=0;rt.library().settings.widgetBackgroundOpacity=.92;
        mc.gui.setScreen(null);gui(originalGui);window(originalWidth,originalHeight);
        log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.stop();
    }
    /** Resize, then open the workspace, then measure after it has rendered at the new size. */
    private static void workspace(String name,int gui,int width,int height){
        steps.add(()->{mc.gui.setScreen(null);window(width,height);});
        steps.add(()->{gui(gui);mc.gui.setScreen(editor);});
        steps.add(()->{
            var window=mc.getWindow();var layout=field(editor,"layout");double pixels=(float)field(editor,"scale")*window.getGuiScale();
            boolean fits=window.getWidth()>=900&&window.getHeight()>=516;
            log(String.format(Locale.ROOT,"INFO %s: window %dx%d, GUI %d, canvas %sx%s, %.3f px per canvas px",name,window.getWidth(),window.getHeight(),window.getGuiScale(),field(layout,"width"),field(layout,"height"),pixels));
            double fit=Math.min(Math.min(window.getWidth()/900.0,window.getHeight()/516.0),window.getGuiScale());
            boolean whole=Math.abs(pixels-Math.round(pixels))<1e-4;
            check(name+" pixel factor is whole or keeps the larger fit, within GUI Scale",!fits||pixels>=1&&pixels<=window.getGuiScale()+1e-4&&(whole?Math.floor(fit)>=fit*.85:Math.abs(pixels-fit)<1e-3));
            shot(name);
        });
    }
    /** Screenshots only, so a released JAR without the readability changes can supply the "before" images. */
    private static void compare(String label,dev.animal.afmr.core.Library lib){
        int[][] sizes={{1920,1080,2},{1920,1080,3},{1366,768,3}};
        for(int[] size:sizes){
            String name=label+"-workspace-"+size[0]+"x"+size[1]+"-gui"+size[2];
            steps.add(()->{mc.gui.setScreen(null);window(size[0],size[1]);});
            steps.add(()->{gui(size[2]);mc.gui.setScreen(editor);});
            steps.add(()->{log(String.format(Locale.ROOT,"INFO %s: %.3f px per canvas px",name,(float)field(editor,"scale")*mc.getWindow().getGuiScale()));shot(name);});
        }
        steps.add(()->{mc.gui.setScreen(null);window(1920,1080);});
        steps.add(()->gui(2));
        for(int rgb:new int[]{0xFFFFFF,0xF0C040}){
            steps.add(()->{lib.settings.widgetBackgroundColor=rgb;lib.settings.widgetBackgroundOpacity=.92;mc.gui.setScreen(open("NowPlayingPreviewScreen",editor,rt));});
            steps.add(()->shot(String.format(Locale.ROOT,"%s-widget-%06x",label,rgb)));
        }
    }
    private static void prepare(){
        mc.options.pauseOnLostFocus=false;rt=ClientRuntime.get();var lib=rt.library();var window=mc.getWindow();
        originalGui=mc.options.guiScale().get();originalWidth=window.getScreenWidth();originalHeight=window.getScreenHeight();
        lib.settings.menuImageEnabled=false;lib.settings.menuBlur=false;lib.settings.menuBackgroundOpacity=1;lib.settings.menuColors=MenuColor.defaults();
        lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;lib.settings.widgetBackgroundColor=0;lib.settings.widgetBackgroundOpacity=.92;
        rt.widgetBackground().enabled(false);rt.themes().cancelPreview();
        editor=new LibraryScreen(null,rt,lib.playlists.getFirst().id);
        String compare=System.getProperty("afmr.probe.compare","");
        if(!compare.isEmpty()){compare(compare,lib);return;}
        for(int gui=1;gui<=4;gui++)workspace("workspace-1080-gui"+gui,gui,1920,1080);
        workspace("workspace-768-gui3",3,1366,768);
        workspace("workspace-720-gui2",2,1280,720);
        steps.add(()->{mc.gui.setScreen(null);window(1920,1080);});
        steps.add(()->{gui(2);mc.gui.setScreen(open("ThemesScreen",editor,rt));});
        // Opening Themes re-selects the matching saved theme, so low contrast is driven through the editor's real field.
        steps.add(()->{check("Default theme shows no contrast warning",warning().isEmpty());shot("themes-default");
            rt.themes().cancelPreview();var themes=open("ThemesScreen",editor,rt);mc.gui.setScreen(themes);mc.gui.setScreen(open("ThemeEditorScreen",themes,rt));});
        steps.add(()->{check("Theme editor default shows no warning",warning().isEmpty());shot("theme-editor");
            @SuppressWarnings("unchecked") var fields=(Map<String,net.minecraft.client.gui.components.EditBox>)field(mc.gui.screen(),"fields");
            fields.get("MUTED_TEXT").setValue("#2C3038");});
        steps.add(()->{check("Editing Secondary text to the button color shows warning",!warning().isEmpty());log("INFO warning text: "+warning());shot("theme-editor-low-contrast");
            @SuppressWarnings("unchecked") var fields=(Map<String,net.minecraft.client.gui.components.EditBox>)field(mc.gui.screen(),"fields");
            fields.get("MUTED_TEXT").setValue("#ABB5C2");});
        steps.add(()->{check("Restoring Secondary text clears warning",warning().isEmpty());mc.gui.screen().onClose();mc.gui.setScreen(null);rt.themes().cancelPreview();});
        for(int[] fill:new int[][]{{0x000000,92},{0xFFFFFF,92},{0xF0C040,92},{0x757575,92},{0xFFFFFF,20}}){
            String name=String.format(Locale.ROOT,"widget-%06x-%d",fill[0],fill[1]);
            steps.add(()->{lib.settings.widgetBackgroundColor=fill[0];lib.settings.widgetBackgroundOpacity=fill[1]/100.0;mc.gui.setScreen(open("NowPlayingPreviewScreen",editor,rt));});
            steps.add(()->{
                int text=(int)field(field(mc.gui.screen(),"widget"),"textColor");boolean translucent=fill[1]<50;
                if(translucent)log("INFO "+name+": translucent fill draws light text with shadow");
                else{check(name+" chooses the more readable text",text==Contrast.widgetText(fill[0]));log(String.format(Locale.ROOT,"INFO %s: text %06x, contrast %.2f:1",name,text,Contrast.ratio(text,fill[0])));}
                shot(name);
            });
        }
    }
}
