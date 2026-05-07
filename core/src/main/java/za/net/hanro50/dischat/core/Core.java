package za.net.hanro50.dischat.core;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import java.awt.Color;

public class Core {
  Consumer<Core> onLaunch;

  JDA jda;
  public Data data;
  public Config config;
  boolean active = false;
  ApplicationInfo info;
  TextChannel channel;
  String webhookurl;
  Lexicon lexicon;
  Consumer<Path> icon;
  ChatConsumer onRespond;
  InfoProvider infoProvider;

  public void loadCommands(BiFunction<String, String, CommandCreateAction> upsertCommand) {
    upsertCommand.apply("link", "link your account").addOption(OptionType.STRING, "code",
        "Link you minecraft account to your discord one").queue();
    upsertCommand.apply("info", "get server info").queue();
    upsertCommand.apply("setup", "Setup the bot again")
        .addOption(OptionType.BOOLEAN, "sure", "Are you sure you really wanna do this?").queue();

    upsertCommand.apply("update", "Updates the slash commands - run after a mod update").queue();

    upsertCommand.apply("set-status-message", "Create a persistent status message that'll update every minute")
        .queue();
  }

  public void setLexicon(Lexicon lexicon) {
    this.lexicon = lexicon;
  }

  public void setChatReponder(ChatConsumer onRespond) {
    this.onRespond = onRespond;
  }

  public void setInfoProvider(InfoProvider infoProvider) {
    this.infoProvider = infoProvider;
  }

  public Core(Path path, Consumer<Core> onLaunch) {

    File file = path.toFile();

    if (!file.exists()) {
      file.mkdirs();
    }
    if (!file.isDirectory()) {
      file.delete();
      file.mkdirs();
    }

    config = Config.deserialize(new File(file, "config.json"));

    data = Data.deserialize(new File(file, "data.json"));

    if (config.token.length() < 5) {
      Constants.LOGGER.error("Invalid token. Please provide a valid discord token. \nConfig file:"
          + new File(file, "config.json").getAbsolutePath());
      return;
    }

    // Avoids a stall on shutdown.
    Thread.startVirtualThread(
        () -> {
          try {
            jda = JDABuilder.createLight(config.token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(new MessageReceiveListener(this))
                .build();

            jda.awaitReady();
            info = jda.retrieveApplicationInfo().complete();
            loadCommands(jda::upsertCommand);

            setChannel(data.channel);
            onLaunch.accept(this);
          } catch (IllegalArgumentException e) {
            Constants.LOGGER.error("Could not load token :(");
            e.printStackTrace();
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          if (data.infoMessage != null && data.infoMessage.contains("-")) {
            var prts = data.infoMessage.split("-");
            var channel = jda.getChannelById(StandardGuildMessageChannel.class, prts[0]);
            channel.retrieveMessageById(prts[1]).queue(
                (message) -> PersistentStatus.schedule(this, message, config.statusUpdateInterval));
          }
        });
  }

  public void setChannel(String channelId) {
    active = false;
    if (channelId == null) {
      String invite = info.getInviteUrl(
          Permission.MANAGE_WEBHOOKS,
          Permission.MESSAGE_SEND,
          Permission.USE_APPLICATION_COMMANDS,
          Permission.VIEW_CHANNEL);

      Constants.LOGGER.info(
          "Discord bot invite: " + invite + "\nThen run !setup in the guild you want to use the bot in");

      return;
    }

    channel = jda.getTextChannelById(channelId);

    if (channel == null) {
      Constants.LOGGER.error("Could not find channel :(. Run the !setup command again");
      data.channel = null;
      data.save();
      return;
    }

    List<Webhook> hooks = channel.retrieveWebhooks().complete();
    for (Webhook hook : hooks) {
      Member owner = hook.getOwner();
      if (owner == null)
        continue;
      if (owner.getIdLong() == info.getIdLong()) {
        webhookurl = hook.getUrl();
        active = true;
        return;
      }
    }

    webhookurl = channel.createWebhook(Constants.MOD_ID).complete().getUrl();
    active = true;
    updateIcon();
  }

  public void sendChat(Chater chater, String message) {
    Constants.LOGGER.info(message + (active ? "T" : "N"));
    if (!active)
      return;
    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);

    WebhookObjects hook = new WebhookObjects(chater, message);

    if (chater.discordID != null) {
      try {
        Member member = channel.getGuild().retrieveMemberById(chater.discordID).complete();
        hook.username = member.getEffectiveName();
        hook.avatar_url = member.getEffectiveAvatarUrl();
      } catch (RuntimeException ignore) {
        ignore.printStackTrace();
      }
    }
    hook.send(webhookurl);
  }

  public void sendDeath(Chater chater, Deathcause cause) {
    var color = "#f89500";
    if (!active || !config.deathMessages)
      return;
    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);

    if (cause.playerAttacker != null) {
      cause.playerAttacker.discordID = data.MinecraftToDiscord.get(cause.playerAttacker.minecraftID);
      if (cause.playerAttacker.discordID != null) {
        Member member = channel.getGuild().retrieveMemberById(cause.playerAttacker.discordID).complete();
        cause.name = member.getEffectiveName();
      }
    }

    else if (cause.name == null) {
      if (cause.attacker == null) {
        cause.name = "Herobrine";
      } else {
        cause.name = lexicon.retrieve(cause.attacker);
        if (cause.attacker.path.equals("death.attack.badRespawnPoint.link")) {
          cause.name = "[" + cause.name + "]";
          color = "#ae00ff";
        }
      }
    }

    this.sendEmbed(chater, lexicon.retrieve(cause.cause), color, cause.name, cause.itemName);
  }

  public void sendEmbed(Chater chater, String text, String color, String... options) {
    var name = chater.name;
    var pfp = "https://mc-heads.net/avatar/" + chater.minecraftID.toString();
    var link = "https://minecraftuuid.com/player/" + chater.minecraftID.toString();
    if (chater.discordID != null) {
      try {
        Member member = channel.getGuild().retrieveMemberById(chater.discordID).complete();
        name = member.getEffectiveName();
        link = "https://discord.com/users/" + chater.discordID;
        pfp = member.getEffectiveAvatarUrl();
      } catch (RuntimeException ignore) {
        ignore.printStackTrace();
      }
    }
    List<Object> args = new ArrayList<>();
    args.add(name);
    for (String option : options)
      args.add(option);

    channel.sendMessageEmbeds(
        new EmbedBuilder()
            .setAuthor(String.format(text, args.toArray()), link, pfp)
            .setColor(Color.decode(color)).build())
        .setSuppressedNotifications(config.silentEmbeds)
        .queue();
  }

  public void sendJoin(Chater chater) {
    PersistentStatus.runUpdate();
    if (!active || !config.joinMessages)
      return;

    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);
    sendEmbed(chater, lexicon.retrieve(NamespaceContainer.literal("multiplayer.player.joined")), "#04ff00");
  }

  public void sendLeave(Chater chater) {
    PersistentStatus.runUpdate();
    if (!active || !config.leaveMessages)
      return;
    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);
    sendEmbed(chater, lexicon.retrieve(NamespaceContainer.literal("multiplayer.player.left")), "#ff0000");
  }

  public void sendAdvancement(Chater chater, String namespace, String category, String title, String description) {
    Constants.LOGGER.debug("Got this advancement::" + namespace + ":" + category + ":" + title + ":" + description);
    if (!active || !config.advancementMessages)
      return;
    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);
    var name = chater.name;
    var pfp = "https://mc-heads.net/avatar/" + chater.minecraftID.toString();
    var link = "https://minecraftuuid.com/player/" + chater.minecraftID.toString();
    if (chater.discordID != null) {
      try {
        Member member = channel.getGuild().retrieveMemberById(chater.discordID).complete();
        name = member.getEffectiveName();
        link = "https://discord.com/users/" + chater.discordID;
        pfp = member.getEffectiveAvatarUrl();
      } catch (RuntimeException ignore) {
        ignore.printStackTrace();
      }
    }
    Constants.LOGGER.info(category);
    var color = Constants.getAdvancementString(category);

    var titleNS = new NamespaceContainer(namespace, title);
    var descriptionNS = new NamespaceContainer(namespace, description);

    var disc = lexicon.retrieve(descriptionNS).replaceAll("\n§7", "\n-# ");

    channel.sendMessageEmbeds(
        new EmbedBuilder()
            .setAuthor(name, link, pfp)
            .setTitle(lexicon.retrieve(titleNS))
            .setDescription(disc)
            .setColor(Color.decode(color)).build())
        .setSuppressedNotifications(config.silentEmbeds)
        .queue();
  }

  public void updateIcon() {
    if (!active || channel == null || icon == null || !config.useGuildIcon)
      return;
    Thread.startVirtualThread(
        () -> {
          try {
            Path targetFile = Path.of(System.getProperty("java.io.tmpdir"),
                "guild_icon_" + this.channel.getGuild().getId() + ".png");

            icon.accept(
                this.channel.getGuild().getIcon().downloadToPath(targetFile).get());
          } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
          }
        });

  }

  public void addSetIconListener(Consumer<Path> icon) {
    this.icon = icon;
    updateIcon();
  }

  public void kill() {
    active = false;
    Constants.LOGGER.info("Starting to shutdown");
    if (jda != null) {
      PersistentStatus.kill();
      jda.shutdown();
      try {
        jda.awaitShutdown(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Constants.LOGGER.info("All connections closed!");
    }
  }

}
