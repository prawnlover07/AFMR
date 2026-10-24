package dev.animal.afmr.probe;

import dev.animal.afmr.*;
import dev.animal.afmr.core.Library;
import dev.animal.afmr.core.SongColumn;
import dev.animal.afmr.core.MenuColor;
import dev.animal.afmr.core.ColorPickerState;
import dev.animal.afmr.core.ReplayGainInfo;
import dev.animal.afmr.core.PlaylistTemplates;
import dev.animal.afmr.core.PlaylistConditions;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import dev.animal.afmr.core.Library.*;
import dev.animal.afmr.ui.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MusicToastDisplayState;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.ColoredRectangleRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Real screen/widget checks using only the separate run/workspace profile and generated WAVs. */
@EventBusSubscriber(modid="afmrprobe",value=Dist.CLIENT)
public final class WorkspaceSuite {
    private static final String resultName=Boolean.getBoolean("afmr.probe.playlistSilence")?"playlist-silence":Boolean.getBoolean("afmr.probe.widgetSettings")?"widget-settings":Boolean.getBoolean("afmr.probe.backgroundImages")?"background-images":Boolean.getBoolean("afmr.probe.playOnJoin")?"play-on-join":Boolean.getBoolean("afmr.probe.backupFromFabric")?"config-backup-from-fabric":Boolean.getBoolean("afmr.probe.backupRestart")?"config-backup-restart":Boolean.getBoolean("afmr.probe.configBackup")?"config-backup":Boolean.getBoolean("afmr.probe.menuSeams")?"menu-seams":Boolean.getBoolean("afmr.probe.colorPickers")?"color-pickers":Boolean.getBoolean("afmr.probe.menuColors")?"menu-colors":"workspace";
    private static final Path results=Path.of("../../results/"+resultName+"-results.txt").toAbsolutePath();
    private static final List<Runnable> steps=new ArrayList<>();
    private static Minecraft mc;
    private static ClientRuntime rt;
    private static Library lib;
    private static LibraryScreen editor;
    private static Playlist target;
    private static Screen timing;
    private static Playlist branch;
    private static Playlist modeFallback;
    private static GameMode originalMode;
    private static double triggerAt;
    private static long triggerSequence;
    private static long jukeboxSequence;
    private static String jukeboxOne,jukeboxTwo;
    private static double recordVolume;
    private static PlaylistConditions oldConditions;
    private static MouseButtonEvent heldEvent;
    private static MusicToastDisplayState toastDisplay;
    private static boolean pauseOnLostFocus;
    private static int ticks,step,passed,failed;
    private static boolean started,finished;
    private static boolean hovering,tooltipBounds,tooltipAnchor;
    private static int hoverX,hoverY,tooltipFrames;
    private static void hover(AbstractWidget widget){
        float scale=mc.gui.screen() instanceof LibraryScreen?(float)field(editor,"scale"):1;
        hoverX=Math.round((widget.getX()+widget.getWidth()/2f)*scale);
        hoverY=Math.round((widget.getY()+widget.getHeight()/2f)*scale);
        hovering=true;tooltipFrames=0;tooltipBounds=tooltipAnchor=true;
        mc.gui.screen().setFocused(null);movePointer(hoverX,hoverY);
    }
    private static void movePointer(int x,int y){
        try{
            var window=mc.getWindow();
            Field fx=mc.mouseHandler.getClass().getDeclaredField("xpos"),fy=mc.mouseHandler.getClass().getDeclaredField("ypos");
            fx.setAccessible(true);fy.setAccessible(true);
            fx.setDouble(mc.mouseHandler,x*(double)window.getScreenWidth()/window.getGuiScaledWidth());
            fy.setDouble(mc.mouseHandler,y*(double)window.getScreenHeight()/window.getGuiScaledHeight());
        }catch(Exception e){throw new RuntimeException(e);}
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.Pre event){
        if(!Boolean.getBoolean("afmr.probe.workspace")||!hovering)return;
        tooltipFrames++;
        int width=event.getComponents().stream().mapToInt(c->c.getWidth(event.getFont())).max().orElse(0);
        int height=event.getComponents().stream().mapToInt(c->c.getHeight(event.getFont())).sum()-2;
        var p=event.getTooltipPositioner().positionTooltip(event.getScreenWidth(),event.getScreenHeight(),event.getX(),event.getY(),width,height);
        tooltipBounds&=p.x()>=4&&p.y()>=4&&p.x()+width+4<=event.getScreenWidth()&&p.y()+height+4<=event.getScreenHeight();
        tooltipAnchor&=Math.abs(event.getX()-hoverX)<=1&&Math.abs(event.getY()-hoverY)<=1;
    }
    private static void checkHover(String name){
        check(name+" rendered",tooltipFrames>0);
        check(name+" within screen",tooltipFrames>0&&tooltipBounds&&tooltipAnchor);
        shot(name);hovering=false;movePointer(0,0);
    }
    private static Object field(Object object,String name){
        try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
        catch(Exception e){throw new RuntimeException(e);}
    }
    private static List<AbstractWidget> widgets(){return mc.gui.screen().children().stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).toList();}
    private static AbstractWidget button(String text){return widgets().stream().filter(w->w.getMessage().getString().equals(text)).findFirst().orElseThrow(()->new IllegalStateException("Missing button: "+text+" on "+mc.gui.screen().getClass().getSimpleName()));}
    private static AbstractWidget prefix(String text){return widgets().stream().filter(w->w.getMessage().getString().startsWith(text)).findFirst().orElseThrow(()->new IllegalStateException("Missing control: "+text));}
    private static void click(String text){click(button(text));}
    private static void click(AbstractWidget w){
        if(!w.active)throw new IllegalStateException("Inactive: "+w.getMessage().getString());
        revealWidget(w);
        Screen screen=mc.gui.screen();float scale=screen instanceof LibraryScreen?(float)field(screen,"scale"):1;
        var e=new MouseButtonEvent((w.getX()+w.getWidth()/2d)*scale,(w.getY()+w.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        screen.mouseClicked(e,false);screen.mouseReleased(e);
    }
    private static void revealWidget(AbstractWidget control){
        var screen=mc.gui.screen();
        if(screen.getClass().getSimpleName().equals("NowPlayingSettingsScreen"))for(int i=0;!control.visible&&i<20;i++)
            screen.mouseScrolled(control.getX(),90,0,control.getY()<79?1:-1);
        if(!control.visible)throw new IllegalStateException("Hidden control: "+control.getMessage().getString());
    }
    private static void row(String listField,int index,boolean twice){
        AbstractWidget list=(AbstractWidget)field(editor,listField);float scale=(float)field(editor,"scale");
        var e=new MouseButtonEvent((list.getX()+40)*scale,(list.getY()+index*14+7)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        editor.mouseClicked(e,twice);editor.mouseReleased(e);
    }
    private static void check(String name,boolean okay){
        if(okay)passed++;else failed++;
        log((okay?"PASS ":"FAIL ")+name);
    }
    private static void log(String text){
        try{Files.writeString(results,text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        catch(Exception e){throw new RuntimeException(e);}Afmr.LOG.info("AFMR WORKSPACE: {}",text);
    }
    private static void shot(String name){Screenshot.grab(mc.gameDirectory,"workspace-"+name+".png",mc.gameRenderer.mainRenderTarget(),1,message->{});}
    @SubscribeEvent public static void tick(ClientTickEvent.Post ignored){
        if(Boolean.getBoolean("afmr.probe.finalReview")||Boolean.getBoolean("afmr.probe.features101"))return;
        if(!Boolean.getBoolean("afmr.probe.workspace")||finished)return;
        mc=Minecraft.getInstance();
        try{
            if(!started){
                if(mc.level==null||mc.player==null||++ticks<80)return;
                Files.createDirectories(results.getParent());Files.writeString(results,"");start();started=true;ticks=0;return;
            }
            if(hovering)movePointer(hoverX,hoverY);
            if(++ticks%15!=0)return;
            if(step==steps.size()){
                finished=true;log("COMPLETE: "+passed+" passed / "+failed+" failed");
                Files.writeString(results.resolveSibling(resultName+"-complete.txt"),passed+" passed / "+failed+" failed\n");mc.gui.setScreen(null);mc.stop();return;
            }
            steps.get(step++).run();
        }catch(Throwable e){failed++;log("FAIL step "+step+": "+e);Afmr.LOG.error("Workspace checks",e);if(!started){finished=true;mc.stop();}}
    }
    private static void showPlaylist(String id){editor=new LibraryScreen(null,rt,id);mc.gui.setScreen(editor);}
    @SuppressWarnings("unchecked") private static List<Track> songRows(){return (List<Track>)field(editor,"songRows");}
    private static void sortHeader(SongColumn column){
        var table=field(editor,"songTable");var columns=(SongColumn[])field(table,"columns");var offsets=(int[])field(table,"offsets");var widths=(int[])field(table,"widths");
        int index=Arrays.asList(columns).indexOf(column),scroll=(int)field(table,"scroll");
        int tableX=(int)field(editor,"tableX");float scale=(float)field(editor,"scale");
        editor.mouseScrolled((tableX+50)*scale,170*scale,-(Math.max(0,offsets[index]-80)-scroll)/40d,0);
        scroll=(int)field(table,"scroll");
        var event=new MouseButtonEvent((tableX+28+offsets[index]-scroll+widths[index]/2d)*scale,170*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        editor.mouseClicked(event,false);editor.mouseReleased(event);
    }
    private static void columnChecks(){
        steps.add(()->{
            var p=lib.playlist("custom:columns");if(p==null){p=lib.createPlaylist("Columns fixture");p.id="custom:columns";}p.tracks.clear();p.enabled=false;
            for(int number:new int[]{2,10}){
                String id="file:columns_"+number;var t=lib.track(id);if(t==null){t=new Track(id,lib.track("file:workspace_a").path,number==2?"Track two":"Track ten",false);lib.tracks.add(t);}
                t.albumArtist="Ensemble";t.artist="Soloist";t.album="Generated album";t.trackNumber=Integer.toString(number);t.discNumber="1";t.genre="Ambient";t.date="2026";
                t.replayGain=new ReplayGainInfo(-6d,0.8,-8d,0.9);p.tracks.put(id,true);
            }
            lib.rebuildTrackIndex();lib.settings.songColumns=SongColumn.defaults();showPlaylist(p.id);click("Columns");
            check("Columns chooser opens",mc.gui.screen().getClass().getSimpleName().equals("SongColumnsScreen"));
            check("Title column stays enabled",!button("Title: on").active);
        });
        steps.add(()->{
            for(var column:SongColumn.values())if(!lib.settings.songColumns.contains(column)){findPage(column.label+":");click(prefix(column.label+":"));}
            check("All additional columns can be enabled",lib.settings.songColumns.equals(EnumSet.allOf(SongColumn.class)));
            check("Column controls fit the screen",widgets().stream().allMatch(w->w.getY()>=0&&w.getBottom()<=mc.gui.screen().height));
            MusicMenu.hide(mc.gui.screen());mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Hidden column choices retained",button("File path: on").active);shot("columns-chooser");
        });
        steps.add(()->{click("Back");check("Columns return to workspace",mc.gui.screen()==editor);
            var table=field(editor,"songTable");check("Extra columns retain readable widths",(int)field(table,"total")>(int)field(table,"viewport"));
            @SuppressWarnings("unchecked") var cells=(Map<String,String[]>)field(editor,"songCells");
            var values=Arrays.asList(cells.get("file:columns_2"));check("Metadata cells show album artist and ReplayGain",values.contains("Ensemble")&&values.contains("-6.00 dB")&&values.contains("-8.00 dB"));
            sortHeader(SongColumn.TRACK_NUMBER);check("Track number sorts numerically",songRows().getFirst().trackNumber.equals("2"));
            sortHeader(SongColumn.TRACK_NUMBER);check("Track number reverse sorting",songRows().getFirst().trackNumber.equals("10"));
        });
        steps.add(()->{
            shot("columns-track-number");sortHeader(SongColumn.RG_ALBUM_GAIN);
            check("Scrolled header chooses correct sort column",field(editor,"sortColumn")==SongColumn.RG_ALBUM_GAIN);
            int x=(int)field(editor,"tableX");float scale=(float)field(editor,"scale");var list=(AbstractWidget)field(editor,"songs");String id=songRows().getFirst().id;
            var e=new MouseButtonEvent((x+5)*scale,(list.getY()+7)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));editor.mouseClicked(e,false);editor.mouseReleased(e);
            check("On switch stays pinned during horizontal scrolling",Boolean.FALSE.equals(lib.playlist("custom:columns").tracks.get(id)));
            int barY=list.getBottom()+5;
            e=new MouseButtonEvent((x+35)*scale,barY*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));editor.mouseClicked(e,false);
            var drag=new MouseButtonEvent((x+(int)field(editor,"tableWidth")-12)*scale,barY*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));editor.mouseDragged(drag,200,0);editor.mouseReleased(drag);
            var table=field(editor,"songTable");check("Horizontal scrollbar drags to final column",(int)field(table,"scroll")==(int)field(table,"total")-(int)field(table,"viewport"));
            row("songs",0,false);check("Scrolled row selection preserves assignment",Boolean.FALSE.equals(lib.playlist("custom:columns").tracks.get(id)));
        });
        steps.add(()->{shot("columns-replaygain");showPlaylist(target.id);check("Column choices apply across playlists",((SongColumn[])field(field(editor,"songTable"),"columns")).length==SongColumn.values().length);
            click("Columns");click("Defaults");check("Default columns restored",lib.settings.songColumns.equals(SongColumn.defaults()));click("Back");
            check("Reducing columns removes horizontal overflow",(int)field(field(editor,"songTable"),"scroll")==0&&(int)field(field(editor,"songTable"),"total")== (int)field(field(editor,"songTable"),"viewport"));rt.save();
        });
    }
    private static void name(String value){widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue(value);}
    private static void findPage(String label){
        for(int page=0;page<16;page++){
            if(widgets().stream().anyMatch(w->w.getMessage().getString().startsWith(label)))return;
            click(">");
        }
        throw new IllegalStateException("No control: "+label);
    }
    private static void pressAutoplay(){
        var w=button("Autoplay here");hover(w);float scale=(float)field(editor,"scale");
        heldEvent=new MouseButtonEvent((w.getX()+w.getWidth()/2d)*scale,(w.getY()+w.getHeight()/2d)*scale,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        editor.mouseClicked(heldEvent,false);
    }
    private static void releaseAutoplay(){editor.mouseReleased(heldEvent);hovering=false;movePointer(0,0);}
    private static void setMenuOpacity(double value){
        var slider=prefix("Background opacity:");
        var event=new MouseButtonEvent(slider.getX()+4+(slider.getWidth()-8)*value,slider.getY()+10,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        mc.gui.screen().mouseClicked(event,false);mc.gui.screen().mouseReleased(event);
    }
    private static void checkMenuRender(String label,double opacity,boolean blur){
        var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);var screen=mc.gui.screen();
        screen.extractBackground(g,0,0,0);g.nextStratum();screen.extractRenderState(g,0,0,0);
        int[] text={0,0},background={-1};
        state.forEachText(t->{text[0]++;if(((int)field(t,"color")>>>24)==255)text[1]++;});
        state.forEachElement(e->{if(e instanceof ColoredRectangleRenderState r&&(r.col1()&0xFFFFFF)==0x141B1B)background[0]=r.col1()>>>24;},GuiRenderState.TraverseRange.ALL);
        check(label+" background alpha",background[0]==(int)Math.round(opacity*255));
        check(label+" text stays opaque",text[0]>10&&text[0]==text[1]);
        check(label+" native blur",((int)field(state,"firstStratumAfterBlur")!=Integer.MAX_VALUE)==(blur&&opacity<1));
        check(label+" does not pause gameplay",!screen.isPauseScreen());
    }
    private static void menuAppearanceChecks(){
        steps.add(()->{mc.options.pauseOnLostFocus=false;showPlaylist(target.id);click("Display");click("Menus");
            check("Menu appearance is independent of widget style",prefix("Background opacity:").active&&prefix("Blur background:").active);
            setMenuOpacity(0.5);check("Slider previews immediately",lib.settings.menuBackgroundOpacity==0.5);
            click(prefix("Blur background:"));check("Menu blur toggle",!lib.settings.menuBlur);checkMenuRender("Settings at 50%",0.5,false);
        });
        steps.add(()->{shot("menus-half-no-blur");click(prefix("Blur background:"));checkMenuRender("Settings with blur",0.5,mc.options.getMenuBackgroundBlurriness()>=1);});
        steps.add(()->{shot("menus-half-blur");setMenuOpacity(0);checkMenuRender("Settings at 0%",0,mc.options.getMenuBackgroundBlurriness()>=1);});
        steps.add(()->{shot("menus-transparent");click("Playlists");checkMenuRender("Workspace at 0%",0,mc.options.getMenuBackgroundBlurriness()>=1);});
        steps.add(()->{shot("menu-workspace-transparent");click("Display");setMenuOpacity(1);click("Playlists");checkMenuRender("Workspace at 100%",1,mc.options.getMenuBackgroundBlurriness()>=1);});
        steps.add(()->{shot("menu-workspace-solid");click("Display");click("Reset menu appearance");check("Menu defaults restored",lib.settings.menuBackgroundOpacity==0.80&&lib.settings.menuBlur);
            check("Widget opacity unchanged",lib.settings.widgetBackgroundOpacity==0.92);click("Playlists");});
        steps.add(()->{shot("menu-workspace-default");mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();});
    }
    @SuppressWarnings("unchecked") private static Map<MenuColor,EditBox> colorFields(){return (Map<MenuColor,EditBox>)field(mc.gui.screen(),"fields");}
    private static final int[] PURPLE_COLORS={0x22142B,0x30203B,0x473150,0x604268,0x624176,0xDD92EE,0xF3E7F6,0xBFA8C9,0x79568A,0xAE77BB};
    private static void editColorPage(){
        colorFields().forEach((role,box)->box.setValue(String.format(Locale.ROOT,"#%06X",PURPLE_COLORS[role.ordinal()])));
    }
    private static void reflowColorEditor(int height){
        var screen=mc.gui.screen();screen.height=height;
        try{var method=screen.getClass().getDeclaredMethod("repositionElements");method.setAccessible(true);method.invoke(screen);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void checkPaletteRender(boolean workspace){
        var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);var screen=mc.gui.screen();
        screen.extractBackground(g,0,0,0);g.nextStratum();screen.extractRenderState(g,0,0,0);
        Set<Integer> rectangles=new HashSet<>();int[] text={0,0};
        state.forEachElement(e->{if(e instanceof ColoredRectangleRenderState r)rectangles.add(r.col1());},GuiRenderState.TraverseRange.ALL);
        state.forEachText(t->{text[0]++;if(((int)field(t,"color")>>>24)==255)text[1]++;});
        check("Custom background RGB and opacity render",rectangles.contains(0x8022142B));
        check("Custom panel RGB and opacity render",rectangles.contains(0x8030203B));
        check("Custom button color renders",rectangles.contains(0x80473150));
        check("Custom text remains opaque",text[0]>5&&text[0]==text[1]);
        if(workspace){check("Custom selected tab color renders",rectangles.contains(0x80624176));check("Custom highlight stays opaque",rectangles.contains(0xFFDD92EE));}
        check("Color editor does not pause gameplay",!screen.isPauseScreen());
    }
    private static void menuColorChecks(){
        lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=0.5;lib.settings.menuImageEnabled=false;
        lib.settings.menuImageOpacity=0.4;lib.settings.widgetBackgroundColor=0x123456;
        steps.add(()->{click("Display");click("Menus");click("Menu colors…");check("Menu color editor opens",mc.gui.screen().getClass().getSimpleName().equals("MenuColorsScreen"));
            check("Color fields fit the screen",widgets().stream().allMatch(w->w.getY()>=0&&w.getBottom()<=mc.gui.screen().height));
            var field=colorFields().get(MenuColor.BACKGROUND);field.setValue("#123");var screen=mc.gui.screen();click("Save & back");check("Invalid RGB blocks navigation",mc.gui.screen()==screen);
            field.setValue("#22142B");check("Valid RGB previews immediately",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x22142B);editColorPage();
        });
        steps.add(()->{shot("menu-colors-editor");click(">");editColorPage();
            check("All menu color families are editable",Arrays.stream(MenuColor.values()).allMatch(role->lib.settings.menuColors.get(role)==PURPLE_COLORS[role.ordinal()]));
            var accent=colorFields().get(MenuColor.ACCENT);accent.setValue("bad");click("<");check("Invalid RGB blocks page change",colorFields().containsKey(MenuColor.ACCENT));accent.setValue("DD92EE");
            MusicMenu.hide(mc.gui.screen());mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Hidden color draft retained",colorFields().get(MenuColor.ACCENT).getValue().equals("DD92EE"));
            int oldHeight=mc.gui.screen().height;reflowColorEditor(oldHeight+220);
            check("Resize retains drafts by color family",colorFields().get(MenuColor.ACCENT).getValue().equals("DD92EE")&&colorFields().get(MenuColor.BACKGROUND).getValue().equals("#22142B"));
            reflowColorEditor(oldHeight);if(!colorFields().containsKey(MenuColor.ACCENT))click(">");
            check("Resize does not remap palette values",Arrays.stream(MenuColor.values()).allMatch(role->lib.settings.menuColors.get(role)==PURPLE_COLORS[role.ordinal()]));checkPaletteRender(false);
        });
        steps.add(()->{shot("menu-colors-text");click("Save & back");click("Playlists");checkPaletteRender(true);});
        steps.add(()->{shot("menu-colors-workspace");click("Timing");checkPaletteRender(false);click("Display");if(button("Menus").active)click("Menus");click("Menu colors…");
            colorFields().get(MenuColor.BACKGROUND).setValue("bad");click("Reset colors");check("Reset colors recovers invalid input",lib.settings.menuColors.equals(MenuColor.defaults()));
            check("Reset colors preserves opacity image and widget settings",lib.settings.menuBackgroundOpacity==0.5&&lib.settings.menuImageOpacity==0.4&&lib.settings.widgetBackgroundColor==0x123456);
            click("Save & back");click("Reset menu appearance");check("Full menu reset restores palette and opacity",lib.settings.menuColors.equals(MenuColor.defaults())&&lib.settings.menuBackgroundOpacity==0.80);
            mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();
        });
    }
    private static EditBox pickerHex(){return widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();}
    private static List<AbstractWidget> pickerAreas(){return widgets().stream().filter(w->w.getClass().getSimpleName().equals("ColorPickerArea")).toList();}
    private static void choosePickerArea(int index,double x,double y,boolean drag){
        var area=pickerAreas().get(index);var screen=mc.gui.screen();
        var e=new MouseButtonEvent(area.getX()+(area.getWidth()-1)*x,area.getY()+(area.getHeight()-1)*y,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
        if(drag)screen.mouseDragged(e,10,10);else screen.mouseClicked(e,false);
        if(!drag)screen.mouseReleased(e);
    }
    private static void colorPickerChecks(){
        lib.settings.menuColors=MenuColor.defaults();lib.settings.menuBackgroundOpacity=0.5;lib.settings.widgetBackgroundColor=0x123456;
        steps.add(()->{click("Display");click("Menus");click("Menu colors…");
            for(var role:MenuColor.values()){
                findPage("Pick "+role.label);click("Pick "+role.label);
                check(role.label+" opens a seeded picker",mc.gui.screen().getClass().getSimpleName().equals("ColorPickerScreen")&&((ColorPickerState)field(mc.gui.screen(),"color")).rgb()==role.defaultRgb);
                click("Cancel");
            }
            while(!colorFields().containsKey(MenuColor.BACKGROUND))click("<");click("Pick Background");
            check("Picker controls fit the screen",widgets().stream().allMatch(w->w.getX()>=0&&w.getRight()<=mc.gui.screen().width&&w.getY()>=0&&w.getBottom()<=mc.gui.screen().height));
            pickerHex().setValue("#FF0000");check("Picker hex previews the menu",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF0000);
            choosePickerArea(1,0.5,1/3d,false);check("Hue strip selects green",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x00FF00);
            choosePickerArea(0,0.5,0.5,false);check("SV square selects expected RGB",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0x408040&&pickerHex().getValue().equals("#408040"));
        });
        steps.add(()->{shot("color-picker-menu");
            var area=pickerAreas().getFirst();var screen=mc.gui.screen();var down=new MouseButtonEvent(area.getX()+10,area.getY()+10,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));screen.mouseClicked(down,false);
            choosePickerArea(0,2,2,true);screen.mouseReleased(down);check("Dragging outside square clamps to black",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0);
            screen.setFocused(area);screen.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_UP,0,0));check("Focused arrows adjust the picker",lib.settings.menuColors.get(MenuColor.BACKGROUND)>0);
            pickerHex().setValue("#12");click("Apply");check("Invalid hex blocks Apply",mc.gui.screen()==screen);
            click("Cancel");check("Cancel restores original menu color",lib.settings.menuColors.get(MenuColor.BACKGROUND)==MenuColor.BACKGROUND.defaultRgb);
            colorFields().get(MenuColor.BACKGROUND).setValue("bad");click("Pick Background");click("#FF4040");click("Apply");
            check("Picker can recover invalid parent hex",lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF4040&&colorFields().get(MenuColor.BACKGROUND).getValue().equals("#FF4040"));
            check("Picker preserves panel opacity",lib.settings.menuBackgroundOpacity==0.5);
            click("Save & back");lib.settings.nowPlayingStyle=NowPlayingStyle.MINECRAFT;click("Widget");check("Unsupported widget style disables picker",!button("Pick Background color").active);
            lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;click("Menus");click("Widget");click("Pick Background color");pickerHex().setValue("#00A0C0");
            check("Widget picker previews independently",lib.settings.widgetBackgroundColor==0x00A0C0&&lib.settings.menuColors.get(MenuColor.BACKGROUND)==0xFF4040);
        });
        steps.add(()->{shot("color-picker-widget");var picker=mc.gui.screen();MusicMenu.hide(picker);mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Picker survives hide and reopen",mc.gui.screen()==picker&&pickerHex().getValue().equals("#00A0C0"));
            click("Apply");check("Widget Apply retains chosen RGB",lib.settings.widgetBackgroundColor==0x00A0C0);
            click("Pick Background color");pickerHex().setValue("#FFFFFF");mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_ESCAPE,0,0));
            check("Escape cancels widget preview",lib.settings.widgetBackgroundColor==0x00A0C0&&mc.gui.screen().getClass().getSimpleName().equals("NowPlayingSettingsScreen"));
            mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();
        });
    }
    private static int backgroundLayers(List<double[]> rectangles,double x,double y){
        int count=0;for(var r:rectangles)if(x>=r[0]&&x<r[2]&&y>=r[1]&&y<r[3])count++;return count;
    }
    private static boolean exactBackgroundEdges(List<double[]> rectangles,int x,int y,int width,int height,float scale){
        for(int i=0;i<width;i++)if(backgroundLayers(rectangles,(x+i+0.5)*scale,(y+0.05)*scale)!=1||backgroundLayers(rectangles,(x+i+0.5)*scale,(y+height-0.05)*scale)!=1)return false;
        for(int i=0;i<height;i++)if(backgroundLayers(rectangles,(x+0.05)*scale,(y+i+0.5)*scale)!=1||backgroundLayers(rectangles,(x+width-0.05)*scale,(y+i+0.5)*scale)!=1)return false;
        return true;
    }
    private static void seamChecks(){
        lib.settings.menuColors=MenuColor.defaults();lib.settings.menuImageEnabled=false;lib.folders.clear();
        for(var p:lib.playlists)p.enabled=false;
        var target=lib.createPlaylist("Seam coverage");target.enabled=false;lib.tracks.removeIf(t->!t.vanilla);
        for(int i=0;i<20;i++){var p=lib.createPlaylist("Seam priority "+i);p.enabled=false;}
        for(int i=0;i<100;i++){var track=new Track("file:seam-"+i,Path.of("../../fixtures/long.wav").toAbsolutePath().normalize().toString(),"Seam "+i,false);lib.tracks.add(track);target.tracks.put(track.id,true);}
        lib.rebuildTrackIndex();rt.engine().reset();
        for(boolean overflow:new boolean[]{false,true})for(double opacity:new double[]{1,0.5})for(int[] size:new int[][]{{427,240},{500,271},{853,479},{1280,681}}){
            steps.add(()->{
                lib.settings.songColumns=overflow?EnumSet.allOf(SongColumn.class):SongColumn.defaults();lib.settings.menuBackgroundOpacity=opacity;
                editor=new LibraryScreen(null,rt,target.id);mc.gui.setScreen(editor);editor.width=size[0];editor.height=size[1];
                try{var method=editor.getClass().getDeclaredMethod("repositionElements");method.setAccessible(true);method.invoke(editor);}catch(Exception e){throw new RuntimeException(e);}
                var state=new GuiRenderState();var g=new GuiGraphicsExtractor(mc,state,0,0);editor.extractRenderState(g,0,0,0);
                var backgrounds=new ArrayList<double[]>();int[] wrongAlpha={0};
                state.forEachElement(element->{if(element instanceof ColoredRectangleRenderState r){
                    int rgb=r.col1()&0xFFFFFF;
                    if(rgb!=0x1B2023&&rgb!=0x202629&&rgb!=0x191D20&&rgb!=0x111619)return;
                    if(r.col1()>>>24!=(int)Math.round(opacity*255))wrongAlpha[0]++;
                    double l=Math.min(r.x0(),r.x1())*r.pose().m00()+r.pose().m20(),right=Math.max(r.x0(),r.x1())*r.pose().m00()+r.pose().m20();
                    double t=Math.min(r.y0(),r.y1())*r.pose().m11()+r.pose().m21(),bottom=Math.max(r.y0(),r.y1())*r.pose().m11()+r.pose().m21();
                    if(r.scissorArea()!=null){l=Math.max(l,r.scissorArea().left());right=Math.min(right,r.scissorArea().right());t=Math.max(t,r.scissorArea().top());bottom=Math.min(bottom,r.scissorArea().bottom());}
                    backgrounds.add(new double[]{l,t,right,bottom});
                }},GuiRenderState.TraverseRange.ALL);
                float scale=(float)field(editor,"scale");boolean covered=wrongAlpha[0]==0;
                for(String name:new String[]{"priorities","folders","songs"}){
                    var list=(AbstractWidget)field(editor,name);covered&=exactBackgroundEdges(backgrounds,list.getX(),list.getY(),list.getWidth(),list.getHeight(),scale);
                }
                if(overflow){var list=(AbstractWidget)field(editor,"songs");covered&=exactBackgroundEdges(backgrounds,list.getX(),list.getBottom(),list.getWidth(),10,scale);}
                check("List and scrollbar edges painted once "+size[0]+"x"+size[1]+" opacity="+opacity+" overflow="+overflow,covered);
            });
        }
        steps.add(()->{lib.settings.songColumns=SongColumn.defaults();lib.settings.menuBackgroundOpacity=1;editor=new LibraryScreen(null,rt,target.id);mc.gui.setScreen(editor);});
        steps.add(()->{shot("menu-seams-fixed");mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();});
    }
    private static void gameModeChecks(){
        steps.add(()->{
            mc.options.pauseOnLostFocus=false;originalMode=rt.context().gameMode();showPlaylist(target.id);
            click("Name & conditions");findPage("Gamemode:");click(prefix("Gamemode:"));
            check("Gamemode editor reachable",mc.gui.screen().getClass().getSimpleName().equals("GameModeConditionScreen"));
            check("All four gamemodes visible",List.of("Survival: on","Creative: off","Adventure: off","Spectator: off").stream().allMatch(s->button(s).active));
            click("Filter: off");click("Survival: on");click("Creative: off");click("Spectator: off");
            var draft=mc.gui.screen();MusicMenu.hide(draft);mc.gui.setScreen(MusicMenu.reopen(null,rt));
            check("Gamemode draft survives hide/reopen",mc.gui.screen()==draft&&button("Creative: on").active&&button("Spectator: on").active);
        });
        steps.add(()->{shot("gamemode-choices");click("Save & back");click("Save & back");
            check("Multiple gamemode choices saved",target.conditions.gameModeEnabled&&target.conditions.gameModes.equals(Set.of(GameMode.CREATIVE,GameMode.SPECTATOR)));
            for(var p:lib.playlists)p.enabled=false;
            target.enabled=true;target.manualOnly=false;target.finishCurrentSong=target.finishCurrentSongOnExit=false;target.tracks.put("file:workspace_a",true);
            modeFallback=lib.createPlaylist("Mode fallback");modeFallback.tracks.put("file:workspace_b",true);lib.movePlaylist(target.id,0);
            lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=0.1;lib.settings.bufferSeconds=0.1;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;lib.settings.continuePlayingOnMatch=false;
            rt.engine().reset();NativeSuite.command("gamemode survival @s");
        });
        steps.add(()->{check("Server Survival observed",rt.context().gameMode()==GameMode.SURVIVAL);check("Survival selects fallback",rt.engine().automaticSelected()==modeFallback);NativeSuite.command("gamemode creative @s");});
        steps.add(()->{check("Server Creative observed",rt.context().gameMode()==GameMode.CREATIVE);check("Creative starts gamemode playlist",rt.engine().automaticSelected()==target&&target.id.equals(rt.engine().sourcePlaylistId()));NativeSuite.command("gamemode adventure @s");});
        steps.add(()->{check("Server Adventure observed",rt.context().gameMode()==GameMode.ADVENTURE);check("Adventure returns to fallback",rt.engine().automaticSelected()==modeFallback&&modeFallback.id.equals(rt.engine().sourcePlaylistId()));NativeSuite.command("gamemode spectator @s");});
        steps.add(()->{check("Server Spectator observed",rt.context().gameMode()==GameMode.SPECTATOR);check("Spectator starts gamemode playlist",rt.engine().automaticSelected()==target&&target.id.equals(rt.engine().sourcePlaylistId()));
            showPlaylist(target.id);click("Name & conditions");findPage("Gamemode:");click(prefix("Gamemode:"));click("Filter: on");click("Save & back");click("Save & back");NativeSuite.command("gamemode survival @s");});
        steps.add(()->{check("Filter off keeps choices and matches Survival",!target.conditions.gameModeEnabled&&target.conditions.gameModes.size()==2&&rt.engine().automaticSelected()==target);
            click("Name & conditions");findPage("Gamemode:");click(prefix("Gamemode:"));click("Filter: off");click("Creative: on");click("Spectator: on");click("Save & back");click("Save & back");});
        steps.add(()->{check("Enabled empty selection falls through",target.conditions.gameModeEnabled&&target.conditions.gameModes.isEmpty()&&rt.engine().automaticSelected()==modeFallback);
            click("Name & conditions");findPage("Gamemode:");click(prefix("Gamemode:"));check("Saved empty selection reopens",button("Filter: on").active&&button("Survival: off").active&&button("Creative: off").active&&button("Adventure: off").active&&button("Spectator: off").active);
            click("Save & back");click("Save & back");NativeSuite.command("gamemode "+originalMode.name().toLowerCase(Locale.ROOT)+" @s");
        });
        steps.add(()->{target.enabled=modeFallback.enabled=false;rt.engine().reset();mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();});
    }
    private static void playOnTriggerChecks(){
        steps.add(()->{
            mc.options.pauseOnLostFocus=false;
            target.conditions=new PlaylistConditions();target.conditions.timeEnabled=true;target.conditions.startTime=14000;target.conditions.endTime=15999;
            target.enabled=modeFallback.enabled=true;target.playOnTrigger=false;target.finishCurrentSong=target.finishCurrentSongOnExit=false;
            target.tracks.clear();target.tracks.put("file:workspace_a",true);modeFallback.tracks.clear();modeFallback.tracks.put("file:workspace_b",true);
            lib.movePlaylist(target.id,0);lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=10;lib.settings.bufferSeconds=5;lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=1;
            rt.engine().reset();showPlaylist(target.id);click("Play on trigger: off");
            check("Play on trigger toggle saves",target.playOnTrigger);check("Trigger toggle keeps finish controls independent",!target.finishCurrentSong&&!target.finishCurrentSongOnExit);
            NativeSuite.command("time pause");NativeSuite.command("time set 13000");
        });
        steps.add(rt::nextSong);
        steps.add(()->check("Trigger source starts before time window",modeFallback.id.equals(rt.engine().sourcePlaylistId())));
        steps.add(()->{triggerAt=rt.now();NativeSuite.command("time set 14000");});
        steps.add(()->{check("Trigger starts normal fade before global buffer",rt.engine().phase()==dev.animal.afmr.core.PlaybackEngine.Phase.FADING&&rt.engine().triggerPending()&&rt.now()-triggerAt<5);
            check("Outgoing source remains during trigger fade",modeFallback.id.equals(rt.engine().sourcePlaylistId()));shot("play-on-trigger-fade");});
        steps.add(()->{check("Triggered song starts without ten-second silence",target.id.equals(rt.engine().sourcePlaylistId())&&rt.now()-triggerAt<5);
            check("Native trigger audio starts without error",rt.engine().current()!=null&&rt.engine().error().isBlank());triggerSequence=rt.engine().playbackSequence();});
        steps.add(()->{check("Matching ticks do not restart triggered song",rt.engine().playbackSequence()==triggerSequence);shot("play-on-trigger");click("Skip / play");});
        steps.add(()->{check("Manual skip still uses normal silence",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>8);
            click("Name & conditions");findPage("Time:");click(prefix("Time:"));
            var fields=widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();fields.get(1).setValue("18000");click("Fit songs to time window: off");});
        steps.add(()->{shot("time-window-fit");click("Save & back");click("Save & back");
            check("Time-window fit saves through editor",target.conditions.fitSongsToTimeWindow&&target.conditions.endTime==18000);
            lib.track("file:workspace_a").durationSeconds=0;rt.playNow();});
        steps.add(()->{});
        steps.add(()->{check("Missing generated WAV duration is cached",lib.track("file:workspace_a").durationSeconds>89&&lib.track("file:workspace_a").durationSeconds<91);
            check("Fitting generated WAV starts automatically",target.id.equals(rt.engine().sourcePlaylistId()));
            click("Name & conditions");findPage("Time:");click(prefix("Time:"));
            widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList().get(1).setValue("14020");click("Save & back");click("Save & back");rt.skip();rt.playNow();});
        steps.add(()->{check("No fitting song keeps playlist priority",rt.engine().automaticSelected()==target&&rt.engine().selected()==target);
            check("No fitting song remains silent",rt.engine().current()==null&&rt.engine().waitingForTimeFit());
            click("Name & conditions");findPage("Time:");click(prefix("Time:"));
            widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList().get(1).setValue("18000");click("Save & back");click("Save & back");rt.playNow();});
        steps.add(()->{check("Wider window resumes a fitting song",target.id.equals(rt.engine().sourcePlaylistId()));target.enabled=modeFallback.enabled=false;rt.engine().reset();NativeSuite.command("time resume");mc.options.pauseOnLostFocus=pauseOnLostFocus;rt.save();});
    }
    @SuppressWarnings("unchecked")
    private static Map<String,EditBox> timingValues(){return (Map<String,EditBox>)field(mc.gui.screen(),"values");}
    private static void jukeboxChecks(){
        steps.add(()->{mc.options.pauseOnLostFocus=false;showPlaylist(modeFallback.id);click("Timing");var fields=timingValues();
            check("Independent fade fields are present",fields.containsKey("Fade in (sec)")&&fields.containsKey("Fade out (sec)")&&fields.size()==5);
            fields.get("Min silence (sec)").setValue("5");fields.get("Max silence (sec)").setValue("5");fields.get("Fade in (sec)").setValue("0.4");fields.get("Fade out (sec)").setValue("61");fields.get("Context buffer (sec)").setValue("0");
            var timing=mc.gui.screen();click("Display");check("Out-of-range fade blocks navigation",mc.gui.screen()==timing);fields.get("Fade out (sec)").setValue("1");
            MusicMenu.hide(timing);mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Both fade drafts survive hide/reopen",timingValues().get("Fade in (sec)").getValue().equals("0.4")&&timingValues().get("Fade out (sec)").getValue().equals("1"));
            click("Display");check("Distinct fades save independently",lib.settings.fadeInSeconds==0.4&&lib.settings.fadeOutSeconds==1);click("Timing");
            var modes=EnumSet.noneOf(JukeboxMode.class);for(int i=0;i<4;i++){modes.add(lib.settings.jukeboxMode);click(prefix("Jukebox:"));}
            check("Jukebox selector exposes off and three behaviors",modes.size()==4);click(prefix("Jukebox:"));
            recordVolume=mc.options.getSoundSourceOptionInstance(SoundSource.RECORDS).get();mc.options.getSoundSourceOptionInstance(SoundSource.RECORDS).set(0.01);
            var pos=mc.player.blockPosition().offset(2,0,0);jukeboxOne=pos.getX()+" "+pos.getY()+" "+pos.getZ();pos=pos.offset(1,0,0);jukeboxTwo=pos.getX()+" "+pos.getY()+" "+pos.getZ();
        });
        steps.add(()->{shot("separate-fades-jukebox");click("Playlists");});
        for(var mode:List.of(JukeboxMode.RESUME,JukeboxMode.AFTER_SILENCE,JukeboxMode.IMMEDIATE)){
            steps.add(()->{lib.settings.jukeboxMode=mode;lib.settings.fadeInSeconds=0.4;lib.settings.fadeOutSeconds=1;lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=5;lib.settings.bufferSeconds=mode==JukeboxMode.IMMEDIATE?5:0;
                modeFallback.enabled=true;modeFallback.conditions=new PlaylistConditions();lib.movePlaylist(modeFallback.id,0);rt.engine().reset();rt.nextSong();NativeSuite.command("setblock "+jukeboxOne+" minecraft:jukebox");});
            steps.add(()->{check(mode+" starts generated AFMR fixture",rt.engine().current()!=null);jukeboxSequence=rt.engine().playbackSequence();NativeSuite.command("item replace block "+jukeboxOne+" container.0 with minecraft:music_disc_13");});
            steps.add(()->check(mode+" detects actual jukebox playback",rt.context().jukeboxPlaying()&&rt.engine().jukeboxActive()));
            steps.add(()->{check(mode+" requests native audio pause after fading",rt.engine().jukeboxHolding()&&(boolean)field(field(rt.engine(),"audio"),"paused"));
                check(mode+" retains or ends the song correctly",mode==JukeboxMode.RESUME?rt.engine().current()!=null:rt.engine().current()==null);NativeSuite.command("setblock "+jukeboxOne+" minecraft:air");});
            steps.add(()->{check(mode+" detects jukebox stop",!rt.context().jukeboxPlaying()&&!rt.engine().jukeboxActive());
                if(mode==JukeboxMode.RESUME)check("Resume retains the same playback sequence",rt.engine().current()!=null&&rt.engine().playbackSequence()==jukeboxSequence&&!(boolean)field(field(rt.engine(),"audio"),"paused"));
                else if(mode==JukeboxMode.AFTER_SILENCE)check("After-silence mode starts a full gap after the record",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>3);
                else check("Immediate mode starts a new song without buffer/gap",rt.engine().current()!=null&&rt.engine().playbackSequence()>jukeboxSequence);
            });
        }
        steps.add(()->{lib.settings.jukeboxMode=JukeboxMode.RESUME;lib.settings.bufferSeconds=0;rt.nextSong();
            NativeSuite.command("setblock "+jukeboxOne+" minecraft:jukebox");NativeSuite.command("setblock "+jukeboxTwo+" minecraft:jukebox");});
        steps.add(()->{NativeSuite.command("item replace block "+jukeboxOne+" container.0 with minecraft:music_disc_13");NativeSuite.command("item replace block "+jukeboxTwo+" container.0 with minecraft:music_disc_cat");});
        steps.add(()->{});
        steps.add(()->{check("Overlapping jukeboxes hold AFMR",rt.engine().jukeboxHolding());NativeSuite.command("setblock "+jukeboxOne+" minecraft:air");});
        steps.add(()->{check("One remaining jukebox keeps AFMR held",rt.context().jukeboxPlaying()&&rt.engine().jukeboxHolding());NativeSuite.command("setblock "+jukeboxTwo+" minecraft:air");});
        steps.add(()->{check("Last jukebox stopping releases AFMR",!rt.context().jukeboxPlaying()&&!rt.engine().jukeboxActive());
            mc.options.getSoundSourceOptionInstance(SoundSource.RECORDS).set(0.0);NativeSuite.command("setblock "+jukeboxOne+" minecraft:jukebox");});
        steps.add(()->NativeSuite.command("item replace block "+jukeboxOne+" container.0 with minecraft:music_disc_13"));
        steps.add(()->{check("Muted records do not suppress AFMR",!rt.context().jukeboxPlaying()&&!rt.engine().jukeboxActive());NativeSuite.command("setblock "+jukeboxOne+" minecraft:air");
            mc.options.getSoundSourceOptionInstance(SoundSource.RECORDS).set(recordVolume);lib.settings.jukeboxMode=JukeboxMode.OFF;modeFallback.enabled=false;rt.engine().reset();mc.options.pauseOnLostFocus=pauseOnLostFocus;mc.options.save();rt.save();});
    }
    private static void templateChecks(){
        steps.add(()->{showPlaylist("day_outside");click("Name & conditions");name("Day template edited");click("Save & back");check("Default name is editable",lib.playlist("day_outside").name.equals("Day template edited"));});
        steps.add(()->{showPlaylist("dark_exploration");click("Name & conditions");findPage("Groups:");click(prefix("Groups:"));check("Default groups reachable",mc.gui.screen().getClass().getSimpleName().equals("ConditionGroupsScreen"));
            var list=widgets().stream().filter(w->w.getClass().getSimpleName().equals("DenseList")).findFirst().orElseThrow();
            var event=new MouseButtonEvent(list.getX()+20,list.getY()+21,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));mc.gui.screen().mouseClicked(event,true);mc.gui.screen().mouseReleased(event);
            click(prefix("Group:"));click("Save & back");check("Dimension branch editable",lib.playlist("dark_exploration").conditions.groups.get(1).enabled);click("Back");click("Save & back");});
        steps.add(()->{lib.playlist("dark_exploration").tracks.put("file:workspace_a",false);click("Clone");name("Workspace branch");click("Clone");branch=lib.playlist((String)field(editor,"playlistId"));
            check("Clone selected and independent",branch!=lib.playlist("dark_exploration")&&branch.conditions!=lib.playlist("dark_exploration").conditions&&Boolean.FALSE.equals(branch.tracks.get("file:workspace_a")));
            check("Clone keeps nested branch",branch.conditions.groups.get(1).enabled);branch.manualOnly=true;oldConditions=branch.conditions;
            click("Autoplay here");check("Autoplay tap does not change conditions",branch.conditions==oldConditions);pressAutoplay();});
        steps.add(()->{shot("autoplay-partial");check("Hold warning replaces label",button("Are you sure? Replaces conditions").getMessage().getString().contains("Replaces"));releaseAutoplay();check("Short hold cancels",branch.conditions==oldConditions);});
        steps.add(()->{pressAutoplay();hoverX=hoverY=0;});
        steps.add(()->{});
        steps.add(()->{releaseAutoplay();check("Moving outside cancels hold",branch.conditions==oldConditions);});
        steps.add(WorkspaceSuite::pressAutoplay);
        steps.add(()->{shot("autoplay-hold");check("Still unchanged before one second",branch.conditions==oldConditions);});
        steps.add(()->{releaseAutoplay();check("Completed hold replaces conditions",branch.conditions!=oldConditions&&branch.conditions.areaEnabled&&branch.conditions.groups.isEmpty()&&branch.conditions.area.radius==64);
            check("Autoplay enables only this playlist",branch.enabled&&!branch.manualOnly&&branch.conditions.area.contains(rt.context())&&!lib.playlist("home").conditions.area.configured);
            check("Autoplay keeps assignments and original template",Boolean.FALSE.equals(branch.tracks.get("file:workspace_a"))&&lib.playlist("dark_exploration").conditions.groups.size()==3);
            oldConditions=branch.conditions;pressAutoplay();click("Timing");});
        steps.add(()->{hovering=false;click("Playlists");check("Navigation cancels an armed hold",branch.conditions==oldConditions);showPlaylist(target.id);row("songs",0,false);rt.preview(lib.track("file:workspace_a"));});
        steps.add(()->{check("Artwork fixture loaded",(boolean)field(field(editor,"art"),"loaded"));click("Skip / play");});
        steps.add(()->{check("Silent gap clears current track and cover",rt.engine().current()==null&&field(field(editor,"art"),"track")==null&&!(boolean)field(field(editor,"art"),"loaded"));check("Preview selection is retained",field(editor,"inspected")==lib.track("file:workspace_a"));shot("footer-cleared");rt.preview(lib.track("file:workspace_b"));});
        steps.add(()->{check("No-cover song does not inherit artwork",rt.engine().current()==lib.track("file:workspace_b")&&field(field(editor,"art"),"track")==lib.track("file:workspace_b")&&!(boolean)field(field(editor,"art"),"loaded"));shot("footer-no-cover");click("Preview");});
        steps.add(()->{check("Selected preview still works",rt.engine().current()==lib.track("file:workspace_a"));click("Display");var seen=EnumSet.noneOf(WidgetCorner.class);for(int i=0;i<5;i++){seen.add(lib.settings.widgetCorner);click(prefix("Corner:"));}check("Corner selector exposes all corners and default",seen.size()==5);mc.gui.setScreen(null);lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;lib.settings.popupSeconds=60;lib.settings.widgetCorner=WidgetCorner.TOP_LEFT;rt.overlay().refresh();});
        for(var corner:new WidgetCorner[]{WidgetCorner.TOP_LEFT,WidgetCorner.TOP_RIGHT,WidgetCorner.BOTTOM_LEFT,WidgetCorner.BOTTOM_RIGHT}){
            steps.add(()->{lib.settings.widgetCorner=corner;rt.overlay().refresh();});
            steps.add(()->shot("card-"+corner.name().toLowerCase(Locale.ROOT)));
        }
        steps.add(()->{lib.settings.nowPlayingStyle=NowPlayingStyle.HYBRID;lib.settings.widgetCorner=WidgetCorner.TOP_RIGHT;rt.overlay().refresh();});
        steps.add(()->{shot("hybrid-top-right");lib.settings.nowPlayingStyle=NowPlayingStyle.MINECRAFT;lib.settings.widgetCorner=WidgetCorner.BOTTOM_RIGHT;toastDisplay=mc.options.musicToast().get();mc.options.musicToast().set(MusicToastDisplayState.PAUSE_AND_TOAST);rt.overlay().refresh();});
        steps.add(()->{var toast=new NowPlayingToast();check("Native toast corner uses screen anchors",toast.xPos(mc.getWindow().getGuiScaledWidth(),1)>0&&toast.yPos(0)>0);check("Native popup captured outside the pause menu",mc.gui.screen()==null);shot("native-bottom-right");rt.skip();mc.options.musicToast().set(toastDisplay);mc.options.pauseOnLostFocus=pauseOnLostFocus;lib.settings.widgetCorner=WidgetCorner.DEFAULT;rt.save();});
    }
    private static void backupChecks(){
        Path config=mc.gameDirectory.toPath().resolve("config/afmr"),backup=mc.gameDirectory.toPath().resolve("full-test.afmrb");
        if(Boolean.getBoolean("afmr.probe.backupFromFabric")){
            try{
                var data=dev.animal.afmr.library.ConfigBackup.read(mc.gameDirectory.toPath().resolve("fabric-import.afmrb"));var expected=data.library();
                check("Fabric backup restores shared appearance on NeoForge",lib.settings.widgetBackgroundColor==expected.settings.widgetBackgroundColor&&lib.settings.menuColors.equals(expected.settings.menuColors));
                check("Fabric backup restores playlists and assignments on NeoForge",new com.google.gson.Gson().toJson(lib.playlists).equals(new com.google.gson.Gson().toJson(expected.playlists)));
                check("Fabric backup restores profile links on NeoForge",lib.profiles.bindings.equals(expected.profiles.bindings)&&lib.profiles.entries().equals(expected.profiles.entries()));
                check("Fabric backup restores AFMR keys on NeoForge",Afmr.backupKeys().equals(data.keys()));
                check("Fabric backup restores history on NeoForge",rt.engine().history().size()==data.historyCount());
                check("Fabric backup carries managed image to NeoForge",Arrays.equals(Files.readAllBytes(config.resolve("menu-background.png")),data.files().get("menu-background.png")));
                check("Fabric backup consumes staged restore on NeoForge",!Files.exists(config.resolve(dev.animal.afmr.library.ConfigBackup.PENDING)));
            }catch(Exception e){throw new RuntimeException(e);}
            if(Boolean.getBoolean("afmr.probe.menuSeams")){editor=new LibraryScreen(null,rt,lib.playlists.getFirst().id);mc.gui.setScreen(editor);seamChecks();}
            return;
        }
        if(Boolean.getBoolean("afmr.probe.backupRestart")){
            check("Restart installs staged settings",lib.settings.menuColors.get(MenuColor.ACCENT)==0x123456&&lib.settings.fadeInSeconds==1.25&&lib.settings.fadeOutSeconds==2.5);
            check("Restart restores editable playlist flags",lib.playlists.stream().anyMatch(p->p.name.equals("Backup native fixture")&&p.playOnTrigger&&p.volume==0.42));
            check("Restart restores all profiles",lib.profiles.entries().stream().anyMatch(p->p.name().equals("Backup second")));
            check("Restart restores AFMR physical shortcut",Afmr.backupKeys().get("key.afmr.inspect").code().equals("key.keyboard.k"));
            check("Restart consumes pending archive",!Files.exists(config.resolve(dev.animal.afmr.library.ConfigBackup.PENDING)));
            check("Restart keeps prior files",Files.isRegularFile(config.resolve("restore-previous/library.json")));
            return;
        }
        for(var p:lib.playlists)p.enabled=false;lib.settings.rescanOnLaunch=false;lib.settings.menuImageEnabled=false;
        lib.settings.menuColors.put(MenuColor.ACCENT,0x123456);lib.settings.fadeInSeconds=1.25;lib.settings.fadeOutSeconds=2.5;
        var p=lib.playlists.stream().filter(v->v.name.equals("Backup native fixture")).findFirst().orElseGet(()->lib.createPlaylist("Backup native fixture"));
        p.enabled=false;p.playOnTrigger=true;p.volume=0.42;
        if(lib.profiles.entries().stream().noneMatch(v->v.name().equals("Backup second")))lib.profiles.create(lib,"Backup second",lib.profiles.activeId);
        var keys=new HashMap<>(Afmr.backupKeys());keys.put("key.afmr.inspect",new dev.animal.afmr.library.ConfigBackup.Key("key.keyboard.k","NONE"));Afmr.restoreKeys(keys);
        editor=new LibraryScreen(null,rt,p.id);mc.gui.setScreen(editor);click("Import / export");click("Full configuration...");
        check("Full backup has a separate compact route",mc.gui.screen().getClass().getSimpleName().equals("ConfigBackupScreen"));
        check("Replacement requires a reviewed import",!button("Replace on restart").active);rt.exportBackup(backup);
        steps.add(()->{
            check("Full archive exported on worker",!rt.scanning()&&Files.isRegularFile(backup));
            try{var data=dev.animal.afmr.library.ConfigBackup.read(backup);check("Export includes complete keys and profiles",data.keys().size()==15&&data.library().profiles.entries().size()>=2);}
            catch(Exception e){throw new RuntimeException(e);}
            lib.settings.menuColors.put(MenuColor.ACCENT,0xABCDEF);rt.save();rt.prepareBackupImport(backup);
        });
        steps.add(()->{
            check("Import validates without replacing live settings",rt.pendingBackup()!=null&&lib.settings.menuColors.get(MenuColor.ACCENT)==0xABCDEF&&!rt.backupQueued());
            check("Review unlocks Apply",button("Replace on restart").active);
            check("Review controls fit viewport",widgets().stream().allMatch(w->w.getX()>=0&&w.getY()>=0&&w.getRight()<=mc.gui.screen().width&&w.getBottom()<=mc.gui.screen().height));
            shot("full-backup-review");click("Back");check("Back cancels uncommitted import",rt.pendingBackup()==null);click("Full configuration...");rt.prepareBackupImport(backup);
        });
        steps.add(()->click("Replace on restart"));
        steps.add(()->{
            check("Apply queues restore without changing current session",rt.backupQueued()&&Files.isRegularFile(config.resolve(dev.animal.afmr.library.ConfigBackup.PENDING))&&lib.settings.menuColors.get(MenuColor.ACCENT)==0xABCDEF);
            check("Repeated apply disabled",!button("Replace on restart").active);shot("full-backup-restart");rt.save();
        });
    }
    private static EditBox widgetField(String name){return (EditBox)field(mc.gui.screen(),name);}
    private static boolean widgetBounds(){
        var visible=widgets().stream().filter(w->w.visible).toList();var screen=mc.gui.screen();
        return visible.stream().allMatch(a->a.getX()>=0&&a.getY()>=0&&a.getRight()<=screen.width&&a.getBottom()<=screen.height)
            &&visible.stream().allMatch(a->visible.stream().allMatch(b->a==b||a.getRight()<=b.getX()||b.getRight()<=a.getX()||a.getBottom()<=b.getY()||b.getBottom()<=a.getY()));
    }
    private static void silenceChecks(){
        rt.engine().reset();lib.settings.bufferSeconds=lib.settings.fadeInSeconds=lib.settings.fadeOutSeconds=0;
        lib.settings.silenceMinSeconds=lib.settings.silenceMaxSeconds=20;lib.settings.playImmediatelyOnJoin=false;
        lib.settings.jukeboxMode=JukeboxMode.OFF;lib.settings.menuImageEnabled=false;
        for(var p:lib.playlists)p.enabled=false;
        var track=lib.tracks.stream().filter(t->!t.vanilla&&t.durationSeconds>=30&&Files.isRegularFile(Path.of(t.path))).findFirst().orElseThrow();
        target=lib.createPlaylist("Silence fixture");target.includeVanilla=false;target.tracks.put(track.id,true);lib.movePlaylist(target.id,0);
        showPlaylist(target.id);
        check("Volume and silence share one row without overlap",button("Silence: default").getX()>=prefix("Volume:").getRight());
        click("Silence: default");
        check("Playlist silence editor opens with inherited default",mc.gui.screen().getClass().getSimpleName().equals("PlaylistSilenceScreen")&&!target.customSilence&&button("Custom silence: off").active);
        check("Small-window controls fit without overlap",widgetBounds());
        widgetField("minimum").setValue("600");widgetField("maximum").setValue("600");click("Custom silence: off");
        check("Fixed ten-minute custom gap can be enabled",target.customSilence&&target.silenceMinSeconds==600&&target.silenceMaxSeconds==600);
        widgetField("minimum").setValue("invalid");var screen=mc.gui.screen();click("Save & back");
        check("Invalid draft blocks saving",mc.gui.screen()==screen&&target.silenceMinSeconds==600);
        click("Custom silence: on");check("Invalid draft also blocks toggling",target.customSilence);
        widgetField("minimum").setValue("601");click("Save & back");check("Reversed range blocks saving",mc.gui.screen()==screen);
        widgetField("minimum").setValue("600");widgetField("maximum").setValue("86401");click("Save & back");check("Over-limit range blocks saving",mc.gui.screen()==screen);
        widgetField("maximum").setValue("600");MusicMenu.hide(screen);mc.gui.setScreen(MusicMenu.reopen(null,rt));
        check("Hide and reopen retains fields",mc.gui.screen()==screen&&widgetField("maximum").getValue().equals("600"));
        int originalHeight=screen.height;widgetReflow(screen,originalHeight+20);check("Resize retains numeric drafts",widgetField("minimum").getValue().equals("600")&&widgetField("maximum").getValue().equals("600"));widgetReflow(screen,originalHeight);
        click("Custom silence: on");click("Custom silence: off");
        steps.add(()->{shot("playlist-silence-editor");click("Save & back");check("Workspace label reflects custom timing",button("Silence: custom").active);});
        steps.add(()->{shot("playlist-silence-workspace");click("Silence: custom");click("Custom silence: on");check("Disabling keeps the saved range",!target.customSilence&&target.silenceMinSeconds==600&&target.silenceMaxSeconds==600);
            click("Save & back");check("Workspace label returns to default",button("Silence: default").active);click("Silence: default");click("Custom silence: off");
            widgetField("minimum").setValue("3");widgetField("maximum").setValue("3");click("Save & back");rt.overridePlaylist(target.id,OverrideMode.UNTIL_RELEASED);});
        steps.add(()->{check("Manual override starts generated music despite custom gap",rt.engine().current()!=null&&target.id.equals(rt.engine().sourcePlaylistId()));triggerSequence=rt.engine().playbackSequence();rt.skip();});
        steps.add(()->check("Skip waits inside the custom three-second gap",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>0&&rt.engine().remainingSilence(rt.now())<=3));
        steps.add(()->{});steps.add(()->{});steps.add(()->{});steps.add(()->{});
        steps.add(()->{check("Generated music restarts after the custom gap",rt.engine().current()!=null&&rt.engine().playbackSequence()==triggerSequence+1&&rt.engine().error().isBlank());
            check("Native audio is active",((dev.animal.afmr.core.PlaybackEngine.Audio)field(rt.engine(),"audio")).playing());
            target.silenceMinSeconds=target.silenceMaxSeconds=600;rt.skip();check("Ten-minute range schedules without waiting in real time",rt.engine().remainingSilence(rt.now())>599);
            rt.save();});
    }
    private static void widgetChecks(){
        lib.settings.nowPlayingStyle=NowPlayingStyle.AFMR;lib.settings.menuImageEnabled=false;lib.settings.menuColors=MenuColor.defaults();
        editor=new LibraryScreen(null,rt,lib.playlists.getFirst().id);mc.gui.setScreen(editor);click("Display");
        check("Display has Widget and Menus without the old sub-tabs",!button("Widget").active&&button("Menus").active&&widgets().stream().noneMatch(w->List.of("Size","Information","Appearance").contains(w.getMessage().getString())));
        check("All five numeric/color fields and seven information switches share Widget",widgets().stream().filter(EditBox.class::isInstance).count()==5&&List.of("Artwork / note:","Song title:","Artist:","Album:","Song length:","Source playlist:","Scroll long text:").stream().allMatch(t->widgets().stream().anyMatch(w->w.getMessage().getString().startsWith(t))));
        check("Small-window top controls fit without overlap",widgetBounds());
        var duration=widgetField("duration");revealWidget(duration);duration.setValue("invalid");var screen=mc.gui.screen();
        screen.mouseScrolled(100,90,0,-1);check("Scrolling retains invalid drafts without committing",widgetField("duration")==duration&&duration.getValue().equals("invalid"));
        click("Menus");check("Invalid widget draft blocks tab navigation",mc.gui.screen()==screen&&(int)field(screen,"page")==0);duration.setValue("9");
        widgetField("scale").setValue("125");widgetField("widgetWidth").setValue("360");
        click("Menus");check("Switching tabs saves all widget fields",lib.settings.popupSeconds==9&&lib.settings.widgetScale==1.25&&lib.settings.widgetWidth==360);
        check("Menus contains only menu controls",widgets().stream().noneMatch(EditBox.class::isInstance)&&button("Custom background…").active&&button("Menu colors…").active);
        check("Menus controls fit without overlap",widgetBounds());click("Widget");
        var before=lib.settings.widgetCorner;click(prefix("Corner:"));check("Corner remains configurable",lib.settings.widgetCorner!=before);
        click(prefix("Style:"));check("Minecraft style keeps native controls and disables custom fields",lib.settings.nowPlayingStyle==NowPlayingStyle.MINECRAFT&&!widgetField("duration").active&&!widgetField("scale").active&&!button("Pick Background color").active&&!button("Preview widget").active&&widgets().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.CycleButton<?> &&w.active));
        click(prefix("Style:"));check("Hybrid retains size and metadata while keeping its native frame",lib.settings.nowPlayingStyle==NowPlayingStyle.HYBRID&&widgetField("scale").active&&!button("Pick Background color").active);
        click(prefix("Style:"));var artist=prefix("Artist:");boolean oldArtist=lib.settings.widgetArtist;click(artist);check("Information toggle works in the consolidated panel",lib.settings.widgetArtist!=oldArtist);
        var text=prefix("Text:");boolean oldWrap=lib.settings.widgetWrap;click(text);check("Text layout remains configurable",lib.settings.widgetWrap!=oldWrap);
        revealWidget(widgetField("backgroundColor"));widgetField("backgroundColor").setValue("#123456");widgetField("opacity").setValue("65");
        check("Bottom controls fit without overlap",widgetBounds());
        int scroll=(int)field(mc.gui.screen(),"scroll");var held=mc.gui.screen();MusicMenu.hide(held);mc.gui.setScreen(MusicMenu.reopen(null,rt));
        check("Hide/reopen retains widget drafts and scroll",mc.gui.screen()==held&&widgetField("backgroundColor").getValue().equals("#123456")&&(int)field(held,"scroll")==scroll);
        steps.add(()->{shot("widget-settings-bottom");click("Pick Background color");pickerHex().setValue("#00A0C0");click("Cancel");
            check("Color picker cancel retains the applied opening color",lib.settings.widgetBackgroundColor==0x123456&&lib.settings.widgetBackgroundOpacity==0.65);
            click("Pick Background color");pickerHex().setValue("#00A0C0");click("Apply");check("Color picker Apply still works",lib.settings.widgetBackgroundColor==0x00A0C0);
            var current=mc.gui.screen();current.setFocused(null);current.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_HOME,0,0));check("Home scrolls to first setting row",(int)field(current,"scroll")==0);
            revealWidget(widgetField("duration"));current.setFocused(widgetField("duration"));current.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_PAGEDOWN,0,0));
            check("Page Down scrolls the panel",(int)field(current,"scroll")>0);
            current.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_END,0,0));
            var bar=widgets().stream().filter(w->w.getClass().getSimpleName().equals("SettingsScrollbar")).findFirst().orElseThrow();
            var down=new MouseButtonEvent(bar.getX()+4,bar.getBottom()-4,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));current.mouseClicked(down,false);
            current.mouseDragged(new MouseButtonEvent(bar.getX()+4,bar.getY()+1,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0)),0,-100);current.mouseReleased(down);
            check("Scrollbar dragging reaches the top",(int)field(current,"scroll")==0);
            click("Preview widget");check("Preview remains directly available",mc.gui.screen().getClass().getSimpleName().equals("NowPlayingPreviewScreen"));
        });
        steps.add(()->{shot("widget-settings-preview");click("Back");
            var screenNow=mc.gui.screen();screenNow.setFocused(null);screenNow.keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_HOME,0,0));
            check("Widget keeps saved values after preview",widgetField("scale").getValue().equals("125")&&widgetField("widgetWidth").getValue().equals("360"));
            // Exercise the existing resize path with simultaneous drafts, then restore actual bounds.
            int height=screenNow.height;widgetReflow(screenNow,height+260);check("Large-window controls fit without overlap",widgetBounds());widgetReflow(screenNow,height);
            revealWidget(button("Pick Background color"));hover(button("Pick Background color"));
        });
        steps.add(()->{checkHover("widget-settings-scrolled-picker");click("Menus");var oldColor=lib.settings.widgetBackgroundColor;click("Reset menu appearance");
            check("Menu reset preserves widget appearance",lib.settings.widgetBackgroundColor==oldColor&&lib.settings.widgetBackgroundOpacity==0.65);
            click("Widget");mc.gui.screen().setFocused(null);mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(InputConstants.KEY_HOME,0,0));rt.save();
        });
        steps.add(()->shot("widget-settings-top"));
    }
    private static void widgetReflow(Screen screen,int height){
        screen.height=height;
        try{var method=Class.forName("dev.animal.afmr.ui.MusicScreen").getDeclaredMethod("repositionElements");method.setAccessible(true);method.invoke(screen);}catch(Exception e){throw new RuntimeException(e);}
    }
    private static com.mojang.blaze3d.platform.NativeImage[] backgroundPixels;
    private static void backgroundChecks(){
        var background=rt.menuBackground();
        background.enabled(false);lib.settings.menuImageName="";lib.settings.menuImageOpacity=1;lib.settings.menuBackgroundOpacity=0.25;
        try{
            var constructor=Class.forName("dev.animal.afmr.ui.MenuBackgroundScreen").getDeclaredConstructor(Screen.class,ClientRuntime.class);
            constructor.setAccessible(true);mc.gui.setScreen((Screen)constructor.newInstance(null,rt));
        }catch(Exception e){throw new RuntimeException(e);}
        background.choose(Path.of("background-fixtures/lossless.webp"));
        steps.add(()->{
            check("Packaged WebP decoder imports through actual menu worker",!background.busy()&&background.status().isBlank()&&(boolean)field(background,"uploaded"));
            check("Static WebP uploads one texture",((Object[])field(background,"textures")).length==1&&lib.settings.menuImageName.equals("lossless.webp"));
            check("Fast import refreshes enabled and remove controls",button("Image: on").active&&button("Remove image").active);
            shot("background-webp");background.choose(Path.of("background-fixtures/disposal.gif"));
        });
        steps.add(()->{
            check("Animated GIF uploads all four composited textures",!background.busy()&&background.status().isBlank()&&((Object[])field(background,"textures")).length==4);
            var ids=(net.minecraft.resources.Identifier[])field(background,"textures");backgroundPixels=new com.mojang.blaze3d.platform.NativeImage[ids.length];
            for(int i=0;i<ids.length;i++)backgroundPixels[i]=((net.minecraft.client.renderer.texture.DynamicTexture)mc.getTextureManager().getTexture(ids[i])).getPixels();
            check("GPU texture pixels preserve transparent and colored patches",backgroundPixels[0].getPixel(20,10)==0&&backgroundPixels[1].getPixel(20,10)==0xff00ff00);
            shot("background-gif-a");
            rt.exportBackup(Path.of("background-animation.afmrb"));
        });
        steps.add(()->{shot("background-gif-b");check("Animation clock follows GIF timing",field(background,"animation") instanceof dev.animal.afmr.core.MenuImageAnimation);
            try{
                var backup=dev.animal.afmr.library.ConfigBackup.read(Path.of("background-animation.afmrb"));
                check("Full backup exports the animation bytes",Arrays.equals(backup.files().get("menu-background.gif"),Files.readAllBytes(Path.of("background-fixtures/disposal.gif"))));
            }catch(Exception e){throw new RuntimeException(e);}
            background.invalidate();check("Resource invalidation releases every frame",closedBackgroundPixels());
        });
        steps.add(()->{
            check("Managed GIF reloads and uploads after invalidation",!background.busy()&&(boolean)field(background,"uploaded")&&((Object[])field(background,"textures")).length==4);
            background.enabled(false);check("Disable releases animation textures",((Object[])field(background,"textures")).length==0);
            background.enabled(true);
        });
        steps.add(()->{check("Re-enable loads the retained animation",(boolean)field(background,"uploaded"));
            background.choose(Path.of("background-fixtures/lossless.webp"));});
        steps.add(()->{
            check("Static replacement releases previous animation",!background.busy()&&((Object[])field(background,"textures")).length==1);
            check("Static replacement removes managed GIF",!Files.exists(Path.of("config/afmr/menu-background.gif")));background.remove();
        });
        steps.add(()->{
            check("Remove clears owned files and image selection",!background.busy()&&lib.settings.menuImageName.isBlank()
                &&!Files.exists(Path.of("config/afmr/menu-background.png"))&&!Files.exists(Path.of("config/afmr/menu-background.gif")));
            check("Original WebP and GIF remain intact",Files.isRegularFile(Path.of("background-fixtures/lossless.webp"))&&Files.isRegularFile(Path.of("background-fixtures/disposal.gif")));
        });
    }
    private static boolean closedBackgroundPixels(){
        for(var image:backgroundPixels){try{image.getPixel(0,0);return false;}catch(IllegalStateException expected){}}
        return true;
    }
    private static void joinChecks(){
        check("Join activates bound profile before reading policy",lib.profiles.activeId.equals("join-test")&&lib.settings.playImmediatelyOnJoin);
        check("Actual world join starts its fresh-context winner",rt.engine().current()!=null&&rt.engine().current().id.equals("file:join-a")&&rt.engine().sourcePlaylistId().equals("join-world")&&rt.engine().error().isBlank());
        check("First start bypasses long silence and buffer",lib.settings.silenceMinSeconds==600&&lib.settings.bufferSeconds==60&&rt.engine().playbackSequence()==1);
        editor=new LibraryScreen(null,rt,"join-world");mc.gui.setScreen(editor);click("Timing");
        check("Timing exposes the saved join toggle",button("Play immediately on join: on").active);
        check("Timing controls fit without overlapping",widgets().stream().allMatch(a->a.getX()>=0&&a.getY()>=0&&a.getRight()<=mc.gui.screen().width&&a.getBottom()<=mc.gui.screen().height)
            &&widgets().stream().allMatch(a->widgets().stream().allMatch(b->a==b||a.getRight()<=b.getX()||b.getRight()<=a.getX()||a.getBottom()<=b.getY()||b.getBottom()<=a.getY())));
        check("Minecraft defaults stays directly available",button("Minecraft defaults").active);
        var values=widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();values.getFirst().setValue("invalid");click("Play immediately on join: on");
        check("Invalid numeric drafts block toggle mutation",lib.settings.playImmediatelyOnJoin);values.getFirst().setValue("600");click("Play immediately on join: on");
        check("Toggle saves without interrupting the playing song",!lib.settings.playImmediatelyOnJoin&&rt.engine().current()!=null);
        click("Play immediately on join: off");var screen=mc.gui.screen();MusicMenu.hide(screen);mc.gui.setScreen(MusicMenu.reopen(null,rt));
        check("Join option survives hide and reopen",mc.gui.screen()==screen&&button("Play immediately on join: on").active);
        steps.add(()->{shot("play-on-join-timing");rt.skip();});steps.add(()->check("Later manual Skip retains normal silence",rt.engine().current()==null&&rt.engine().remainingSilence(rt.now())>590));
    }
    private static void start(){
        rt=ClientRuntime.get();lib=rt.library();
        pauseOnLostFocus=mc.options.pauseOnLostFocus;mc.options.pauseOnLostFocus=false;
        if(Boolean.getBoolean("afmr.probe.playlistSilence")){silenceChecks();return;}
        if(Boolean.getBoolean("afmr.probe.widgetSettings")){widgetChecks();return;}
        if(Boolean.getBoolean("afmr.probe.backgroundImages")){backgroundChecks();return;}
        if(Boolean.getBoolean("afmr.probe.playOnJoin")){joinChecks();return;}
        if(Boolean.getBoolean("afmr.probe.configBackup")){backupChecks();return;}
        if(Boolean.getBoolean("afmr.probe.menuColors")){
            editor=new LibraryScreen(null,rt,lib.playlists.getFirst().id);mc.gui.setScreen(editor);if(Boolean.getBoolean("afmr.probe.menuSeams"))seamChecks();else if(Boolean.getBoolean("afmr.probe.colorPickers"))colorPickerChecks();else menuColorChecks();return;
        }
        lib.settings.menuBackgroundOpacity=0.80;lib.settings.menuBlur=true;
        lib.settings.jukeboxMode=JukeboxMode.OFF;
        lib.settings.nowPlayingStyle=NowPlayingStyle.MINECRAFT;lib.settings.widgetCorner=WidgetCorner.DEFAULT;
        lib.playlists=new ArrayList<>(PlaylistTemplates.create());
        for(Playlist p:lib.playlists){p.includeVanilla=false;p.enabled=false;}
        Path tone=Path.of("../../fixtures/long.wav").toAbsolutePath().normalize();
        check("Generated audio fixture exists",Files.isRegularFile(tone));
        lib.tracks.removeIf(t->!t.vanilla);lib.tracks.add(new Track("file:workspace_a",tone.resolveSibling("with-cover/long.wav").toString(),"Workspace tone A",false));
        lib.tracks.add(new Track("file:workspace_b",tone.toString(),"Workspace tone B",false));lib.rebuildTrackIndex();
        target=lib.createPlaylist("Workspace regression");target.enabled=false;target.includeVanilla=false;
        editor=new LibraryScreen(null,rt,target.id);mc.gui.setScreen(editor);
        steps.add(()->{
            check("Six direct navigation controls",List.of("Playlists","Library","Timing","Display","Keys","Inspector").stream().allMatch(t->widgets().stream().anyMatch(w->w.getMessage().getString().equals(t))));
            ((EditBox)field(editor,"librarySearch")).setValue("Workspace tone");
            check("Library below priority",((AbstractWidget)field(editor,"folders")).getY()>((AbstractWidget)field(editor,"priorities")).getY()+((AbstractWidget)field(editor,"priorities")).getHeight());
        });
        steps.add(()->{check("Scaled workspace tooltip fixture",(float)field(editor,"scale")<1);hover(prefix("Finish song on exit:"));});
        // The first rendered tooltip can be delayed by world/renderer startup work.
        steps.add(()->{});
        steps.add(()->checkHover("tooltip-right-edge"));
        steps.add(()->hover(button("Automatic")));
        steps.add(()->checkHover("tooltip-bottom-edge"));
        steps.add(()->{click("Timing");hover(widgets().stream().filter(EditBox.class::isInstance).findFirst().orElseThrow());});
        steps.add(()->{checkHover("tooltip-timing-field");click("Display");hover(button("Preview widget"));});
        steps.add(()->{check("Disabled preview fixture",!button("Preview widget").active);checkHover("tooltip-disabled-control");click("Playlists");});
        steps.add(()->{row("folders",1,true);check("Double click assigns a library song",target.tracks.containsKey("file:workspace_a"));});
        steps.add(()->{row("folders",2,false);click("Add selected / folder ->");check("Selected song assigned on same screen",target.tracks.size()==2);});
        steps.add(()->{row("songs",0,false);click("Disable");check("Bulk disable",Boolean.FALSE.equals(target.tracks.get("file:workspace_a")));click("Enable");check("Bulk enable",Boolean.TRUE.equals(target.tracks.get("file:workspace_a")));});
        steps.add(()->{click(prefix("Finish song on entry:"));check("Entry independent",target.finishCurrentSong&&!target.finishCurrentSongOnExit);click(prefix("Finish song on exit:"));check("Exit independent",target.finishCurrentSong&&target.finishCurrentSongOnExit);});
        steps.add(()->{click("Top");check("Priority top",lib.playlists.getFirst()==target);click("Down");check("Priority down",lib.playlists.get(1)==target);click("Last");check("Priority last",lib.playlists.getLast()==target);});
        steps.add(()->{click("Until released");check("Held override with Automatic off",target.id.equals(rt.engine().overrideId())&&rt.engine().overrideMode()==OverrideMode.UNTIL_RELEASED);});
        steps.add(()->check("Generated fixture actually starts",rt.engine().current()!=null&&target.id.equals(rt.engine().sourcePlaylistId())&&rt.engine().error().isBlank()));
        steps.add(()->{click("Pause");check("Pause control",rt.engine().paused());click("Next");check("Next keeps held override",target.id.equals(rt.engine().overrideId()));});
        steps.add(()->{click("Resume");check("Resume control",!rt.engine().paused());click("Automatic");check("Release override",rt.engine().overrideId()==null);});
        steps.add(()->{shot("main");click("Play one song");check("One-song override",rt.engine().overrideMode()==OverrideMode.ONE_SONG);click("Automatic");});
        steps.add(()->{click("Timing");timing=mc.gui.screen();check("Timing direct navigation",timing.getClass().getSimpleName().equals("SettingsScreen"));var fields=widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();fields.getFirst().setValue("invalid");click("Display");check("Invalid timing blocks navigation",mc.gui.screen()==timing);});
        steps.add(()->{check("Hide settings",MusicMenu.hide(timing));mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Draft page retained",mc.gui.screen()==timing);var fields=widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).toList();check("Invalid draft retained",fields.getFirst().getValue().equals("invalid"));fields.getFirst().setValue("12");fields.get(1).setValue("34");click("Display");check("Timing saved on navigation",lib.settings.silenceMinSeconds==12&&lib.settings.silenceMaxSeconds==34);});
        steps.add(()->{shot("display");check("Display direct navigation",mc.gui.screen().getClass().getSimpleName().equals("NowPlayingSettingsScreen"));click(prefix("Style:"));check("Display style changes",lib.settings.nowPlayingStyle==NowPlayingStyle.HYBRID);check("Widget numeric controls remain",widgets().stream().filter(EditBox.class::isInstance).count()==5);check("All metadata switches remain",List.of("Artwork / note:","Song title:","Artist:","Album:","Song length:","Source playlist:","Scroll long text:").stream().allMatch(t->widgets().stream().anyMatch(w->w.getMessage().getString().startsWith(t))));});
        steps.add(()->{click("Keys");check("Keys direct navigation",mc.gui.screen().getClass().getSimpleName().equals("OverrideBindingsScreen"));click("Minecraft key binds");check("Physical key bindings reachable",mc.gui.screen().getClass().getSimpleName().equals("KeyBindsScreen"));mc.gui.screen().onClose();});
        steps.add(()->{Set<Integer> slots=new HashSet<>();for(int page=0;page<8;page++){for(var w:widgets()){String label=w.getMessage().getString();if(label.length()>3&&label.charAt(0)>='1'&&label.charAt(0)<='8'&&label.charAt(2)=='[')slots.add(Integer.parseInt(label.substring(0,1)));}var next=button(">");if(!next.active)break;click(next);}check("All eight override slots reachable",slots.size()==8);click("Playlists");});
        steps.add(()->{click("Add music");check("Unified import dialog",mc.gui.screen().getTitle().getString().equals("Add music"));click("Library only");check("Files/folders can import without assignment",button("Files").active&&button("Folder").active&&!button("Foobar playlist").active);click("Back");});
        steps.add(()->{click("Name & conditions");check("Custom condition editor reachable",mc.gui.screen().getClass().getSimpleName().equals("PlaylistConditionsScreen"));check("Match all/any reachable",prefix("Conditions: match ").active);click("Save & back");});
        steps.add(()->{row("songs",0,false);click("Remove");check("Remove keeps library file",target.tracks.size()==1&&lib.track("file:workspace_a")!=null);row("folders",0,false);click("Add selected / folder ->");check("Whole-folder assignment",target.tracks.size()==2);click("Normalize");check("Normalization progress reachable",mc.gui.screen().getClass().getSimpleName().equals("LoudnessScreen"));check("Normalization report/cancel retained",button("Back").active);click("Back");});
        steps.add(()->{click("Inspector");check("Inspector direct navigation",mc.gui.screen().getClass().getSimpleName().equals("PlaybackInspectorScreen"));check("Retry failed retained",button("Retry failed").active);click("Details");check("Inspector details retained",mc.gui.screen().getClass().getSimpleName().equals("InspectorDetailsScreen"));mc.gui.screen().onClose();click("Playlists");});
        steps.add(()->{int before=lib.playlists.size();click("New");widgets().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow().setValue("Workspace created");click("Create");check("Create in workspace",lib.playlists.size()==before+1&&mc.gui.screen()==editor);click("Delete");click("Yes");check("Delete custom playlist",lib.playlists.size()==before);});
        steps.add(()->{check("Library search preserved",((EditBox)field(editor,"librarySearch")).getValue().equals("Workspace tone"));MusicMenu.hide(editor);mc.gui.setScreen(MusicMenu.reopen(null,rt));check("Workspace instance retained",mc.gui.screen()==editor);check("Two library and song viewports remain",field(editor,"folders")!=null&&field(editor,"songs")!=null);shot("retained");});
        templateChecks();
        menuAppearanceChecks();
        gameModeChecks();
        playOnTriggerChecks();
        jukeboxChecks();
        columnChecks();
    }
}
