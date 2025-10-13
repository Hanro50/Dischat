package za.net.hanro50.dischat.neoforge;

import java.nio.file.Path;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import za.net.hanro50.dischat.common.Universal;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = Constants.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class Dischat {
  ModContainer modContainer;
  MinecraftServer server;

  public Dischat(IEventBus modEventBus, ModContainer modContainer) {
    this.modContainer = modContainer;
    NeoForge.EVENT_BUS.register(this);
  }

  @SubscribeEvent
  // Heals an entity by half a heart every time they jump.
  private void onChatEvent(ServerChatEvent event) {
    Universal.onChatEvent(event.getPlayer(), event.getMessage().plainCopy().getString());
  }

  @SubscribeEvent
  private void onDeathEvent(LivingDeathEvent event) {
    Entity entity = event.getEntity();
    DamageSource damageSource = event.getSource();
    if (!(entity instanceof Player))
      return;
    Universal.onDeathEvent((Player) entity, damageSource);
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  public void onServerStarting(ServerAboutToStartEvent event) {
    Universal.setServer(server);
    Thread.startVirtualThread(
        () -> {
          Constants.LOGGER
              .info(event.getServer().getFile("config/" + Constants.MOD_ID).toAbsolutePath().toString());

          Path config = server.getFile("config/" + Constants.MOD_ID).toAbsolutePath();
          Constants.core = new Core(config, server.getServerVersion(), Universal::broadcastChatMessage);
        });

  }

  @SubscribeEvent
  public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
    Universal.onJoinEvent(event.getEntity());
  }

  @SubscribeEvent
  public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
    Universal.onLeaveEvent(event.getEntity());
  }

  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Universal.linkMeCommand);
  }

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }
}
