package dev.animal.afmr;

import dev.animal.afmr.core.StructureContext;
import dev.animal.afmr.core.ServerConditionSession;
import dev.animal.afmr.network.*;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/** Client thread owns request scheduling and snapshots; server objects never cross threads. */
@EventBusSubscriber(modid="afmr",value=Dist.CLIENT)
public final class ClientConditions {
    private static Object connection,player;
    private static String dimension="";
    private static final ServerConditionSession session=new ServerConditionSession();
    private static double clock;
    private static boolean supported;
    private ClientConditions(){}
    @SubscribeEvent public static void handlers(RegisterClientPayloadHandlersEvent event){
        event.register(ConditionObservation.TYPE,(value,context)->accept(value));
    }
    public static void clear(){connection=player=null;dimension="";supported=false;session.clear();}
    static void update(Minecraft mc,double now,int radius,boolean combat){
        clock=now;
        if(mc.level==null||mc.player==null||mc.getConnection()==null){clear();return;}
        String current=mc.level.dimension().identifier().toString();
        if(connection!=mc.getConnection()||player!=mc.player||!dimension.equals(current)){
            clear();connection=mc.getConnection();player=mc.player;dimension=current;
            supported=mc.getConnection().hasChannel(ConditionQuery.TYPE)&&mc.getConnection().hasChannel(ConditionObservation.TYPE);
        }
        if(!supported)supported=mc.getConnection().hasChannel(ConditionQuery.TYPE)&&mc.getConnection().hasChannel(ConditionObservation.TYPE);
        if(!supported)return;
        if(session.request(now,radius,combat)){
            mc.getConnection().send(new ConditionQuery(session.token(),dimension,mc.player.getId(),radius,combat,session.catalog().isEmpty()));
        }
    }
    private static void accept(ConditionObservation value){
        var mc=Minecraft.getInstance();
        if(connection!=mc.getConnection()||player!=mc.player||mc.player==null
            ||value.entityId()!=mc.player.getId()||!dimension.equals(value.dimension()))return;
        session.receive(value.token(),clock,value.targeted(),value.structures(),value.catalog());
    }
    static boolean supported(){return supported;}
    static Set<String> catalog(){return session.catalog();}
    static StructureContext structures(){return session.structures(clock);}
    static boolean targeted(){return session.targeted(clock);}
}
