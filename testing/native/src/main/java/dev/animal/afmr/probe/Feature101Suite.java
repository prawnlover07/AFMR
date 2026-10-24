package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.LibraryScreen;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** New feature checks against generated music and one synthetic client-side player only. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class Feature101Suite {
    private record Step(String name,BooleanSupplier ready,Runnable action){}
    private static final List<Step> steps=new ArrayList<>();
    private static final Path result=Path.of("../../results/features-101-results.txt");
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static Playlist a,b;
    private static Track longSong;
    private static RemotePlayer bot;
    private static int startup,elapsed,index,passed,failed;
    private static boolean started,finished;
    private static final List<Double> starts=new ArrayList<>();
    private static long observedSequence;
    private static double pauseTime;
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.features101")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.level==null||mc.player==null||++startup<60)return;
                Files.createDirectories(result.toAbsolutePath().getParent());Files.writeString(result,"");
                rt=ClientRuntime.get();lib=rt.library();mc.options.pauseOnLostFocus=false;prepare();started=true;return;}
            if(index==steps.size()){finish();return;}
            if(++elapsed%5!=0||elapsed<15)return;
            var step=steps.get(index);if(!step.ready().getAsBoolean()){if(elapsed<600)return;throw new IllegalStateException("Timed out: "+step.name());}
            log("STEP "+step.name());index++;elapsed=0;step.action().run();
        }catch(Throwable error){failed++;log("FAIL "+error);error.printStackTrace();try{shot("failure");}catch(Exception ignoredShot){}finish();}
    }
    private static void log(String s){try{Files.writeString(result,s+"\n",StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}System.out.println("AFMR FEATURES101 "+s);}
    private static void check(String s,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+s);}
    private static void add(String name,Runnable action){steps.add(new Step(name,()->true,action));}
    private static void await(String name,BooleanSupplier ready,Runnable action){steps.add(new Step(name,ready,action));}
    private static Object field(Object obj,String name){for(Class<?> c=obj.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(obj);}catch(NoSuchFieldException e){}catch(Exception e){throw new RuntimeException(e);}throw new IllegalArgumentException(name);}
    private static AbstractWidget prefix(String label){return mc.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow(()->new IllegalStateException(label+" on "+mc.gui.screen().getClass().getSimpleName()));}
    private static void click(AbstractWidget w){
        if(!w.active||!w.visible)throw new IllegalStateException("Unavailable control: "+w.getMessage().getString());
        float scale=mc.gui.screen() instanceof LibraryScreen?(float)field(mc.gui.screen(),"scale"):1;
        clickAt((w.getX()+w.getWidth()/2d)*scale,(w.getY()+w.getHeight()/2d)*scale);
    }
    private static void click(String label){click(prefix(label));}
    private static void clickAt(double x,double y){var screen=mc.gui.screen();var e=new MouseButtonEvent(x,y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(e,false);screen.mouseReleased(e);}
    private static void show(){mc.gui.setScreen(new LibraryScreen(null,rt,a.id));}
    private static void open(String name,Object... args){try{
        var type=Class.forName("dev.animal.afmr.ui."+name);var constructor=type.getDeclaredConstructors()[0];constructor.setAccessible(true);mc.gui.setScreen((Screen)constructor.newInstance(args));
    }catch(Exception e){throw new RuntimeException(e);}}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"features-101-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private static boolean playing(){var sound=(SoundInstance)field(field(rt.engine(),"audio"),"sound");return rt.engine().current()!=null&&sound!=null&&mc.getSoundManager().isActive(sound);}
    private static void prepare(){
        rt.activateProfile(PlaylistProfiles.DEFAULT);rt.engine().reset();lib.profiles.bindings.clear();
        lib.playlists=new ArrayList<>();a=new Playlist("feature-a","A");b=new Playlist("feature-b","B");a.includeVanilla=b.includeVanilla=false;b.enabled=false;lib.playlists.add(a);lib.playlists.add(b);
        lib.settings.rescanOnLaunch=false;lib.settings.songsBeforeSilence=3;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=3;
        lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=lib.settings.bufferSeconds=0;lib.settings.jukeboxMode=JukeboxMode.OFF;
        lib.settings.nowPlaying=true;lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;lib.settings.popupDelaySeconds=3;
        for(int i=0;i<3;i++){var t=new Track("file:feature_short_"+i,Path.of("../../fixtures/short.wav").toAbsolutePath().normalize().toString(),"Generated short "+i,false);lib.tracks.add(t);a.tracks.put(t.id,true);}
        longSong=new Track("file:feature_long",Path.of("../../fixtures/long.wav").toAbsolutePath().normalize().toString(),"Generated long",false);lib.tracks.add(longSong);lib.rebuildTrackIndex();
        rt.overridePlaylist(a.id,OverrideMode.UNTIL_RELEASED);
        await("Three actual songs then a gap",()->{
            var e=rt.engine();if(e.current()!=null&&e.playbackSequence()!=observedSequence){observedSequence=e.playbackSequence();starts.add(rt.now());check("Grouped song has native audio",playing());}
            return starts.size()>=3&&e.current()==null;
        },()->{check("Exactly three songs before silence",starts.size()==3);check("No ordinary gap inside group",starts.get(1)-starts.get(0)<3&&starts.get(2)-starts.get(1)<3);check("Full gap follows group",rt.engine().remainingSilence(rt.now())>1.5);
            a.tracks.clear();a.tracks.put(longSong.id,true);rt.overridePlaylist(a.id,OverrideMode.UNTIL_RELEASED);});
        await("Delayed card starts hidden",Feature101Suite::playing,()->{
            check("Three-second card delay is armed",Double.isNaN((double)field(rt.overlay(),"shownAt")));rt.togglePause();pauseTime=rt.now();
        });
        await("Pause holds delay",()->rt.now()-pauseTime>1.2,()->{check("No card appears while paused",Double.isNaN((double)field(rt.overlay(),"shownAt")));check("Playback clock is held",rt.engine().trackElapsed(rt.now())<2);rt.togglePause();});
        await("Card appears after unpaused playback",()->!Double.isNaN((double)field(rt.overlay(),"shownAt")),()->{
            check("Card waits three playback seconds",rt.engine().trackElapsed(rt.now())>=3);show();click("Add playing to playlist");
        });
        add("Native toast uses a new track's playback clock",()->{
            lib.settings.nowPlayingStyle=NowPlayingStyle.MINECRAFT;lib.settings.popupDelaySeconds=3;
            observedSequence=rt.engine().playbackSequence();rt.overridePlaylist(a.id,OverrideMode.UNTIL_RELEASED);
        });
        await("Native toast starts hidden",()->rt.engine().playbackSequence()>observedSequence&&playing(),()->{
            check("Native toast and pause-menu metadata wait for delay",rt.overlay().nativeSongName()==null&&!(boolean)field(rt.overlay(),"nativeShown"));
        });
        await("Native toast appears",()->(boolean)field(rt.overlay(),"nativeShown"),()->{
            check("Native toast receives metadata after three playback seconds",rt.overlay().nativeSongName()!=null&&rt.engine().trackElapsed(rt.now())>=3);
            lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;rt.overlay().refresh();
            check("Style refresh does not restart an elapsed delay",!Double.isNaN((double)field(rt.overlay(),"shownAt")));
        });
        add("Current-song destination",()->{check("Current song destination opens",mc.gui.screen().getClass().getSimpleName().equals("TrackPlaylistScreen"));shot("add-playing");
            var list=mc.gui.screen().children().stream().filter(c->c.getClass().getSimpleName().equals("DenseList")).map(AbstractWidget.class::cast).findFirst().orElseThrow();clickAt(list.getX()+12,list.getY()+21);});
        add("Copy assignment preserves source",()->{check("Destination receives current song",Boolean.TRUE.equals(b.tracks.get(longSong.id)));check("Original assignment stays on",Boolean.TRUE.equals(a.tracks.get(longSong.id)));check("Adding assignment preserves playback",rt.engine().current()==longSong&&playing());
            show();click("Timing");click("Songs before silence:");});
        add("Global count editor",()->{((EditBox)field(mc.gui.screen(),"count")).setValue("4");shot("global-count");click("Save & back");check("Global count saves",lib.settings.songsBeforeSilence==4);
            open("SongCountScreen",mc.gui.screen(),rt,a);});
        add("Playlist count override",()->{click("Override global count:");((EditBox)field(mc.gui.screen(),"count")).setValue("2");click("Save & back");check("Playlist count is independent",a.customSongCount&&a.songsBeforeSilence==2&&lib.settings.songsBeforeSilence==4);
            open("NearbyPlayerConditionScreen",mc.gui.screen(),rt,b.conditions);});
        add("Nearby-player editor",()->{((EditBox)field(mc.gui.screen(),"name")).setValue("AFMRThemeBot");((EditBox)field(mc.gui.screen(),"radius")).setValue("5");click("Nearby player:");shot("nearby-player");click("Save & back");check("Player condition saves",b.conditions.nearbyPlayerEnabled&&b.conditions.nearbyPlayerRadius==5);
            bot=new RemotePlayer(mc.level,new GameProfile(java.util.UUID.fromString("aabbccdd-1234-1234-1234-123456789abc"),"AFMRThemeBot"));bot.setId(1970000101);bot.setPos(mc.player.getX()+3,mc.player.getY(),mc.player.getZ());mc.level.players().add(bot);
            b.enabled=true;b.playOnTrigger=true;lib.movePlaylist(b.id,0);rt.returnAutomatic();});
        await("Nearby player wins automatically",()->b.id.equals(rt.engine().sourcePlaylistId())&&playing(),()->{
            check("Client-visible nearby player selects theme",b.conditions.matches(rt.context(),lib.settings));b.conditions.nearbyPlayerUuid=bot.getUUID().toString();check("UUID matches tracked player",b.conditions.matches(rt.context(),lib.settings));
            b.finishCurrentSongOnExit=true;bot.setPos(mc.player.getX()+100,mc.player.getY(),mc.player.getZ());
        });
        await("Leaving radius uses existing finish-on-exit",()->!b.conditions.matches(rt.context(),lib.settings),()->{
            check("Theme obeys finish-on-exit",b.id.equals(rt.engine().sourcePlaylistId())&&a==rt.engine().selected());b.finishCurrentSongOnExit=false;rt.nextSong();
        });
        await("Manual Next returns to normal playlist",()->a.id.equals(rt.engine().sourcePlaylistId())&&playing(),()->{
            check("Normal playlist resumes",a==rt.engine().selected());mc.level.players().removeIf(player->player==bot);bot=null;
            check("Diagnostic summary omits player identity",!rt.diagnosticSummary(b.id).contains("AFMRThemeBot")&&!rt.diagnosticSummary(b.id).contains("aabbccdd"));rt.save();
        });
    }
    private static void finish(){if(finished)return;finished=true;if(bot!=null&&mc.level!=null)mc.level.players().removeIf(player->player==bot);log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.gui.setScreen(null);mc.stop();}
}
