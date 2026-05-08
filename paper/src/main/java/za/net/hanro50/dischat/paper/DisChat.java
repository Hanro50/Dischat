package za.net.hanro50.dischat.paper;

import java.nio.file.Path;
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
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.CachedServerIcon;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import za.net.hanro50.dischat.chatx.ColorText;
import za.net.hanro50.dischat.chatx.LinkText;
import za.net.hanro50.dischat.chatx.Mention;
import za.net.hanro50.dischat.chatx.Message;
import za.net.hanro50.dischat.objects.Chater;
import za.net.hanro50.dischat.objects.Deathcause;
import za.net.hanro50.dischat.objects.InfoProvider;
import za.net.hanro50.dischat.lang.NamespaceContainer;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Core;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class DisChat extends JavaPlugin implements Listener {
  File serverImage = new java.io.File("server-icon.png");
  private CachedServerIcon customIcon;

  @Override
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this, this);
    if (Constants.core != null)
      Constants.core.kill();
    this.getCommand("linkme").setExecutor(new LinkMeCommand());

    Constants.core = new Core(Path.of(this.getDataFolder().toURI()), this::onLaunch);
    Constants.core.setLexicon(new PaperLexicon(this.getServer().getVersion(), Constants.core.config.lang));
  }

  private void onLaunch(Core core) {
    core.addSetIconListener(this::setStatusIcon);
    core.setChatReponder(this::broadcastChatMessage);
    core.setInfoProvider(this::getInfo);
    core.updateIcon();
  }

  private void setStatusIcon(Path path) {

    BufferedImage bufferedImage;
    try {
      bufferedImage = ImageIO.read(path.toFile());
      serverImage = new File(this.getDataFolder(), "server.png");

      BufferedImage outputImage = new BufferedImage(64, 64, bufferedImage.getType());
      Graphics2D g2d = outputImage.createGraphics();
      g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
      g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      g2d.drawImage(bufferedImage, 0, 0, 64, 64, null);
      g2d.dispose();

      ImageIO.write(outputImage, "PNG", serverImage);
      this.customIcon = Bukkit.loadServerIcon(outputImage);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private InfoProvider.Result getInfo() {
    var info = new InfoProvider.Result();
    if (Bukkit.getServer() == null)
      return null;

    double[] tps = this.getServer().getTPS();
    info.tps = tps[0]; // Average TPS (1 minute)
    info.maxPlayers = this.getServer().getMaxPlayers();
    info.onlinePlayerCount = this.getServer().getOnlinePlayers().size();

    try {
      if (serverImage.exists())
        info.icon = java.nio.file.Files.readAllBytes(serverImage.toPath());
    } catch (java.io.IOException e) {
    }

    return info;
  }

  private String getMcUsername(Chater chater) {
    if (chater.minecraftID != null) {
      UUID uuid = UUID.fromString(chater.minecraftID);
      var plr = this.getServer().getOfflinePlayer(uuid);
      if (plr != null && plr.hasPlayedBefore())
        return plr.getName();

    }
    return chater.name;
  }

  public void broadcastChatMessage(Chater chater, Message message) {
    Thread.startVirtualThread(() -> {
      TextComponent.Builder base = Component.text();
      for (var element : message.elements) {
        if (element instanceof Mention mention) {
          String chatter = "@" + getMcUsername(mention.person);
          TextComponent display = Component.text(chatter).decorate(TextDecoration.BOLD);

          if (mention.color != null) {
            display.color(TextColor.color(mention.color.getRGB()));
          }

          base.append(display);
          continue;
        }
        if (element instanceof LinkText link) {

          TextComponent linkComponent = Component.text(link.content)
              .color(NamedTextColor.GREEN)
              .clickEvent(ClickEvent.openUrl(link.url))
              .hoverEvent(HoverEvent.showText(Component.translatable("chat.link.open")))
              .insertion(link.url);

          base.append(linkComponent);
          continue;
        }
        if (element instanceof ColorText text) {
          TextComponent display = Component.text(text.content).decorate(TextDecoration.BOLD);

          if (text.color != null) {
            display.color(TextColor.color(text.color.getRGB()));
          }

          base.append(display);
          continue;
        }
        base.append(Component.text(element.content));
      }

      final var name = getMcUsername(chater);

      final var f = Component.text("<" + name + "> ").append(base);
      this.getServer().broadcast(f);
    });
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

  @EventHandler
  public void onServerPing(ServerListPingEvent event) {
    if (customIcon != null)
      event.setServerIcon(customIcon);

  }
}