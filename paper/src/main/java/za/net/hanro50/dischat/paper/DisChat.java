package za.net.hanro50.dischat.paper;

import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;
import za.net.hanro50.dischat.core.Deathcause;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class DisChat extends JavaPlugin implements Listener {
  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    if (Constants.core != null)
      Constants.core.kill();
    // this.getCommand("linkme").setExecutor(new LinkMeCommand());

    Constants.core = new Core(Path.of(this.getDataFolder().toURI()), this::onChat);
    Constants.core.setLexicon(new Lexicon(this.getServer().getVersion(), Constants.core.config.lang));
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

    this.getServer().broadcast(Component.text("<" + chater.name + "> " + content).asComponent());
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
  void PlayerAdvancementDoneEvent(PlayerAdvancementDoneEvent event) {

    var displayInfo = event.getAdvancement().getDisplay();

    if (displayInfo == null || !displayInfo.doesAnnounceToChat())
      return;

    var title = "";
    var titleComponent = displayInfo.title();
    Constants.LOGGER.info(titleComponent.getClass().getName());
    if (titleComponent instanceof TranslatableComponent translatable)
      title = translatable.key();

    var discription = "";
    var descComponent = displayInfo.description();
    Constants.LOGGER.info(descComponent.getClass().getName());

    if (descComponent instanceof TranslatableComponent translatable)
      discription = translatable.key();

    String[] parts = event.getAdvancement().getKey().getKey().split("/");
    String namespace = event.getAdvancement().getKey().getNamespace();
    String category = namespace;
    if (parts.length > 1)
      category += ":" + parts[0];

    Player player = event.getPlayer();

    Constants.LOGGER.info(namespace + ":-:" + category + ":-:" + title + ":-:" + discription);

    Constants.core.sendAdvancement(
        new Chater(player.getUniqueId().toString(), player.getName()),
        namespace,
        category,
        title,
        discription);

  }

  @EventHandler
  void onPlayerDeath(PlayerDeathEvent event) {
    Deathcause cause = new Deathcause();
    Player playerVictim = event.getEntity();

    if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent) {
      EntityDamageByEntityEvent LastD = (EntityDamageByEntityEvent) event.getEntity().getLastDamageCause();
      Entity Damager = LastD.getDamager();
      cause.cause = NamespaceContainer.literal(Mappings.DMTranslate(LastD.getCause()));
      // TNT is annoying
      if (Damager instanceof TNTPrimed) {
        Damager = ((TNTPrimed) Damager).getSource();
      } else if (Damager instanceof Projectile) {
        ProjectileSource shooter = ((Projectile) Damager).getShooter();
        if (shooter instanceof Entity)
          Damager = (Entity) shooter;
      } else if (Damager instanceof FallingBlock) {
        // death.attack.anvil
        FallingBlock block = (FallingBlock) Damager;
        if (block.getBlockData().getMaterial() == Material.ANVIL)
          cause.cause = NamespaceContainer.literal("death.attack.anvil");

        Damager = null;
      }

      // TNT can sometimes do weird stuff
      if (Damager != null) {
        cause.attacker = NamespaceContainer.literal("entity.minecraft." + Damager.getType().toString().toLowerCase());
        if (Damager.getCustomName() != null)
          cause.name = Damager.getCustomName();

        if (Damager instanceof LivingEntity) {
          ItemMeta Meta = null;
          EntityEquipment inv = ((LivingEntity) Damager).getEquipment();
          Meta = inv.getItemInMainHand().getItemMeta();
          try {
            if (Meta.hasDisplayName())
              cause.itemName = Meta.getDisplayName();
          } catch (NullPointerException err) {
            Constants.LOGGER.info("Nullpointer? Assuming item has no meta data then");
          }
        }
      }
    } else {
      cause.cause = NamespaceContainer.literal(Mappings.DMTranslate(event.getEntity().getLastDamageCause().getCause()));
    }
    if (event.getEntity().getKiller() != null) {
      var player = event.getEntity().getKiller();
      cause.playerAttacker = new Chater(player.getUniqueId().toString(), player.getName());
      cause.name = cause.playerAttacker.name;
    }
    Chater victim = new Chater(playerVictim.getUniqueId().toString(), playerVictim.getName());
    Constants.core.sendDeath(victim, cause);
  }

  @EventHandler
  public void onChatEvent(AsyncPlayerChatEvent event) {
    Player player = event.getPlayer();
    Constants.core.sendChat(new Chater(player.getUniqueId().toString(), player.getName()), event.getMessage());
  }

}