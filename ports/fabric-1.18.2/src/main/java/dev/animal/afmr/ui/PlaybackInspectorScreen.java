package dev.animal.afmr.ui;

import net.minecraft.network.chat.Component;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaybackInspector;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;

public final class PlaybackInspectorScreen extends MusicScreen {
    private DenseList<PlaybackInspector.Entry> list;
    private List<PlaybackInspector.Entry> entries=List.of();
    private String chosen="",reason="",playing="",source="",winner="";
    private int ticks;
    private FlatButton addPlaying,pause,copy;
    private int copyFeedbackTicks;
    private List<net.minecraft.util.FormattedCharSequence> reasonTooltip=List.of();
    public PlaybackInspectorScreen(Screen parent,ClientRuntime runtime){super("Playback inspector",parent,runtime);}
    @Override protected void init(){
        if(MusicMenu.retainWidgets(this,menuFont==runtime.fonts().font(CustomFonts.Target.MENU))){font=menuFont;return;}
        super.init();list=addRenderableWidget(new DenseList<>(left,104,contentWidth,Math.max(28,bodyBottom()-187),
            (g,e,x,y,w,hover)->{if(e.id().equals(chosen)||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                menuFont.drawShadow(g,fit("#"+e.rank()+" "+e.name()+" | "+e.status(),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));},
            (e,clickX,clickY,button,twice)->{chosen=e.id();if(twice)details();},PlaybackInspector.Entry::name));
        addPlaying=button("Add playing to playlist",left,bodyBottom()-77,contentWidth,()->{
            var track=runtime.engine().current();if(track!=null)minecraft.setScreen(new TrackPlaylistScreen(this,runtime,track.id,track.title));
        });addPlaying.active=runtime.engine().current()!=null;
        int quarter=(contentWidth-12)/4;
        button("Details",left,bodyBottom()-53,quarter,this::details);
        button("Retry failed",left+quarter+4,bodyBottom()-53,quarter,()->{runtime.engine().retryFailed();refresh();});
        button("Next song",left+2*(quarter+4),bodyBottom()-53,quarter,()->{runtime.nextSong();refresh();});
        pause=button("Pause music",left+3*(quarter+4),bodyBottom()-53,quarter,()->{runtime.togglePause();refresh();});
        copy=button("Copy diagnostic summary",left,bodyBottom()-29,contentWidth-110,this::copySummary);
        var copyDiagnosticSummaryHelpText=new net.minecraft.network.chat.TranslatableComponent(
            "afmr.copy.playbackinspector.copy_diagnostic_summary");
        if(!copyDiagnosticSummaryHelpText.getString().isBlank())copy.description(copyDiagnosticSummaryHelpText);
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);refresh();
    }
    private void refresh(){
        var lib=runtime.library();var e=runtime.engine();
        addPlaying.active=e.current()!=null;entries=runtime.inWorld()?PlaybackInspector.inspect(lib,runtime.context(),runtime.applicableVanilla(),e):List.of();list.items(entries);
        if(lib.playlist(chosen)==null&&!entries.isEmpty()){
            chosen=lib.playlist(e.sourcePlaylistId())!=null?e.sourcePlaylistId():e.selected()!=null?e.selected().id:entries.get(0).id();
            for(var row:entries)if(row.id().equals(chosen)){list.reveal(row);break;}
        }
        playing=e.current()==null?"Nothing":e.current().title;var p=lib.playlist(e.sourcePlaylistId());source=p==null?(e.previewing()?"Manual preview":e.queuedPlaying()?"Manual queue":"None"):p.name;
        winner=(e.automaticSelected()==null?"None":e.automaticSelected().name)+(e.overrideId()==null?"":" (overridden)");reason=runtime.inWorld()?PlaybackInspector.waiting(e,lib,runtime.context(),runtime.applicableVanilla(),runtime.now()):new net.minecraft.network.chat.TranslatableComponent("afmr.copy.playbackinspector.join_a_world_to_inspect_live_playback").getString();
        reasonTooltip=menuFont.split(new net.minecraft.network.chat.TextComponent(reason),Math.min(400,contentWidth));
        pause.setMessage(new net.minecraft.network.chat.TextComponent(e.paused()?"Resume music":"Pause music"));
    }
    private void details(){for(var e:entries)if(e.id().equals(chosen)){minecraft.setScreen(new InspectorDetailsScreen(this,runtime,e));break;}}
    private void copySummary(){
        try{
            minecraft.keyboardHandler.setClipboard(runtime.diagnosticSummary(chosen));
            copy.setMessage(new net.minecraft.network.chat.TextComponent("Summary copied!"));
        }catch(Exception error){
            dev.animal.afmr.Afmr.LOG.warn("AFMR diagnostic clipboard copy failed",error);
            copy.setMessage(new net.minecraft.network.chat.TextComponent("Copy failed; try again"));
        }
        copyFeedbackTicks=60;
    }
    @Override public void tick(){
        super.tick();if(++ticks%20==0)refresh();
        if(copyFeedbackTicks>0&&--copyFeedbackTicks==0)copy.setMessage(new net.minecraft.network.chat.TextComponent("Copy diagnostic summary"));
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        String[] lines={"Playing: "+playing,"Song source: "+source,"Automatic choice: "+winner,reason};
        for(int i=0;i<lines.length;i++)menuFont.drawShadow(g,fit(lines[i],contentWidth),left,47+i*13,i==3?MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF):MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        if(x>=left&&x<left+contentWidth&&y>=86&&y<99)net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,reasonTooltip,x,y);
    }
}
