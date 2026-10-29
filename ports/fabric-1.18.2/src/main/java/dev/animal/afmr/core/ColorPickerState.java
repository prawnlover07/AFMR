package dev.animal.afmr.core;

/** RGB/HSV state with stable hue through gray/black selections. No rendering or allocation. */
public final class ColorPickerState {
    private double hue,saturation,value;
    public ColorPickerState(int rgb){rgb(rgb);}
    public double hue(){return hue;}
    public double saturation(){return saturation;}
    public double value(){return value;}
    public void hue(double hue){this.hue=unit(hue);}
    public void saturationValue(double saturation,double value){this.saturation=unit(saturation);this.value=unit(value);}
    private static double unit(double n){return Double.isFinite(n)?Math.max(0,Math.min(1,n)):0;}
    public int rgb(){return rgb(hue,saturation,value);}
    public void rgb(int rgb){
        double r=((rgb>>16)&255)/255d,g=((rgb>>8)&255)/255d,b=(rgb&255)/255d;
        double max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b)),delta=max-min;
        value=max;saturation=max==0?0:delta/max;
        if(delta>0){
            double h=max==r?(g-b)/delta:max==g?2+(b-r)/delta:4+(r-g)/delta;
            hue=h/6;if(hue<0)hue+=1;
        }
    }
    public static int rgb(double hue,double saturation,double value){
        double h=unit(hue)*6,s=unit(saturation),v=unit(value),f=h-Math.floor(h);
        double p=v*(1-s),q=v*(1-f*s),t=v*(1-(1-f)*s);
        double r,g,b;
        switch((int)h%6){
            case 0->{r=v;g=t;b=p;}case 1->{r=q;g=v;b=p;}case 2->{r=p;g=v;b=t;}
            case 3->{r=p;g=q;b=v;}case 4->{r=t;g=p;b=v;}default->{r=v;g=p;b=q;}
        }
        return (int)Math.round(r*255)<<16|(int)Math.round(g*255)<<8|(int)Math.round(b*255);
    }
}
