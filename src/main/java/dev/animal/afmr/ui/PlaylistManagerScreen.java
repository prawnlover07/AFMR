package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.Map;

final class PlaylistManagerScreen extends MusicScreen {
    private final LibraryScreen editor;
    private String selected;
    private DenseList<Playlist> list;
    private final Map<String,Integer> ranks=new HashMap<>();
    private String selectedHint="";
    PlaylistManagerScreen(LibraryScreen parent,ClientRuntime runtime,String selected){super("Playlists & priority",parent,runtime);editor=parent;this.selected=selected;}
    void select(String id){selected=id;}
    @Override protected void init(){
        super.init();var playlists=runtime.library().playlists;
        ranks.clear();for(int i=0;i<playlists.size();i++)ranks.put(playlists.get(i).id,i+1);
        if(runtime.library().playlist(selected)==null)selected=playlists.getFirst().id;
        list=addRenderableWidget(new DenseList<>(left,65,contentWidth,Math.max(28,height-148),this::drawRow,(p,e,twice)->{selected=p.id;rebuildWidgets();if(twice)edit();},p->p.name));
        list.items(playlists);var p=runtime.library().playlist(selected);list.reveal(p);
        selectedHint=p.custom()?p.conditions.summary():"Default playlist: settings and order are editable; deletion is protected.";
        int quarter=(contentWidth-12)/4,rank=playlists.indexOf(p);
        button("Move up",left,height-77,quarter,()->move(rank-1)).active=rank>0;
        button("Move down",left+quarter+4,height-77,quarter,()->move(rank+1)).active=rank<playlists.size()-1;
        button("Move to top",left+2*(quarter+4),height-77,quarter,()->move(0)).active=rank>0;
        button("Move to bottom",left+3*(quarter+4),height-77,quarter,()->move(playlists.size()-1)).active=rank<playlists.size()-1;
        button("New playlist",left,height-53,quarter,()->minecraft.gui.setScreen(new NewPlaylistScreen(this,runtime)));
        button("Settings",left+quarter+4,height-53,quarter,this::edit);
        button("Edit songs",left+2*(quarter+4),height-53,quarter,()->{editor.selectPlaylist(selected);minecraft.gui.setScreen(editor);});
        button("Delete",left+3*(quarter+4),height-53,quarter,this::delete).active=p.custom()&&!runtime.scanning();
        button("Back",left+contentWidth-104,height-29,104,this::onClose);
    }
    private void move(int index){runtime.library().movePlaylist(selected,index);runtime.save();rebuildWidgets();}
    private void edit(){minecraft.gui.setScreen(new PlaylistRulesScreen(this,editor,runtime,runtime.library().playlist(selected)));}
    private void delete(){
        var p=runtime.library().playlist(selected);
        minecraft.gui.setScreen(new ConfirmScreen(yes->{if(yes){runtime.library().removePlaylist(selected);runtime.save();}minecraft.gui.setScreen(this);},
            Component.literal("Delete "+p.name+"?"),Component.literal("Removes this playlist and its assignments. Music files and other playlists are kept.")));
    }
    private void drawRow(GuiGraphicsExtractor g,Playlist p,int x,int y,int width,boolean hover){
        boolean chosen=p.id.equals(selected);if(chosen||hover)g.fill(x,y,x+width,y+14,chosen?0xFF35594E:0xFF2C373A);
        int rank=ranks.get(p.id);
        g.text(font,"#"+rank,x+3,y+3,0xFF8DE0BE);
        g.text(font,fit(p.name,width-150),x+38,y+3,p.enabled?0xFFDCE6E0:0xFF788880);
        g.text(font,(p.custom()?"custom":"default")+" / "+(p.enabled?"auto on":"auto off"),x+width-109,y+3,0xFF8FA7A0);
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("#1 is highest. The first matching playlist with enabled songs plays.",contentWidth),left,48,0xFF8FA7A0);
        g.text(font,fit(selectedHint,contentWidth-112),left,height-23,0xFF8DE0BE);
    }
}
