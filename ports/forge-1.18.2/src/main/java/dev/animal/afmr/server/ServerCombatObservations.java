package dev.animal.afmr.server;
import dev.animal.afmr.core.CombatOpponent;
import dev.animal.afmr.network.CombatHit;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import java.util.*;
/** Server-thread ownership. Merge same-tick hits; flush at most 64 clients/tick. */
public final class ServerCombatObservations {
 private static final Map<UUID,Pending> players=new HashMap<>();
 private static final ArrayDeque<Pending> dirty=new ArrayDeque<>();
 private static final class Pending {
  final ServerPlayer player;
  String dimension;int entity,tick;
  boolean queued,incoming,outgoing;
  CombatOpponent damage=CombatOpponent.NONE,attack=CombatOpponent.NONE;
  Pending(ServerPlayer player){this.player=player;}
 }
 private ServerCombatObservations(){}
 public static void hurt(LivingEntity victim,DamageSource source){
  if(victim.level.isClientSide)return;
  Entity attacker=source.getEntity();if(attacker==null)attacker=source.getDirectEntity();
  if(attacker instanceof Projectile projectile)attacker=projectile.getOwner();
  if(victim instanceof ServerPlayer player)record(player,true,CombatMobs.classify(attacker,player));
  if(attacker instanceof ServerPlayer player&&attacker!=victim)record(player,false,CombatMobs.classify(victim,player));
 }
 private static void record(ServerPlayer player,boolean incoming,CombatOpponent opponent){
  if(player.connection==null||player.hasDisconnected()||!dev.animal.afmr.network.ConditionNetworking.CHANNEL.isRemotePresent(player.connection.connection))return;
  var pending=players.get(player.getUUID());
  if(pending==null||pending.player!=player){if(pending!=null&&pending.queued)dirty.remove(pending);pending=new Pending(player);players.put(player.getUUID(),pending);}
  String dimension=player.level.dimension().location().toString();
  if(pending.queued&&(pending.entity!=player.getId()||!dimension.equals(pending.dimension))){dirty.remove(pending);pending.queued=pending.incoming=pending.outgoing=false;pending.damage=pending.attack=CombatOpponent.NONE;}
  if(!pending.queued){pending.dimension=dimension;pending.entity=player.getId();pending.tick=player.level.getServer().getTickCount();pending.queued=true;dirty.addLast(pending);}
  if(incoming){pending.incoming=true;if(opponent.ordinal()>pending.damage.ordinal())pending.damage=opponent;}
  else {pending.outgoing=true;if(opponent.ordinal()>pending.attack.ordinal())pending.attack=opponent;}
 }
 public static void flush(MinecraftServer server){
  int count=Math.min(64,dirty.size());
  for(int i=0;i<count;i++){
   var pending=dirty.removeFirst();pending.queued=false;var player=pending.player;
   if(player.connection!=null&&!player.hasDisconnected()&&player.level.getServer()==server&&pending.entity==player.getId()&&pending.dimension.equals(player.level.dimension().location().toString())&&server.getTickCount()-pending.tick<=40){
    if(pending.incoming)send(player,new CombatHit(pending.dimension,pending.entity,true,pending.damage));
    if(pending.outgoing)send(player,new CombatHit(pending.dimension,pending.entity,false,pending.attack));
   }
   pending.incoming=pending.outgoing=false;pending.damage=pending.attack=CombatOpponent.NONE;
  }
 }
 private static void send(ServerPlayer player,CombatHit hit){
  dev.animal.afmr.network.ConditionNetworking.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->player),hit);
 }
 public static void logout(ServerPlayer player){var pending=players.remove(player.getUUID());if(pending!=null&&pending.queued)dirty.remove(pending);}
 public static void clear(){players.clear();dirty.clear();}
}
