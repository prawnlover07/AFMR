package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.ListeningHistory;
import dev.animal.afmr.core.PlaybackQueue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

final class ListeningScreen extends MusicScreen {
    private record Row(long id,String label,String trackId,PlaybackQueue.Entry queued,ListeningHistory.Entry played,List<FormattedCharSequence> tooltip) {}
    private static final DateTimeFormatter DATE=DateTimeFormatter.ofPattern("MMM d HH:mm:ss").withZone(ZoneId.systemDefault());
    private DenseList<Row> list;
    private List<Row> rows=List.of();
    private boolean showHistory;
    private long chosen=-1,revision=-1;
    private int ticks;
    private FlatButton first,second,third,fourth,useQueue;

    ListeningScreen(Screen parent,ClientRuntime runtime){super("Queue & listening history",parent,runtime);}
    @Override protected void init(){
        super.init();int half=(contentWidth-6)/2;
        var queue=button("Queue",left,47,half,()->tab(false));queue.selected=!showHistory;
        var history=button("History",left+half+6,47,half,()->tab(true));history.selected=showHistory;
        list=addRenderableWidget(new DenseList<>(left,76,contentWidth,Math.max(28,bodyBottom()-185),(g,row,x,y,w,hover)->{
            if(row.id()==chosen||hover)net.minecraft.client.gui.GuiComponent.fill(g,x,y,x+w,y+14,row.id()==chosen?MenuAppearance.color(MenuColor.SELECTION,0xFF304963):MenuAppearance.color(MenuColor.HOVER,0xFF2C303E));
            menuFont.drawShadow(g,fit(row.label(),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
            if(hover)net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,row.tooltip(),x+8,y+14);
        },(row,clickX,clickY,button,twice)->{chosen=row.id();controls();},Row::label));
        int quarter=(contentWidth-12)/4,thirdWidth=(contentWidth-8)/3;
        if(showHistory){
            first=button("Play now",left,bodyBottom()-101,thirdWidth,this::replay);
            second=button("Queue",left+thirdWidth+4,bodyBottom()-101,thirdWidth,this::enqueueHistory);
            third=button("Add to playlist",left+2*(thirdWidth+4),bodyBottom()-101,thirdWidth,()->{
                var row=selected();if(row!=null)minecraft.setScreen(new TrackPlaylistScreen(this,runtime,row.played().trackId(),row.played().title()));
            });
            fourth=button("Clear history",left,bodyBottom()-77,contentWidth,()->{runtime.clearHistory();chosen=-1;refresh();});
            var historyPlayNowHelpText=Component.translatable("afmr.copy.listening.history_play_now");
            if(!historyPlayNowHelpText.getString().isBlank())first.description(historyPlayNowHelpText);
            var historyQueueHelpText=Component.translatable("afmr.copy.listening.history_queue");
            if(!historyQueueHelpText.getString().isBlank())second.description(historyQueueHelpText);
            var historyAddToPlaylistHelpText=Component.translatable("afmr.copy.listening.history_add_to_playlist");
            if(!historyAddToPlaylistHelpText.getString().isBlank())third.description(historyAddToPlaylistHelpText);
        }else{
            first=button("Up",left,bodyBottom()-101,quarter,()->move(-1));
            second=button("Down",left+quarter+4,bodyBottom()-101,quarter,()->move(1));
            third=button("Remove",left+2*(quarter+4),bodyBottom()-101,quarter,()->{var row=selected();if(row!=null){runtime.engine().queue().remove(row.id());chosen=-1;refresh();}});
            fourth=button("Clear queue",left+3*(quarter+4),bodyBottom()-101,quarter,()->{runtime.engine().queue().clear();chosen=-1;refresh();});
            var clearQueueHelpText=Component.translatable("afmr.copy.listening.clear_queue");
            if(!clearQueueHelpText.getString().isBlank())fourth.description(clearQueueHelpText);
            useQueue=button("Use queue next",left,bodyBottom()-77,half,()->{runtime.engine().useQueueNext();controls();});
            var useQueueNextHelpText=Component.translatable("afmr.copy.listening.use_queue_next");
            if(!useQueueNextHelpText.getString().isBlank())useQueue.description(useQueueNextHelpText);
            button("Next song now",left+half+6,bodyBottom()-77,half,runtime::nextSong);
        }
        button("Back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);refresh();
    }
    private void tab(boolean history){showHistory=history;chosen=-1;useQueue=null;rebuildWidgets();}
    private Row selected(){for(var row:rows)if(row.id()==chosen)return row;return null;}
    private void refresh(){
        var engine=runtime.engine();var result=new ArrayList<Row>();
        if(showHistory){
            for(var entry:engine.history().snapshot()){
                String when=DATE.format(Instant.ofEpochMilli(entry.startedAt()));
                String origin=entry.origin().name().toLowerCase(java.util.Locale.ROOT);
                String label=when+" | "+entry.title()+(entry.artist().isBlank()?"":" — "+entry.artist());
                String detail=label+"\nAlbum: "+entry.album()+"\nSource: "+(entry.playlistName().isBlank()?origin:entry.playlistName())+" / "+origin+"\n"+entry.reason();
                result.add(new Row(entry.id(),label,entry.trackId(),null,entry,menuFont.split(Component.literal(detail),Math.min(480,contentWidth))));
            }
            revision=engine.history().revision();
        }else{
            int position=0;
            for(var entry:engine.queue().snapshot()){
                String label=(++position)+". "+entry.title()+(entry.artist().isBlank()?"":" — "+entry.artist())+" | "+entry.playlistName();
                result.add(new Row(entry.id(),label,entry.trackId(),entry,null,menuFont.split(Component.literal(label),Math.min(480,contentWidth))));
            }
            revision=engine.queue().revision();
        }
        rows=List.copyOf(result);list.items(rows);controls();
    }
    private void controls(){
        var row=selected();boolean available=row!=null&&runtime.library().track(row.trackId())!=null;
        if(showHistory){first.active=second.active=available&&runtime.inWorld();third.active=available;fourth.active=true;}
        else{
            int index=row==null?-1:rows.indexOf(row);first.active=index>0;second.active=index>=0&&index<rows.size()-1;
            third.active=row!=null;fourth.active=!rows.isEmpty();useQueue.active=!rows.isEmpty()&&runtime.engine().queueWaitingForOverride();
        }
    }
    private void move(int direction){
        var row=selected();if(row==null)return;runtime.engine().queue().move(row.id(),rows.indexOf(row)+direction);refresh();
    }
    private void replay(){var row=selected();if(row!=null){var track=runtime.library().track(row.trackId());if(track!=null)runtime.preview(track);refresh();}}
    private void enqueueHistory(){
        var row=selected();if(row==null)return;var entry=row.played();
        runtime.queueTrack(entry.trackId(),runtime.library().playlist(entry.playlistId())==null?"":entry.playlistId());controls();
    }
    @Override public void tick(){
        super.tick();var engine=runtime.engine();
        long current=showHistory?engine.history().revision():engine.queue().revision();
        if(current!=revision)refresh();else if(++ticks%20==0)controls();
    }
    @Override protected void drawBody(PoseStack g,int x,int y,float delta){
        var engine=runtime.engine();String status=showHistory?rows.size()+" / "+ListeningHistory.LIMIT+" saved entries"
            :engine.queueWaitingForOverride()?"Queue waiting for playlist override":engine.queue().size()+" queued / normal silence between songs";
        if(!showHistory&&!engine.queueMessage().isBlank())status+=" | "+engine.queueMessage();
        menuFont.drawShadow(g,fit(status,contentWidth),left,bodyBottom()-49,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        if(x>=left&&x<left+contentWidth&&y>=bodyBottom()-51&&y<bodyBottom()-38)
            net.minecraft.client.Minecraft.getInstance().screen.renderTooltip(g,Component.literal(status),x,y);
    }
}
