package dev.animal.afmr.ui;

import dev.animal.afmr.core.SongColumn;
import java.util.Set;

/** Bounded horizontal geometry. The assignment switch stays pinned while metadata scrolls. */
final class SongTableLayout {
    static final int PINNED=28, BAR=10;
    final SongColumn[] columns;
    final int[] offsets,widths;
    final int viewport,total;
    private int scroll;
    SongTableLayout(Set<SongColumn> visible,int rowWidth,int previousScroll){
        columns=SongColumn.normalize(visible).toArray(SongColumn[]::new);
        viewport=Math.max(1,rowWidth-PINNED);offsets=new int[columns.length];widths=new int[columns.length];
        int sum=0;for(int i=0;i<columns.length;i++){widths[i]=columns[i].width;sum+=widths[i];}
        if(sum<viewport){widths[0]+=viewport-sum;sum=viewport;}
        total=sum;int x=0;for(int i=0;i<columns.length;i++){offsets[i]=x;x+=widths[i];}
        scroll(previousScroll);
    }
    int scroll(){return scroll;}
    void scroll(int value){scroll=Math.max(0,Math.min(value,maxScroll()));}
    int maxScroll(){return Math.max(0,total-viewport);}
    boolean overflow(){return total>viewport;}
    int x(int column){return PINNED+offsets[column]-scroll;}
    int columnAt(double x){
        if(x<PINNED||x>=PINNED+viewport)return -1;
        double position=x-PINNED+scroll;
        for(int i=0;i<columns.length;i++)if(position<offsets[i]+widths[i])return i;
        return -1;
    }
    int thumbWidth(){return Math.min(viewport,Math.max(24,viewport*viewport/total));}
    int thumbX(){return maxScroll()==0?0:(int)((long)(viewport-thumbWidth())*scroll/maxScroll());}
    void dragThumb(double position){
        int travel=viewport-thumbWidth();scroll(travel<=0?0:(int)Math.round(position*maxScroll()/travel));
    }
}
