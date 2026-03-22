package za.net.hanro50.dischat.common;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

import javax.imageio.ImageIO;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Deathcause;
import za.net.hanro50.dischat.core.InfoProvider;
import za.net.hanro50.dischat.core.NamespaceContainer;
import za.net.hanro50.dischat.mixin.MinecraftServerAccessor;

/**
 * The core of every mod loaded based wrapper for MDCB (MC Discord chat Bridge)
 */
public class Universal {
  static MinecraftServer server;

  public static void setStatusIcon(Path path) {
    try {
      BufferedImage bufferedImage = ImageIO.read(path.toFile());
      BufferedImage outputImage = new BufferedImage(64, 64, bufferedImage.getType());
      Graphics2D g2d = outputImage.createGraphics();
      // Set rendering hints for better quality (optional, but recommended)
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      // Draw the input image onto the output image, scaling it
      g2d.drawImage(bufferedImage, 0, 0, 64, 64, null);
      // Dispose of the Graphics2D object to free up resources
      g2d.dispose();

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      ImageIO.write(outputImage, "PNG", byteArrayOutputStream);

      ((MinecraftServerAccessor) server)
          .dischat$setStatusIcon(new ServerStatus.Favicon(byteArrayOutputStream.toByteArray()));

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public static void setIconUpdateListener() {
    Thread.startVirtualThread(
        () -> Constants.core.addSetIconListener(path -> setStatusIcon(path)));
  }

  public static void setServer(MinecraftServer server) {
    Universal.server = server;

  }

  public static void onChatEvent(ServerPlayer player, String message) {
    Thread.startVirtualThread(
        () -> Constants.core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()), message));
  }

  public static InfoProvider.Result getInfo() {
    var info = new InfoProvider.Result();
    info.tps = server.tickRateManager().tickrate();
    info.maxPlayers = server.getMaxPlayers();
    info.onlinePlayerCount = server.getPlayerCount();

    var icon = ((MinecraftServerAccessor) server).dischat$getStatusIcon();
    if (icon != null)
      info.icon = icon.iconBytes();

    return info;
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
            var player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
              GameProfile user = player.getGameProfile();
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
                        try {
                          return style.withColor(ChatFormatting.GREEN)
                              .withClickEvent(new ClickEvent.OpenUrl(new URI(link.link)))
                              .withHoverEvent(
                                  new HoverEvent.ShowText(Component.translatable("chat.link.open")))
                              .withInsertion(link.link);
                        } catch (URISyntaxException e) {
                          return style.withColor(ChatFormatting.GREEN)
                              .withInsertion(link.link);
                        }
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

          dc.cause = new NamespaceContainer(damageSource.typeHolder().getRegisteredName().split(":")[0],
              "death.attack." + damageSource.getMsgId());
          if (dc.cause.path.equals("death.attack.badRespawnPoint")) {
            dc.cause.path += ".message";
            dc.attacker = new NamespaceContainer("minecraft",
                "death.attack.badRespawnPoint.link");
            Constants.core.sendDeath(victem, dc);
            return;
          }

          damageSource.getLocalizedDeathMessage(entity);
          Entity causingEntity = damageSource.getEntity();
          Entity directEntity = damageSource.getDirectEntity();
          if (causingEntity == null && directEntity == null) {
            causingEntity = entity.getKillCredit();
            if (causingEntity == null) {
              Constants.core.sendDeath(victem, dc);
              return;
            }
          }

          causingEntity = causingEntity == null ? directEntity : causingEntity;
          if (causingEntity instanceof LivingEntity livingEntity) {
            var item = livingEntity.getMainHandItem();
            if (!item.isEmpty() && item.has(DataComponents.CUSTOM_NAME)) {
              dc.cause.path += ".item";
              dc.itemName = item.getDisplayName().getString();
            }
          }
          if (!dc.cause.path.endsWith(".item"))
            dc.cause.path += ".player";

          if (causingEntity instanceof Player player) {
            dc.playerAttacker = new Chater(player.getStringUUID(),
                player.getName().getString());
            dc.name = dc.playerAttacker.name;
          } else {
            Identifier resourceLocation = EntityType.getKey(causingEntity.getType());

            dc.attacker = new NamespaceContainer(resourceLocation.getNamespace(),
                causingEntity.getType().getDescriptionId());
            if (causingEntity.hasCustomName())
              dc.name = causingEntity.getCustomName().getString();
          }

          Constants.core.sendDeath(victem, dc);
          return;
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
