package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaybackEngine;
import dev.animal.afmr.library.LoudnessScan;
import dev.animal.afmr.library.LibraryStore;
import dev.animal.afmr.ui.LibraryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit normalization through actual screens, generated WAVs and disposable profiles only. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class NormalizationSuite {
    private static final boolean multiplayer=Boolean.getBoolean("afmr.probe.multiplayer");
    private static final Path results=Path.of("../../results/normalization-"+(multiplayer?"multiplayer":"singleplayer")+"-results.txt").toAbsolutePath();
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static Playlist playlist;
    private static LibraryScreen workspace;
    private static Path fixture;
    private static byte[] original;
    private static LoudnessScan.Job job;
    private static long sequence;
    private static int startup,stage,ticks,passed,failed;
    private static boolean finished;
    private static Object field(Object object,String name)throws Exception{
        var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);
    }
    @SuppressWarnings("unchecked") private static AbstractWidget button(String label)throws Exception{
        var controls=new ArrayList<AbstractWidget>();for(var child:mc.gui.screen().children())if(child instanceof AbstractWidget widget)controls.add(widget);
        if(mc.gui.screen() instanceof LibraryScreen)controls.addAll((List<AbstractWidget>)field(mc.gui.screen(),"menuButtons"));
        return controls.stream().filter(w->w.getMessage().getString().equals(label)).findFirst().orElse(null);
    }
    private static void click(String label)throws Exception{
        var w=Objects.requireNonNull(button(label),label);Screen screen=mc.gui.screen();
        double scale=screen instanceof LibraryScreen?(float)field(screen,"scale"):1;
        var e=new MouseButtonEvent((w.getX()+w.getWidth()/2d)*scale,(w.getY()+w.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        screen.mouseClicked(e,false);screen.mouseReleased(e);
    }
    private static void check(String name,boolean okay)throws Exception{
        if(okay)passed++;else failed++;
        Files.writeString(results,(okay?"PASS ":"FAIL ")+name+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.normalization")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(stage==5){if(++ticks>=20){finished=true;mc.gui.setScreen(null);mc.stop();}return;}
            if(stage==0){
                if(mc.level==null||mc.player==null||++startup<80)return;
                if(multiplayer&&Boolean.getBoolean("afmr.probe.stopServer")){mc.player.connection.sendCommand("stop");stage=5;ticks=0;return;}
                Files.createDirectories(results.getParent());Files.writeString(results,"");
                mc.options.pauseOnLostFocus=false;rt=ClientRuntime.get();lib=rt.library();rt.engine().reset();
                check("test world is running unpaused",!mc.isPaused());
                check("requested singleplayer or actual remote server context",multiplayer?mc.getSingleplayerServer()==null&&mc.getCurrentServer()!=null:mc.getSingleplayerServer()!=null);
                fixture=Path.of("../../fixtures/long.wav").toAbsolutePath().normalize();
                original=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(fixture));
                playlist=lib.createPlaylist("Normalization gameplay fixture");playlist.enabled=false;playlist.includeVanilla=false;
                for(int i=0;i<2;i++)addTrack(i);
                lib.rebuildTrackIndex();rt.preview(lib.track("file:manual-normalization-0"));sequence=rt.engine().playbackSequence();
                workspace=new LibraryScreen(null,rt,playlist.id);mc.gui.setScreen(workspace);click("Playlist actions ...");click("Balance song volumes...");job=rt.loudnessJob();
                check("Normalize immediately starts the explicit batch",job!=null&&job.total()==2);
                check("progress screen does not request a pause",mc.gui.screen().getClass().getSimpleName().equals("LoudnessScreen")&&!mc.gui.screen().isPauseScreen());
                stage=1;ticks=0;return;
            }
            if(++ticks>2400)throw new IllegalStateException("Normalization native timeout at stage "+stage);
            if(stage==1){
                if(job.report()==null||rt.lastLoudnessReport()!=job.report()||button("Normalize again")==null)return;
                check("manual scan completes during unpaused gameplay",!mc.isPaused()&&rt.inWorld()&&!job.report().cancelled()&&job.report().count(LoudnessScan.Source.SCANNED)==2);
                check("client applies measured results",lib.track("file:manual-normalization-0").loudness!=null&&lib.track("file:manual-normalization-1").loudness!=null);
                check("music continues without a restart",rt.engine().current()!=null&&rt.engine().playbackSequence()==sequence&&((PlaybackEngine.Audio)field(rt.engine(),"audio")).playing());
                for(int i=0;i<42;i++){addTrack(i);lib.track("file:manual-normalization-"+i).loudness=null;}
                lib.rebuildTrackIndex();click("Normalize again");job=rt.loudnessJob();click("Back");
                check("Back returns to the workspace while the job continues",mc.gui.screen()==workspace&&job!=null&&job.total()==42&&!job.cancelled());
                stage=2;ticks=0;return;
            }
            if(stage==2){
                if(job.completed()==0&&job.report()==null)return;
                check("scan makes progress after closing its screen",job.completed()>0&&!mc.isPaused());
                click("Playlist actions ...");click("Balance song volumes...");check("reopening progress reuses the running batch",rt.loudnessJob()==job);
                click("Cancel");check("Cancel reaches the active job",job.cancelled());stage=3;ticks=0;return;
            }
            if(stage==3){
                if(job.report()==null||rt.lastLoudnessReport()!=job.report()||button("Normalize again")==null)return;
                check("cancellation publishes a partial report",job.report().cancelled()&&!job.report().rows().isEmpty()&&job.report().rows().size()<job.total());
                check("completed measurements survive cancellation",job.report().rows().stream().allMatch(r->r.measurement()!=null&&lib.track(r.track().id()).loudness!=null));
                check("unprocessed songs remain unmeasured",playlist.tracks.keySet().stream().map(lib::track).anyMatch(t->t.loudness==null));
                check("music is still active after cancellation",rt.engine().current()!=null&&rt.engine().playbackSequence()==sequence&&((PlaybackEngine.Audio)field(rt.engine(),"audio")).playing());
                check("original generated audio is unchanged",Arrays.equals(original,MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(fixture))));
                rt.save();stage=4;ticks=0;return;
            }
            if(stage==4&&ticks>=30){
                try(var store=new LibraryStore(mc.gameDirectory.toPath().resolve("config/afmr/library.json"))){
                    var saved=store.load();
                    check("normalization measurements persist to the normal library file",job.report().rows().stream().allMatch(r->saved.track(r.track().id())!=null&&saved.track(r.track().id()).loudness!=null));
                }
                check("no title-screen or pause transition was required",rt.inWorld()&&!mc.isPaused());finish();
            }
        }catch(Throwable e){
            try{failed++;Files.writeString(results,"FAIL "+e+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);finish();}
            catch(Exception nested){throw new RuntimeException(nested);}
        }
    }
    private static void addTrack(int i){
        String id="file:manual-normalization-"+i;var t=lib.track(id);
        if(t==null){t=new Track(id,fixture.toString(),"Generated normalization "+i,false);t.durationSeconds=90;lib.tracks.add(t);}
        t.path=fixture.toString();t.loudness=null;playlist.tracks.put(id,true);
    }
    private static void finish()throws Exception{
        Files.writeString(results,"COMPLETE: "+passed+" passed / "+failed+" failed\n",StandardOpenOption.APPEND);
        finished=true;mc.gui.setScreen(null);mc.stop();
    }
}
