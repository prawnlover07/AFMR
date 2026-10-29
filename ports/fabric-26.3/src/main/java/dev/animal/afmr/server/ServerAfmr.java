package dev.animal.afmr.server;

import dev.animal.afmr.network.ConditionNetworking;
import net.fabricmc.api.ModInitializer;

public final class ServerAfmr implements ModInitializer {
    @Override public void onInitialize(){ConditionNetworking.register();ServerConditions.register();}
}
