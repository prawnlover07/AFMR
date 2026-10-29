package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.PlaylistProfiles.Info;
import dev.animal.afmr.library.PlaylistPresets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.util.*;

final class PresetImportScreen extends MusicScreen {
    private final ClientRuntime.PresetImport pending;
    private String destination;
    private String newProfileName;
    private net.minecraft.client.gui.components.EditBox profileName;
    private PlaylistPresets.Placement placement=PlaylistPresets.Placement.ABOVE_SELECTED;
    private int locations,mouseX,mouseY,replacements;
    private FlatButton add;
    private boolean includeSongs;
    private FlatButton folders;
    private FlatButton linked;
    private Playlist tooltipPlaylist;
    private List<FormattedCharSequence> tooltip=List.of();
    private String statusText="";
    private List<FormattedCharSequence> statusTooltip=List.of();
    PresetImportScreen(Screen parent,ClientRuntime runtime,ClientRuntime.PresetImport pending){
        super("Import playlist setup",parent,runtime);this.pending=pending;
        newProfileName=pending.fileName().replaceFirst("(?i)\\.afmrp$", "").replaceAll("\\p{Cc}", "").strip();
        if(newProfileName.isBlank())newProfileName="Imported playlists";if(newProfileName.length()>80)newProfileName=newProfileName.substring(0,80);
    }
    @Override protected void init(){
        super.init();profileName=null;tooltipPlaylist=null;statusText="";locations=PlaylistPresets.locations(pending.data());
        List<Info> profiles=runtime.library().profiles.entries();
        String name=destination==null?"New profile":profiles.stream().filter(p->p.id().equals(destination)).map(Info::name).findFirst().orElse("Deleted profile: choose another");
        var targetButton=button("Destination: "+name,left,48,contentWidth,()->{
            int index=-1;for(int i=0;i<profiles.size();i++)if(profiles.get(i).id().equals(destination)){index=i;break;}
            if(profileName!=null)newProfileName=profileName.getValue();
            destination=destination==null?profiles.get(0).id():index+1>=profiles.size()?null:profiles.get(index+1).id();rebuildWidgets();
        });
        var help=Component.translatable("afmr.copy.presetimport.destination");
        if(!help.getString().isBlank())targetButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(help));
        replacements=0;
        if(destination!=null)for(var imported:pending.data().playlists)if(runtime.library().profiles.playlists(runtime.library(),destination).stream().anyMatch(p->PlaylistPresets.matchName(p.name).equals(PlaylistPresets.matchName(imported.name))))replacements++;
        String position=switch(placement){case ABOVE_SELECTED->"Above selected playlist";case TOP->"Top priority";case BOTTOM->"Bottom priority";};
        if(destination==null){profileName=edit(newProfileName,"New profile name",left,74,contentWidth,value->newProfileName=value);profileName.setMaxLength(80);}
        else button("Insert new: "+position,left,74,contentWidth,()->{placement=PlaylistPresets.Placement.values()[(placement.ordinal()+1)%PlaylistPresets.Placement.values().length];rebuildWidgets();});
        int half=(contentWidth-6)/2,third=(contentWidth-8)/3;
        toggle("Import songs",left,100,third,()->includeSongs,()->{
            includeSongs=!includeSongs;if(includeSongs&&runtime.presetSongPlan()==null)runtime.preparePresetSongs(pending);rebuildWidgets();
        }).active=!pending.data().songs.isEmpty();
        folders=button("Find song files...",left+third+4,100,third,()->minecraft.gui.setScreen(new PresetSongFoldersScreen(this,runtime,pending)));
        linked=button("Search linked libraries",left+2*(third+4),100,third,()->{
            if(runtime.presetSearchActive())runtime.cancelPresetSearch();
            else{includeSongs=true;runtime.searchPresetLibraries(pending);rebuildWidgets();}
        });
        var rows=addRenderableWidget(new DenseList<Playlist>(left,127,contentWidth,Math.max(28,bodyBottom()-217),this::drawPlaylist,(p,event,twice)->{},p->p.name));
        rows.items(pending.data().playlists);
        button("Cancel",left,bodyBottom()-29,half,this::onClose);
        add=button(destination==null?"Create profile and import":"Replace "+replacements+" / add "+(pending.data().playlists.size()-replacements),left+half+6,bodyBottom()-29,half,()->{
            var plan=runtime.presetSongPlan();
            var result=runtime.applyPresetImport(pending,destination,newProfileName,placement,includeSongs);
            if(result!=null)((PlaylistSharingScreen)parent).imported(result.ids(),result.profileId(),includeSongs&&plan!=null&&plan.missing()>0);
        });controls();
    }
    private boolean profileLimit(){return destination==null&&runtime.library().profiles.inactive.size()+1>=dev.animal.afmr.core.PlaylistProfiles.LIMIT;}
    private void controls(){
        add.active=runtime.pendingPreset()==pending&&!runtime.scanning()&&(destination==null?runtime.library().profiles.inactive.size()+1<dev.animal.afmr.core.PlaylistProfiles.LIMIT&&!newProfileName.isBlank():runtime.library().profiles.exists(destination))&&(locations==0||runtime.inWorld())&&(!includeSongs||runtime.presetSongPlan()!=null);
        folders.active=includeSongs&&!runtime.scanning()&&!pending.data().folders.isEmpty();
        linked.setMessage(Component.literal(runtime.presetSearchCancelling()?"Cancelling...":runtime.presetSearchActive()?"Cancel search":"Search linked libraries"));
        linked.active=runtime.presetSearchActive()?!runtime.presetSearchCancelling():!runtime.scanning()&&!pending.data().folders.isEmpty();
    }
    private void drawPlaylist(GuiGraphicsExtractor g,Playlist p,int x,int y,int width,boolean hover){
        g.text(menuFont,fit(p.name,width-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFEDF4F6),false);
        if(hover){
            if(tooltipPlaylist!=p){
                tooltipPlaylist=p;
                String description=p.conditions.summary()+"\nAutomatic: "+p.enabled+"; manual only: "+p.manualOnly
                    +"\nFinish on entry: "+p.finishCurrentSong+"; exit: "+p.finishCurrentSongOnExit
                    +"\nPlay on trigger: "+p.playOnTrigger+"; Minecraft music: "+p.includeVanilla+"; volume: "+Math.round(p.volume*100)+"%"
                    +"\nOptional song assignments: "+p.tracks.size();
                tooltip=menuFont.split(Component.literal(description),Math.max(100,Math.min(400,contentWidth)));
            }
            g.setTooltipForNextFrame(menuFont,tooltip,mouseX,mouseY);
        }
    }
    @Override public void tick(){controls();}
    @Override public void onClose(){runtime.cancelPresetImport(pending);super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        mouseX=x;mouseY=y;
        var plan=runtime.presetSongPlan();
        String songs=!includeSongs?"Settings only; "+pending.data().songs.size()+" optional song references in this file.":plan==null?"Finding songs...":plan.found()+" found / "+plan.missing()+" missing files / "+plan.unavailableResources()+" unavailable resource songs";
        g.text(menuFont,fit(songs,contentWidth),left,bodyBottom()-81,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        String location=locations==0?"No configured locations.":runtime.inWorld()?locations+" locations bind to the current world/server; dimensions stay as saved.":"Join a world/server to bind "+locations+" saved locations before importing.";
        if(runtime.scanning())ScanProgressView.draw(g,menuFont,runtime.scanProgress(),left,bodyBottom()-66,contentWidth);
        else{
            g.text(menuFont,fit(location,contentWidth),left,bodyBottom()-65,locations>0&&!runtime.inWorld()?0xFFFFB0A7:MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
            g.text(menuFont,fit(profileLimit()?"Profile limit reached: choose an existing profile or delete one first.":runtime.taskStatus(),contentWidth),left,bodyBottom()-48,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFABB5C2));
        }
        if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-50&&y<bodyBottom()-36){
            if(!statusText.equals(runtime.taskStatus())){statusText=runtime.taskStatus();statusTooltip=menuFont.split(Component.literal(statusText),Math.max(100,Math.min(400,contentWidth)));}
            g.setTooltipForNextFrame(menuFont,statusTooltip,x,y);
        }
    }
}
