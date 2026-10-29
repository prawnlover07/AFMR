package dev.animal.afmr.server;
@net.minecraftforge.fml.common.Mod("afmr")
public final class ServerAfmr {
 public ServerAfmr(){dev.animal.afmr.network.ConditionNetworking.register();ServerConditions.register();}
}
