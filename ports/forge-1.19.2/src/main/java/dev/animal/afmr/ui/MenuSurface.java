package dev.animal.afmr.ui;

import java.util.List;
import dev.animal.afmr.core.MenuColor;

/** Cached, non-overlapping backgrounds prevent stacked alpha from hiding the game. */
record MenuSurface(int x0,int y0,int x1,int y1,MenuColor color,int rgb) {
    static void cutOut(List<MenuSurface> surfaces,int x0,int y0,int x1,int y1){
        // Newly appended pieces lie outside the cut, so only visit the original rectangles.
        for(int i=surfaces.size()-1;i>=0;i--){
            var s=surfaces.get(i);
            int l=Math.max(s.x0,x0),t=Math.max(s.y0,y0),r=Math.min(s.x1,x1),b=Math.min(s.y1,y1);
            if(l>=r||t>=b)continue;
            surfaces.remove(i);
            if(s.y0<t)surfaces.add(new MenuSurface(s.x0,s.y0,s.x1,t,s.color,s.rgb));
            if(b<s.y1)surfaces.add(new MenuSurface(s.x0,b,s.x1,s.y1,s.color,s.rgb));
            if(s.x0<l)surfaces.add(new MenuSurface(s.x0,t,l,b,s.color,s.rgb));
            if(r<s.x1)surfaces.add(new MenuSurface(r,t,s.x1,b,s.color,s.rgb));
        }
    }
}
