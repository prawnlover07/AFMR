package dev.animal.afmr.ui;


import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.NowPlayingInfo;
import dev.animal.afmr.core.NowPlayingLayout;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.resources.ResourceLocation;

/** Shared drawing for the two customizable styles and their settings preview. */
final class NowPlayingWidget {
    static final ResourceLocation COVER=new ResourceLocation("afmr","now_playing_cover");
    private NowPlayingInfo info;
    private String[] lines=new String[0];
    private String[] fitted=new String[0];
    private int[] lineWidths=new int[0];
    private int textWidth=-1;
    private boolean wrapped;
    private NowPlayingLayout layout;
    private int availableWidth,availableHeight;

    void content(NowPlayingInfo next){
        info=next;textWidth=-1;layout=null;
    }
    NowPlayingLayout layout(Settings settings,int width,int height){
        if(layout==null||availableWidth!=width||availableHeight!=height){
            availableWidth=width;availableHeight=height;
            var base=NowPlayingLayout.fit(settings.widgetWidth,settings.widgetScale,0,
                settings.widgetArtwork,width,height);
            int space=base.width()-base.textX()-7;
            if(textWidth!=space||wrapped!=settings.widgetWrap){
                textWidth=space;wrapped=settings.widgetWrap;
                var font=Minecraft.getInstance().font;
                lines=info==null?new String[0]:wrapped?info.wrap(space,font::width):info.lines;
                fitted=new String[lines.length];lineWidths=new int[lines.length];
                for(int i=0;i<lines.length;i++){
                    String text=lines[i];lineWidths[i]=font.width(text);
                    fitted[i]=lineWidths[i]<=space?text:font.plainSubstrByWidth(text,Math.max(0,space-font.width("…")))+"…";
                }
            }
            layout=NowPlayingLayout.fit(settings.widgetWidth,settings.widgetScale,lines.length,
                settings.widgetArtwork,width,height);
        }
        return layout;
    }

    void draw(GuiGraphics g,Settings settings,NowPlayingLayout layout,float x,float y,float alpha,boolean hasCover,double age){
        if(info==null||(lines.length==0&&!settings.widgetArtwork))return;
        var font=Minecraft.getInstance().font;
        int width=layout.width(),height=layout.height(),space=width-layout.textX()-7;
        g.pose().pushPose();g.pose().translate(x,y,0);g.pose().scale((float)layout.scale(),(float)layout.scale(),1);
        g.fill(0,0,width,height,color(settings.widgetBackgroundColor,alpha*(float)settings.widgetBackgroundOpacity));
        if(settings.widgetArtwork){
            int size=layout.iconSize(),iconY=(height-size)/2;
            if(hasCover){
                RenderSystem.enableBlend();g.setColor(1,1,1,alpha);
                g.blit(COVER,7,iconY,size,size,0,0,256,256,256,256);
                g.setColor(1,1,1,1);RenderSystem.disableBlend();
            }
            else{
                g.drawString(font,"♪",7+(size-6)/2,iconY+(size-8)/2,color(0x8DE0BE,alpha),false);
            }
        }
        int textY=(height-(fitted.length*14-5))/2;
        g.enableScissor((int)Math.floor(x+layout.textX()*layout.scale()),(int)Math.floor(y),
            (int)Math.ceil(x+(width-7)*layout.scale()),(int)Math.ceil(y+height*layout.scale()));
        for(int i=0;i<fitted.length;i++){
            int rgb=0xF0F7F3;
            int offset=settings.widgetScroll?(int)NowPlayingLayout.scrollOffset(lineWidths[i],space,age):0;
            g.drawString(font,settings.widgetScroll?lines[i]:fitted[i],layout.textX()-offset,textY+i*14,color(rgb,alpha),false);
        }
        g.disableScissor();
        g.pose().popPose();
    }

    private static int color(int rgb,float alpha){return ((int)(255*alpha)<<24)|(rgb&0xFFFFFF);}
}
