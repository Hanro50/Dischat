package za.net.hanro50.dischat.common;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Deathcause;

/**
 * The core of every mod loaded based wrapper for MDCB (MC Discord chat Bridge)
 */
public class Universal {
  static MinecraftServer server;

  public static void setServer(MinecraftServer server) {
    Universal.server = server;
  }

  public static void onChatEvent(ServerPlayer player, String message) {
    Thread.startVirtualThread(
        () -> Constants.core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()), message));
  }

  public static void broadcastChatMessage(Chater chater, String message, Collection<Link> links) {
    Thread.startVirtualThread(
        () -> {
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
          final var fname = name;
          final var fchatMessage = chatMessage;
          server.getPlayerList().getPlayers().forEach((serverplayer) -> {
            OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(fchatMessage);

            ChatType.Bound bind = ChatType.bind(ChatType.CHAT, server.registryAccess(),
                Component.literal(fname));
            serverplayer.sendChatMessage(outgoingChatMessage, false, bind);
          });
        });
  }

  public static void onDeathEvent(Player entity, DamageSource damageSource) {
    Thread.startVirtualThread(
        () -> {
          Chater victem = new Chater(entity.getStringUUID(), entity.getName().getString());

          Deathcause dc = new Deathcause();
          dc.cause = "death.attack." + damageSource.getMsgId();

          Entity attackerEntity = damageSource.getEntity();

          if (attackerEntity != null) {
            if (attackerEntity instanceof Player) {
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
  }

  public static void onJoinEvent(Player player) {
    Thread.startVirtualThread(
        () -> Constants.core.sendJoin(new Chater(player.getStringUUID(), player.getName().getString())));
  }

  public static void onLeaveEvent(Player player) {
    Thread.startVirtualThread(
        () -> Constants.core.sendLeave(new Chater(player.getStringUUID(), player.getName().getString())));
  }

  public static final LiteralArgumentBuilder<CommandSourceStack> linkMeCommand = Commands.literal("linkme")
      .executes(context -> {
        var source = context.getSource().getEntity();
        if (source instanceof ServerPlayer) {
          ServerPlayer player = (ServerPlayer) source;
          Constants.core.data.requestLink(player.getStringUUID(), (code) -> {
            context.getSource().sendSystemMessage(Component.literal(
                "Link code is <" + code + ">\nUse the /link command on the bot to complete linking"));
          });
        }
        return 1;
      });
}
