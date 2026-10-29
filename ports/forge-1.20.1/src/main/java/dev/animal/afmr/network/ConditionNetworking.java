package dev.animal.afmr.network;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.Optional;
public final class ConditionNetworking {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new net.minecraft.resources.ResourceLocation("afmr","conditions"),()->"1",version->version.equals("1")||version.equals(NetworkRegistry.ABSENT)||version.equals(NetworkRegistry.ACCEPTVANILLA),version->version.equals("1")||version.equals(NetworkRegistry.ABSENT)||version.equals(NetworkRegistry.ACCEPTVANILLA));
 private ConditionNetworking(){}
 public static void register(){
  CHANNEL.registerMessage(0,ConditionQuery.class,ConditionQuery::write,ConditionQuery::read,(query,supplier)->{var ctx=supplier.get();ctx.enqueueWork(()->{var p=ctx.getSender();if(p!=null&&CHANNEL.isRemotePresent(p.connection.connection))dev.animal.afmr.server.ServerConditions.request(p,query);});ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));
  CHANNEL.registerMessage(1,ConditionObservation.class,ConditionObservation::write,ConditionObservation::read,(value,supplier)->{var ctx=supplier.get();ctx.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.animal.afmr.ClientConditions.accept(value)));ctx.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_CLIENT));
 }
}
