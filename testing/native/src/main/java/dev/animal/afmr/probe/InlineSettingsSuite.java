package dev.animal.afmr.probe;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.ui.LibraryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;
import java.util.*;

/** Real rendered input checks; only the disposable workspace profile is changed. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class InlineSettingsSuite {
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static LibraryScreen screen;
    private static Playlist playlist,other;
    private static Object panel;
    private static final List<Runnable> steps=new ArrayList<>();
    private static final Path results=Path.of("../../results/inline-settings-results.txt");
    private static int startup,ticks,index,passed,failed;
    private static boolean started,finished;
    private static PlaylistConditions before;
    private static MouseButtonEvent held;
    private static long holdAt;
    private static Object field(Object object,String name){
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}catch(Exception ex){throw new RuntimeException(ex);}throw new IllegalArgumentException(name);
    }
    private static void log(String text){try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}System.out.println("AFMR INLINE "+text);}
    private static void check(String label,boolean okay){if(okay)passed++;else failed++;log((okay?"PASS ":"FAIL ")+label);}
    private static List<AbstractWidget> widgets(){return mc.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private static AbstractWidget widget(String label){return widgets().stream().filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow(()->new IllegalStateException(label));}
    private static float scale(){return mc.gui.screen() instanceof LibraryScreen?(float)field(mc.gui.screen(),"scale"):1;}
    private static void clickAt(double x,double y,int button){var current=mc.gui.screen();var event=new MouseButtonEvent(x,y,new MouseButtonInfo(button,0));current.mouseClicked(event,false);current.mouseReleased(event);}
    private static void click(AbstractWidget widget){
        if(!widget.visible){try{var method=panel.getClass().getDeclaredMethod("scrollTo",int.class);method.setAccessible(true);for(int i=0;i<50&&!widget.visible;i++)method.invoke(panel,i);}catch(Exception ex){throw new RuntimeException(ex);}}
        if(!widget.visible||!widget.active)throw new IllegalStateException("Unavailable: "+widget.getMessage().getString());
        float scale=scale();clickAt((widget.getX()+widget.getWidth()/2d)*scale,(widget.getY()+widget.getHeight()/2d)*scale,InputConstants.MOUSE_BUTTON_LEFT);
    }
    private static void click(String label){click(widget(label));}
    @SuppressWarnings("unchecked") private static EditBox input(String key){return ((Map<String,EditBox>)field(panel,"fields")).get(key);}
    private static void menu(String label){
        @SuppressWarnings("unchecked") var menu=(List<AbstractWidget>)field(screen,"menuButtons");
        click(menu.stream().filter(w->w.getMessage().getString().startsWith(label)).findFirst().orElseThrow());
    }
    private static void show(){mc.gui.setScreen(screen);panel=field(screen,"settingsPanel");}
    private static void areaEdit(){var row=widget("Playback area");var edit=widgets().stream().filter(w->w.getMessage().getString().equals(">")&&w.getY()==row.getY()).findFirst().orElseThrow();click(edit);panel=field(screen,"settingsPanel");}
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"inline-settings-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,text->{});}
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(!Boolean.getBoolean("afmr.probe.inlineSettings")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){if(mc.level==null||mc.player==null||++startup<80)return;Files.createDirectories(results.toAbsolutePath().getParent());Files.writeString(results,"");prepare();started=true;return;}
            if(held!=null){var window=mc.getWindow();for(String name:new String[]{"xpos","ypos"}){var f=mc.mouseHandler.getClass().getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?held.x()*window.getScreenWidth()/window.getGuiScaledWidth():held.y()*window.getScreenHeight()/window.getGuiScaledHeight());}}
            if(++ticks%10!=0)return;
            if(index==steps.size()){finish();return;}
            steps.get(index++).run();
        }catch(Throwable error){failed++;log("FAIL "+error);error.printStackTrace();shot("failure");finish();}
    }
    private static void pointAtHold(){
        try{var window=mc.getWindow();for(String name:new String[]{"xpos","ypos"}){var f=mc.mouseHandler.getClass().getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?held.x()*window.getScreenWidth()/window.getGuiScaledWidth():held.y()*window.getScreenHeight()/window.getGuiScaledHeight());}}catch(Exception ex){throw new RuntimeException(ex);}
    }
    private static void finish(){finished=true;log("COMPLETE: "+passed+" passed / "+failed+" failed");mc.gui.setScreen(null);mc.stop();}
    private static void prepare(){
        mc.options.pauseOnLostFocus=false;rt=ClientRuntime.get();rt.engine().reset();var lib=rt.library();
        lib.settings.menuImageEnabled=false;lib.settings.menuBlur=false;lib.settings.menuBackgroundOpacity=1;lib.settings.menuColors=dev.animal.afmr.core.MenuColor.defaults();
        playlist=lib.createPlaylist("Inline settings fixture");other=lib.createPlaylist("Untouched fixture");
        playlist.enabled=false;playlist.includeVanilla=false;playlist.conditions.cover=PlaylistConditions.State.NO;
        playlist.tracks.put("file:workspace_a",false);playlist.conditions.startTime=1234;
        screen=new LibraryScreen(null,rt,playlist.id);show();
        steps.add(()->{shot("default");check("Workspace retains independent music library",widget("Search music").visible);check("Playback area action is visible",widget("Set playback area here").visible);check("No name and conditions navigation",widgets().stream().noneMatch(w->w.getMessage().getString().equals("Name & conditions")));});
        steps.add(()->{click("Play automatically");check("Automatic checkbox enables selection",playlist.enabled&&!playlist.manualOnly);click("Play automatically");check("Automatic checkbox disables selection",!playlist.enabled);});
        steps.add(()->{click("Sky exposure");check("Row toggles condition off",playlist.conditions.cover==PlaylistConditions.State.ANY);click("Show rules");click("Sky exposure");check("Negative state survives reenable",playlist.conditions.cover==PlaylistConditions.State.NO);});
        steps.add(()->{areaEdit();check("Area edit expands inline",mc.gui.screen()==screen&&input("radius")!=null);check("Edit leaves filter off",!playlist.conditions.areaEnabled);input("radius").setValue("90");click("Use current location");check("Inline capture saves current world and 90 block radius",playlist.conditions.areaEnabled&&playlist.conditions.area.configured&&playlist.conditions.area.radius==90&&playlist.conditions.area.contains(rt.context()));});
        steps.add(()->{shot("area");input("radius").setValue("bad");click("Fewer rules");check("Invalid radius blocks action without loss",input("radius").getValue().equals("bad"));input("radius").setValue("96");areaEdit();check("Collapse saves valid radius",playlist.conditions.area.radius==96);});
        steps.add(()->{
            areaEdit();input("radius").setValue("9e");
            dev.animal.afmr.ui.MusicMenu.hide(screen);mc.gui.setScreen(dev.animal.afmr.ui.MusicMenu.reopen(null,rt));panel=field(screen,"settingsPanel");
            check("Hide retains invalid inline draft",input("radius").getValue().equals("9e"));
            int oldWidth=screen.width,oldHeight=screen.height;screen.width=426;screen.height=240;
            try{var reflow=LibraryScreen.class.getDeclaredMethod("repositionElements");reflow.setAccessible(true);reflow.invoke(screen);
                panel=field(screen,"settingsPanel");check("Small resize retains invalid draft",input("radius").getValue().equals("9e"));
                screen.width=oldWidth;screen.height=oldHeight;reflow.invoke(screen);panel=field(screen,"settingsPanel");
            }catch(Exception ex){throw new RuntimeException(ex);}
            input("radius").setValue("96");areaEdit();
        });
        steps.add(()->{click("Custom gap between songs");input("max").setValue("900");input("min").setValue("600");input("volume").setValue("45");click("Include Minecraft music");check("Inline gap and volume apply together",playlist.customSilence&&playlist.silenceMinSeconds==600&&playlist.silenceMaxSeconds==900&&playlist.volume==.45);check("Minecraft toggle retains functionality",playlist.includeVanilla);});
        steps.add(()->{click("Finish songs before switching away");check("Exit finish checkbox changes only exit flag",playlist.finishCurrentSongOnExit&&!playlist.finishCurrentSong);click("Let the previous song finish");check("Entry finish remains independently editable",playlist.finishCurrentSong&&playlist.finishCurrentSongOnExit);click("Skip wait on automatic start");check("Trigger option retained",playlist.playOnTrigger);});
        steps.add(()->{shot("playback");click("Playlist actions");menu("Rename playlist");check("Rename opens dedicated screen",mc.gui.screen().getClass().getSimpleName().equals("RenamePlaylistScreen"));((EditBox)field(mc.gui.screen(),"name")).setValue("Renamed inline fixture");click("Rename");panel=field(screen,"settingsPanel");check("Rename preserves playlist and configuration",playlist.name.equals("Renamed inline fixture")&&playlist.conditions.area.radius==96);});
        steps.add(()->{var list=(AbstractWidget)field(screen,"priorities");float scale=scale();int offset=(int)field(list,"offset"),rank=lib.playlists.indexOf(playlist);if(rank-offset<0||rank-offset>=list.getHeight()/14){try{var reveal=list.getClass().getDeclaredMethod("reveal",Object.class);reveal.setAccessible(true);reveal.invoke(list,playlist);offset=(int)field(list,"offset");}catch(Exception ex){throw new RuntimeException(ex);}}clickAt((list.getX()+30)*scale,(list.getY()+(rank-offset)*14+7)*scale,InputConstants.MOUSE_BUTTON_RIGHT);check("Right-click opens playlist actions",!((List<?>)field(screen,"menuButtons")).isEmpty());menu("Duplicate playlist");((EditBox)field(mc.gui.screen(),"name")).setValue("Inline branch");click("Duplicate");check("Duplicate retains assignments and settings",lib.playlists.stream().anyMatch(p->p.name.equals("Inline branch")&&Boolean.FALSE.equals(p.tracks.get("file:workspace_a"))&&p.conditions.area.radius==96));});
        steps.add(()->{screen=new LibraryScreen(null,rt,playlist.id);show();before=playlist.conditions;var button=widget("Set playback area here");float scale=scale();held=new MouseButtonEvent((button.getX()+button.getWidth()/2d)*scale,(button.getY()+button.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(held,false);check("Hold immediately shows replacement warning",button.getMessage().getString().contains("Replaces conditions"));screen.mouseReleased(held);held=null;check("Tap retains original conditions",playlist.conditions==before);});
        steps.add(()->{var button=widget("Set playback area here");float scale=scale();held=new MouseButtonEvent((button.getX()+button.getWidth()/2d)*scale,(button.getY()+button.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));pointAtHold();screen.mouseClicked(held,false);holdAt=System.nanoTime();});
        steps.add(()->{if(System.nanoTime()-holdAt<1_200_000_000L){index--;return;}screen.mouseReleased(held);held=null;check("Completed hold replaces only selected playlist conditions",playlist.conditions!=before&&playlist.conditions.areaEnabled&&playlist.conditions.area.radius==64&&!other.conditions.areaEnabled);check("Area replacement preserves song assignments",Boolean.FALSE.equals(playlist.tracks.get("file:workspace_a")));});
        steps.add(()->{shot("final");check("Selected playlist does not force playback",rt.engine().overrideId()==null);
            ((EditBox)field(screen,"librarySearch")).setValue("__AFMR_NO_MATCH__");
            var list=(AbstractWidget)field(screen,"folders");float scale=scale();
            boolean consumed=screen.mouseClicked(new MouseButtonEvent((list.getX()+20)*scale,(list.getY()+7)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false);
            check("Library search produced an empty viewport",((List<?>)field(list,"items")).isEmpty());
            check("Empty list rejects clicks before playing a sound",!list.mouseClicked(new MouseButtonEvent(list.getX()+20,list.getY()+7,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),false));
        });
    }
}
