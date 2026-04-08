package za.net.hanro50.dischat.paper;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.UUID;
import java.awt.RenderingHints;

import javax.imageio.ImageIO;

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
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.CachedServerIcon;

import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;
import za.net.hanro50.dischat.core.Deathcause;
import za.net.hanro50.dischat.core.InfoProvider;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class DisChat extends JavaPlugin implements Listener {

  CachedServerIcon guildIcon;

  private void startup() {
    Constants.core.addSetIconListener(this::onIcon);
    Constants.core.addInfoListener(this::onInfoRequest);
    Constants.core.addOnChatListener(this::onChat);
  }

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    if (Constants.core != null)
      Constants.core.kill();
    this.getCommand("linkme").setExecutor(new LinkMeCommand());
    Constants.core = new Core(Path.of(this.getDataFolder().toURI()), this::startup);
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

    this.getServer().broadcastMessage("<" + chater.name + "> " + content);
  }

  private void onIcon(Path path) {
    if (path == null)
      return;
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

      guildIcon = this.getServer().loadServerIcon(outputImage);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private InfoProvider.Result onInfoRequest() {
    var info = new InfoProvider.Result();
    var server = this.getServer();

    info.tps = server.getServerTickManager().getTickRate();
    info.maxPlayers = server.getMaxPlayers();
    info.onlinePlayerCount = server.getOnlinePlayers().size();

    var icon = guildIcon == null ? server.getServerIcon() : guildIcon;
    try {
      var field = icon.getClass().getField("value");

      var result = field.get(icon);
      if (result != null) {
        if (result instanceof byte[])
          info.icon = (byte[]) result;

        if (result instanceof String)
          info.icon = ((String) result).getBytes();
      }
    } catch (Throwable e) {
      e.printStackTrace();
    }
    // if (icon != null)
    // info.icon = icon.iconBytes();

    return info;
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
    String[] advancement = event.getAdvancement().getKey().getKey().split("/");
    if (advancement.length != 2) {
      Constants.LOGGER.warn("INVALID ADVANCEMENT: " + event.getAdvancement().getKey().getKey());
    }
    if (advancement[0].equalsIgnoreCase("recipes")) {
      return;
    }
    Player player = event.getPlayer();

    Constants.core.sendAdvancement(new Chater(player.getUniqueId().toString(),
        player.getName()), "minecraft",
        advancement[0],
        advancement[1]);

  }

  @EventHandler
  void onServerImage(ServerListPingEvent event) {
    if (guildIcon == null)
      return;
    event.setServerIcon(guildIcon);
    return;
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