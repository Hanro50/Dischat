package za.net.hanro50.dischat.common;

import java.util.HashMap;
import java.util.Map;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.context.ImmutableContextSet;
import net.minecraft.server.level.ServerPlayer;
import za.net.hanro50.dischat.core.Constants;

public class LP implements ContextCalculator<ServerPlayer> {
  static LuckPerms api;

  public static void boot() {
    api = LuckPermsProvider.get();
    api.getContextManager().registerCalculator(new LP());
  }

  @Override
  public void calculate(ServerPlayer target, ContextConsumer consumer) {
    Constants.core.getUserContext(target.getStringUUID(), consumer::accept);

  }

  @Override
  public ContextSet estimatePotentialContexts() {
    ImmutableContextSet.Builder builder = ImmutableContextSet.builder();
    Constants.core.getPotentialContexts(builder::add);

    return builder.build();

  }
}
