package dev.animal.afmr.ui;

import dev.animal.afmr.core.MenuColor;

import dev.animal.afmr.ClientRuntime;
import net.minecraft.client.gui.components.Tooltip;
import dev.animal.afmr.core.PlaylistConditions;
import dev.animal.afmr.core.PlaylistConditions.State;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class CoverConditionScreen extends MusicScreen {
    private final PlaylistConditions conditions;
    private State state;
    private FlatButton choice;
    private EditBox depth;
    private String error="";
    CoverConditionScreen(Screen parent,ClientRuntime runtime,PlaylistConditions conditions){super("Cover condition",parent,runtime);this.conditions=conditions;}
    private String label(){return "Cover: "+switch(state){case ANY->"any";case YES->"covered";case NO->"open sky";};}
    @Override protected void init(){
        super.init();state=conditions.cover;
        choice=button(label(),left,47,contentWidth,()->{state=State.values()[(state.ordinal()+1)%3];choice.setMessage(Component.literal(label()));});
        depth=edit(""+conditions.minCoverDepth,"Minimum cover depth",left+contentWidth-100,91,100,null);
        var minimumCoverDepthHelpText=Component.translatable("afmr.copy.covercondition.minimum_cover_depth");
        if(!minimumCoverDepthHelpText.getString().isBlank())depth.setTooltip(Tooltip.create(minimumCoverDepthHelpText));
        button("Save & back",left+contentWidth-104,bodyBottom()-29,104,this::onClose);
    }
    @Override public void onClose(){
        try{if(state==State.YES){double value=Double.parseDouble(depth.getValue());if(!Double.isFinite(value)||value<0||value>4096)throw new NumberFormatException();conditions.minCoverDepth=value;}
            conditions.cover=state;runtime.save();super.onClose();}
        catch(NumberFormatException e){error="Minimum solid cover depth: 0-4096 blocks.";}
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float delta){
        g.drawString(menuFont,fit("Minimum depth when covered",contentWidth-108),left,97,MenuAppearance.color(MenuColor.TEXT,0xFFE3EAED));
        var c=runtime.context();String current=runtime.inWorld()?"Current: "+(c.covered()?"covered":"open")+" / depth "+(int)Math.max(0,c.coverDepth()):"Not in world";
        g.drawString(menuFont,fit(current,contentWidth),left,168,MenuAppearance.color(MenuColor.ACCENT,0xFF90C6FF));
        g.drawString(menuFont,fit(error,contentWidth),left,bodyBottom()-48,0xFFFFA0A0);
    }
}
