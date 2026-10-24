package dev.animal.afmr.smoke;

import dev.animal.afmr.*;
import dev.animal.afmr.audio.*;
import dev.animal.afmr.core.*;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.sounds.*;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Disposable generated fixtures only. This source set is excluded from the release JAR. */
public final class FabricSmoke implements ClientModInitializer {
    private record Step(int ticks,Runnable action){}
    private record Sample(int source,int state,float offset,float gain,int error,long advances){}
    private final List<Step> steps=new ArrayList<>();
    private final Path result=Path.of(Boolean.getBoolean("afmr.smoke.playlistSilence")?"playlist-silence-results.txt":Boolean.getBoolean("afmr.smoke.widgetSettings")?"widget-settings-results.txt":Boolean.getBoolean("afmr.smoke.backgroundImages")?"background-images-results.txt":Boolean.getBoolean("afmr.smoke.playOnJoin")?"play-on-join-results.txt":Boolean.getBoolean("afmr.smoke.backupRestart")?"backup-restart-results.txt":"smoke-results.txt");
    private Minecraft mc;
    private ClientRuntime rt;
    private Library lib;
    private PlaybackEngine engine;
    private int tick,stage,wait,index,passed,failed;
    private boolean finished;
    private LibraryScreen workspace;
    private Track a,b;
    private volatile Sample sample=new Sample(0,0,0,0,0,0);
    private float pauseOffset;
    private int pausedSource;
    private long pausedAdvances;
    private float fullVolumeGain;
    @Override public void onInitializeClient(){ClientTickEvents.END_CLIENT_TICK.register(this::tick);}
    private void log(String s){try{Files.writeString(result,s+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}System.out.println("AFMR SMOKE "+s);}
    private void check(boolean condition,String name){if(condition){passed++;log("PASS "+name);}else{failed++;log("FAIL "+name+" | native="+sample);}}
    private void step(int ticks,Runnable action){steps.add(new Step(ticks,action));}
    private void tick(Minecraft client){
        if(Boolean.getBoolean("afmr.smoke.finalReview"))return;
        if(finished)return;mc=client;
        try{
            if(++tick>18000)throw new IllegalStateException("Smoke timeout");
            if(stage==0){
                if(mc.screen==null||mc.getOverlay()!=null||tick<80)return;
                Files.writeString(result,"");log("Fabric 1.20.1 disposable smoke run");
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(3);mc.options.simulationDistance().set(5);
                rt=ClientRuntime.get();lib=rt.library();engine=rt.engine();
                check(rt.enabled(),"runtime initializes");check(lib.tracks.stream().anyMatch(t->t.vanilla),"1.20.1 vanilla catalog indexed");
                workspace=new LibraryScreen(null,rt,Boolean.getBoolean("afmr.smoke.backupRestart")?lib.playlists.get(0).id:"day_outside");mc.setScreen(workspace);
                if(Boolean.getBoolean("afmr.smoke.backupRestart")){restartBackupChecks();stage=3;wait=20;return;}
                stage=1;wait=20;return;
            }
            if(stage==1){
                if(wait-->0)return;screenshot("title-workspace.png");check(mc.screen==workspace,"workspace opens at title screen");
                mc.setScreen(null);
                if(Files.isDirectory(Path.of("saves","AFMR Smoke")))mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"AFMR Smoke");
                else mc.createWorldOpenFlows().createFreshLevel("AFMR Smoke",new LevelSettings("AFMR Smoke",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(123456789L,false,false),WorldPresets::createNormalWorldDimensions);
                stage=2;return;
            }
            if(stage==2){if(mc.level==null||mc.player==null)return;if(wait++<80)return;if(Boolean.getBoolean("afmr.smoke.playlistSilence"))silenceChecks();else if(Boolean.getBoolean("afmr.smoke.widgetSettings"))widgetChecks();else if(Boolean.getBoolean("afmr.smoke.backgroundImages"))backgroundChecks();else if(Boolean.getBoolean("afmr.smoke.playOnJoin"))joinChecks();else prepare();stage=3;wait=0;return;}
            if(stage==3){
                nativeSample();
                if(wait-->0)return;
                if(index==steps.size()){finish();return;}
                Step next=steps.get(index++);next.action().run();wait=next.ticks();
            }
        }catch(Throwable e){failed++;log("ERROR "+e);e.printStackTrace();finish();}
    }
    private void joinChecks(){
        check(lib.profiles.activeId.equals("join-test")&&lib.settings.playImmediatelyOnJoin,"join activates bound profile before reading policy");
        check(engine.current()!=null&&engine.current().id.equals("file:join-a")&&engine.sourcePlaylistId().equals("join-world"),"actual world join starts the correct context playlist");
        check(lib.settings.silenceMinSeconds==600&&lib.settings.bufferSeconds==60&&engine.playbackSequence()==1,"first start bypasses long silence and buffer");
        workspace=new LibraryScreen(null,rt,"join-world");mc.setScreen(workspace);open("SettingsScreen");
        check(widget("Play immediately on join: on").active,"Timing exposes saved join toggle");
        var controls=mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();
        check(controls.stream().allMatch(a->a.getX()>=0&&a.getY()>=0&&a.getX()+a.getWidth()<=mc.screen.width&&a.getY()+a.getHeight()<=mc.screen.height)
            &&controls.stream().allMatch(a->controls.stream().allMatch(b->a==b||a.getX()+a.getWidth()<=b.getX()||b.getX()+b.getWidth()<=a.getX()||a.getY()+a.getHeight()<=b.getY()||b.getY()+b.getHeight()<=a.getY())),"Timing controls fit without overlap");
        check(widget("Minecraft defaults").active,"Minecraft defaults stays directly available");
        var values=controls.stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();values.get(0).setValue("invalid");click("Play immediately on join: on");
        check(lib.settings.playImmediatelyOnJoin,"invalid numeric drafts block toggle mutation");values.get(0).setValue("600");click("Play immediately on join: on");
        check(!lib.settings.playImmediatelyOnJoin&&engine.current()!=null,"toggle saves without interrupting song");click("Play immediately on join: off");
        var screen=mc.screen;MusicMenu.hide(screen);mc.setScreen(MusicMenu.reopen(null,rt));check(mc.screen==screen&&widget("Play immediately on join: on").active,"join option survives hide and reopen");
        step(10,()->{});
        step(20,()->{check(sample.state()==AL10.AL_PLAYING&&sample.gain()>0&&sample.error()==0,"join music reaches actual native playback");screenshot("play-on-join-timing.png");rt.skip();});
        step(10,()->check(engine.current()==null&&engine.remainingSilence(rt.now())>590,"later manual Skip retains normal silence"));
    }
    private List<AbstractWidget> widgetControls(){return mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private EditBox widgetField(String name){try{return (EditBox)field(mc.screen,name);}catch(Exception e){throw new RuntimeException(e);}}
    private boolean widgetBounds(){
        var visible=widgetControls().stream().filter(w->w.visible).toList();
        return visible.stream().allMatch(a->a.getX()>=0&&a.getY()>=0&&a.getX()+a.getWidth()<=mc.screen.width&&a.getY()+a.getHeight()<=mc.screen.height)
            &&visible.stream().allMatch(a->visible.stream().allMatch(b->a==b||a.getX()+a.getWidth()<=b.getX()||b.getX()+b.getWidth()<=a.getX()||a.getY()+a.getHeight()<=b.getY()||b.getY()+b.getHeight()<=a.getY()));
    }
    private void silenceChecks(){
        engine.reset();lib.settings.bufferSeconds=lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=20;lib.settings.playImmediatelyOnJoin=false;
        lib.settings.jukeboxMode=JukeboxMode.OFF;lib.settings.menuImageEnabled=false;
        for(var p:lib.playlists)p.enabled=false;
        var track=lib.tracks.stream().filter(t->!t.vanilla&&t.durationSeconds>=30&&Files.isRegularFile(Path.of(t.path))).findFirst().orElseThrow();
        Playlist target=lib.createPlaylist("Silence fixture");target.includeVanilla=false;target.tracks.put(track.id,true);lib.movePlaylist(target.id,0);
        workspace=new LibraryScreen(null,rt,target.id);mc.setScreen(workspace);
        check(widget("Silence: default").getX()>=(widget("Volume: 100%").getX()+widget("Volume: 100%").getWidth()),"Volume and silence share one row without overlap");
        click("Silence: default");
        check(mc.screen.getClass().getSimpleName().equals("PlaylistSilenceScreen")&&!target.customSilence&&widget("Custom silence: off").active,"Playlist silence editor opens with inherited default");
        check(widgetBounds(),"Small-window controls fit without overlap");
        widgetField("minimum").setValue("600");widgetField("maximum").setValue("600");click("Custom silence: off");
        check(target.customSilence&&target.silenceMinSeconds==600&&target.silenceMaxSeconds==600,"Fixed ten-minute custom gap can be enabled");
        widgetField("minimum").setValue("invalid");var screen=mc.screen;click("Save & back");
        check(mc.screen==screen&&target.silenceMinSeconds==600,"Invalid draft blocks saving");
        click("Custom silence: on");check(target.customSilence,"Invalid draft also blocks toggling");
        widgetField("minimum").setValue("601");click("Save & back");check(mc.screen==screen,"Reversed range blocks saving");
        widgetField("minimum").setValue("600");widgetField("maximum").setValue("86401");click("Save & back");check(mc.screen==screen,"Over-limit range blocks saving");
        widgetField("maximum").setValue("600");MusicMenu.hide(screen);mc.setScreen(MusicMenu.reopen(null,rt));
        check(mc.screen==screen&&widgetField("maximum").getValue().equals("600"),"Hide and reopen retains fields");
        int originalHeight=screen.height;screen.resize(mc,screen.width,originalHeight+20);check(widgetField("minimum").getValue().equals("600")&&widgetField("maximum").getValue().equals("600"),"Resize retains numeric drafts");screen.resize(mc,screen.width,originalHeight);
        click("Custom silence: on");click("Custom silence: off");
        step(15,()->{screenshot("playlist-silence-editor.png");click("Save & back");check(widget("Silence: custom").active,"Workspace label reflects custom timing");});
        step(15,()->{screenshot("playlist-silence-workspace.png");click("Silence: custom");click("Custom silence: on");check(!target.customSilence&&target.silenceMinSeconds==600&&target.silenceMaxSeconds==600,"Disabling keeps the saved range");
            click("Save & back");check(widget("Silence: default").active,"Workspace label returns to default");click("Silence: default");click("Custom silence: off");
            widgetField("minimum").setValue("3");widgetField("maximum").setValue("3");click("Save & back");rt.overridePlaylist(target.id,OverrideMode.UNTIL_RELEASED);});
        step(15,()->{check(engine.current()!=null&&target.id.equals(engine.sourcePlaylistId()),"Manual override starts generated music despite custom gap");pausedAdvances=engine.playbackSequence();rt.skip();});
        step(15,()->check(engine.current()==null&&engine.remainingSilence(rt.now())>0&&engine.remainingSilence(rt.now())<=3,"Skip waits inside the custom three-second gap"));
        step(15,()->{});step(15,()->{});step(15,()->{});step(15,()->{});
        step(15,()->{check(engine.current()!=null&&engine.playbackSequence()==pausedAdvances+1&&engine.error().isBlank(),"Generated music restarts after the custom gap");
            check(sample.state()==AL10.AL_PLAYING&&sample.error()==0,"Native audio is active");
            target.silenceMinSeconds=target.silenceMaxSeconds=600;rt.skip();check(engine.remainingSilence(rt.now())>599,"Ten-minute range schedules without waiting in real time");
            rt.save();});
    }
    private void widgetChecks()throws Exception{
        lib.settings.menuImageEnabled=false;lib.settings.menuColors=MenuColor.defaults();open("NowPlayingSettingsScreen");
        check(!widget("Widget").active&&widget("Menus").active&&widgetControls().stream().noneMatch(w->List.of("Size","Information","Appearance").contains(w.getMessage().getString())),"Display has only Widget and Menus");
        check(widgetControls().stream().filter(EditBox.class::isInstance).count()==5&&List.of("Artwork / note:","Song title:","Artist:","Album:","Song length:","Source playlist:","Scroll long text:").stream().allMatch(t->widgetControls().stream().anyMatch(w->w.getMessage().getString().startsWith(t))),"all fields and information switches share Widget");
        check(widgetBounds(),"small-window top controls fit without overlap");
        var duration=widgetField("duration");duration.setValue("invalid");var screen=mc.screen;screen.mouseScrolled(100,90,-1);
        check(widgetField("duration")==duration&&duration.getValue().equals("invalid"),"scrolling retains invalid drafts");click("Menus");
        check(mc.screen==screen&&(int)field(screen,"page")==0,"invalid draft blocks tab navigation");duration.setValue("9");widgetField("scale").setValue("125");widgetField("widgetWidth").setValue("360");
        click("Menus");check(lib.settings.popupSeconds==9&&lib.settings.widgetScale==1.25&&lib.settings.widgetWidth==360,"tab switch saves all widget fields");
        check(widgetControls().stream().noneMatch(EditBox.class::isInstance)&&widget("Custom background…").active&&widget("Menu colors…").active,"Menus contains only menu appearance controls");
        check(widgetBounds(),"menu controls fit without overlap");click("Widget");
        var corner=widgetControls().stream().filter(w->w.getMessage().getString().startsWith("Corner:")).findFirst().orElseThrow();var oldCorner=lib.settings.widgetCorner;click(corner.getMessage().getString());check(lib.settings.widgetCorner!=oldCorner,"corner remains configurable");
        var artist=widgetControls().stream().filter(w->w.getMessage().getString().startsWith("Artist:")).findFirst().orElseThrow();boolean oldArtist=lib.settings.widgetArtist;click(artist.getMessage().getString());check(lib.settings.widgetArtist!=oldArtist,"information toggle works in consolidated panel");
        var text=widgetControls().stream().filter(w->w.getMessage().getString().startsWith("Text:")).findFirst().orElseThrow();boolean oldWrap=lib.settings.widgetWrap;click(text.getMessage().getString());check(lib.settings.widgetWrap!=oldWrap,"text layout remains configurable");
        mc.screen.setFocused(null);mc.screen.keyPressed(InputConstants.KEY_END,0,0);widgetField("backgroundColor").setValue("#123456");widgetField("opacity").setValue("65");
        check(widgetBounds(),"bottom controls fit without overlap");int scroll=(int)field(mc.screen,"scroll");var held=mc.screen;MusicMenu.hide(held);mc.setScreen(MusicMenu.reopen(null,rt));
        check(mc.screen==held&&widgetField("backgroundColor").getValue().equals("#123456")&&(int)field(held,"scroll")==scroll,"hide/reopen retains drafts and scroll");
        step(15,()->{try{
            screenshot("widget-settings-bottom.png");click("Pick Background color");pickerHex().setValue("#00A0C0");click("Cancel");
            check(lib.settings.widgetBackgroundColor==0x123456&&lib.settings.widgetBackgroundOpacity==0.65,"picker Cancel restores applied opening color");
            click("Pick Background color");pickerHex().setValue("#00A0C0");click("Apply");check(lib.settings.widgetBackgroundColor==0x00A0C0,"picker Apply retains color");
            mc.screen.setFocused(null);mc.screen.keyPressed(InputConstants.KEY_HOME,0,0);check((int)field(mc.screen,"scroll")==0,"Home reaches first row");
            mc.screen.keyPressed(InputConstants.KEY_PAGEDOWN,0,0);check((int)field(mc.screen,"scroll")>0,"Page Down scrolls panel");
            mc.screen.keyPressed(InputConstants.KEY_END,0,0);var bar=widgetControls().stream().filter(w->w.getClass().getSimpleName().equals("SettingsScrollbar")).findFirst().orElseThrow();
            mc.screen.mouseClicked(bar.getX()+4,bar.getY()+bar.getHeight()-4,0);mc.screen.mouseDragged(bar.getX()+4,bar.getY()+1,0,0,-100);mc.screen.mouseReleased(bar.getX()+4,bar.getY()+1,0);
            check((int)field(mc.screen,"scroll")==0,"scrollbar drag reaches top");click("Preview widget");check(mc.screen.getClass().getSimpleName().equals("NowPlayingPreviewScreen"),"Preview stays directly available");
        }catch(Exception e){throw new RuntimeException(e);}});
        step(15,()->{try{
            screenshot("widget-settings-preview.png");click("Back");check(widgetField("scale").getValue().equals("125")&&widgetField("widgetWidth").getValue().equals("360"),"widget values survive preview");
            int height=mc.screen.height;mc.screen.resize(mc,mc.screen.width,height+260);check(widgetBounds(),"large-window controls fit without overlap");mc.screen.resize(mc,mc.screen.width,height);
            click("Menus");int color=lib.settings.widgetBackgroundColor;click("Reset menu appearance");check(lib.settings.widgetBackgroundColor==color&&lib.settings.widgetBackgroundOpacity==0.65,"menu reset preserves widget appearance");
            click("Widget");mc.screen.setFocused(null);mc.screen.keyPressed(InputConstants.KEY_HOME,0,0);rt.save();
        }catch(Exception e){throw new RuntimeException(e);}});
        step(10,()->screenshot("widget-settings-top.png"));
    }
    private com.mojang.blaze3d.platform.NativeImage[] backgroundPixels;
    private void backgroundChecks(){
        var background=rt.menuBackground();background.enabled(false);lib.settings.menuImageName="";lib.settings.menuImageOpacity=1;lib.settings.menuBackgroundOpacity=0.25;
        open("MenuBackgroundScreen");background.choose(Path.of("background-fixtures/lossless.webp"));
        step(20,()->{
            try{
                check(!background.busy()&&background.status().isBlank()&&(boolean)field(background,"uploaded"),"packaged WebP imports through actual menu worker");
                check(((Object[])field(background,"textures")).length==1&&lib.settings.menuImageName.equals("lossless.webp"),"static WebP uploads one texture");
                check(widget("Image: on").active&&widget("Remove image").active,"fast import refreshes enabled and remove controls");
            }catch(Exception e){throw new RuntimeException(e);}
            screenshot("background-webp.png");background.choose(Path.of("background-fixtures/disposal.gif"));
        });
        step(20,()->{
            try{
                check(!background.busy()&&background.status().isBlank()&&((Object[])field(background,"textures")).length==4,"animated GIF uploads all four composited textures");
                var ids=(net.minecraft.resources.ResourceLocation[])field(background,"textures");backgroundPixels=new com.mojang.blaze3d.platform.NativeImage[ids.length];
                for(int i=0;i<ids.length;i++)backgroundPixels[i]=((net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(ids[i])).getPixels();
                check(backgroundPixels[0].getPixelRGBA(20,10)==0&&backgroundPixels[1].getPixelRGBA(20,10)==0xff00ff00,"native textures preserve transparency and colored patches");
            }catch(Exception e){throw new RuntimeException(e);}
            screenshot("background-gif-a.png");rt.exportBackup(Path.of("background-animation.afmrb"));
        });
        step(15,()->{
            screenshot("background-gif-b.png");
            try{
                check(field(background,"animation") instanceof MenuImageAnimation,"animation clock uses frame timing");
                var backup=dev.animal.afmr.library.ConfigBackup.read(Path.of("background-animation.afmrb"));
                check(Arrays.equals(backup.files().get("menu-background.gif"),Files.readAllBytes(Path.of("background-fixtures/disposal.gif"))),"full backup exports animation bytes");
            }catch(Exception e){throw new RuntimeException(e);}
            background.invalidate();boolean closed=true;
            for(var image:backgroundPixels){try{image.getPixelRGBA(0,0);closed=false;}catch(IllegalStateException expected){}}
            check(closed,"resource invalidation releases every frame");
        });
        step(20,()->{
            try{check(!background.busy()&&(boolean)field(background,"uploaded")&&((Object[])field(background,"textures")).length==4,"managed animation reloads after invalidation");
                background.enabled(false);check(((Object[])field(background,"textures")).length==0,"disable releases animation textures");
            }catch(Exception e){throw new RuntimeException(e);}background.enabled(true);
        });
        step(20,()->{try{check((boolean)field(background,"uploaded"),"re-enable loads retained animation");}catch(Exception e){throw new RuntimeException(e);}background.choose(Path.of("background-fixtures/lossless.webp"));});
        step(20,()->{
            try{check(!background.busy()&&((Object[])field(background,"textures")).length==1,"static replacement releases old animation");}catch(Exception e){throw new RuntimeException(e);}
            check(!Files.exists(Path.of("config/afmr/menu-background.gif")),"static replacement removes owned GIF");background.remove();
        });
        step(20,()->{
            check(!background.busy()&&lib.settings.menuImageName.isBlank()&&!Files.exists(Path.of("config/afmr/menu-background.png"))&&!Files.exists(Path.of("config/afmr/menu-background.gif")),"remove clears both owned files and selection");
            check(Files.isRegularFile(Path.of("background-fixtures/lossless.webp"))&&Files.isRegularFile(Path.of("background-fixtures/disposal.gif")),"original WebP and GIF remain intact");
        });
    }
    private void prepare(){
        check(rt.inWorld(),"integrated world joined");
        check(!rt.worldKey().isBlank(),"world profile key available");
        check(org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_getGlobalInt("tinyfd_verbose")>=0,"native picker library loads");
        for(String file:new String[]{"test.mp3","test.flac","test.ogg","test.wav","test24.flac","test24.wav","testfloat.wav"}){
            try(var pcm=PcmDecoder.open(Path.of("codecs",file))){check(pcm.readNBytes(128).length>0,"packaged decoder reads "+file);}
            catch(Exception error){throw new RuntimeException("Packaged decoder "+file,error);}
        }
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;lib.settings.bufferSeconds=0;
        lib.settings.replayGainEnabled=false;lib.settings.popupSeconds=60;
        for(var p:lib.playlists)p.enabled=false;
        engine.reset();lib.tracks.removeIf(t->!t.vanilla);
        a=track("file:smoke-a","smoke-a.wav","Smoke A");b=track("file:smoke-b","smoke-b.wav","Smoke B");
        lib.tracks.add(a);lib.tracks.add(b);lib.rebuildTrackIndex();
        var playlist=lib.playlist("day_outside");playlist.tracks.put(a.id,true);playlist.tracks.put(b.id,true);
        rt.save();
        step(50,()->rt.preview(a));
        step(20,()->{check(engine.current()==a,"local WAV starts");check(sample.state()==AL10.AL_PLAYING&&sample.error()==0,"actual OpenAL playback starts");rt.togglePause();});
        step(30,()->{check(engine.paused()&&sample.state()==AL10.AL_PAUSED,"manual pause reaches native channel");pauseOffset=sample.offset();pausedSource=sample.source();pausedAdvances=sample.advances();});
        step(20,()->{check(Math.abs(sample.offset()-pauseOffset)<0.05,"paused sample position stays fixed");rt.togglePause();});
        step(20,()->{check(sample.state()==AL10.AL_PLAYING&&sample.source()==pausedSource&&sample.advances()>pausedAdvances+3,"resume advances the same native source");rt.overridePlaylist(playlist.id,OverrideMode.UNTIL_RELEASED);});
        step(20,()->{check(engine.overrideId()!=null,"override works with Automatic off");rt.nextSong();});
        step(20,()->{check(engine.current()!=null,"Next retains playback");check(engine.history().size()>=2,"history records successful starts");mc.setScreen(workspace);});
        step(20,()->{fullVolumeGain=sample.gain();playlist.volume=0.25;rt.save();});
        step(20,()->{check(fullVolumeGain>0&&Math.abs(sample.gain()-fullVolumeGain*0.25)<0.01,"playlist volume reaches native gain");playlist.volume=1;rt.save();rt.queueTrack(b.id,playlist.id);rt.nextSong();});
        step(10,()->check(engine.current()==b&&engine.queue().empty(),"queued request starts through native playback"));
        step(20,()->{check(mc.screen==workspace,"workspace renders during playback");screenshot("world-workspace.png");});
        for(String name:new String[]{"SourcesScreen","SettingsScreen","NowPlayingSettingsScreen","OverrideBindingsScreen","PlaybackInspectorScreen","ListeningScreen","ProfilesScreen","PlaylistConditionsScreen"}){
            step(8,()->open(name));step(1,()->{check(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals(name),name+" renders");screenshot(name+".png");});
        }
        step(30,()->{mc.setScreen(null);rt.preview(lib.tracks.stream().filter(t->t.vanilla).findFirst().orElseThrow());});
        step(15,()->{check(engine.current()!=null&&engine.current().vanilla&&sample.state()==AL10.AL_PLAYING,"vanilla music streams through AFMR");rt.togglePause();});
        step(15,()->{check(sample.state()==AL10.AL_PAUSED,"vanilla music pauses natively");rt.togglePause();});
        step(15,()->check(sample.state()==AL10.AL_PLAYING,"vanilla music resumes natively"));
        step(10,()->{mc.setScreen(null);rt.preview(a);rt.togglePause();});
        step(15,()->check(engine.paused()&&sample.state()==AL10.AL_PAUSED,"pause requested during asynchronous stream startup survives attachment"));
        step(20,()->{rt.togglePause();rt.returnAutomatic();rt.skip();});
        step(10,()->{check(engine.overrideId()==null,"return to automatic clears override");mc.setScreen(workspace);});
        step(10,()->{check(MusicMenu.hide(workspace),"workspace hides");mc.setScreen(MusicMenu.reopen(null,rt));});
        step(10,()->{check(mc.screen==workspace,"workspace reopens");rt.save();});
        step(40,()->rt.importPaths(List.of(Path.of("import-smoke.wav").toAbsolutePath()),lib.profiles.activeId,"day_outside",true));
        step(10,()->check(!rt.scanning()&&lib.tracks.stream().anyMatch(t->!t.vanilla&&t.path.endsWith("import-smoke.wav")),"generated music imports on the library worker"));
        menuColorChecks();
        colorPickerChecks();
        seamChecks();
        backupChecks();
    }
    @SuppressWarnings("unchecked") private Map<MenuColor,EditBox> colorFields(){
        try{return (Map<MenuColor,EditBox>)field(mc.screen,"fields");}catch(Exception e){throw new RuntimeException(e);}
    }
    private void click(String label){
        var w=mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(widget->widget.getMessage().getString().equals(label)).findFirst().orElseThrow();
        if(mc.screen.getClass().getSimpleName().equals("NowPlayingSettingsScreen"))for(int i=0;!w.visible&&i<20;i++)
            mc.screen.mouseScrolled(w.getX(),90,w.getY()<79?1:-1);
        if(!w.visible)throw new IllegalStateException("Hidden control: "+label);
        double scale=1;try{if(mc.screen instanceof LibraryScreen)scale=(float)field(mc.screen,"scale");}catch(Exception e){throw new RuntimeException(e);}
        double x=(w.getX()+w.getWidth()/2d)*scale,y=(w.getY()+w.getHeight()/2d)*scale;
        mc.screen.mouseClicked(x,y,0);mc.screen.mouseReleased(x,y,0);
    }
    private void reflowColorEditor(int height){
        var screen=mc.screen;screen.height=height;
        try{var method=screen.getClass().getDeclaredMethod("repositionElements");method.setAccessible(true);method.invoke(screen);}catch(Exception e){throw new RuntimeException(e);}
    }
    private void menuColorChecks(){
        step(10,()->{lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=0.5;lib.settings.widgetBackgroundColor=0x123456;open("NowPlayingSettingsScreen");click("Menus");click("Menu colors…");
            check(mc.screen.getClass().getSimpleName().equals("MenuColorsScreen"),"menu color editor opens");
            var field=colorFields().get(MenuColor.BACKGROUND);field.setValue("#123");var screen=mc.screen;click("Save & back");check(mc.screen==screen,"invalid RGB blocks navigation");field.setValue("#22142B");
            check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x22142B,"valid menu color previews immediately");
            colorFields().get(MenuColor.PANELS).setValue("#30203B");colorFields().get(MenuColor.CONTROLS).setValue("#473150");colorFields().get(MenuColor.HOVER).setValue("#604268");colorFields().get(MenuColor.SELECTION).setValue("#624176");
        });
        step(10,()->{screenshot("menu-colors-editor.png");click(">");colorFields().get(MenuColor.ACCENT).setValue("#DD92EE");colorFields().get(MenuColor.TEXT).setValue("#F3E7F6");
            colorFields().get(MenuColor.MUTED_TEXT).setValue("#BFA8C9");colorFields().get(MenuColor.BORDERS).setValue("#79568A");colorFields().get(MenuColor.SCROLLBARS).setValue("#AE77BB");
            MusicMenu.hide(mc.screen);mc.setScreen(MusicMenu.reopen(null,rt));check(colorFields().get(MenuColor.ACCENT).getValue().equals("#DD92EE"),"hidden color drafts retained");
            int oldHeight=mc.screen.height;reflowColorEditor(oldHeight+220);
            check(colorFields().get(MenuColor.ACCENT).getValue().equals("#DD92EE")&&colorFields().get(MenuColor.BACKGROUND).getValue().equals("#22142B"),"resize retains drafts by color family");
            reflowColorEditor(oldHeight);if(!colorFields().containsKey(MenuColor.ACCENT))click(">");
            check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x22142B&&lib.settings.menuColors.get(MenuColor.ACCENT)==0xDD92EE,"resize does not remap palette values");
            check(lib.settings.menuBackgroundOpacity==0.5&&lib.settings.widgetBackgroundColor==0x123456,"menu palette preserves opacity and widget settings");
        });
        step(10,()->{screenshot("menu-colors-text.png");click("Save & back");mc.setScreen(workspace);check(!workspace.isPauseScreen(),"themed workspace remains non-pausing");});
        step(10,()->{screenshot("menu-colors-workspace.png");open("NowPlayingSettingsScreen");click("Menus");click("Menu colors…");colorFields().get(MenuColor.BACKGROUND).setValue("bad");click("Reset colors");
            check(lib.settings.menuColors.equals(MenuColor.defaults()),"reset colors recovers invalid input");check(lib.settings.menuBackgroundOpacity==0.5&&lib.settings.widgetBackgroundColor==0x123456,"reset colors preserves other appearance");
            click("Save & back");click("Reset menu appearance");check(lib.settings.menuColors.equals(MenuColor.defaults())&&lib.settings.menuBackgroundOpacity==0.8,"full menu reset restores defaults");rt.save();
        });
    }
    private EditBox pickerHex(){return mc.screen.children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private List<AbstractWidget> pickerAreas(){return mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(w->w.getClass().getSimpleName().equals("ColorPickerArea")).toList();}
    private void choosePickerArea(int index,double x,double y){
        var area=pickerAreas().get(index);double px=area.getX()+(area.getWidth()-1)*x,py=area.getY()+(area.getHeight()-1)*y;
        mc.screen.mouseClicked(px,py,0);mc.screen.mouseReleased(px,py,0);
    }
    private void colorPickerChecks(){
        step(10,()->{lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=0.5;lib.settings.widgetBackgroundColor=0x123456;
            open("NowPlayingSettingsScreen");click("Menus");click("Menu colors…");
            for(var role:MenuColor.values()){
                while(!colorFields().containsKey(role))click(">");click("Pick "+role.label);
                check(mc.screen.getClass().getSimpleName().equals("ColorPickerScreen")&&pickerHex().getValue().equals(String.format(Locale.ROOT,"#%06X",role.defaultRgb)),role.label+" opens a seeded picker");click("Cancel");
            }
            while(!colorFields().containsKey(MenuColor.BACKGROUND))click("<");click("Pick Background");
            check(mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).allMatch(w->w.getX()>=0&&w.getX()+w.getWidth()<=mc.screen.width&&w.getY()>=0&&w.getY()+w.getHeight()<=mc.screen.height),"picker controls fit screen");
            pickerHex().setValue("#FF0000");check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF0000,"picker hex previews menu color");
            choosePickerArea(1,0.5,1/3d);check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x00FF00,"hue strip selects green");
            choosePickerArea(0,0.5,0.5);check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x408040&&pickerHex().getValue().equals("#408040"),"SV square selects expected RGB");
        });
        step(10,()->{screenshot("color-picker-menu.png");var area=pickerAreas().get(0);var screen=mc.screen;
            screen.mouseClicked(area.getX()+10,area.getY()+10,0);screen.mouseDragged(area.getX()+area.getWidth()*2,area.getY()+area.getHeight()*2,0,10,10);screen.mouseReleased(area.getX()+area.getWidth()*2,area.getY()+area.getHeight()*2,0);
            check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0,"dragging outside picker clamps to black");screen.setFocused(area);screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_UP,0,0);
            check(lib.settings.menuColors.get(MenuColor.BACKGROUND)>0,"focused arrows adjust picker");
            pickerHex().setValue("#12");click("Apply");check(mc.screen==screen,"invalid picker hex blocks Apply");click("Cancel");check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==MenuColor.BACKGROUND.defaultRgb,"picker Cancel restores menu color");
            colorFields().get(MenuColor.BACKGROUND).setValue("bad");click("Pick Background");click("#FF4040");click("Apply");check(lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF4040&&colorFields().get(MenuColor.BACKGROUND).getValue().equals("#FF4040"),"picker recovers invalid parent hex");
            check(lib.settings.menuBackgroundOpacity==0.5,"picker preserves panel opacity");click("Save & back");click("Widget");click("Pick Background color");pickerHex().setValue("#00A0C0");
            check(lib.settings.widgetBackgroundColor==0x00A0C0&&lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF4040,"widget picker previews independently");
        });
        step(10,()->{screenshot("color-picker-widget.png");var picker=mc.screen;MusicMenu.hide(picker);mc.setScreen(MusicMenu.reopen(null,rt));check(mc.screen==picker&&pickerHex().getValue().equals("#00A0C0"),"picker survives hide and reopen");click("Apply");
            check(lib.settings.widgetBackgroundColor==0x00A0C0,"widget picker Apply keeps RGB");click("Pick Background color");pickerHex().setValue("#FFFFFF");mc.screen.keyPressed(com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE,0,0);
            check(lib.settings.widgetBackgroundColor==0x00A0C0&&mc.screen.getClass().getSimpleName().equals("NowPlayingSettingsScreen"),"Escape cancels widget picker preview");rt.save();
        });
    }
    private void seamChecks(){
        step(30,()->{
            try{
                var image=new java.awt.image.BufferedImage(1,1,java.awt.image.BufferedImage.TYPE_INT_RGB);image.setRGB(0,0,0xFF00FF);
                Path file=Path.of("seam-backdrop.png");javax.imageio.ImageIO.write(image,"png",file.toFile());rt.menuBackground().choose(file);
            }catch(Exception e){throw new RuntimeException(e);}
            lib.settings.menuBackgroundOpacity=1;lib.settings.songColumns=EnumSet.allOf(SongColumn.class);
            var playlist=lib.playlist("day_outside");
            for(int i=0;i<100;i++){
                String id="file:seam-smoke-"+i;if(lib.track(id)!=null)continue;
                var t=Library.copyTrack(a);t.id=id;t.title="Seam fixture "+i;lib.tracks.add(t);playlist.tracks.put(id,true);
            }
            for(int i=0;i<20;i++)if(lib.playlist("custom:seam-"+i)==null){var p=lib.createPlaylist("Seam playlist "+i);p.id="custom:seam-"+i;p.enabled=false;}
            lib.rebuildTrackIndex();workspace=new LibraryScreen(null,rt,playlist.id);mc.setScreen(workspace);
        });
        step(10,()->{
            check(!rt.menuBackground().busy()&&lib.settings.menuImageEnabled,"seam check uses a contrasting image behind opaque panels");
            try(var image=Screenshot.takeScreenshot(mc.getMainRenderTarget())){
                double scale=(float)field(workspace,"scale")*mc.getWindow().getGuiScale();int holes=0;
                for(String name:List.of("priorities","folders","songs")){
                    var list=(AbstractWidget)field(workspace,name);
                    int x0=(int)Math.ceil(list.getX()*scale-0.5),x1=(int)Math.floor((list.getX()+list.getWidth())*scale-0.5);
                    int y0=(int)Math.ceil(list.getY()*scale-0.5),y1=(int)Math.floor((list.getY()+list.getHeight())*scale-0.5);
                    for(int x=x0;x<=x1;x++)for(int y:new int[]{y0,y1})if((image.getPixelRGBA(x,y)&0xFFFFFF)==0xFF00FF)holes++;
                    for(int y=y0;y<=y1;y++)for(int x:new int[]{x0,x1})if((image.getPixelRGBA(x,y)&0xFFFFFF)==0xFF00FF)holes++;
                }
                var songs=(AbstractWidget)field(workspace,"songs");int tableX=(int)field(workspace,"tableX"),tableWidth=(int)field(workspace,"tableWidth");
                int barY=songs.getY()+songs.getHeight();
                for(int x=(int)Math.ceil(tableX*scale);x<(int)Math.floor((tableX+tableWidth)*scale);x++)
                    for(int y=(int)Math.ceil(barY*scale);y<(int)Math.floor((barY+10)*scale);y++)if((image.getPixelRGBA(x,y)&0xFFFFFF)==0xFF00FF)holes++;
                check(holes==0,"framebuffer has no exposed image at list perimeters or horizontal scrollbar strip");
                screenshot("workspace-seams-fixed.png");
            }catch(Exception e){throw new RuntimeException(e);}
            rt.menuBackground().enabled(false);lib.settings.songColumns=SongColumn.defaults();rt.save();
        });
    }
    private void backupChecks(){
        Path backup=Path.of("fabric-full-test.afmrb").toAbsolutePath();
        step(20,()->{mc.setScreen(workspace);click("Import / export");click("Full configuration...");
            check(mc.screen.getClass().getSimpleName().equals("ConfigBackupScreen"),"full backup route opens");
            check(!widget("Replace on restart").active,"full backup replacement requires review");rt.exportBackup(backup);
        });
        step(20,()->{check(!rt.scanning()&&Files.isRegularFile(backup),"full backup exports on worker");rt.prepareBackupImport(backup);});
        step(10,()->{
            check(rt.pendingBackup()!=null&&widget("Replace on restart").active,"full backup import shows reviewed replacement");
            check(rt.pendingBackup().keys().size()==15,"full backup includes physical AFMR shortcuts");
            screenshot("full-backup-review.png");click("Back");check(rt.pendingBackup()==null,"Back cancels full backup import");
        });
    }
    private AbstractWidget widget(String label){return mc.screen.children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).filter(w->w.getMessage().getString().equals(label)).findFirst().orElseThrow();}
    private void restartBackupChecks(){
        check(lib.settings.menuColors.get(MenuColor.ACCENT)==0x123456&&lib.settings.fadeInSeconds==1.25&&lib.settings.fadeOutSeconds==2.5,"NeoForge backup restores shared settings on Fabric startup");
        check(lib.playlists.stream().anyMatch(p->p.name.equals("Backup native fixture")&&p.playOnTrigger&&p.volume==0.42),"NeoForge backup restores editable playlist flags on Fabric");
        check(lib.profiles.entries().stream().anyMatch(p->p.name().equals("Backup second")),"NeoForge backup restores all profiles on Fabric");
        check(Afmr.backupKeys().get("key.afmr.inspect").code().equals("key.keyboard.k"),"NeoForge backup restores physical shortcut on Fabric");
        Path config=mc.gameDirectory.toPath().resolve("config/afmr");
        check(!Files.exists(config.resolve(dev.animal.afmr.library.ConfigBackup.PENDING)),"Fabric startup consumes pending backup");
        check(Files.isRegularFile(config.resolve("restore-previous/library.json")),"Fabric startup keeps prior configuration");
        check(!Files.exists(config.resolve("menu-background.png")),"backup without image removes the old managed background");
        step(20,()->{screenshot("cross-version-restored.png");click("Import / export");click("Full configuration...");});
        step(10,()->{check(mc.screen.getClass().getSimpleName().equals("ConfigBackupScreen"),"restored configuration remains editable");screenshot("full-backup-restored.png");});
    }
    private Track track(String id,String file,String title){var t=new Track(id,Path.of(file).toAbsolutePath().toString(),title,false);t.durationSeconds=120;t.artist="Generated fixture";return t;}
    private void open(String name){
        try{
            Class<?> type=Class.forName("dev.animal.afmr.ui."+name);Constructor<?> ctor;
            if(name.equals("PlaylistConditionsScreen")){
                ctor=type.getDeclaredConstructor(Screen.class,ClientRuntime.class,Playlist.class,PlaylistConditions.class,int.class);ctor.setAccessible(true);
                var p=lib.playlist("day_outside");mc.setScreen((Screen)ctor.newInstance(workspace,rt,p,p.conditions,0));
            }else{ctor=type.getDeclaredConstructor(Screen.class,ClientRuntime.class);ctor.setAccessible(true);mc.setScreen((Screen)ctor.newInstance(workspace,rt));}
        }catch(Exception e){throw new RuntimeException(name,e);}
    }
    private static Object field(Object object,String name)throws Exception{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void nativeSample()throws Exception{
        Object audio=field(engine,"audio");SoundInstance sound=(SoundInstance)field(audio,"sound");
        Object soundEngine=field(mc.getSoundManager(),"soundEngine");
        @SuppressWarnings("unchecked") var map=(Map<SoundInstance,ChannelAccess.ChannelHandle>)field(soundEngine,"instanceToChannel");
        var handle=sound==null?null:map.get(sound);
        if(handle==null){sample=new Sample(0,0,0,0,0,0);return;}
        handle.execute(channel->{try{
            int source=(int)field(channel,"source");float offset=AL10.alGetSourcef(source,AL11.AL_SEC_OFFSET);
            long advances=sample.advances()+(source==sample.source()&&Math.abs(offset-sample.offset())>0.002?1:0);
            sample=new Sample(source,AL10.alGetSourcei(source,AL10.AL_SOURCE_STATE),offset,AL10.alGetSourcef(source,AL10.AL_GAIN),AL10.alGetError(),advances);
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    private void screenshot(String name){Screenshot.grab(mc.gameDirectory,name,mc.getMainRenderTarget(),message->{});}
    private void finish(){
        if(finished)return;finished=true;log("RESULT passed="+passed+" failed="+failed);
        try{if(rt!=null){mc.setScreen(null);ClientRuntime.shutdown();}}catch(Exception e){log("Shutdown failure: "+e);}
        mc.stop();
    }
}
