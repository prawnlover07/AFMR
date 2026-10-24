package dev.animal.afmr.probe;

import dev.animal.afmr.Afmr;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.library.*;
import dev.animal.afmr.ui.LibraryScreen;
import dev.animal.afmr.ui.MusicMenu;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Packaged release interactions in run/integration, never the player's game profile. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class IntegrationSuite {
    private record Step(String name, int delay, BooleanSupplier ready, Runnable action) {}
    private record Sample(long sequence,int source,int state,float offset,float gain,int error) {}
    private static final List<Step> steps=new ArrayList<>();
    private static final Path root=Path.of("integration-fixtures").toAbsolutePath().normalize();
    private static final Path results=Path.of("../../results/"+(Boolean.getBoolean("afmr.probe.fresh")?"fresh":"integration"+(Boolean.getBoolean("afmr.probe.restart")?"-restart":""))+"-results.txt").toAbsolutePath().normalize();
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static LibraryScreen editor;
    private static Playlist ambient,manual,location;
    private static String copyProfile,importedLocation,trackA,trackB;
    private static long sequence;
    private static float fullGain;
    private static Sample frozen;
    private static int startup,index,elapsed,passed,failed;
    private static boolean started,finished;
    // The existing sound executor owns OpenAL. It publishes only an immutable observation.
    private static volatile Sample audio=new Sample(0,0,0,0,0,0);
    private static Object field(Object value,String name){
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{
            Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);
        }catch(NoSuchFieldException ignored){}catch(Exception e){throw new RuntimeException(e);}
        throw new IllegalArgumentException(name);
    }
    private static void log(String text){
        try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception e){throw new RuntimeException(e);}Afmr.LOG.info("AFMR INTEGRATION: {}",text);
    }
    private static void check(String name,boolean okay){
        if(okay)passed++;else failed++;
        log((okay?"PASS ":"FAIL ")+name+" | source="+rt.engine().sourcePlaylistId()+" phase="+rt.engine().phase()+" audio="+audio);
    }
    private static void add(String name,int delay,Runnable action){steps.add(new Step(name,delay,()->true,action));}
    private static void await(String name,BooleanSupplier ready,Runnable action){steps.add(new Step(name,10,ready,action));}
    private static boolean idle(){return !rt.scanning();}
    private static boolean playing(){return audio.sequence()==rt.engine().playbackSequence()&&audio.source()!=0&&audio.state()==AL10.AL_PLAYING&&audio.error()==0;}
    @SuppressWarnings("unchecked") private static void sample(){
        var engine=rt.engine();Object nativeEngine=field(mc.getSoundManager(),"soundEngine");
        var channels=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(nativeEngine,"instanceToChannel");
        var sound=(SoundInstance)field(field(engine,"audio"),"sound");
        var handle=sound==null?null:channels.get(sound);long seq=engine.playbackSequence();
        if(handle==null){audio=new Sample(seq,0,0,0,0,0);return;}
        handle.execute(channel->{int source=(int)field(channel,"source");
            audio=new Sample(seq,source,AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE),AL10.alGetSourcef(source,AL11.AL_SEC_OFFSET),AL10.alGetSourcef(source,AL10.AL_GAIN),AL10.alGetError());});
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.integration")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){
                if(mc.player==null||mc.level==null||++startup<120)return;
                Files.createDirectories(results.getParent());Files.writeString(results,"");
                rt=ClientRuntime.get();lib=rt.library();started=true;
                if(Boolean.getBoolean("afmr.probe.fresh"))freshChecks();else if(Boolean.getBoolean("afmr.probe.restart"))restartChecks();else setup();return;
            }
            sample();if(index==steps.size()){finish();return;}
            Step next=steps.get(index);elapsed++;
            if(elapsed<next.delay()||elapsed%5!=0)return;
            if(!next.ready().getAsBoolean()){
                if(elapsed<1200)return;
                check("TIMEOUT: "+next.name(),false);index++;elapsed=0;return;
            }
            log("STEP "+index+" "+next.name());index++;elapsed=0;
            try{next.action().run();}catch(Throwable e){failed++;log("FAIL exception: "+e);Afmr.LOG.error("Integration test",e);}
        }catch(Throwable e){failed++;log("FAIL fatal: "+e);finished=true;mc.stop();}
    }
    private static void finish()throws Exception{
        finished=true;rt.save();
        if(!Boolean.getBoolean("afmr.probe.interactive")){
            ClientRuntime.shutdown();
            check("Shutdown saves Default transition before closing writer",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
            ClientRuntime.disconnect();rt.save();ClientRuntime.shutdown();
            check("Late logout and screen saves do not reopen closed runtime",!rt.enabled()&&!rt.taskStatus().startsWith("Could not save"));
            try(var store=new LibraryStore(Path.of("config/afmr/library.json"))){
                var saved=store.load();check("Final writer flush preserves profile",saved.profiles.exists(copyProfile==null?PlaylistProfiles.DEFAULT:copyProfile)&&saved.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
            }
        }
        log("COMPLETE: "+passed+" passed / "+failed+" failed");
        Files.writeString(results.resolveSibling(results.getFileName().toString().replace("results","complete")),passed+" passed / "+failed+" failed\n");
        if(Boolean.getBoolean("afmr.probe.interactive"))show(ambient==null?lib.playlists.getFirst().id:ambient.id);else mc.stop();
    }
    private static List<AbstractWidget> widgets(){return mc.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private static AbstractWidget control(String text){return widgets().stream().filter(w->w.getMessage().getString().equals(text)).findFirst().orElseThrow(()->new IllegalStateException("Missing "+text+" on "+mc.gui.screen().getClass().getSimpleName()));}
    private static AbstractWidget prefix(String text){return widgets().stream().filter(w->w.getMessage().getString().startsWith(text)).findFirst().orElseThrow(()->new IllegalStateException("Missing "+text));}
    private static void click(String text){click(control(text));}
    private static void click(AbstractWidget w){point(w.getX()+w.getWidth()/2d,w.getY()+w.getHeight()/2d);}
    private static void point(double x,double y){
        var screen=mc.gui.screen();float scale=screen instanceof LibraryScreen?(float)field(screen,"scale"):1;
        var e=new MouseButtonEvent(x*scale,y*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        screen.mouseClicked(e,false);screen.mouseReleased(e);
    }
    private static void row(String name,int index){var w=(AbstractWidget)field(mc.gui.screen(),name);point(w.getX()+30,w.getY()+14*index+7);}
    private static void name(String value){widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue(value);}
    private static void show(String id){editor=new LibraryScreen(null,rt,id);mc.gui.setScreen(editor);}
    private static boolean screen(String name){return mc.gui.screen().getClass().getSimpleName().equals(name);}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"integration-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    private static void checkedIO(IORunnable action){try{action.run();}catch(Exception e){throw new RuntimeException(e);}}
    @FunctionalInterface private interface IORunnable{void run()throws Exception;}
    private static void setup()throws Exception{
        check("Fresh isolated profile starts on Default",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT));
        mc.options.pauseOnLostFocus=false;mc.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.2);
        mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(1.0);
        lib.playlists=new ArrayList<>(PlaylistTemplates.create());lib.folders.clear();lib.tracks.removeIf(t->!t.vanilla);lib.rebuildTrackIndex();
        for(var p:lib.playlists){p.enabled=false;p.includeVanilla=false;}
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=3;lib.settings.bufferSeconds=0;
        lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0.3;lib.settings.jukeboxMode=JukeboxMode.OFF;
        lib.settings.replayGainEnabled=false;lib.settings.rescanOnLaunch=false;
        ambient=lib.createPlaylist("Integration ambient");ambient.includeVanilla=false;ambient.enabled=false;
        manual=lib.createPlaylist("Integration manual");manual.includeVanilla=false;manual.enabled=false;manual.manualOnly=true;
        Path old=root.resolve("original/Album");Files.createDirectories(old);
        Files.copy(Path.of("../../fixtures/long.wav"),old.resolve("Long.wav"),StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of("../../fixtures/short.wav"),old.resolve("Short.wav"),StandardCopyOption.REPLACE_EXISTING);
        rt.importPaths(List.of(root.resolve("original")),lib.profiles.activeId,ambient.id,true);
        await("Folder scan",IntegrationSuite::idle,()->{
            var tracks=lib.tracks.stream().filter(t->!t.vanilla).toList();check("Generated folder imports two tracks",tracks.size()==2&&ambient.tracks.size()==2);
            trackA=tracks.stream().filter(t->t.path.endsWith("Long.wav")).findFirst().orElseThrow().id;
            trackB=tracks.stream().filter(t->t.path.endsWith("Short.wav")).findFirst().orElseThrow().id;
            // Keep volume/override observations on the long fixture; the FIFO test explicitly uses Short.
            ambient.tracks.put(trackB,false);
            manual.tracks.put(trackA,true);manual.tracks.put(trackB,true);show(ambient.id);
            check("New workspace navigation remains accessible",control("Profiles").active&&control("Queue / history").active&&control("Import / export").active);
            click("Until released");check("Automatic-off playlist accepts override",ambient.id.equals(rt.engine().overrideId()));
        });
        volumeChecks();queueChecks();profileChecks();repairChecks();presetChecks();
        add("Prepare restart",20,()->{
            rt.activateProfile(copyProfile);ambient=lib.playlists.stream().filter(p->p.name.equals("Integration ambient")).findFirst().orElseThrow();
            show(ambient.id);
            ambient.enabled=false;ambient.volume=0.37;lib.settings.silenceMinSeconds=17;lib.settings.silenceMaxSeconds=29;
            lib.profiles.bind(rt.worldKey(),copyProfile);rt.preview(lib.track(trackA));rt.queueTrack(trackB,manual.id);rt.togglePause();
            checkedIO(()->Files.writeString(root.resolve("expected-profile.txt"),copyProfile));
            rt.save();check("Restart fixture has queue and pause to clear",rt.engine().queue().size()>0&&rt.engine().paused());
        });
        add("Shutdown flush",30,()->check("History has real native starts before shutdown",rt.engine().history().size()>5));
    }
    private static void volumeChecks(){
        await("Native initial playback",()->playing()&&(float)field(rt.engine(),"currentGain")>=0.999f,()->{
            check("Imported WAV opens a real native channel",playing());fullGain=audio.gain();sequence=rt.engine().playbackSequence();
            click(prefix("Volume:"));check("Volume opens dedicated page",screen("PlaylistVolumeScreen"));
            click(prefix("Volume:"));check("Slider sets 50 percent",ambient.volume==0.5);
        });
        add("Observe half gain",25,()->{
            check("Playlist volume halves native gain",playing()&&Math.abs(audio.gain()-fullGain*0.5)<0.005);
            check("Live volume preserves channel and song",rt.engine().playbackSequence()==sequence);
            var slider=prefix("Volume:");point(slider.getX()+4,slider.getY()+10);
        });
        add("Observe muted gain",15,()->{
            check("Zero playlist volume mutes real channel",ambient.volume==0&&audio.gain()==0&&rt.engine().current()!=null);
            click("Reset to 100%");check("Volume reset restores setting",ambient.volume==1);click("Back");
        });
        add("Observe reset gain",15,()->{
            check("Volume reset restores native gain",Math.abs(audio.gain()-fullGain)<0.005);shot("volume-workspace");
            ambient.volume=0.25;rt.preview(lib.track(trackA));
        });
        await("Preview native playback",()->playing()&&(float)field(rt.engine(),"currentGain")>=0.999f,()->check("Library preview uses normal volume",Math.abs(audio.gain()-fullGain)<0.005));
    }
    private static void queueChecks(){
        add("Queue from workspace",5,()->{
            rt.engine().queue().clear();rt.clearHistory();show(ambient.id);row("songs",0);click("Queue");
            check("Workspace queues assigned song",rt.engine().queue().size()==1);
            rt.queueTrack(trackB,manual.id);rt.queueTrack(trackA,manual.id);click("Queue / history");
            check("Queue page opens",screen("ListeningScreen"));row("list",2);click("Up");
            check("Queue row Up preserves request identity",rt.engine().queue().snapshot().get(1).trackId().equals(trackA));
            click("Down");check("Queue row Down restores order",rt.engine().queue().snapshot().get(1).trackId().equals(trackB));
            click("Remove");check("Remove only chosen request",rt.engine().queue().size()==2);shot("queue");
            click("Clear queue");check("Clear queue leaves playing song",rt.engine().queue().empty()&&rt.engine().current()!=null);
            rt.queueTrack(trackB,manual.id);rt.queueTrack(trackA,manual.id);rt.skip();
        });
        add("Queue normal silence",20,()->check("Queue observes normal three-second silence",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>1));
        await("Queued short song starts",()->playing()&&rt.engine().queuedPlaying(),()->{
            check("Queued short song starts from automatic-off manual playlist",rt.engine().current().id.equals(trackB)&&manual.id.equals(rt.engine().sourcePlaylistId()));
            check("History records queue source",rt.engine().history().snapshot().getFirst().origin()==ListeningHistory.Origin.QUEUE);
        });
        await("Next queued song after natural end",()->playing()&&rt.engine().current().id.equals(trackA),()->{
            check("Queue drains FIFO after natural completion",rt.engine().queue().empty()&&rt.engine().queuedPlaying());
            check("Natural queued starts each create history entry",rt.engine().history().size()==2);
            frozen=audio;sequence=rt.engine().playbackSequence();rt.togglePause();
        });
        add("Queued native pause",30,()->{
            check("Queued music pauses native source",audio.state()==AL10.AL_PAUSED&&rt.engine().paused());frozen=audio;
        });
        add("Queued pause position",30,()->{
            check("Queued paused position stays fixed",audio.source()==frozen.source()&&Math.abs(audio.offset()-frozen.offset())<0.002);
            rt.togglePause();
        });
        add("Queue resume",10,()->{
            check("Queued resume advances same native source",playing()&&audio.source()==frozen.source()&&Math.abs(audio.offset()-frozen.offset())>0.02);
            check("Pause and resume do not duplicate history",rt.engine().history().size()==2&&rt.engine().playbackSequence()==sequence);
            rt.queueTrack(trackA,manual.id);rt.overridePlaylist(ambient.id,OverrideMode.UNTIL_RELEASED);
        });
        await("New override playback",IntegrationSuite::playing,()->{
            check("New override retains waiting queue",rt.engine().queueWaitingForOverride()&&rt.engine().queue().size()==1);
            show(ambient.id);click("Queue / history");click("Use queue next");click("Next song now");
        });
        await("Queue resumes over held override",()->playing()&&rt.engine().queuedPlaying(),()->{
            check("Queue resumes with retained override",manual.id.equals(rt.engine().sourcePlaylistId())&&ambient.id.equals(rt.engine().overrideId()));
            manual.tracks.put(trackB,false);check("Disabled song cannot queue",!rt.queueTrack(trackB,manual.id));manual.tracks.put(trackB,true);
            click("History");check("History UI opens with real starts",screen("ListeningScreen")&&(boolean)field(mc.gui.screen(),"showHistory"));
            row("list",0);click(widgets().stream().filter(w->w.getMessage().getString().equals("Queue")).toList().getLast());check("History re-queues song",rt.engine().queue().size()==1);
            click("Add to playlist");check("History destination selector opens",screen("HistoryPlaylistScreen"));mc.gui.screen().onClose();
            row("list",0);click("Play now");check("History replay keeps pending queue",rt.engine().queue().size()==1);shot("history");
        });
    }
    private static void profileChecks(){
        add("Profiles UI copy",20,()->{
            show(ambient.id);click("Profiles");check("Profiles menu opens",screen("ProfilesScreen"));
            check("Default cannot be deleted",!control("Delete selected").active);name("Integration copy");click("Copy selected");
            copyProfile=(String)field(mc.gui.screen(),"selected");
            check("Copy remains inactive until Use now",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT)&&lib.profiles.exists(copyProfile));
            click("Link current");check("Link current binds exact world",lib.profiles.forWorld(rt.worldKey()).equals(copyProfile));
            click("Linked worlds");check("Linked worlds menu lists binding",screen("ProfileLinksScreen"));click("Back");
            rt.togglePause();click("Use now");
            check("Use now selects copied profile",lib.profiles.activeId.equals(copyProfile)&&screen("LibraryScreen"));
            check("Profile switch clears queue pause and override",rt.engine().queue().empty()&&!rt.engine().paused()&&rt.engine().overrideId()==null&&rt.engine().current()==null);
            var copied=lib.playlist(ambient.id);check("Copied playlist owns independent settings",copied!=ambient&&copied.conditions!=ambient.conditions&&copied.volume==0.25);
            copied.volume=0.61;lib.settings.silenceMinSeconds=7;lib.settings.silenceMaxSeconds=9;
            check("Shared music survives profile switch",lib.track(trackA)!=null&&lib.track(trackB)!=null);
            rt.activateProfile(PlaylistProfiles.DEFAULT);
            check("Default volume and timing stay independent",lib.playlist(ambient.id).volume==0.25&&lib.settings.silenceMinSeconds==3);
            rt.activateProfile(copyProfile);check("Copied settings return on activation",lib.playlist(ambient.id).volume==0.61&&lib.settings.silenceMinSeconds==7);
            rt.activateProfile(PlaylistProfiles.DEFAULT);show(ambient.id);shot("profiles");
        });
        add("Import owns originating profile",5,()->{
            rt.importPaths(List.of(Path.of("../../fixtures/with-cover/long.wav").toAbsolutePath()),copyProfile,manual.id,false);
            rt.activateProfile(copyProfile);
        });
        await("Originating-profile import completion",IntegrationSuite::idle,()->{
            check("Import finishes in originating profile",lib.playlist(manual.id).tracks.size()==3);
            rt.activateProfile(PlaylistProfiles.DEFAULT);check("Import does not assign other profile",lib.playlist(manual.id).tracks.size()==2);
        });
    }
    private static void repairChecks(){
        add("Move generated folder",5,()->{
            checkedIO(()->Files.move(root.resolve("original"),root.resolve("moved")));
            rt.rescan();show(ambient.id);click("Repair / missing");
        });
        await("Missing source diagnostics",IntegrationSuite::idle,()->{
            check("Rescan reports moved root and songs",rt.sourceHealth()!=null&&rt.sourceHealth().issues().size()==3);
            check("Repair menu displays missing sources",screen("LibraryRepairScreen"));shot("missing-folder");
            rt.prepareRepair(new LibraryRepair.Request(true,root.resolve("original").toString(),root.resolve("moved")));
        });
        await("Repair preview",()->idle()&&rt.pendingRepair()!=null,()->{
            check("Folder preview finds exact relative paths",rt.pendingRepair().found()==2&&rt.pendingRepair().missing()==0);
            check("Repair preview opens automatically",screen("RepairPreviewScreen"));
            check("Preview has not mutated library paths",lib.track(trackA).path.contains("original"));click("Cancel");
            check("Cancel preserves missing original references",rt.pendingRepair()==null&&lib.track(trackA).path.contains("original"));
            rt.prepareRepair(new LibraryRepair.Request(true,root.resolve("original").toString(),root.resolve("moved")));
        });
        await("Apply moved folder",()->idle()&&rt.pendingRepair()!=null,()->{
            shot("repair-preview");click("Apply repair");
            check("Apply reconnects same track identity",lib.track(trackA).path.equals(root.resolve("moved/Album/Long.wav").toString()));
            check("Folder link follows new root",lib.folders.stream().anyMatch(f->f.path.equals(root.resolve("moved").toString())));
            check("Inactive profile retains repaired assignments",lib.profiles.playlists(lib,copyProfile).stream().filter(p->p.id.equals(ambient.id)).findFirst().orElseThrow().tracks.containsKey(trackA));
        });
        await("Repaired source health",IntegrationSuite::idle,()->{
            check("Repair follow-up clears missing report",rt.sourceHealth().issues().isEmpty());rt.rescan();
        });
        await("Rescan repaired root",IntegrationSuite::idle,()->{
            check("Rescan preserves repaired IDs without duplicates",lib.tracks.stream().filter(t->!t.vanilla).count()==3&&lib.track(trackA)!=null&&ambient.tracks.size()==2);
            rt.overridePlaylist(ambient.id,OverrideMode.UNTIL_RELEASED);rt.nextSong();
        });
        await("Repaired native playback",IntegrationSuite::playing,()->{
            check("Repaired song plays through native decoder",ambient.id.equals(rt.engine().sourcePlaylistId())&&rt.engine().error().isBlank());
            rt.engine().reset();
        });
        add("Rename stopped generated song",20,()->{
            checkedIO(()->Files.move(root.resolve("moved/Album/Short.wav"),root.resolve("moved/Album/Renamed.wav")));rt.checkLibrary();
        });
        await("Renamed song detection",IntegrationSuite::idle,()->{
            check("Renamed song remains individual missing item",rt.sourceHealth().issues().size()==1&&rt.sourceHealth().issues().getFirst().id().equals(trackB));
            rt.prepareRepair(new LibraryRepair.Request(false,trackB,root.resolve("moved/Album/Renamed.wav")));
        });
        await("Renamed song repair preview",()->idle()&&rt.pendingRepair()!=null,()->{
            // This report screen has already shown a prior plan; a new one must still open.
            check("Individual repair opens a new preview",screen("RepairPreviewScreen"));click("Apply repair");
        });
        await("Renamed song repaired",IntegrationSuite::idle,()->{
            check("Individual repair keeps assignments and clears report",lib.track(trackB).path.endsWith("Renamed.wav")&&ambient.tracks.containsKey(trackB)&&rt.sourceHealth().issues().isEmpty());
            checkedIO(()->check("Folder and individual repair preserve generated audio bytes",
                Files.mismatch(root.resolve("moved/Album/Long.wav"),Path.of("../../fixtures/long.wav"))==-1
                &&Files.mismatch(root.resolve("moved/Album/Renamed.wav"),Path.of("../../fixtures/short.wav"))==-1));
        });
    }
    private static void presetChecks(){
        add("Export portable setups",5,()->{
            location=lib.createPlaylist("Integration shared location");location.enabled=false;location.includeVanilla=false;location.volume=0.42;
            location.finishCurrentSong=true;location.finishCurrentSongOnExit=true;location.playOnTrigger=true;
            location.conditions.areaEnabled=true;var area=location.conditions.area;area.configured=true;area.world=rt.worldKey();area.dimension=rt.context().dimension();
            area.x=rt.context().x();area.y=rt.context().y();area.z=rt.context().z();area.radius=73;
            location.tracks.put(trackA,true);show(location.id);click("Import / export");
            check("Sharing selection menu opens",screen("PlaylistSharingScreen"));click("None");check("Empty export is disabled",!control("Export selected...").active);
            click("All");shot("sharing-selection");
            rt.exportPresets(root.resolve("shared.afmrp"),PlaylistPresets.capture(lib,Set.of(ambient.id,location.id),false));
        });
        await("Export written",IntegrationSuite::idle,()->{
            checkedIO(()->{String raw=Files.readString(root.resolve("shared.afmrp"));
                check("Shared file omits music and private world identifiers",!raw.contains(rt.worldKey().replace("\\","\\\\"))&&!raw.contains("Long.wav")&&!raw.contains("Renamed.wav"));});
            rt.preparePresetImport(root.resolve("shared.afmrp"),lib.profiles.activeId,ambient.id);
        });
        await("Sharing review",()->idle()&&rt.pendingPreset()!=null,()->{
            check("Imported setup opens preview before mutation",screen("PresetImportScreen"));check("Location import enabled inside world",control("Add 2 playlists").active);
            click("Cancel");check("Cancel clears pending import",rt.pendingPreset()==null);
            rt.preparePresetImport(root.resolve("shared.afmrp"),lib.profiles.activeId,ambient.id);
        });
        await("Review same file again",()->idle()&&rt.pendingPreset()!=null,()->{
            click(prefix("Insert:"));check("Priority can change to top",control("Insert: Top priority").active);shot("sharing-preview");click("Add 2 playlists");
            check("Import creates new ordered playlist copies",lib.playlists.get(0).name.equals(ambient.name)&&lib.playlists.get(1).name.equals(location.name)&&!lib.playlists.get(0).id.equals(ambient.id));
            var p=lib.playlists.get(1);importedLocation=p.id;
            check("Imported playlist retains flags and volume",p.volume==0.42&&p.finishCurrentSong&&p.finishCurrentSongOnExit&&p.playOnTrigger&&!p.enabled&&!p.includeVanilla);
            check("Imported location binds to current world and retains radius",p.conditions.area.world.equals(rt.worldKey())&&p.conditions.area.radius==73);
            check("Imported playlists carry no song assignments",p.tracks.isEmpty()&&lib.playlists.getFirst().tracks.isEmpty());
            check("Import returns to fresh selected workspace",screen("LibraryScreen"));
            checkedIO(()->Files.writeString(root.resolve("invalid.afmrp"),"{\"format\":\"afmr-playlists\",\"playlists\":[{\"name\":\"bad\",\"unknown\":true}]}"));
            rt.preparePresetImport(root.resolve("invalid.afmrp"),lib.profiles.activeId,ambient.id);
        });
        await("Invalid import rejected",IntegrationSuite::idle,()->{
            check("Malformed setup leaves no pending import",rt.pendingPreset()==null&&rt.taskStatus().startsWith("Playlist import failed:"));
            rt.exportPresets(root.resolve("wrong-extension.txt"),PlaylistPresets.capture(lib,Set.of(location.id),false));
            check("Wrong export extension produces actionable message",rt.taskStatus().contains(".afmrp")&&!rt.scanning());
        });
    }
    private static void restartChecks()throws Exception{
        copyProfile=Files.readString(root.resolve("expected-profile.txt"));
        check("Actual game restart activates world-linked profile",lib.profiles.activeId.equals(copyProfile));
        check("World link survived shutdown",lib.profiles.forWorld(rt.worldKey()).equals(copyProfile));
        ambient=lib.playlists.stream().filter(p->p.name.equals("Integration ambient")).findFirst().orElseThrow();
        check("Playlist volume survives game restart",ambient.volume==0.37);
        check("Profile timing survives game restart",lib.settings.silenceMinSeconds==17&&lib.settings.silenceMaxSeconds==29);
        check("Session queue pause and override clear after restart",rt.engine().queue().empty()&&!rt.engine().paused()&&rt.engine().overrideId()==null);
        check("Listening history survives actual game shutdown",rt.engine().history().size()>5);
        check("Repaired paths survive restart",lib.tracks.stream().anyMatch(t->t.path.endsWith("moved\\Album\\Renamed.wav")));
        rt.activateProfile(PlaylistProfiles.DEFAULT);
        check("Other profile retains imported setups",lib.playlists.stream().filter(p->p.name.equals("Integration shared location")).count()==2);
        check("Shared setup locations survive restart",lib.playlists.stream().filter(p->p.name.equals("Integration shared location")).allMatch(p->p.conditions.area.radius==73&&p.conditions.area.world.equals(rt.worldKey())));
        show(ambient.id);click("Queue / history");click("History");shot("history-after-restart");
        check("Persistent history visible in native screen",screen("ListeningScreen")&&((List<?>)field(mc.gui.screen(),"rows")).size()>5);
        rt.activateProfile(copyProfile);show(ambient.id);click("Until released");
        await("Restart native decoder",IntegrationSuite::playing,()->{
            check("Repaired library plays after full restart",playing());
            rt.engine().queue().clear();rt.returnAutomatic();rt.skip();
            show(ambient.id);click("Queue / history");click("History");click("Clear history");
            check("History Clear resets live native screen",rt.engine().history().size()==0&&((List<?>)field(mc.gui.screen(),"rows")).isEmpty());
            rt.save();shot("fresh-profile-after-restart");
        });
    }
    private static void freshChecks(){
        check("Fresh installation loads successfully without AFMR config",rt.enabled());
        check("Fresh installation seeds exactly six templates",lib.playlists.size()==6);
        check("Fresh default template priority",lib.playlists.stream().map(p->p.id).toList().equals(List.of("combat","home","dark_exploration","underground","night_outside","day_outside")));
        check("Fresh template Minecraft switches and volumes use intended defaults",lib.playlists.stream().allMatch(p->p.volume==1&&p.includeVanilla!=p.id.equals("dark_exploration")));
        check("Fresh timing uses Minecraft-like ten to twenty minutes",lib.settings.silenceMinSeconds==600&&lib.settings.silenceMaxSeconds==1200);
        check("Fresh fades and buffer add no extra delay",lib.settings.fadeInSeconds==0&&lib.settings.fadeOutSeconds==0&&lib.settings.bufferSeconds==0);
        check("Fresh now-playing uses Minecraft style",lib.settings.nowPlayingStyle==NowPlayingStyle.MINECRAFT);
        check("Fresh library has no local imports or folder links",lib.tracks.stream().allMatch(t->t.vanilla)&&lib.folders.isEmpty());
        check("Fresh history and queue are empty",rt.engine().history().size()==0&&rt.engine().queue().empty());
        check("Fresh setup uses unlinked Default profile",lib.profiles.activeId.equals(PlaylistProfiles.DEFAULT)&&lib.profiles.bindings.isEmpty());
        check("Fresh Next and Pause keys are unbound",Arrays.stream(mc.options.keyMappings).filter(k->Set.of("key.afmr.next","key.afmr.pause").contains(k.getName())).allMatch(k->k.getKey().getValue()==InputConstants.UNKNOWN.getValue()));
        show("day_outside");shot("fresh-installation");rt.nextSong();
        await("Fresh vanilla playback",IntegrationSuite::playing,()->check("Fresh vanilla soundtrack plays through actual native channel",rt.engine().current().vanilla&&playing()));
    }
}
