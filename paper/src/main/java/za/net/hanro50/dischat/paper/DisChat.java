package za.net.hanro50.dischat.paper;

import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;

@SuppressWarnings("deprecation")
public class DisChat extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    if (Constants.core != null)
      Constants.core.kill();

    Constants.core = new Core(this.getDataPath(), this.getServer().getMinecraftVersion(), this::onChat);
  }

  private void onChat(Chater chater, String content, Collection<Link> link) {
    if (chater.minecraftID != null) {
      UUID uuid = UUID.fromString(chater.minecraftID);
      Player player = this.getServer().getPlayer(uuid);
      if (player != null) {
        player.chat(content);
        return;
      }
    }

    this.getServer().broadcastMessage("<" + chater.name + "> " + content);
  }

  @Override
  public void onDisable() {
    if (Constants.core != null)
      Constants.core.kill();
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Constants.core.sendJoin(new Chater(player.getUniqueId().toString(), player.getName()));
  }

  @EventHandler
  public void onPlayerLeave(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    Constants.core.sendLeave(new Chater(player.getUniqueId().toString(), player.getName()));
  }

  @EventHandler
  public void onAdvancementDone(PlayerAdvancementDoneEvent event) {

  }

  @EventHandler
  public void onDeathEvent(EntityDeathEvent event) {

  }

  @EventHandler
  public void onChatEvent(AsyncPlayerChatEvent event) {

  }
}