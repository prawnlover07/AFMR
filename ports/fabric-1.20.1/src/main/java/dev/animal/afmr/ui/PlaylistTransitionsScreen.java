package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Playlist;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Two priority-ordered columns: choose the outgoing playlist, then its destination. */
final class PlaylistTransitionsScreen extends MusicScreen {
    private String sourceId;
    PlaylistTransitionsScreen(Screen parent,ClientRuntime runtime,String sourceId){super("Playlist transitions",parent,runtime);this.sourceId=sourceId;}
    @Override protected void init(){
        super.init();int half=(contentWidth-8)/2,body=Math.max(16,bodyBottom()-116);
        var from=addRenderableWidget(new DenseList<Playlist>(left,76,half,body,
            (g,p,x,y,w,hover)->{
                if(p.id.equals(sourceId))g.fill(x,y,x+w,y+14,MenuAppearance.color(MenuColor.SELECTION,0xFF304963));
                g.drawString(menuFont,fit(p.name,w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
            },(p,clickX,clickY,button,twice)->{sourceId=p.id;rebuildWidgets();},p->p.name));
        from.items(runtime.library().playlists);
        var source=runtime.library().playlist(sourceId);
        var labels=new java.util.HashMap<String,String>();
        for(var p:runtime.library().playlists){var rule=source==null?null:source.transitions.get(p.id);labels.put(p.id,p.name+" / "+(rule==null?"default":rule.summary()));}
        var to=addRenderableWidget(new DenseList<Playlist>(left+half+8,76,half,body,
            (g,p,x,y,w,hover)->{
                g.drawString(menuFont,fit(labels.get(p.id),w-8),x+4,y+3,MenuAppearance.color(MenuColor.TEXT,0xFFE6E8ED));
            },(p,clickX,clickY,button,twice)->{if(source!=null)minecraft.setScreen(new TransitionRuleScreen(this,runtime,source,p));},p->p.name));
        to.items(runtime.library().playlists.stream().filter(p->!p.id.equals(sourceId)).toList());
        var help=button("How rules work",left,bodyBottom()-29,140,()->{});
        var text=Component.translatable("afmr.copy.transitions.rules");if(!text.getString().isBlank())help.setTooltip(net.minecraft.client.gui.components.Tooltip.create(text));
        button("Back",left+contentWidth-100,bodyBottom()-29,100,this::onClose);
    }
    @Override protected void drawBody(GuiGraphics g,int x,int y,float dt){
        var source=runtime.library().playlist(sourceId);
        g.drawString(menuFont,"From playlist",left,59,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
        g.drawString(menuFont,fit("To / "+(source==null?"choose source":source.name),(contentWidth-8)/2),left+(contentWidth-8)/2+8,59,MenuAppearance.color(MenuColor.ACCENT,0xFFBCBDF8));
    }
}
