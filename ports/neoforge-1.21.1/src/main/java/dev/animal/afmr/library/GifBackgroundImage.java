package dev.animal.afmr.library;

import javax.imageio.ImageReader;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import org.w3c.dom.Node;

/** Composites GIF patches on a bounded canvas, including both restore disposal methods. */
final class GifBackgroundImage {
    static final int MAX_FRAMES=512;
    static final long FRAME_PIXELS=16_777_216; // 64 MiB of decoded RGBA, across all frames.
    private record Patch(int x,int y,int w,int h,int millis,String disposal,boolean transparent){}
    private GifBackgroundImage(){}

    static MenuBackgroundImage.Prepared read(ImageReader reader,byte[] source)throws IOException{
        if(source.length==0||source[source.length-1]!=0x3b)throw new IOException("Incomplete GIF image.");
        var stream=reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
        Node screen=child(stream,"LogicalScreenDescriptor");
        int width=number(screen,"logicalScreenWidth"),height=number(screen,"logicalScreenHeight");
        MenuBackgroundImage.dimensions(width,height);
        Patch[] patches=new Patch[MAX_FRAMES];int count=0;long sourcePixels=0;
        for(int i=0;i<=MAX_FRAMES;i++){
            try{reader.getWidth(i);}catch(IndexOutOfBoundsException end){break;}
            if(i==MAX_FRAMES)throw new IOException("Choose a GIF with no more than 512 frames.");
            Node metadata=reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
            Node descriptor=child(metadata,"ImageDescriptor"),control=child(metadata,"GraphicControlExtension");
            int x=number(descriptor,"imageLeftPosition"),y=number(descriptor,"imageTopPosition");
            int w=number(descriptor,"imageWidth"),h=number(descriptor,"imageHeight");
            MenuBackgroundImage.dimensions(w,h);
            if(x<0||y<0||x+w>width||y+h>height)throw new IOException("GIF frame extends beyond its canvas.");
            sourcePixels+=(long)w*h;
            if(sourcePixels>256_000_000)throw new IOException("GIF exceeds the total decoding limit. Choose a smaller animation.");
            int delay=control==null?0:number(control,"delayTime");
            patches[count++]=new Patch(x,y,w,h,delay==0?100:delay*10,
                control==null?"none":attribute(control,"disposalMethod"),control!=null&&attribute(control,"transparentColorFlag").equals("TRUE"));
        }
        if(count==0)throw new IOException("GIF has no frames.");
        double scale=Math.min(1,Math.min((double)MenuBackgroundImage.MAX_EDGE/Math.max(width,height),
            Math.sqrt((double)FRAME_PIXELS/((long)width*height*count))));
        // Floor keeps all retained frames within the memory budget, even after rounding.
        int w=Math.max(1,(int)(width*scale)),h=Math.max(1,(int)(height*scale));
        int background=0;Node table=child(stream,"GlobalColorTable");
        if(table!=null){
            int index=number(table,"backgroundColorIndex");
            for(Node color=table.getFirstChild();color!=null;color=color.getNextSibling())if(number(color,"index")==index)
                background=0xff000000|(number(color,"red")<<16)|(number(color,"green")<<8)|number(color,"blue");
        }
        var canvas=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var previous=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        var graphics=canvas.createGraphics();var saved=previous.createGraphics();
        var frames=new MenuBackgroundImage.Frame[count];boolean[] warning={false};
        reader.addIIOReadWarningListener((r,message)->warning[0]=true);
        try{
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(new Color(patches[0].transparent?0:background,true));graphics.fillRect(0,0,w,h);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            saved.setComposite(AlphaComposite.Src);
            for(int i=0;i<count;i++){
                Patch patch=patches[i];
                if(patch.disposal.equals("restoreToPrevious"))saved.drawImage(canvas,0,0,null);
                var parameters=reader.getDefaultReadParam();
                int sample=Math.max(1,(int)(1/scale));parameters.setSourceSubsampling(sample,sample,0,0);
                var decoded=reader.read(i,parameters);
                if(decoded==null||warning[0])throw new IOException("GIF frame could not be decoded.");
                int x0=(int)Math.round((double)patch.x*w/width),y0=(int)Math.round((double)patch.y*h/height);
                int x1=(int)Math.round((double)(patch.x+patch.w)*w/width),y1=(int)Math.round((double)(patch.y+patch.h)*h/height);
                try{
                    graphics.setComposite(AlphaComposite.SrcOver);
                    graphics.drawImage(decoded,x0,y0,x1,y1,0,0,decoded.getWidth(),decoded.getHeight(),null);
                }finally{decoded.flush();}
                frames[i]=new MenuBackgroundImage.Frame(MenuBackgroundImage.png(canvas),patch.millis);
                if(patch.disposal.equals("restoreToPrevious")){
                    graphics.setComposite(AlphaComposite.Src);graphics.drawImage(previous,0,0,null);
                }else if(patch.disposal.equals("restoreToBackgroundColor")){
                    graphics.setComposite(AlphaComposite.Src);graphics.setColor(new Color(patch.transparent?0:background,true));
                    graphics.fillRect(x0,y0,x1-x0,y1-y0);
                }
            }
            return new MenuBackgroundImage.Prepared(count>1?source:frames[0].png(),frames,w,h);
        }finally{graphics.dispose();saved.dispose();canvas.flush();previous.flush();}
    }
    private static Node child(Node parent,String name){
        if(parent!=null)for(Node n=parent.getFirstChild();n!=null;n=n.getNextSibling())if(n.getNodeName().equals(name))return n;
        return null;
    }
    private static String attribute(Node node,String name)throws IOException{
        Node attribute=node==null?null:node.getAttributes().getNamedItem(name);
        if(attribute==null)throw new IOException("Incomplete GIF metadata.");return attribute.getNodeValue();
    }
    private static int number(Node node,String name)throws IOException{return Integer.parseInt(attribute(node,name));}
}
