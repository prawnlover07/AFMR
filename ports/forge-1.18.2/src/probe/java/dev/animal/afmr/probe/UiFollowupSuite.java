package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.sounds.SoundSource;
import com.mojang.blaze3d.platform.InputConstants;


import java.nio.file.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Follow-up UI/font/mute checks against generated assets in the disposable profile only. */
@net.minecraftforge.fml.common.Mod("afmr_probe")
public final class UiFollowupSuite {
    public UiFollowupSuite(){net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ClientTickEvent e)->{if(e.phase==net.minecraftforge.event.TickEvent.Phase.END){tick();CompanionSuite.run(Minecraft.getInstance());IntegrationSuite.tick();}});}
    private static boolean loadingWorld;
    private static int menuTicks;
    private record Step(String name,BooleanSupplier ready,Runnable action){}
    private record AudioSample(SoundInstance sound,int state,float offset,float gain,float listenerGain,double progress){}
    private static volatile AudioSample audio=new AudioSample(null,0,0,0,1,0);
    private static SoundInstance otherSound;
    private static volatile int otherState;
    private static final List<Step> steps=new ArrayList<>();
    private static final Path results=Boolean.getBoolean("afmr.smoke.nowPlayingKey")?Path.of("now-playing-key-results.txt"):Boolean.getBoolean("afmr.smoke.navigation")?Path.of("navigation-results.txt"):Boolean.getBoolean("afmr.smoke.clarity")?Path.of("clarity-results.txt"):Boolean.getBoolean("afmr.smoke.muteWarning")?Path.of("mute-warning-results.txt"):Path.of("ui-followup-results.txt");
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static LibraryScreen screen;
    private static Playlist p,q;
    private static ColorTheme openingTheme;
    private static String savedTheme;
    private static Track first,second;
    private static int startup,index,elapsed,passed,failed;
    private static boolean started,finished;
    private static Object panel;
    private static long mutedAt,sequence;
    private static double primaryAt;
    private static java.util.concurrent.CompletableFuture<Boolean> decodedPosition;
    private static float pausedOffset;
    private static double mutedProgress;
    private static final Map<String,int[]> tabs=new HashMap<>();
    private static Object field(Object value,String name){for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(value);}catch(NoSuchFieldException ignored){}catch(Exception error){throw new RuntimeException(error);}throw new IllegalArgumentException(name);}
    private static void set(Object object,String name,Object value){try{var field=object.getClass().getDeclaredField(name);field.setAccessible(true);field.set(object,value);}catch(Exception error){throw new RuntimeException(error);}}
    private static void log(String text){try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception error){throw new RuntimeException(error);}System.out.println("AFMR UI FOLLOWUP "+text);}
    private static void check(String label,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+label);}
    private static List<AbstractWidget> widgets(){return mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private static AbstractWidget widget(String label){return widgets().stream().filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow(()->new IllegalStateException(label+" on "+mc.screen.getClass().getSimpleName()));}
    private static float scale(){return mc.screen instanceof LibraryScreen?(float)field(mc.screen,"scale"):1;}
    private static void clickAt(double x,double y,int button){var current=mc.screen;current.mouseClicked(x,y,button);current.mouseReleased(x,y,button);}
    private static void click(AbstractWidget control){
        if(!control.visible&&mc.screen instanceof LibraryScreen){panel=field(screen,"settingsPanel");try{var scroll=panel.getClass().getDeclaredMethod("scrollTo",int.class);scroll.setAccessible(true);for(int i=0;i<80&&!control.visible;i++)scroll.invoke(panel,i);}catch(Exception error){throw new RuntimeException(error);}}
        if(!control.active||!control.visible)throw new IllegalStateException("Unavailable "+control.getMessage().getString());float scale=scale();clickAt((control.x+control.getWidth()/2d)*scale,(control.y+control.getHeight()/2d)*scale,InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static void click(String label){click(widget(label));}
    @SuppressWarnings("unchecked") private static void menu(String label){click(((List<AbstractWidget>)field(screen,"menuButtons")).stream().filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow());}
    private static void add(String name,Runnable action){steps.add(new Step(name,()->true,action));}
    private static void await(String name,BooleanSupplier ready,Runnable action){steps.add(new Step(name,ready,action));}
    private static void show(){mc.setScreen(screen);panel=field(screen,"settingsPanel");}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"ui-followup-"+name+".png",mc.getMainRenderTarget(),message->{});}
    private static void rightSong(int row){var list=(AbstractWidget)field(screen,"songs");float scale=scale();clickAt((list.x+50)*scale,(list.y+row*14+7)*scale,InputConstants.MOUSE_BUTTON_RIGHT);}
    @SuppressWarnings("unchecked") private static void sample(){
        if(rt==null)return;Object engine=field(mc.getSoundManager(),"soundEngine");var channels=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(engine,"instanceToChannel");var sound=(SoundInstance)field(field(rt.engine(),"audio"),"sound");var handle=sound==null?null:channels.get(sound);
        if(handle!=null)handle.execute(channel->{
            int source=(int)field(channel,"source");float offset=org.lwjgl.openal.AL10.alGetSourcef(source,org.lwjgl.openal.AL11.AL_SEC_OFFSET);var previous=audio;
            // Streaming unqueues completed buffers, so AL_SEC_OFFSET may wrap. Count observed forward progress.
            double progress=previous.sound==sound?previous.progress+Math.max(0,offset-previous.offset):0;
            audio=new AudioSample(sound,org.lwjgl.openal.AL10.alGetSourcei(source,org.lwjgl.openal.AL10.AL_SOURCE_STATE),offset,org.lwjgl.openal.AL10.alGetSourcef(source,org.lwjgl.openal.AL10.AL_GAIN),org.lwjgl.openal.AL10.alGetListenerf(org.lwjgl.openal.AL10.AL_GAIN),progress);
        });
        if(otherSound!=null){var other=channels.get(otherSound);if(other==null)otherState=org.lwjgl.openal.AL10.AL_STOPPED;else other.execute(channel->otherState=org.lwjgl.openal.AL10.alGetSourcei((int)field(channel,"source"),org.lwjgl.openal.AL10.AL_SOURCE_STATE));}
    }
    public static void tick(){
        if(!Boolean.getBoolean("afmr.smoke.uiFollowup")||finished)return;mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.level==null||mc.player==null){if(!loadingWorld&&mc.screen!=null&&mc.getOverlay()==null&&++menuTicks>=60){loadingWorld=true;mc.setScreen(null);if(Files.isDirectory(Path.of("saves/AFMR Smoke")))mc.loadLevel("AFMR Smoke");
                else {var registries=net.minecraft.core.RegistryAccess.BUILTIN.get();mc.createLevel("AFMR Smoke",new net.minecraft.world.level.LevelSettings("AFMR Smoke",net.minecraft.world.level.GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.DataPackConfig.DEFAULT),registries,net.minecraft.world.level.levelgen.WorldGenSettings.makeDefault(registries));}}return;}if(++startup<80)return;Files.createDirectories(results.toAbsolutePath().getParent());Files.writeString(results,"");if(Boolean.getBoolean("afmr.smoke.packagedCodecs")){DecoderChecks.run();finished=true;mc.stop();return;}prepare();started=true;return;}
            sample();if(++elapsed%5!=0||elapsed<10)return;
            if(index==steps.size()){finish();return;}
            var step=steps.get(index);if(!step.ready.getAsBoolean()){if(elapsed<1800)return;throw new IllegalStateException("Timed out: "+step.name+"; font="+rt.fonts().status()+"; audio="+audio);}
            index++;elapsed=0;log("STEP "+step.name);step.action.run();
        }catch(Throwable error){failed++;log("FAIL "+error);error.printStackTrace();shot("failure");finish();}
    }
    private static void finish(){finished=true;log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.setScreen(null);mc.stop();}
    @SuppressWarnings("unchecked") private static void prepare(){
        try{SurfaceCoverChecks.run();}catch(Exception e){throw new RuntimeException(e);}
        rt=ClientRuntime.get();lib=rt.library();rt.engine().reset();lib.settings.menuFontId=lib.settings.widgetFontId="minecraft:default";lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=.25;lib.settings.menuImageEnabled=false;
        mc.options.pauseOnLostFocus=false;mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(1d));mc.options.setSoundCategoryVolume(SoundSource.MUSIC,(float)(1d));
        lib.playlists=new ArrayList<>();p=new Playlist("ui-followup-a","Follow-up A");q=new Playlist("ui-followup-b","Follow-up B");p.includeVanilla=q.includeVanilla=false;p.enabled=q.enabled=false;lib.playlists.add(p);lib.playlists.add(q);
        for(int i=0;i<18;i++){var extra=new Playlist("ui-followup-"+i,"Priority "+i);extra.enabled=false;lib.playlists.add(extra);}
        Path song=Path.of("../../../testing/native/fixtures/long.wav").toAbsolutePath().normalize();first=new Track("file:ui_followup_a",song.toString(),"Generated A",false);second=new Track("file:ui_followup_b",song.toString(),"Generated B",false);lib.tracks.add(first);lib.tracks.add(second);lib.rebuildTrackIndex();p.tracks.put(first.id,true);p.tracks.put(second.id,true);
        lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=lib.settings.bufferSeconds=0;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.jukeboxMode=JukeboxMode.OFF;
        screen=new LibraryScreen(null,rt,p.id);show();
        if(Boolean.getBoolean("afmr.smoke.nowPlayingKey")){popupKeyChecks();return;}
        if(Boolean.getBoolean("afmr.smoke.navigation")){
            navigationChecks();
            if(Boolean.getBoolean("afmr.smoke.clarity")){add("Reset for theme regression",()->{rt.engine().reset();((Map<?,?>)field(screen,"pages")).clear();p.enabled=q.enabled=false;p.conditions=new PlaylistConditions();p.tracks.put(first.id,true);p.tracks.put(second.id,true);lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;});clarityChecks();}
            return;
        }
        if(Boolean.getBoolean("afmr.smoke.clarity")){clarityChecks();return;}
        if(Boolean.getBoolean("afmr.smoke.muteWarning")){rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);muteChecks();return;}
        add("Empty / active rules",()->{check("No active filters gives Show rules",widget("Show rules").visible);check("Song toolbar actions are absent",widgets().stream().noneMatch(w->Set.of("Normalize","Preview song","Queue","Enable","Disable","Remove").contains(w.getMessage().getString())));click("Show rules");click("Time of day");click("Fewer rules");check("Only the enabled filter appears compactly",widgets().stream().noneMatch(w->w.getMessage().getString().startsWith("Playback area:"))&&widget("Time of day").visible);});
        add("Undo and redo",()->{click("Undo");check("Undo restores the empty condition set",!p.conditions.timeEnabled&&widget("Show rules").visible);click("Redo");check("Redo restores the filter",p.conditions.timeEnabled);click("Undo");});
        add("Priority drag",()->{var rows=(AbstractWidget)field(screen,"priorities");float scale=scale();double x=(rows.x+40)*scale,y=(rows.y+7)*scale;screen.mouseClicked(x,y,0);screen.mouseDragged(x,y+2*14*scale,0,0,28*scale);screen.mouseReleased(x,y+2*14*scale,0);check("Dragging commits a new priority",lib.playlists.indexOf(p)==2);click("Undo");check("Undo restores priority",lib.playlists.get(0)==p);click("Redo");check("Redo restores drag order",lib.playlists.indexOf(p)==2);click("Undo");});
        add("Song context menu",()->{var selection=(Set<String>)field(screen,"songSelection");selection.add(first.id);selection.add(second.id);rightSong(0);check("Song right-click keeps the existing multi-selection",selection.size()==2);check("Context controls force opaque backgrounds",((List<?>)field(screen,"menuButtons")).stream().allMatch(button->(boolean)field(button,"opaque")));});
        add("Song menu render and edit",()->{shot("song-menu");menu("Disable 2 songs");check("Song menu applies to the full selection",Boolean.FALSE.equals(p.tracks.get(first.id))&&Boolean.FALSE.equals(p.tracks.get(second.id)));click("Undo");check("Song edits undo",Boolean.TRUE.equals(p.tracks.get(first.id))&&Boolean.TRUE.equals(p.tracks.get(second.id)));});
        add("Stable navigation",()->{float scale=scale();for(String name:List.of("Playlists","Library","Timing","Display","Shortcuts","Playback status","Queue / history","Profiles")){var w=widget(name);tabs.put(name,new int[]{Math.round(w.x*scale),Math.round(w.y*scale),Math.round(w.getWidth()*scale),Math.round(w.getHeight()*scale)});}click("Timing");for(var entry:tabs.entrySet()){var w=widget(entry.getKey());int[] b=entry.getValue();check("Stable tab "+entry.getKey(),Math.abs(w.x-b[0])<=1&&Math.abs(w.y-b[1])<=1&&Math.abs(w.getWidth()-b[2])<=1&&Math.abs(w.getHeight()-b[3])<=1);}});
        add("Timing render",()->{shot("timing-tabs");click("Playlists");});
        add("Background import",()->{try{var image=new java.awt.image.BufferedImage(4,4,java.awt.image.BufferedImage.TYPE_INT_RGB);for(int x=0;x<4;x++)for(int y=0;y<4;y++)image.setRGB(x,y,0x00FF00);Path file=Path.of("ui-followup-background.png");javax.imageio.ImageIO.write(image,"png",file.toFile());rt.menuBackground().choose(file);}catch(Exception error){throw new RuntimeException(error);}});
        await("Background uploaded",()->!rt.menuBackground().busy(),()->{check("Custom background is loaded",(boolean)field(rt.menuBackground(),"uploaded"));shot("background");});
        add("Background present on playlist view",()->{try{var image=javax.imageio.ImageIO.read(Path.of("screenshots/ui-followup-background.png").toFile());float scale=scale();int x=Math.round(((int)field(screen,"tableX")+100)*scale),y=Math.round(300*scale);int rgb=image.getRGB(x,y);check("Playlist view draws the green image behind its surfaces",(rgb>>8&255)>140&&(rgb>>16&255)<80&&(rgb&255)<80);}catch(Exception error){throw new RuntimeException(error);}});
        add("Resource-pack menu font",()->{String choice=rt.fonts().choices().stream().filter(id->!id.equals("minecraft:default")&&!id.startsWith("afmr:")).findFirst().orElseThrow();rt.fonts().select(CustomFonts.Target.MENU,choice);show();check("Menu font can use a loaded resource-pack font",rt.fonts().font(CustomFonts.Target.MENU)!=mc.font);check("Menu font leaves widget font independent",rt.fonts().selection(CustomFonts.Target.WIDGET).equals("minecraft:default"));rt.fonts().select(CustomFonts.Target.WIDGET,choice);check("Widget supports resource-pack fonts independently",rt.fonts().font(CustomFonts.Target.WIDGET)!=mc.font&&rt.fonts().selection(CustomFonts.Target.MENU).equals(choice));rt.fonts().select(CustomFonts.Target.WIDGET,"minecraft:default");rt.fonts().select(CustomFonts.Target.MENU,"minecraft:default");show();});
        add("Import menu TTF",()->rt.fonts().choose(CustomFonts.Target.MENU,Path.of("../src/test/resources/fonts/afmr-fixture.ttf")));
        await("Imported menu font ready",()->!rt.fonts().busy(),()->{check("Imported menu font selected",rt.fonts().selection(CustomFonts.Target.MENU).equals("afmr:custom_menu"));check("Imported glyph metrics are used",rt.fonts().font(CustomFonts.Target.MENU).width("WWWW")!=mc.font.width("WWWW"));check("Imported menu font retains missing-glyph fallback",rt.fonts().font(CustomFonts.Target.MENU).width("Ω文")==mc.font.width("Ω文"));check("TTF import leaves widget default",rt.fonts().selection(CustomFonts.Target.WIDGET).equals("minecraft:default"));rt.fonts().choose(CustomFonts.Target.WIDGET,Path.of("../src/test/resources/fonts/afmr-fixture.ttf"));});
        await("Independent widget TTF ready",()->!rt.fonts().busy(),()->{check("Widget font imports independently",rt.fonts().selection(CustomFonts.Target.WIDGET).equals("afmr:custom_widget")&&rt.fonts().selection(CustomFonts.Target.MENU).equals("afmr:custom_menu"));check("Widget metrics use its selected font",rt.fonts().font(CustomFonts.Target.WIDGET).width("WWWW")!=mc.font.width("WWWW"));check("Imported widget font retains missing-glyph fallback",rt.fonts().font(CustomFonts.Target.WIDGET).width("Ω文")==mc.font.width("Ω文"));rt.fonts().select(CustomFonts.Target.MENU,"minecraft:default");show();rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);});
        muteChecks();
    }
    private static void muteChecks(){
        await("Generated music active",()->rt.engine().current()!=null&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING,()->{
            sequence=rt.engine().playbackSequence();mutedProgress=audio.progress;pausedOffset=audio.offset;mutedAt=System.nanoTime();mc.options.setSoundCategoryVolume(SoundSource.MUSIC,(float)(0d));
        });
        await("Music mute leaves playback running",()->System.nanoTime()-mutedAt>1_300_000_000L,()->{
            log("MEASURE music: progress="+(audio.progress-mutedProgress)+"; gain="+audio.gain+"; listener="+audio.listenerGain);
            check("Music mute does not pause AFMR",!rt.engine().paused()&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING);
            check("Muted song position advances",audio.progress>mutedProgress+.5);
            check("Music gain is exactly zero",audio.gain==0);check("Menu detects Minecraft mute",rt.minecraftMusicMuted());
            check("Mute retains the same song channel",rt.engine().playbackSequence()==sequence&&rt.engine().current()!=null);shot("muted");
            mc.options.setSoundCategoryVolume(SoundSource.MUSIC,(float)(1d));mutedAt=System.nanoTime();
        });
        await("Music unmute leaves playback running",()->System.nanoTime()-mutedAt>350_000_000L&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING,()->{
            check("Unmute retains the song without changing pause",!rt.engine().paused()&&rt.engine().playbackSequence()==sequence);
            check("Unmute clears the warning",!rt.minecraftMusicMuted());
            otherSound=net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.MUSIC_DISC_CAT,1f);mc.getSoundManager().play(otherSound);
        });
        await("Ordinary sound active",()->otherState==org.lwjgl.openal.AL10.AL_PLAYING,()->{mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(0d));mutedProgress=audio.progress;pausedOffset=audio.offset;mutedAt=System.nanoTime();});
        await("Master mute leaves playback running",()->System.nanoTime()-mutedAt>1_300_000_000L,()->{
            log("MEASURE master: progress="+(audio.progress-mutedProgress)+"; gain="+audio.gain+"; listener="+audio.listenerGain);
            check("Master mute does not pause AFMR",!rt.engine().paused()&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING);
            check("Master-muted song position advances",audio.progress>mutedProgress+.5);
            check("Master effective gain is exactly zero",audio.gain*audio.listenerGain==0);check("Master mute displays warning",rt.minecraftMusicMuted());
            check("Master mute still stops ordinary sounds",otherState==org.lwjgl.openal.AL10.AL_STOPPED);
            mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(1d));mutedAt=System.nanoTime();
        });
        await("Master unmute leaves playback running",()->System.nanoTime()-mutedAt>350_000_000L&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING,()->{
            check("Master unmute retains the same song",rt.engine().playbackSequence()==sequence&&!rt.engine().paused());
            check("Unmute leaves ordinary sounds stopped",!mc.getSoundManager().isActive(otherSound));
            rt.togglePause();pausedOffset=audio.offset;mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(0d));mutedAt=System.nanoTime();
        });
        await("Manual pause survives mute",()->System.nanoTime()-mutedAt>1_200_000_000L,()->{
            check("Manual pause remains at its position",rt.engine().paused()&&audio.state==org.lwjgl.openal.AL10.AL_PAUSED&&Math.abs(audio.offset-pausedOffset)<.2);
            mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(1d));mutedAt=System.nanoTime();
        });
        await("Manual pause survives unmute",()->System.nanoTime()-mutedAt>350_000_000L,()->{
            check("Unmute does not undo manual pause",rt.engine().paused()&&audio.state==org.lwjgl.openal.AL10.AL_PAUSED);rt.togglePause();
        });
        await("Manual resume still works",()->audio.state==org.lwjgl.openal.AL10.AL_PLAYING,()->{
            check("Manual resume retains playback",!rt.engine().paused()&&rt.engine().playbackSequence()==sequence);
            if(Boolean.getBoolean("afmr.smoke.muteWarning"))naturalMutedStarts();
            rt.fonts().select(CustomFonts.Target.WIDGET,"minecraft:default");rt.menuBackground().enabled(false);lib.settings.menuBackgroundOpacity=.8;rt.save();show();shot("final");
        });
    }

    private static boolean currentNative(){return audio.sound==field(field(rt.engine(),"audio"),"sound")&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING;}
    private static void naturalMutedStarts(){
        rt.engine().reset();p.tracks.clear();
        for(int i=0;i<2;i++){var t=new Track("file:mute_short_"+i,Path.of("../../../testing/native/fixtures/short.wav").toAbsolutePath().normalize().toString(),"Generated muted short "+i,false);t.durationSeconds=1.5;lib.tracks.add(t);p.tracks.put(t.id,true);}lib.rebuildTrackIndex();
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=1;lib.settings.songsBeforeSilence=1;
        mc.options.setSoundCategoryVolume(SoundSource.MUSIC,(float)(0d));rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);
        for(String mode:List.of("Music","Master")){
            await(mode+" muted new native stream",()->rt.engine().current()!=null&&currentNative()&&audio.gain*audio.listenerGain==0,()->{check(mode+" mute permits a new silent native stream",!rt.engine().paused()&&rt.minecraftMusicMuted());sequence=rt.engine().playbackSequence();});
            await(mode+" muted natural end and gap",()->rt.engine().current()==null&&rt.engine().phase()==PlaybackEngine.Phase.SILENCE,()->check(mode+" muted completion enters normal silence",rt.engine().remainingSilence(rt.now())>0&&!rt.engine().paused()));
            await(mode+" muted next natural song",()->rt.engine().playbackSequence()>sequence&&rt.engine().current()!=null&&currentNative()&&audio.gain*audio.listenerGain==0,()->{check(mode+" muted silence ends with another silent native stream",!rt.engine().paused());
                if(mode.equals("Music")){rt.engine().reset();mc.options.setSoundCategoryVolume(SoundSource.MUSIC,(float)(1d));mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(0d));rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);}
                else{mc.options.setSoundCategoryVolume(SoundSource.MASTER,(float)(1d));}
            });
        }
        await("Unmute after natural progression",()->currentNative()&&audio.gain*audio.listenerGain>0,()->check("Unmute restores gain without pausing",!rt.engine().paused()&&!rt.minecraftMusicMuted()));
    }
    private static void point(double x,double y){
        var window=mc.getWindow();set(mc.mouseHandler,"xpos",x*window.getScreenWidth()/window.getGuiScaledWidth());set(mc.mouseHandler,"ypos",y*window.getScreenHeight()/window.getGuiScaledHeight());
    }
    private static void hoverPlaylist(int row){var list=(AbstractWidget)field(screen,"priorities");float scale=scale();point((list.x+list.getWidth()-20)*scale,(list.y+row*14+7)*scale);}
    @SuppressWarnings("unchecked") private static void iconChecks(){
        add("Hover playlist action",()->{p.name="A long playlist name that must leave the menu button clear";screen.setFocused(null);hoverPlaylist(1);});
        await("Hovered playlist menu visible",()->((AbstractWidget)field(screen,"playlistActions")).visible&&field(screen,"actionPlaylist")==q,()->{
            var action=(AbstractWidget)field(screen,"playlistActions");var list=(AbstractWidget)field(screen,"priorities");
            check("Playlist actions occupy the reserved right edge",action.x+action.getWidth()==list.x+list.getWidth()-8);
            click(action);check("Hover menu targets its playlist",field(screen,"playlistId").equals(q.id)&&((List<?>)field(screen,"menuButtons")).size()==12);
            check("Menu click does not arm a priority drag",!(boolean)invoke(field(screen,"priorityDrag"),"armed"));
            check("Playlist context menu stays opaque",((List<AbstractWidget>)field(screen,"menuButtons")).stream().allMatch(w->(boolean)field(w,"opaque")));
        });
        add("Playlist menu render",()->{shot("transport-playlist-menu");menu("Rename playlist");((EditBox)field(mc.screen,"name")).setValue("Renamed from row menu");click("Rename");check("Row menu rename affects only its target",q.name.equals("Renamed from row menu")&&p.name.startsWith("A long"));click("Undo");check("Row menu rename participates in Undo",q.name.equals("Follow-up B"));p.name="A long playlist name that must leave the menu button clear";rt.save();point(-100,-100);var list=(AbstractWidget)field(screen,"priorities");screen.setFocused(list);if(!list.isFocused())list.changeFocus(true);});
        await("Keyboard playlist action",()->((AbstractWidget)field(screen,"playlistActions")).visible&&field(screen,"actionPlaylist")==q,()->{
            screen.keyPressed(InputConstants.KEY_F10,0,1);check("Shift+F10 opens playlist actions",((List<?>)field(screen,"menuButtons")).size()==12);screen.keyPressed(InputConstants.KEY_ESCAPE,0,0);
            var list=(AbstractWidget)field(screen,"priorities");float scale=scale();double x=(list.x+50)*scale,y=(list.y+21)*scale;
            clickAt(x,y,InputConstants.MOUSE_BUTTON_LEFT);screen.mouseClicked(x,y,InputConstants.MOUSE_BUTTON_LEFT);
            screen.mouseDragged(x,(list.y+1)*scale,InputConstants.MOUSE_BUTTON_LEFT,0,-20*scale);screen.mouseReleased(x,(list.y+1)*scale,InputConstants.MOUSE_BUTTON_LEFT);
            check("Priority dragging still works beside the row action",lib.playlists.indexOf(q)==0);click("Undo");
            list=(AbstractWidget)field(screen,"priorities");screen.setFocused(list);if(!list.isFocused())list.changeFocus(true);set(list,"offset",Math.max(0,lib.playlists.size()-list.getHeight()/14)); // The next render must hide the offscreen selected row action.
        });
        add("Offscreen action hidden",()->{check("Offscreen playlist button is not interactive",!((AbstractWidget)field(screen,"playlistActions")).visible);var list=(AbstractWidget)field(screen,"priorities");set(list,"offset",0);point(-100,-100);screen.setFocused(list);if(!list.isFocused())list.changeFocus(true);});
        await("Fresh row hit testing",()->((AbstractWidget)field(screen,"playlistActions")).visible,()->{
            var action=(AbstractWidget)field(screen,"playlistActions");var list=(AbstractWidget)field(screen,"priorities");set(list,"offset",1);var expected=lib.playlists.get(2);
            click(action);check("Click after scrolling uses the actual row",field(screen,"playlistId").equals(expected.id));screen.keyPressed(InputConstants.KEY_ESCAPE,0,0);
            show();list=(AbstractWidget)field(screen,"priorities");set(list,"offset",0);float scale=scale();clickAt((list.x+50)*scale,(list.y+7)*scale,InputConstants.MOUSE_BUTTON_LEFT);
            var panel=field(screen,"settingsPanel");((EditBox)((Map<?,?>)field(panel,"fields")).get("volume")).setValue("-1");
            list=(AbstractWidget)field(screen,"priorities");clickAt((list.x+list.getWidth()-20)*scale,(list.y+21)*scale,InputConstants.MOUSE_BUTTON_LEFT);
            check("Row menu respects invalid playlist drafts",field(screen,"playlistId").equals(p.id)&&((List<?>)field(screen,"menuButtons")).isEmpty());((EditBox)((Map<?,?>)field(panel,"fields")).get("volume")).setValue("100");
            point(-100,-100);p.enabled=true;show();
        });
        add("Primary play icon",()->{var primary=(AbstractWidget)field(screen,"playPause");check("Empty transport offers Play now",primary.getMessage().getString().equals("Play now")&&field(primary,"icon").toString().equals("PLAY"));rt.engine().rescheduleSilence(rt.now(),lib);primaryAt=rt.now();click(primary);});
        await("Primary starts music",()->rt.engine().current()!=null&&attached(org.lwjgl.openal.AL10.AL_PLAYING)&&((AbstractWidget)field(screen,"playPause")).getMessage().getString().equals("Pause"),()->{
            check("Primary changes to Pause while playing",field(field(screen,"playPause"),"icon").toString().equals("PAUSE"));check("Primary Play now bypasses a fresh gap",rt.now()-primaryAt<3);click((AbstractWidget)field(screen,"playPause"));
        });
        await("Primary resumes music",()->rt.engine().paused()&&((AbstractWidget)field(screen,"playPause")).getMessage().getString().equals("Resume"),()->{
            check("Paused transport has a Play icon and Resume label",field(field(screen,"playPause"),"icon").toString().equals("PLAY"));sequence=rt.engine().playbackSequence();click((AbstractWidget)field(screen,"nextTrack"));
            check("Next icon requests the next song without clearing pause",rt.engine().playbackSequence()==sequence&&rt.engine().current()==null&&rt.engine().paused()&&rt.engine().remainingSilence(rt.now())==0);click((AbstractWidget)field(screen,"playPause"));
        });
        await("Skip and wait stays distinct",()->!rt.engine().paused()&&attached(org.lwjgl.openal.AL10.AL_PLAYING),()->{check("Next starts after explicit Resume",rt.engine().playbackSequence()==sequence+1);hoverPlaylist(0);});
        add("Transport icon render",()->{shot("transport-icons");click("Skip & wait");check("Skip and wait starts the usual gap",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>9);});
        add("Pause a silent gap",()->{check("Skip and wait is disabled without a song",!((AbstractWidget)field(screen,"skipWait")).active);rt.engine().setPaused(true,rt.now());});
        await("Resume a silent gap",()->((AbstractWidget)field(screen,"playPause")).getMessage().getString().equals("Resume"),()->{
            double remaining=rt.engine().remainingSilence(rt.now());click((AbstractWidget)field(screen,"playPause"));
            check("Resume retains the remaining silence",!rt.engine().paused()&&rt.engine().current()==null&&Math.abs(rt.engine().remainingSilence(rt.now())-remaining)<.1);
            click((AbstractWidget)field(screen,"nextTrack"));
        });
        await("Next bypasses the gap",()->rt.engine().current()!=null&&attached(org.lwjgl.openal.AL10.AL_PLAYING),()->{check("Next icon starts immediately from a gap",true);rt.engine().reset();p.enabled=false;point(-100,-100);show();});
    }
    private static Object invoke(Object object,String name){try{var method=object.getClass().getDeclaredMethod(name);method.setAccessible(true);return method.invoke(object);}catch(Exception error){throw new RuntimeException(error);}}
    private static boolean attached(int state){return audio.sound()==field(field(rt.engine(),"audio"),"sound")&&audio.state()==state;}
    private static void query(String value){((EditBox)field(mc.screen,"search")).setValue(value);}
    private static void enter(){mc.screen.keyPressed(InputConstants.KEY_RETURN,0,0);}
    private static double popupAt;
    private static long popupSequence,popupGeneration;
    private static int popupHistory;
    private static Object popupSound;
    private static void pressPopup(){net.minecraft.client.KeyMapping.click(InputConstants.getKey("key.keyboard.f8"));}
    private static void popupKeyChecks(){
        lib.settings.nowPlaying=true;lib.settings.popupSeconds=2;lib.settings.popupDelaySeconds=60;lib.settings.widgetArtwork=true;

        first.path=Path.of("../../../testing/native/fixtures/with-cover/long.wav").toAbsolutePath().normalize().toString();
        add("Configure actual popup key",()->{
            var keys=Arrays.stream(mc.options.keyMappings).filter(k->k.getName().startsWith("key.afmr.")).toList();
            var key=keys.stream().filter(k->k.getName().equals("key.afmr.show_now_playing")).findFirst().orElseThrow();
            check("Sixteen keys in one AFMR category",keys.size()==16&&keys.stream().map(k->k.getCategory()).distinct().count()==1);
            check("Popup key defaults to unbound",key.getDefaultKey().equals(InputConstants.UNKNOWN));
            var saved=new LinkedHashMap<>(dev.animal.afmr.Afmr.backupKeys());saved.put(key.getName(),new dev.animal.afmr.library.ConfigBackup.Key("key.keyboard.f8","NONE"));
            dev.animal.afmr.Afmr.restoreKeys(saved);check("Popup key survives capture and restore",dev.animal.afmr.Afmr.backupKeys().get(key.getName()).code().equals("key.keyboard.f8"));
            mc.setScreen(null); pressPopup();
        });
        add("Empty playback",()->{check("Key during silence does not start music",rt.engine().current()==null);rt.preview(first);});
        await("Song and cached cover",()->audio.sound!=null&&audio.state==org.lwjgl.openal.AL10.AL_PLAYING&&(boolean)field(rt.overlay(),"hasCover"),()->{
            check("Normal popup still waits for its start delay",Double.isNaN((double)field(rt.overlay(),"shownAt")));
            popupSequence=rt.engine().playbackSequence();popupSound=audio.sound;popupGeneration=(long)field(rt.overlay(),"generation");
            rt.engine().enqueue(second.id,p.id,lib);popupHistory=rt.engine().history().size();pressPopup();
        });
        await("Explicit popup before start delay",()->!Double.isNaN((double)field(rt.overlay(),"shownAt")),()->{
            check("Hotkey skips the automatic start delay",rt.engine().trackPlayed(rt.now())<60);popupAt=(double)field(rt.overlay(),"shownAt");shot("manual-now-playing");
        });
        await("Usual display duration elapses",()->rt.now()-popupAt>2.1,()->{check("Hotkey uses the configured display duration",lib.settings.popupSeconds==2&&rt.now()-popupAt>2);pressPopup();});
        await("Reopen expired popup",()->(double)field(rt.overlay(),"shownAt")>popupAt,()->{
            check("Hotkey restarts the full display interval",rt.now()-(double)field(rt.overlay(),"shownAt")<1);
            check("Song and native stream are unchanged",rt.engine().current()==first&&audio.sound==popupSound&&rt.engine().playbackSequence()==popupSequence);
            check("Queue and history are unchanged",rt.engine().queue().size()==1&&rt.engine().history().size()==popupHistory);
            check("Album artwork is reused",(boolean)field(rt.overlay(),"hasCover")&&(long)field(rt.overlay(),"generation")==popupGeneration);
            check("Duration and delay settings are unchanged",lib.settings.popupSeconds==2&&lib.settings.popupDelaySeconds==60);
            popupAt=(double)field(rt.overlay(),"shownAt");rt.togglePause();pressPopup();
        });
        await("Show a paused song",()->(double)field(rt.overlay(),"shownAt")>popupAt&&audio.state==org.lwjgl.openal.AL10.AL_PAUSED,()->{
            check("Showing metadata does not resume paused audio",rt.engine().paused()&&audio.sound==popupSound);popupAt=(double)field(rt.overlay(),"shownAt");pressPopup();pressPopup();
        });
        await("Refresh while already visible",()->(double)field(rt.overlay(),"shownAt")>popupAt,()->check("Repeated presses retain playback identity",rt.engine().paused()&&rt.engine().playbackSequence()==popupSequence));

        add("Visibility disabled",()->{lib.settings.nowPlaying=false;rt.overlay().tick();popupAt=(double)field(rt.overlay(),"shownAt");pressPopup();});
        add("Respect visibility",()->{check("Hidden widget stays disabled",Double.compare(popupAt,(double)field(rt.overlay(),"shownAt"))==0&&!lib.settings.nowPlaying);rt.engine().reset();pressPopup();});
        add("No song remains no song",()->check("Hotkey cannot select a song after playback stops",rt.engine().current()==null));
    }

    @SuppressWarnings("unchecked") private static void navigationChecks(){
        lib.settings.menuBackgroundOpacity=.95;first.durationSeconds=second.durationSeconds=90;iconChecks();
        add("Verify decoded position",()->{
            var path=Path.of(first.path);
            decodedPosition=java.util.concurrent.CompletableFuture.supplyAsync(()->{
                try(var expected=dev.animal.afmr.audio.PcmDecoder.open(path);var actual=new dev.animal.afmr.audio.LocalAudioStream(path,false,null)){
                    long bytes=(long)(actual.getFormat().getFrameRate()*7.25)*actual.getFormat().getFrameSize();
                    expected.skipNBytes(bytes);byte[] wanted=expected.readNBytes(1024);
                    dev.animal.afmr.audio.StreamSeek.skip(actual,7.25,()->false);var buffer=actual.read(1024);
                    byte[] got=new byte[buffer.remaining()];buffer.get(got);return java.util.Arrays.equals(wanted,got);
                }catch(Exception error){throw new RuntimeException(error);}
            });
        });
        await("Decoded position matches",()->decodedPosition.isDone(),()->check("Seeking produces PCM from the requested file position",decodedPosition.join()));
        add("Selection counts",()->{var selected=(Set<String>)field(screen,"songSelection");selected.add(first.id);selected.add(second.id);rightSong(0);
            check("Song actions state the affected count",((List<AbstractWidget>)field(screen,"menuButtons")).stream().anyMatch(w->w.getMessage().getString().equals("Remove 2 songs")));
            screen.keyPressed(InputConstants.KEY_ESCAPE,0,0);rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED);
        });
        await("Audio starts",()->attached(org.lwjgl.openal.AL10.AL_PLAYING)&&rt.engine().current()!=null,()->{
            sequence=rt.engine().playbackSequence();rt.queueTrack(second.id,p.id);
            var list=(AbstractWidget)field(screen,"priorities");float scale=scale();clickAt((list.x+50)*scale,(list.y+21)*scale,InputConstants.MOUSE_BUTTON_LEFT);
            ((EditBox)field(screen,"songSearch")).setValue("no matches");click((AbstractWidget)field(screen,"nowPlayingTitle"));
            check("Clicking the title finds its source playlist",field(screen,"playlistId").equals(p.id));
            check("Locate clears the filter and selects the current song",field(screen,"songQuery").equals("")&&((Set<String>)field(screen,"songSelection")).contains(rt.engine().current().id));
            check("Locate preserves playback and pending queue",rt.engine().playbackSequence()==sequence&&rt.engine().queue().size()==1);
        });
        add("Workspace transport render",()->{shot("navigation-workspace");click("Pause");var bar=(AbstractWidget)field(screen,"seekBar");check("Seek hit area stays above the status line",bar.y+bar.getHeight()<=(int)field(screen,"vh")-10);float scale=scale();double x=(bar.x+(bar.getWidth()-94)*.4)*scale,y=(bar.y+8)*scale;
            double old=rt.engine().trackElapsed(rt.now());screen.mouseClicked(x,y,InputConstants.MOUSE_BUTTON_LEFT);
            check("Holding a seek previews without changing audio",Math.abs(rt.engine().trackElapsed(rt.now())-old)<.1);
            screen.mouseReleased(x,y,InputConstants.MOUSE_BUTTON_LEFT);
            check("Seek release updates the song position",Math.abs(rt.engine().trackElapsed(rt.now())-36)<.1);
        });
        await("Paused seek attaches",()->attached(org.lwjgl.openal.AL10.AL_PAUSED),()->{
            check("Native channel remains paused after seeking",rt.engine().paused()&&Math.abs(rt.engine().trackElapsed(rt.now())-36)<.1);
            check("Seek keeps playback identity and queue",rt.engine().playbackSequence()==sequence&&rt.engine().queue().size()==1);click("Resume");
        });
        await("Seek resumes",()->attached(org.lwjgl.openal.AL10.AL_PLAYING)&&rt.engine().trackElapsed(rt.now())>36.1,()->{
            check("Native playback resumes from the target",rt.engine().trackElapsed(rt.now())<40);
            for(int i=0;i<12;i++)rt.engine().seek(10+i,rt.now());
        });
        await("Latest seek wins",()->attached(org.lwjgl.openal.AL10.AL_PLAYING)&&rt.engine().trackElapsed(rt.now())>=21.1,()->{
            check("Rapid seeks retain only the latest target",rt.engine().trackElapsed(rt.now())<25&&rt.engine().error().isBlank());
        });
        for(int i=0;i<12;i++){final int target=i;add("Repeated seek "+i,()->rt.engine().seek(10+target,rt.now()));}
        add("Search timing",()->{click("Search settings");query("fade in");enter();});
        await("Search focuses timing",()->mc.screen.getFocused() instanceof EditBox box&&box.getMessage().getString().equals("Fade in (sec)"),()->{
            check("Search opens and focuses the precise setting",mc.screen.getClass().getSimpleName().equals("SettingsScreen"));
            check("Global booleans use checkboxes",widget("Normalization").getClass().getSimpleName().equals("ToggleButton"));
            var bar=(AbstractWidget)field(mc.screen,"seekBar");check("Page actions stay above the transport strip",widget("Save & back").y+20<bar.y);
            var page=mc.screen;var draft=(EditBox)page.getFocused();draft.setValue("-1");MusicMenu.hide(page);mc.setScreen(MusicMenu.reopen(null,rt));
            check("Reopen retains the actual page controls",mc.screen==page&&page.children().contains(draft));
            check("Reopen preserves invalid draft and focus",draft.getValue().equals("-1")&&page.getFocused()==draft&&draft.isFocused());
            check("Reopen restores the selected menu font",field(page,"font")==field(page,"menuFont"));
            click("Search settings");
            check("Search cannot bypass invalid numeric drafts",mc.screen.getClass().getSimpleName().equals("SettingsScreen"));
            ((EditBox)field(mc.screen,"searchTarget")).setValue("0");click("Search settings");query("widget font");enter();
        });
        add("Direct font destination",()->{check("Font search opens the widget font chooser",mc.screen.getClass().getSimpleName().equals("FontSettingsScreen"));shot("navigation-font");click("Search settings");query("home");enter();});
        add("Home search destination",()->{check("Home search returns to the playback-area action",mc.screen==screen&&widget("Set playback area here").isFocused());
            var row=widget("Playback area");var edit=widgets().stream().filter(w->w.getMessage().getString().equals(">")&&w.y==row.y).findFirst().orElseThrow();click(edit);
            check("Details arrow opens inline controls without enabling a rule",widgets().stream().anyMatch(w->w.getMessage().getString().equals("Radius (blocks)"))&&!p.conditions.areaEnabled);
            click("Playback area");check("Rule checkbox toggles without navigating",mc.screen==screen&&p.conditions.areaEnabled);click("Undo");
            var vanilla=lib.tracks.stream().filter(t->t.vanilla).findFirst().orElseThrow();if(vanilla.durationSeconds<=0)vanilla.durationSeconds=120;rt.preview(vanilla);
        });
        await("Vanilla preview starts",()->attached(org.lwjgl.openal.AL10.AL_PLAYING)&&rt.engine().current()!=null&&rt.engine().current().vanilla,()->{
            check("Resource-backed music still plays",rt.engine().previewing());check("Resource-backed song accepts seeking",rt.engine().seek(5,rt.now()));
        });
        await("Vanilla seek starts",()->attached(org.lwjgl.openal.AL10.AL_PLAYING)&&rt.engine().trackElapsed(rt.now())>=5.1,()->{
            check("Resource-backed seek plays from the target",rt.engine().trackElapsed(rt.now())<10&&rt.engine().error().isBlank());
            for(int i=0;i<8;i++){final int position=5+i;/* latest same-tick request wins */rt.engine().seek(position,rt.now());}
            click("Display");click("Menus");
        });
        add("Collect decoded buffers",()->{System.gc();check("Resource seek survives buffer collection",rt.engine().error().isBlank());});
        add("Menus render",()->{shot("navigation-menus");check("Fabric menu exposes opacity without unsupported blur",widget("Background opacity")!=null&&widgets().stream().noneMatch(w->w.getMessage().getString().startsWith("Blur background")));check("Menu navigation uses arrows",widget("Themes >")!=null);
            var page=mc.screen;int w=page.width,h=page.height;page.resize(mc,420,240);
            check("Small menus reserve the playback strip",widget("Save & back").y+20<((AbstractWidget)field(page,"seekBar")).y);
            page.keyPressed(InputConstants.KEY_END,0,0);
            check("Small menus keep the last setting reachable",widget("Menu font:").visible);page.resize(mc,w,h);click("Playlists");});
    }
    private static void clarityChecks(){
        add("Readable rule summaries",()->{
            p.conditions.timeEnabled=true;p.conditions.startTime=12000;p.conditions.endTime=23999;
            p.conditions.areaEnabled=true;p.conditions.area.configured=true;p.conditions.area.radius=64;show();
            check("Time rule shows clock hours",widget("Time of day: 18:00").getMessage().getString().contains("06:00"));
            check("Area rule shows radius",widget("Playback area: 64 blocks")!=null);
            var selection=(Set<String>)field(screen,"songSelection");selection.add(first.id);selection.add(second.id);
            set(screen,"songAnchor",first.id);set(screen,"inspected",first);var list=(AbstractWidget)field(screen,"songs");screen.setFocused(list);if(!list.isFocused())list.changeFocus(true);
        });
        await("Focused song menu button",()->((AbstractWidget)field(screen,"songActions")).visible,()->{
            var action=(AbstractWidget)field(screen,"songActions");check("Focused row exposes a visible actions button",action.visible);screen.keyPressed(InputConstants.KEY_F10,0,1);check("Shift+F10 opens song actions",((List<?>)field(screen,"menuButtons")).size()==5);screen.keyPressed(InputConstants.KEY_ESCAPE,0,0);click(action);
            check("Left-click opens the existing song menu",((List<?>)field(screen,"menuButtons")).size()==5);
            check("Menu retains multi-selection",((Set<?>)field(screen,"songSelection")).size()==2);
        });
        add("Song action render",()->{shot("clarity-song-menu");menu("Disable 2 songs");check("Action applies to both selected songs",Boolean.FALSE.equals(p.tracks.get(first.id))&&Boolean.FALSE.equals(p.tracks.get(second.id)));click("Undo");});
        add("Start source playlist",()->rt.overridePlaylist(p.id,OverrideMode.UNTIL_RELEASED));
        await("Source identity",()->rt.engine().current()!=null,()->{
            var list=(AbstractWidget)field(screen,"priorities");float scale=scale();clickAt((list.x+60)*scale,(list.y+21)*scale,InputConstants.MOUSE_BUTTON_LEFT);
            check("Editing selection can differ from playing source",field(screen,"playlistId").equals(q.id)&&rt.engine().sourcePlaylistId().equals(p.id));
        });
        add("Playing source render",()->{shot("clarity-source-marker");var list=(AbstractWidget)field(screen,"priorities");float scale=scale();clickAt((list.x+60)*scale,(list.y+7)*scale,InputConstants.MOUSE_BUTTON_LEFT);});
        add("Playing song render",()->{shot("clarity-song-marker");check("Current song belongs to the marked source",rt.engine().current()!=null&&rt.engine().sourcePlaylistId().equals(p.id));
            rt.returnAutomatic();rt.engine().reset();p.enabled=true;p.conditions.timeEnabled=p.conditions.areaEnabled=false;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=134;lib.settings.bufferSeconds=0;
        });
        await("Gap countdown",()->((String)field(screen,"playbackLabel")).startsWith("Next song in "),()->{check("Footer gives a countdown",((String)field(screen,"playbackLabel")).contains(":"));p.tracks.put(first.id,false);p.tracks.put(second.id,false);rt.save();});
        await("Empty playback reason",()->((String)field(screen,"playbackLabel")).contains("No enabled songs"),()->{check("Footer explains an empty matching playlist",true);shot("clarity-empty-playlist");click("Display");click("Menus");click("Themes");});
        add("Create preview theme",()->{check("Themes starts a separate preview",rt.themes().preview()!=null);lib.settings.savedThemes.clear();click("Save as");((EditBox)field(mc.screen,"name")).setValue("Clarity theme");click("Save");savedTheme=lib.settings.savedThemes.get(0).id;openingTheme=ColorTheme.capture("Opening",lib.settings);click("Edit colors");});
        add("Edit draft and change page",()->{((Map<String,EditBox>)field(mc.screen,"fields")).get("BACKGROUND").setValue("#553355");check("Palette previews without changing settings",rt.themes().draft().menuColors.get(MenuColor.BACKGROUND)==0x553355&&openingTheme.matches(lib.settings));click(">");check("Paging does not save the named theme",ColorTheme.find(lib.settings,savedTheme).matches(openingTheme));click("Cancel");check("Editor Cancel restores its opening preview",rt.themes().draft().matches(openingTheme));click("Edit colors");});
        add("Hide an uncommitted draft",()->{((Map<String,EditBox>)field(mc.screen,"fields")).get("BACKGROUND").setValue("#442244");MusicMenu.hide(mc.screen);check("Hidden preview is suspended",rt.themes().preview()==null&&openingTheme.matches(lib.settings));mc.setScreen(MusicMenu.reopen(null,rt));check("Reopening retains the uncommitted draft",rt.themes().draft().menuColors.get(MenuColor.BACKGROUND)==0x442244);});
        add("Apply theme",()->{click("Apply");check("Editor Apply returns directly to Display",mc.screen.getClass().getSimpleName().equals("NowPlayingSettingsScreen"));check("Apply commits both settings and named theme",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x442244&&ColorTheme.find(lib.settings,savedTheme).menuColors.get(MenuColor.BACKGROUND)==0x442244);check("Applied preview is released",rt.themes().preview()==null);click("Themes");});
        add("Preview built-in and cancel",()->{var list=(AbstractWidget)field(mc.screen,"themes");clickAt(list.x+15,list.y+7,InputConstants.MOUSE_BUTTON_LEFT);check("Choosing a theme only previews it",rt.themes().draft().menuColors.get(MenuColor.BACKGROUND)==0x17191D&&lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x442244);click("Cancel");check("Theme Cancel keeps committed colors",rt.themes().preview()==null&&lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x442244);click("Themes");click("Edit colors");});
        add("Invalid draft can be canceled",()->{((Map<String,EditBox>)field(mc.screen,"fields")).get("BACKGROUND").setValue("bad");click("Cancel");check("Cancel accepts an invalid draft without saving",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x442244);click("Cancel");openingTheme.apply(lib.settings);rt.save();click("Playlists");});
        add("Final workspace render",()->shot("clarity-final"));
    }
}
