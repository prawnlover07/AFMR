package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaybackInspector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;

public final class PlaybackInspectorScreen extends MusicScreen {
    private DenseList<PlaybackInspector.Entry> list;
    private List<PlaybackInspector.Entry> entries=List.of();
    private String chosen="",reason="",playing="",source="",winner="";
    private int ticks;
    private net.minecraft.client.gui.components.Button pause;
    private List<net.minecraft.util.FormattedCharSequence> reasonTooltip=List.of();
    public PlaybackInspectorScreen(Screen parent,ClientRuntime runtime){super("Playback inspector",parent,runtime);}
    @Override protected void init(){
        super.init();list=addRenderableWidget(new DenseList<>(left,104,contentWidth,Math.max(28,height-163),
            (g,e,x,y,w,hover)->{if(e.id().equals(chosen)||hover)g.fill(x,y,x+w,y+14,0xFF35594E);
                g.text(font,fit("#"+e.rank()+" "+e.name()+" | "+e.status(),w-8),x+4,y+3,0xFFD9E8E0);},
            (e,event,twice)->{chosen=e.id();if(twice)details();},PlaybackInspector.Entry::name));
        int quarter=(contentWidth-12)/4;
        button("Details",left,height-53,quarter,this::details);
        button("Retry failed",left+quarter+4,height-53,quarter,()->{runtime.engine().retryFailed();refresh();});
        button("Next song",left+2*(quarter+4),height-53,quarter,()->{runtime.nextSong();refresh();});
        pause=button("Pause music",left+3*(quarter+4),height-53,quarter,()->{runtime.togglePause();refresh();});
        button("Back",left+contentWidth-104,height-29,104,this::onClose);refresh();
    }
    private void refresh(){
        var lib=runtime.library();var e=runtime.engine();entries=runtime.inWorld()?PlaybackInspector.inspect(lib,runtime.context(),runtime.applicableVanilla(),e):List.of();list.items(entries);
        if(lib.playlist(chosen)==null&&!entries.isEmpty()){
            chosen=lib.playlist(e.sourcePlaylistId())!=null?e.sourcePlaylistId():e.selected()!=null?e.selected().id:entries.getFirst().id();
            for(var row:entries)if(row.id().equals(chosen)){list.reveal(row);break;}
        }
        playing=e.current()==null?"Nothing":e.current().title;var p=lib.playlist(e.sourcePlaylistId());source=p==null?(e.previewing()?"Manual preview":"None"):p.name;
        winner=(e.automaticSelected()==null?"None":e.automaticSelected().name)+(e.overrideId()==null?"":" (overridden)");reason=runtime.inWorld()?PlaybackInspector.waiting(e,lib,runtime.context(),runtime.now()):"Join a world to inspect live playback";
        reasonTooltip=font.split(net.minecraft.network.chat.Component.literal(reason),Math.min(400,contentWidth));
        pause.setMessage(net.minecraft.network.chat.Component.literal(e.paused()?"Resume music":"Pause music"));
    }
    private void details(){for(var e:entries)if(e.id().equals(chosen)){minecraft.gui.setScreen(new InspectorDetailsScreen(this,runtime,e));break;}}
    @Override public void tick(){super.tick();if(++ticks%20==0)refresh();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        String[] lines={"Playing: "+playing,"Song source: "+source,"Automatic choice: "+winner,reason};
        for(int i=0;i<lines.length;i++)g.text(font,fit(lines[i],contentWidth),left,47+i*13,i==3?0xFF8DE0BE:0xFFD9E8E0);
        if(x>=left&&x<left+contentWidth&&y>=86&&y<99)g.setTooltipForNextFrame(font,reasonTooltip,x,y);
    }
}
