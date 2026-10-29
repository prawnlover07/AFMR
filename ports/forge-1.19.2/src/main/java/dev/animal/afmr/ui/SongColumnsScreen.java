package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.SongColumn;
import net.minecraft.network.chat.Component;

/** Global song-table visibility, saved immediately and shared by every playlist. */
final class SongColumnsScreen extends MusicScreen {
    private int page;
    SongColumnsScreen(LibraryScreen parent,ClientRuntime runtime){super("Song columns",parent,runtime);}
    @Override protected void init(){
        super.init();int half=(contentWidth-12)/2,index=0,size=2*pageSize(50,61);
        var columns=SongColumn.values();page=Math.max(0,Math.min(page,(columns.length-1)/size));
        for(int i=page*size;i<Math.min(columns.length,(page+1)*size);i++){
            var column=columns[i];
            int row=index/2,col=index++%2;
            var button=toggle(column.label,left+col*(half+12),50+row*22,half,()->runtime.library().settings.songColumns.contains(column),()->{
                var visible=runtime.library().settings.songColumns;
                if(!visible.remove(column))visible.add(column);
                runtime.save();rebuildWidgets();
            });
            button.active=column!=SongColumn.TITLE;
            var rgTrackGainHelpText=switch(column){
                case RG_TRACK_GAIN->Component.translatable("afmr.copy.songcolumns.rg_track_gain");
                case RG_TRACK_PEAK->Component.translatable("afmr.copy.songcolumns.rg_track_peak");
                case RG_ALBUM_GAIN,RG_ALBUM_PEAK->Component.translatable("afmr.copy.songcolumns.rg_album_gain_rg_album_peak");
                case AFMR_GAIN->Component.translatable("afmr.copy.songcolumns.afmr_gain");
                case TITLE->Component.translatable("afmr.copy.songcolumns.title");
                default->Component.translatable("afmr.copy.songcolumns.other_columns",column.label.toLowerCase(java.util.Locale.ROOT));
            };
            if(!rgTrackGainHelpText.getString().isBlank())button.description(rgTrackGainHelpText);
        }
        if(columns.length>size)pages(page,size,columns.length,bodyBottom()-53,value->{page=value;rebuildWidgets();});
        button("Defaults",left,bodyBottom()-29,100,()->{runtime.library().settings.songColumns=SongColumn.defaults();runtime.save();rebuildWidgets();});
        button("Back",left+contentWidth-100,bodyBottom()-29,100,this::onClose);
    }
}
