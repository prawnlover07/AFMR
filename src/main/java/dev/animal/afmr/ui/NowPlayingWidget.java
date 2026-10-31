package dev.animal.afmr.ui;

import dev.animal.afmr.core.Library.NowPlayingStyle;
import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.Library.Settings;
import dev.animal.afmr.core.NowPlayingInfo;
import dev.animal.afmr.core.NowPlayingLayout;
import dev.animal.afmr.core.Contrast;
import net.minecraft.client.color.ColorLerper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared drawing for the two customizable styles and their settings preview. */
final class NowPlayingWidget {
    static final Identifier COVER=Identifier.fromNamespaceAndPath("afmr","now_playing_cover");
    private static final Identifier TOAST=Identifier.withDefaultNamespace("toast/now_playing");
    private static final Identifier NOTES=Identifier.withDefaultNamespace("icon/music_notes");
    private NowPlayingInfo info;
    private String[] lines=new String[0];
    private String[] fitted=new String[0];
    private int[] lineWidths=new int[0];
    private int textWidth=-1;
    private boolean wrapped;
    private net.minecraft.client.gui.Font measuredFont;
    private NowPlayingLayout layout;
    private int availableWidth,availableHeight;
    private int textBackground=-1,textColor;

    void content(NowPlayingInfo next){
        info=next;textWidth=-1;layout=null;
    }
    NowPlayingLayout layout(Settings settings,int width,int height){
        var selectedFont=ClientRuntime.get().fonts().font(CustomFonts.Target.WIDGET);
        if(measuredFont!=selectedFont){measuredFont=selectedFont;layout=null;textWidth=-1;}
        if(layout==null||availableWidth!=width||availableHeight!=height){
            availableWidth=width;availableHeight=height;
            var base=NowPlayingLayout.fit(settings.widgetWidth,settings.widgetScale,0,
                settings.widgetArtwork,settings.nowPlayingStyle==NowPlayingStyle.HYBRID,width,height);
            int space=base.width()-base.textX()-7;
            if(textWidth!=space||wrapped!=settings.widgetWrap){
                textWidth=space;wrapped=settings.widgetWrap;
                var font=ClientRuntime.get().fonts().font(CustomFonts.Target.WIDGET);
                lines=info==null?new String[0]:wrapped?info.wrap(space,font::width):info.lines;
                fitted=new String[lines.length];lineWidths=new int[lines.length];
                for(int i=0;i<lines.length;i++){
                    String text=lines[i];lineWidths[i]=font.width(text);
                    fitted[i]=lineWidths[i]<=space?text:font.plainSubstrByWidth(text,Math.max(0,space-font.width("…")))+"…";
                }
            }
            layout=NowPlayingLayout.fit(settings.widgetWidth,settings.widgetScale,lines.length,
                settings.widgetArtwork,settings.nowPlayingStyle==NowPlayingStyle.HYBRID,width,height);
        }
        return layout;
    }

    void draw(GuiGraphicsExtractor g,Settings settings,NowPlayingLayout layout,float x,float y,float alpha,boolean hasCover,double age){
        if(info==null||(lines.length==0&&!settings.widgetArtwork))return;
        var font=ClientRuntime.get().fonts().font(CustomFonts.Target.WIDGET);
        var appearance=ClientRuntime.get().themes().preview();
        boolean hybrid=settings.nowPlayingStyle==NowPlayingStyle.HYBRID;
        int background=appearance==null?settings.widgetBackgroundColor:appearance.widgetBackground;
        double opacity=appearance==null?settings.widgetBackgroundOpacity:appearance.widgetOpacity;
        if(textBackground!=background){textBackground=background;textColor=Contrast.widgetText(background);}
        int width=layout.width(),height=layout.height(),space=width-layout.textX()-7;
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale((float)layout.scale(),(float)layout.scale());
        boolean image=false;
        if(hybrid)g.blitSprite(RenderPipelines.GUI_TEXTURED,TOAST,0,0,width,height,color(0xFFFFFF,alpha));
        else if(!(image=ClientRuntime.get().widgetBackground().draw(g,width,height,alpha)))
            g.fill(0,0,width,height,color(background,alpha*(float)opacity));
        if(settings.widgetArtwork){
            int size=layout.iconSize(),iconY=(height-size)/2;
            if(hasCover)g.blit(RenderPipelines.GUI_TEXTURED,COVER,7,iconY,0,0,size,size,256,256,256,256,color(0xFFFFFF,alpha));
            else{
                int noteColor=ColorLerper.getLerpedColor(ColorLerper.Type.MUSIC_NOTE,(int)(System.currentTimeMillis()/25%Integer.MAX_VALUE));
                g.blitSprite(RenderPipelines.GUI_TEXTURED,NOTES,7+(size-16)/2,iconY+(size-16)/2,16,16,color(noteColor,alpha));
            }
        }
        int textY=(height-(fitted.length*14-5))/2;
        boolean shadow=hybrid||image||alpha*opacity<.5;
        int rgb=hybrid?0xD3D3D3:image||alpha*opacity<.5?Contrast.WIDGET_LIGHT:textColor;
        g.enableScissor(layout.textX(),0,width-7,height);
        for(int i=0;i<fitted.length;i++){
            int offset=settings.widgetScroll?(int)NowPlayingLayout.scrollOffset(lineWidths[i],space,age):0;
            g.text(font,settings.widgetScroll?lines[i]:fitted[i],layout.textX()-offset,textY+i*14,color(rgb,alpha),shadow);
        }
        g.disableScissor();
        g.pose().popMatrix();
    }

    private static int color(int rgb,float alpha){return ((int)(255*alpha)<<24)|(rgb&0xFFFFFF);}
}
