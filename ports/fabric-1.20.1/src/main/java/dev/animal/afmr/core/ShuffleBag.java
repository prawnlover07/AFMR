package dev.animal.afmr.core;

import dev.animal.afmr.core.Library.Track;
import java.util.*;

/** One Fisher-Yates order per playlist cycle; membership changes preserve songs already drawn. */
public final class ShuffleBag {
    private Set<String> members=Set.of();
    private final Set<String> played=new HashSet<>();
    private final List<String> order=new ArrayList<>();
    private int cursor;
    public Track next(List<Track> eligible,String previous,Random random){
        if(eligible.isEmpty())return null;
        Map<String,Track> available=new HashMap<>((int)(eligible.size()/0.75f)+1);for(var t:eligible)available.put(t.id,t);
        boolean changed=members.size()!=available.size()||!members.containsAll(available.keySet());
        if(changed||cursor>=order.size()){
            if(!changed)played.clear();
            order.clear();for(String id:available.keySet())if(!played.contains(id))order.add(id);
            if(order.isEmpty()){played.clear();order.addAll(available.keySet());}
            // Fisher-Yates: uniformly choose each position from the remaining prefix.
            for(int i=order.size()-1;i>0;i--){int j=random.nextInt(i+1);String id=order.get(i);order.set(i,order.get(j));order.set(j,id);}
            members=new HashSet<>(available.keySet());cursor=0;
        }
        if(order.get(cursor).equals(previous)&&cursor+1<order.size()){
            int j=cursor+1+random.nextInt(order.size()-cursor-1);String id=order.get(cursor);order.set(cursor,order.get(j));order.set(j,id);
        }
        String id=order.get(cursor++);played.add(id);return available.get(id);
    }
}
