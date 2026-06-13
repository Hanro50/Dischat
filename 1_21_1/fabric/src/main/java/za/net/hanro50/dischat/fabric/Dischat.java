package za.net.hanro50.dischat.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;

import java.io.File;
import java.nio.file.Path;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ChatType;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import za.net.hanro50.dischat.common.Universal;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;

public class Dischat implements DedicatedServerModInitializer {

  @Override
  public void onInitializeServer() {
    if (Constants.core != null)
      Constants.core.kill();

    Path path = FabricLoader.getInstance().getConfigDir();
    String version = FabricLoader.getInstance().getRawGameVersion();
    path = Path.of(new File(path.toFile(), Constants.MOD_ID).toURI());
    Constants.core = new Core(path, Universal::onLaunch);
    Constants.core.setLexicon(new FabricLexicon(version, Constants.core.config.lang));

    ServerLifecycleEvents.SERVER_STARTED.register(Universal::setServer);

    ServerMessageEvents.CHAT_MESSAGE
        .register((PlayerChatMessage message, ServerPlayer player, ChatType.Bound type) -> {
          Universal.onChatEvent(player, message.decoratedContent().getString());
        });
    ServerPlayerEvents.JOIN.register(Universal::onJoinEvent);
    ServerPlayerEvents.LEAVE.register(Universal::onLeaveEvent);

    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
      if (!(entity instanceof Player))
        return;
      Universal.onDeathEvent((Player) entity, damageSource);
    });

    CommandRegistrationCallback.EVENT
        .register((dispatcher, registryAccess, environment) -> dispatcher.register(Universal.linkMeCommand));

  }
}