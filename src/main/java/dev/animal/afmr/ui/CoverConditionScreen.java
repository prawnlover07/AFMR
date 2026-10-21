package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class CoverConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private State state;
    private Button choice;
    private EditBox depth;
    private String error="";
    CoverConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){super("Cover condition",parent,runtime);this.conditions=conditions;}
    private String label(){return "Cover: "+switch(state){case ANY->"any";case YES->"covered";case NO->"open sky";};}
    @Override protected void init(){
        super.init();state=conditions.cover;
        choice=button(label(),left,47,contentWidth,()->{state=State.values()[(state.ordinal()+1)%3];choice.setMessage(Component.literal(label()));});
        depth=edit(""+conditions.minCoverDepth,"Minimum cover depth",left+contentWidth-100,91,100,null);
        button("Save & back",left+contentWidth-104,height-29,104,this::onClose);
    }
    @Override public void onClose(){
        try{if(state==State.YES){double value=Double.parseDouble(depth.getValue());if(!Double.isFinite(value)||value<1||value>4096)throw new NumberFormatException();conditions.minCoverDepth=value;}
            conditions.cover=state;runtime.save();super.onClose();}
        catch(NumberFormatException e){error="Minimum solid cover depth: 1-4096 blocks.";}
    }
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,fit("Minimum depth when covered",contentWidth-108),left,97,0xFFD9E8E0);
        g.text(font,fit("Uses solid overhead terrain. Water and leaves alone are excluded.",contentWidth),left,128,0xFF8FA7A0);
        g.text(font,fit("The global Underground Y cutoff does not affect this filter.",contentWidth),left,146,0xFF8FA7A0);
        var c=runtime.context();String current=runtime.inWorld()?"Current: "+(c.covered()?"covered":"open")+" / depth "+(int)Math.max(0,c.coverDepth()):"Join a world to see the current value.";
        g.text(font,fit(current,contentWidth),left,168,0xFF8DE0BE);
        g.text(font,fit(error,contentWidth),left,height-48,0xFFFFA0A0);
    }
}
