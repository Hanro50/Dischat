package za.net.hanro50.dischat.forge;

import java.nio.file.Path;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import za.net.hanro50.dischat.common.Universal;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(value = Constants.MOD_ID)
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Dischat {
  static MinecraftServer server;

  public Dischat(FMLJavaModLoadingContext context) {
  }

  @SubscribeEvent
  // Heals an entity by half a heart every time they jump.
  static public void onChatEvent(ServerChatEvent event) {
    Universal.onChatEvent(event.getPlayer(), event.getMessage().plainCopy().getString());
  }

  @SubscribeEvent
  static public void onDeathEvent(LivingDeathEvent event) {
    Entity entity = event.getEntity();
    DamageSource damageSource = event.getSource();
    if (!(entity instanceof Player))
      return;
    Universal.onDeathEvent((Player) entity, damageSource);
  }

  // You can use SubscribeEvent and let the Event Bus discover methods to call
  @SubscribeEvent
  static public void onServerStarting(ServerAboutToStartEvent event) {
    server = event.getServer();
    Universal.setServer(server);
    Thread.startVirtualThread(
        () -> {
          Constants.LOGGER
              .info(event.getServer().getFile("config/" + Constants.MOD_ID).toAbsolutePath().toString());

          Path config = server.getFile("config/" + Constants.MOD_ID).toAbsolutePath();
          Constants.core = new Core(config, Universal::broadcastChatMessage);
          Constants.core.setLexicon(new ForgeLexicon(server.getServerVersion(), Constants.core.config.lang));
          Universal.setIconUpdateListener();
        });

  }

  @SubscribeEvent
  static public void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
    Universal.onJoinEvent(event.getEntity());
  }

  @SubscribeEvent
  static public void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
    Universal.onLeaveEvent(event.getEntity());
  }

  static public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(Universal.linkMeCommand);
  }

  @SubscribeEvent
  static public void registerCommands(RegisterCommandsEvent event) {
    register(event.getDispatcher());
  }
}
