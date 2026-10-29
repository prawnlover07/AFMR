package dev.animal.afmr.ui;

/** Priority drag geometry; a click never becomes a move until the threshold is crossed. */
final class PriorityDrag {
    private String id;
    private double startX,startY,x,y;
    private int slot;
    private boolean dragging;
    void begin(String id,double x,double y){this.id=id;startX=this.x=x;startY=this.y=y;dragging=false;}
    boolean armed(){return id!=null;}
    boolean dragging(){return dragging;}
    String id(){return id;}
    double x(){return x;}
    double y(){return y;}
    int slot(){return slot;}
    void update(double x,double y,int top,int offset,int count){
        this.x=x;this.y=y;if(!armed())return;
        double dx=x-startX,dy=y-startY;if(dx*dx+dy*dy>=16)dragging=true;
        slot=Math.max(0,Math.min(count,offset+(int)Math.floor((y-top)/DenseList.ROW+.5)));
    }
    int target(int source){return slot>source?slot-1:slot;}
    void cancel(){id=null;dragging=false;}
}
