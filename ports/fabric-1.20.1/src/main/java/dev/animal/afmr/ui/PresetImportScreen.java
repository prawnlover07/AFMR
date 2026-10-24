package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistProfiles.Info;
import dev.animal.afmr.library.PlaylistPresets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class PresetImportScreen extends MusicScreen {
    private final ClientRuntime.PresetImport pending;
    private String destination;
    private PlaylistPresets.Placement placement=PlaylistPresets.Placement.ABOVE_SELECTED;
    private int locations,mouseX,mouseY;
    private FlatButton add;
    private boolean includeSongs;
    private FlatButton folders;
    private Playlist tooltipPlaylist;
    private List<FormattedCharSequence> tooltip=List.of();
    private String statusText="";
    private List<FormattedCharSequence> statusTooltip=List.of();
    PresetImportScreen(Screen parent,ClientRuntime runtime,ClientRuntime.PresetImport pending){
        super("Import playlist setup",parent,runtime);this.pending=pending;destination=pending.profileId();
    }
    @Override protected void init(){
        super.init();tooltipPlaylist=null;statusText="";locations=PlaylistPresets.locations(pending.data());
        List<Info> profiles=runtime.library().profiles.entries();
        String name=profiles.stream().filter(p->p.id().equals(destination)).map(Info::name).findFirst().orElse("Deleted profile: choose another");
        button("Destination: "+name,left,48,contentWidth,()->{
            int index=-1;for(int i=0;i<profiles.size();i++)if(profiles.get(i).id().equals(destination)){index=i;break;}
            destination=profiles.get((index+1)%profiles.size()).id();rebuildWidgets();
        });
        String position=switch(placement){case ABOVE_SELECTED->"Above selected playlist";case TOP->"Top priority";case BOTTOM->"Bottom priority";};
        button("Insert: "+position,left,74,contentWidth,()->{placement=PlaylistPresets.Placement.values()[(placement.ordinal()+1)%PlaylistPresets.Placement.values().length];rebuildWidgets();});
        int half=(contentWidth-6)/2;
        button("Import songs: "+(includeSongs?"On":"Off"),left,100,half,()->{
            includeSongs=!includeSongs;if(includeSongs&&runtime.presetSongPlan()==null)runtime.preparePresetSongs(pending);rebuildWidgets();
        }).active=!pending.data().songs.isEmpty();
        folders=button("Locate music folders...",left+half+6,100,half,()->minecraft.setScreen(new PresetSongFoldersScreen(this,runtime,pending)));
        var rows=addRenderableWidget(new DenseList<Playlist>(left,127,contentWidth,Math.max(28,height-217),this::drawPlaylist,(p,clickX,clickY,button,twice)->{},p->p.name));
        rows.items(pending.data().playlists);
        button("Cancel",left,height-29,half,this::onClose);
        add=button("Add "+pending.data().playlists.size()+" playlists",left+half+6,height-29,half,()->{
            var plan=runtime.presetSongPlan();
            var ids=runtime.applyPresetImport(pending,destination,placement,includeSongs);
            if(!ids.isEmpty())((PlaylistSharingScreen)parent).imported(ids,destination,includeSongs&&plan!=null&&plan.missing()>0);
        });controls();
    }
    private void controls(){
        add.active=runtime.pendingPreset()==pending&&!runtime.scanning()&&runtime.library().profiles.exists(destination)&&(locations==0||runtime.inWorld())&&(!includeSongs||runtime.presetSongPlan()!=null);
        folders.active=includeSongs&&!runtime.scanning()&&!pending.data().folders.isEmpty();
    }
    private void drawPlaylist(GuiGraphics g,Playlist p,int x,int y,int width,boolean hover){
        g.drawString(font,fit(p.name,width-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3F2E9),false);
        if(hover){
            if(tooltipPlaylist!=p){
                tooltipPlaylist=p;
                String description=p.conditions.summary()+"\nAutomatic: "+p.enabled+"; manual only: "+p.manualOnly
                    +"\nFinish on entry: "+p.finishCurrentSong+"; exit: "+p.finishCurrentSongOnExit
                    +"\nPlay on trigger: "+p.playOnTrigger+"; Minecraft music: "+p.includeVanilla+"; volume: "+Math.round(p.volume*100)+"%"
                    +"\nOptional song assignments: "+p.tracks.size();
                tooltip=font.split(Component.literal(description),Math.max(100,Math.min(400,contentWidth)));
            }
            g.renderTooltip(font,tooltip,mouseX,mouseY);
        }
    }
    @Override public void tick(){controls();}
    @Override public void onClose(){runtime.cancelPresetImport(pending);super.onClose();}
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        mouseX=x;mouseY=y;
        var plan=runtime.presetSongPlan();
        String songs=!includeSongs?"Settings only; "+pending.data().songs.size()+" optional song references in this file.":plan==null?"Finding songs...":plan.found()+" found / "+plan.missing()+" missing files / "+plan.unavailableResources()+" unavailable resource songs";
        g.drawString(font,fit(songs,contentWidth),left,height-81,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        String location=locations==0?"No configured locations.":runtime.inWorld()?locations+" locations bind to the current world/server; dimensions stay as saved.":"Join a world/server to bind "+locations+" saved locations before importing.";
        if(runtime.scanning())ScanProgressView.draw(g,font,runtime.scanProgress(),left,height-66,contentWidth);
        else{
            g.drawString(font,fit(location,contentWidth),left,height-65,locations>0&&!runtime.inWorld()?0xFFFFB0A7:MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
            g.drawString(font,fit(runtime.taskStatus(),contentWidth),left,height-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
        }
        if(x>=left&&x<left+contentWidth&&y>=height-50&&y<height-36){
            if(!statusText.equals(runtime.taskStatus())){statusText=runtime.taskStatus();statusTooltip=font.split(Component.literal(statusText),Math.max(100,Math.min(400,contentWidth)));}
            g.renderTooltip(font,statusTooltip,x,y);
        }
    }
}
