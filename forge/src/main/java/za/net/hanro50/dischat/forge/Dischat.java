package za.net.hanro50.dischat.forge;

import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;
import za.net.hanro50.dischat.core.Deathcause;
import za.net.hanro50.dischat.core.ChatConsumer.Link;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = Constants.MOD_ID)
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Dischat {
  static MinecraftServer server;

  public Dischat(FMLJavaModLoadingContext context) {
  }

  static private void broadcast(Chater chater, String message, Collection<Link> links) {
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
  }

  @SubscribeEvent
  // Heals an entity by half a heart every time they jump.
  static private void onChatEvent(ServerChatEvent event) {
    ServerPlayer player = event.getPlayer();
    Thread.startVirtualThread(
        () -> Constants.core.sendChat(new Chater(player.getStringUUID(), player.getName().getString()),
            event.getMessage().plainCopy().getString()));

  }

  @SubscribeEvent
  static private void onDeathEvent(LivingDeathEvent event) {
    Entity entity = event.getEntity();
    DamageSource damageSource = event.getSource();
    if (!(entity instanceof Player))
      return;

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

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  static public void onServerStarting(ServerAboutToStartEvent event) {
    server = event.getServer();

    Thread.startVirtualThread(
        () -> {
          Constants.LOGGER
              .info(event.getServer().getFile("config/" + Constants.MOD_ID).toAbsolutePath().toString());

          Path config = server.getFile("config/" + Constants.MOD_ID).toAbsolutePath();
          Constants.core = new Core(config, server.getServerVersion(), Dischat::broadcast);
        });

  }

  @SubscribeEvent
  static public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
    Player player = event.getEntity();

    Thread.startVirtualThread(
        () -> Constants.core.sendJoin(new Chater(player.getStringUUID(), player.getName().getString())));
  }

  @SubscribeEvent
  static public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
    Player player = event.getEntity();

    Thread.startVirtualThread(
        () -> Constants.core.sendLeave(new Chater(player.getStringUUID(), player.getName().getString())));
  }

  static public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Commands.literal("linkme").executes((command) -> execute(command)));
  }

  static private int execute(CommandContext<CommandSourceStack> command) {
    if (command.getSource().getEntity() instanceof ServerPlayer) {
      ServerPlayer player = (ServerPlayer) command.getSource().getEntity();

      Constants.core.data.requestLink(player.getStringUUID(), (code) -> {
        player.sendSystemMessage(Component.literal(
            "Link code is <" + code + ">\nUse the /link command on the bot to complete linking"));
      });
    }
    return Command.SINGLE_SUCCESS;
  }

  @SubscribeEvent
  static public void registerCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }
}
