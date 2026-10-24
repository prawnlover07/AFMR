package dev.animal.afmr.ui;

import dev.animal.afmr.ClientRuntime;
import dev.animal.afmr.core.ColorPickerState;
import dev.animal.afmr.core.MenuColor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import java.util.Locale;
import java.util.function.IntConsumer;

/** Live previews are owned by the caller; Cancel restores the opening RGB value. */
final class ColorPickerScreen extends MusicScreen {
    private static final int[] PRESETS={0x000000,0xFFFFFF,0xFF4040,0xFFB040,0x80D080,0x40B8E0,0x8070E0,0xD080C0};
    private final ColorPickerState color;
    private final int original;
    private final IntConsumer preview;
    private EditBox hex;
    private boolean updating;
    private String error="";
    private int detailX,detailWidth;
    ColorPickerScreen(Screen parent,ClientRuntime runtime,String name,int rgb,IntConsumer preview){
        super(name,parent,runtime);original=rgb&0xFFFFFF;color=new ColorPickerState(original);this.preview=preview;
    }
    @Override protected void init(){
        super.init();int size=Math.max(40,Math.min(180,Math.min(height-99,contentWidth-164)));
        detailX=left+size+36;detailWidth=Math.max(70,contentWidth-size-36);
        addRenderableWidget(new ColorPickerArea(left,50,size,size,false,color,this::changed));
        addRenderableWidget(new ColorPickerArea(left+size+10,50,14,size,true,color,this::changed));
        hex=edit(String.format(Locale.ROOT,"#%06X",color.rgb()),"#RRGGBB",detailX,63,Math.min(120,detailWidth),null);hex.setMaxLength(7);
        hex.setResponder(value->{if(!updating)try{color.rgb(MenuColor.parse(value));error="";preview.accept(color.rgb());}catch(IllegalArgumentException e){error="Use six hex digits";}});
        for(int i=0;i<PRESETS.length;i++){
            int rgb=PRESETS[i],y=140+i/4*24;if(y+18>height-47)continue;
            addRenderableWidget(new ColorSwatchButton(String.format(Locale.ROOT,"#%06X",rgb),detailX+i%4*24,y,20,18,()->rgb,()->{color.rgb(rgb);changed();}));
        }
        button("Cancel",left,height-29,110,this::onClose);
        button("Apply",left+contentWidth-110,height-29,110,()->{if(saveChanges())minecraft.gui.setScreen(parent);});
    }
    private void changed(){
        updating=true;hex.setValue(String.format(Locale.ROOT,"#%06X",color.rgb()));updating=false;error="";preview.accept(color.rgb());
    }
    @Override protected boolean saveChanges(){
        try{MenuColor.parse(hex.getValue());}catch(IllegalArgumentException e){error="Use six hex digits";return false;}
        preview.accept(color.rgb());runtime.save();return true;
    }
    @Override public void onClose(){preview.accept(original);runtime.save();super.onClose();}
    @Override protected void drawBody(GuiGraphicsExtractor g,int x,int y,float delta){
        g.text(font,"Hex",detailX,50,MenuAppearance.color(MenuColor.TEXT,0xFFD9E8E0));
        int half=Math.min(120,detailWidth)/2;
        g.fill(detailX,94,detailX+half,114,0xFF000000|original);g.fill(detailX+half+4,94,detailX+half*2+4,114,0xFF000000|color.rgb());
        g.text(font,"Original",detailX,119,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));g.text(font,"New",detailX+half+4,119,MenuAppearance.color(MenuColor.MUTED_TEXT,0xFFA9BDB3));
        if(!error.isBlank())g.text(font,fit(error,contentWidth),left,height-44,0xFFFFA0A0);
    }
}
