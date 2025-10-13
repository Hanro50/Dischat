package za.net.hanro50.dischat.fabric;

import net.fabricmc.api.DedicatedServerModInitializer;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;
import za.net.hanro50.dischat.core.Deathcause;
import za.net.hanro50.dischat.core.ChatConsumer.Link;

public class Dischat implements DedicatedServerModInitializer {
  MinecraftServer server;

  @Override
  public void onInitializeServer() {
    if (Constants.core != null)
      Constants.core.kill();

    Path path = FabricLoader.getInstance().getConfigDir();
    String version = FabricLoader.getInstance().getRawGameVersion();
    path = Path.of(new File(path.toFile(), Constants.MOD_ID).toURI());
    Constants.core = new Core(path, version, (chater, message, links) -> {
      if (server == null)
        return;
      PlayerChatMessage chatMessage;
      String name = chater.name;
      if (chater.minecraftID != null) {
        UUID uuid = UUID.fromString(chater.minecraftID);
        if (server.services().profileResolver().fetchById(uuid).isPresent()) {
          GameProfile user = server.services().profileResolver().fetchById(uuid).get();
          name = user.name();
        }
      }
      final var fname = name;
      if (links.size() > 0) {
        chatMessage = PlayerChatMessage.system("");

        String text = message;
        if (text.length() > 0)
          text += "\n";

        chatMessage = chatMessage.withUnsignedContent(
            Component.literal(text).append(
                ComponentUtils.formatAndSortList(links, (Link link) -> {
                  return Component.literal("[" + link.name + "]").withStyle((style) -> {
                    return style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(link.link)))
                        .withHoverEvent(new HoverEvent.ShowText(
                            Component.translatable("chat.link.open")))
                        .withInsertion(link.link);
                  });
                })));

      } else {
        chatMessage = PlayerChatMessage.system(message);
      }
      OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(chatMessage);

      server.getPlayerList().getPlayers().forEach((serverplayer) -> {
        ChatType.Bound bind = ChatType.bind(ChatType.CHAT, server.registryAccess(),
            Component.literal(fname));
        serverplayer.sendChatMessage(outgoingChatMessage, false, bind);
      });

    });

    ServerLifecycleEvents.SERVER_STARTED.register((MinecraftServer _server) -> {
      this.server = _server;
    });
    ServerMessageEvents.CHAT_MESSAGE
        .register((PlayerChatMessage message, ServerPlayer player, ChatType.Bound type) -> {
          Thread.startVirtualThread(
              () -> Constants.core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()),
                  message.decoratedContent().plainCopy().getString()));
        });
    ServerPlayerEvents.JOIN.register((ServerPlayer player) -> {
      Thread.startVirtualThread(
          () -> Constants.core.sendJoin(new Chater(player.getStringUUID(), player.getName().getString())));
    });
    ServerPlayerEvents.LEAVE.register((ServerPlayer player) -> {
      Thread.startVirtualThread(
          () -> Constants.core.sendLeave(new Chater(player.getStringUUID(), player.getName().getString())));
    });

    ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
      if (!(entity instanceof Player))
        return;

      Thread.startVirtualThread(
          () -> {

            Chater victem = new Chater(entity.getStringUUID(), entity.getName().getString());

            Deathcause dc = new Deathcause();
            dc.cause = "death.attack." + damageSource.getMsgId();

            Entity attackerEntity = damageSource.getEntity();
            damageSource.getLocalizedDeathMessage(entity);

            if (attackerEntity != null) {
              if (attackerEntity instanceof LivingEntity) {
                LivingEntity livingAttacker = (LivingEntity) attackerEntity;
                ItemStack item = livingAttacker.getMainHandItem();
                if (item != null && item.getCustomName() != null) {
                  dc.cause += ".item";
                  dc.itemName = "[" + item.getCustomName().getString() + "]";
                }

              }
              if (attackerEntity instanceof Player) {
                if (dc.itemName == null)
                  dc.cause += ".player";
                Player player = (Player) attackerEntity;
                dc.playerAttacker = new Chater(player.getStringUUID(), player.getName().getString());
                dc.name = dc.playerAttacker.name;
              } else {
                dc.attacker = attackerEntity.getType().getDescriptionId();
                if (attackerEntity.hasCustomName())
                  dc.name = attackerEntity.getCustomName().getString();
              }
            }

            Constants.core.sendDeath(victem, dc);
          });
    });

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
      dispatcher.register(Commands.literal("linkme").executes(context -> {
        var source = context.getSource().getEntity();
        if (source instanceof ServerPlayer) {
          ServerPlayer player = (ServerPlayer) source;
          Constants.core.data.requestLink(player.getStringUUID(), (code) -> {
            context.getSource().sendSystemMessage(Component.literal(
                "Link code is <" + code + ">\nUse the /link command on the bot to complete linking"));
          });
        }
        return 1;
      }));
    });

  }
}