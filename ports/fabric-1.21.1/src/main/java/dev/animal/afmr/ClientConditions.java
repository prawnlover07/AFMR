package dev.animal.afmr;

import dev.animal.afmr.core.StructureContext;
import dev.animal.afmr.core.ServerConditionSession;
import dev.animal.afmr.network.*;
import java.util.Set;
import net.minecraft.client.Minecraft;

/** Client thread owns request scheduling and snapshots; server objects never cross threads. */
public final class ClientConditions {
    private static Object connection,player;
    private static String dimension="";
    private static final ServerConditionSession session=new ServerConditionSession();
    private static double clock;
    private static boolean supported;
    private ClientConditions(){}
    public static void register(){
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(ConditionObservation.TYPE,(value,context)->accept(value));
    }
    public static void clear(){connection=player=null;dimension="";supported=false;session.clear();}
    static void update(Minecraft mc,double now,int radius,boolean combat){
        clock=now;
        if(mc.level==null||mc.player==null||mc.getConnection()==null){clear();return;}
        String current=mc.level.dimension().location().toString();
        if(connection!=mc.getConnection()||player!=mc.player||!dimension.equals(current)){
            clear();connection=mc.getConnection();player=mc.player;dimension=current;
            supported=net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(ConditionQuery.TYPE)&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.getGlobalReceivers().contains(ConditionObservation.TYPE.id());
        }
        if(!supported)supported=net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.canSend(ConditionQuery.TYPE)&&net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.getGlobalReceivers().contains(ConditionObservation.TYPE.id());
        if(!supported)return;
        if(session.request(now,radius,combat)){
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new ConditionQuery(session.token(),dimension,mc.player.getId(),radius,combat,session.catalog().isEmpty()));
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
