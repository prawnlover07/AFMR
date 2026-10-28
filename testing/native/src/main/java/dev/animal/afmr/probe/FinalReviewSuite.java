package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Focused final-copy/clipboard/restore checks; disposable profile and generated assets only. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class FinalReviewSuite {
    private record Step(String name,BooleanSupplier ready,Runnable action){}
    private static final List<Step> steps=new ArrayList<>();
    private static final boolean restart=Boolean.getBoolean("afmr.probe.reviewRestart");
    private static final Path results=Path.of("../../results/final-review"+(restart?"-restart":"")+"-results.txt");
    private static final Path backup=Path.of("final-review.afmrb");
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static LibraryScreen editor;
    private static Playlist target;
    private static Track track;
    private static int startup,elapsed,index,passed,failed;
    private static boolean started,finished;
    private static String copied;

    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.finalReview")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){
                if(mc.level==null||mc.player==null||++startup<60)return;
                Files.createDirectories(results.toAbsolutePath().getParent());Files.writeString(results,"");
                rt=ClientRuntime.get();lib=rt.library();mc.options.pauseOnLostFocus=false;
                if(restart)restartChecks();else prepare();
                started=true;elapsed=0;return;
            }
            if(index==steps.size()){finish();return;}
            if(++elapsed%10!=0||elapsed<20)return;
            var step=steps.get(index);
            if(!step.ready().getAsBoolean()){
                if(elapsed<600)return;
                throw new IllegalStateException("Timed out: "+step.name());
            }
            log("STEP "+step.name());index++;elapsed=0;step.action().run();
        }catch(Throwable failure){failed++;log("FAIL "+failure+" on "+(mc.gui.screen()==null?"none":mc.gui.screen().getClass().getSimpleName()));try{shot("failure-"+index);}catch(Exception ignoredShot){}failure.printStackTrace();finish();}
    }
    private static void check(String name,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+name);}
    private static void log(String message){try{Files.writeString(results,message+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}System.out.println("AFMR FINAL REVIEW "+message);}
    private static void add(String name,Runnable action){steps.add(new Step(name,()->true,action));}
    private static void await(String name,BooleanSupplier ready,Runnable action){steps.add(new Step(name,ready,action));}
    private static Object field(Object value,String name){
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);}
        catch(NoSuchFieldException ignored){}catch(Exception e){throw new RuntimeException(e);}
        throw new IllegalArgumentException(name);
    }
    private static List<AbstractWidget> widgets(){return mc.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private static AbstractWidget prefix(String label){return widgets().stream().filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow(()->new IllegalStateException("Missing control: "+label));}
    private static void click(String label){click(prefix(label));}
    private static void click(AbstractWidget control){
        log("CLICK "+control.getMessage().getString()+" on "+mc.gui.screen().getClass().getSimpleName());
        if(!control.active)throw new IllegalStateException("Inactive: "+control.getMessage().getString());
        for(int i=0;!control.visible&&i<24;i++)mc.gui.screen().mouseScrolled(control.getX()+2,90,0,control.getY()<79?1:-1);
        if(!control.visible)throw new IllegalStateException("Hidden control");
        float scale=mc.gui.screen() instanceof LibraryScreen?(float)field(mc.gui.screen(),"scale"):1;
        var event=new MouseButtonEvent((control.getX()+control.getWidth()/2d)*scale,(control.getY()+control.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        var before=mc.gui.screen();boolean handled=before.mouseClicked(event,false);before.mouseReleased(event);
        log("INPUT "+event.x()+","+event.y()+" scale="+scale+" bounds="+control.getX()+","+control.getY()+","+control.getWidth()+","+control.getHeight()+" handled="+handled+" screen="+mc.gui.screen().getClass().getSimpleName());
    }
    private static void show(){editor=new LibraryScreen(null,rt,target.id);mc.gui.setScreen(editor);}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"final-review-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private static boolean playing(){
        var sound=(SoundInstance)field(field(rt.engine(),"audio"),"sound");
        return rt.engine().current()!=null&&sound!=null&&mc.getSoundManager().isActive(sound);
    }
    private static Object tooltipValue(AbstractWidget control){return field(field(control,control.getClass().getSimpleName().equals("FlatButton")?"help":"tooltip"),"tooltip");}
    private static void tooltip(String prefix,String key){
        var control=prefix(prefix);var expected=Component.translatable(key).getString();
        check("Author copy applied: "+key,tooltipValue(control)!=null&&((Component)field(tooltipValue(control),"message")).getString().equals(expected)&&!expected.isBlank());
        try{var mouse=mc.mouseHandler;var window=mc.getWindow();
            Field x=mouse.getClass().getDeclaredField("xpos"),y=mouse.getClass().getDeclaredField("ypos");x.setAccessible(true);y.setAccessible(true);
            x.setDouble(mouse,(control.getX()+control.getWidth()/2d)*window.getScreenWidth()/window.getGuiScaledWidth());
            y.setDouble(mouse,(control.getY()+control.getHeight()/2d)*window.getScreenHeight()/window.getGuiScaledHeight());
        }catch(Exception e){throw new RuntimeException(e);}
    }
    private static void copy(){
        long sequence=rt.engine().playbackSequence();var current=rt.engine().current();boolean paused=rt.engine().paused();
        click((AbstractWidget)field(mc.gui.screen(),"copy"));copied=mc.keyboardHandler.getClipboard();
        check("Real clipboard contains diagnostic header",copied.startsWith("AFMR diagnostic summary"));
        check("Clipboard confirmation is visible",((AbstractWidget)field(mc.gui.screen(),"copy")).getMessage().getString().equals("Summary copied!"));
        check("Copy preserves playback",sequence==rt.engine().playbackSequence()&&current==rt.engine().current()&&paused==rt.engine().paused());
        check("Clipboard omits private fixture names, IDs, paths and location",!copied.contains("do-not-share")&&!copied.contains(track.path)&&!copied.contains(rt.worldKey())&&!copied.contains(System.getProperty("user.home")));
    }
    private static void prepare()throws Exception{
        check("Packaged runtime initialized",rt.enabled());rt.activateProfile(PlaylistProfiles.DEFAULT);lib.profiles.bindings.clear();
        lib.playlists=new ArrayList<>(PlaylistTemplates.create());for(var p:lib.playlists){p.enabled=false;p.includeVanilla=false;}
        lib.settings.rescanOnLaunch=false;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=3;
        lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=lib.settings.bufferSeconds=0;lib.settings.jukeboxMode=JukeboxMode.OFF;
        lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=.4;rt.menuBackground().enabled(false);
        target=lib.createPlaylist("do-not-share playlist");target.id="review-do-not-share";target.enabled=false;target.includeVanilla=false;
        target.conditions.areaEnabled=true;target.conditions.area.world=rt.worldKey();target.conditions.area.x=111222;target.conditions.area.z=333444;
        var nested=new PlaylistConditions();nested.label="do-not-share group";target.conditions.groups.add(nested);
        Path audio=Path.of("../../fixtures/long.wav").toAbsolutePath().normalize();check("Generated audio exists",Files.isRegularFile(audio));
        track=new Track("review-do-not-share-track",audio.toString(),"do-not-share song",false);lib.tracks.add(track);target.tracks.put(track.id,true);lib.rebuildTrackIndex();
        check("Disabled automatic playlist accepts a held override",rt.overridePlaylist(target.id,OverrideMode.UNTIL_RELEASED));
        await("Native playback and inspector",FinalReviewSuite::playing,()->{check("Native sound is active",playing());show();});
        add("Open inspector",()->{click("Playback status");});
        add("Copy live playback",()->{copy();check("Clipboard captures source and override aliases",copied.contains("source: #"+(lib.playlists.indexOf(target)+1))&&copied.contains("Override: #"+(lib.playlists.indexOf(target)+1)+"; mode: UNTIL_RELEASED"));
            check("Clipboard captures safe location result",copied.contains("Location: configured=false; matches=false (location details omitted)"));shot("inspector");click("Pause music");});
        add("Copy paused playback",()->{copy();check("Clipboard reflects actual pause",copied.contains("reason: PAUSED")&&copied.contains("paused: true"));click("Resume music");
            rt.engine().failed(track.id,"AccessDeniedException: do-not-share "+track.path,rt.now(),lib);});
        add("Copy playback failure",()->{copy();check("Failure category replaces raw message",copied.contains("Playback error: access denied (details omitted)")&&!copied.contains("AccessDeniedException"));click("Retry failed");});
        add("Copy after retry",()->{copy();check("Retry clears reported current error",copied.contains("Playback error: none")&&copied.contains("failed songs this session: 0"));click("Back");click("Timing");
            check("Timing no longer contains rescan or global combat switches",widgets().stream().noneMatch(w->w.getMessage().getString().startsWith("Rescan on launch:")||w.getMessage().getString().startsWith("Combat music:")));
            tooltip("Play immediately on join:","afmr.copy.settings.play_immediately_on_join");});
        add("Timing author tooltip",()->{shot("timing-tooltip");click("Combat settings");check("Combat toggle lives in Combat settings",prefix("Combat music:").active);
            tooltip("Combatants only","afmr.copy.combatsettings.combat_triggers");});
        add("Combat author tooltip",()->{shot("combat-tooltip");click("Save & back");click("Library");
            check("Rescan switch lives beside repair",prefix("Rescan on launch:").active&&prefix("Repair / missing").active);click("Rescan on launch");check("Rescan preference toggles",lib.settings.rescanOnLaunch);
            click("Display");click("Menus");check("Cleared menu opacity has no empty tooltip",tooltipValue(prefix("Background opacity:"))==null);});
        add("Menu appearance",()->{shot("menus");click("Custom background");check("Cleared sizing explanation has no tooltip",tooltipValue(prefix("Sizing:"))==null);
            check("Cleared image opacity has no tooltip",tooltipValue(prefix("Image opacity:"))==null);rt.menuBackground().choose(Path.of("background-fixtures/disposal.gif"));});
        await("Generated GIF background",()->!rt.menuBackground().busy()&&(boolean)field(rt.menuBackground(),"uploaded"),()->{
            check("Generated animated background uploads",((Object[])field(rt.menuBackground(),"textures")).length==4);shot("animated-background");click("Back");click("Widget");
            while(!prefix("Style:").getMessage().getString().contains("AFMR"))click("Style:");
            check("Cleared widget-corner explanation has no tooltip",tooltipValue(prefix("Corner:"))==null);click("Preview widget");});
        add("Widget preview",()->{shot("widget-preview");click("Back");var screen=mc.gui.screen();
            var scale=(EditBox)field(screen,"scale");scale.setValue("125");MusicMenu.hide(screen);mc.gui.setScreen(MusicMenu.reopen(null,rt));
            check("Hidden settings retain author-copy widgets and draft",mc.gui.screen()==screen&&((EditBox)field(screen,"scale")).getValue().equals("125"));
            int height=screen.height;screen.height=height+120;
            try{var reflow=Class.forName("dev.animal.afmr.ui.MusicScreen").getDeclaredMethod("repositionElements");reflow.setAccessible(true);reflow.invoke(screen);
                check("Reflow retains numeric drafts",((EditBox)field(screen,"scale")).getValue().equals("125"));screen.height=height;reflow.invoke(screen);
            }catch(Exception e){throw new RuntimeException(e);}
            click("Save & back");lib.settings.widgetWidth=337;target.volume=.37;target.customSilence=true;target.silenceMinSeconds=target.silenceMaxSeconds=3;rt.save();
            show();click("Import / export");click("Full configuration...");rt.exportBackup(backup);
        });
        await("Export complete animated configuration",()->!rt.scanning()&&Files.isRegularFile(backup),()->{
            check("Full backup includes enabled GIF",readBackupHasGif());rt.prepareBackupImport(backup);
        });
        await("Review configuration replacement",()->rt.pendingBackup()!=null&&!rt.scanning(),()->{
            check("Replacement requires a reviewed backup",prefix("Replace on restart").active);shot("backup-review");click("Replace on restart");
        });
        await("Stage restore for a real restart",rt::backupQueued,()->{check("Current session remains intact before restart",lib.settings.widgetWidth==337&&target.volume==.37);check("Pending archive exists for actual restart",Files.isRegularFile(Path.of("config/afmr").resolve(dev.animal.afmr.library.ConfigBackup.PENDING)));});
    }
    private static boolean readBackupHasGif(){try{return dev.animal.afmr.library.ConfigBackup.read(backup).files().containsKey("menu-background.gif");}catch(Exception e){throw new RuntimeException(e);}}
    private static void restartChecks(){
        target=lib.playlist("review-do-not-share");
        check("Reviewed restore initializes after actual restart",rt.enabled()&&target!=null);
        check("Playback flags and volume survive restore",target!=null&&!target.enabled&&target.volume==.37&&target.customSilence&&target.silenceMinSeconds==3);
        check("Appearance and rescan setting survive restore",lib.settings.widgetWidth==337&&lib.settings.widgetScale==1.25&&lib.settings.rescanOnLaunch);
        check("Enabled animated background survives restore",lib.settings.menuImageEnabled&&lib.settings.menuImageName.endsWith(".gif")&&Files.isRegularFile(Path.of("config/afmr/menu-background.gif")));
        check("Pending archive is consumed",!Files.exists(Path.of("config/afmr").resolve(dev.animal.afmr.library.ConfigBackup.PENDING)));
        show();click("Display");click("Menus");
        await("Restored GIF texture upload",()->!rt.menuBackground().busy()&&(boolean)field(rt.menuBackground(),"uploaded"),()->{
            check("Restored GIF uploads every frame",((Object[])field(rt.menuBackground(),"textures")).length==4);shot("restored-background");
        });
    }
    private static void finish(){
        if(finished)return;finished=true;log("COMPLETE: "+passed+" passed / "+failed+" failed");
        mc.gui.setScreen(null);mc.stop();
    }
}
