package za.net.hanro50.dischat.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import za.net.hanro50.dischat.core.ChatConsumer.Link;
import za.net.hanro50.dischat.core.Core.MessageReceiveListener;

import java.awt.Color;

public class Core {
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

  public class MessageReceiveListener extends ListenerAdapter {

    Core parent;

    private MessageReceiveListener(Core parent) {
      this.parent = parent;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
      parent.loadCommands(event.getGuild()::upsertCommand);
    }

    public MessageCreateData infoDialog() {

      var info = parent.infoProvider.accept();

      var color = "#1591EA";

      var embedbuilder = new EmbedBuilder();

      var messageBuilder = new MessageCreateBuilder();

      if (info == null)
        return messageBuilder.setContent("Could not gather info").build();

      embedbuilder.setTitle("Server status");

      embedbuilder.setColor(Color.decode(color));

      var detail = "";

      detail += "Online players: " + info.onlinePlayerCount + "/" + info.maxPlayers + '\n';

      detail += "TPS: " + info.tps;

      embedbuilder.setDescription(detail);

      if (info.icon != null) {
        messageBuilder.addFiles(FileUpload.fromData(info.icon, "server.png"));
        embedbuilder.setThumbnail("attachment://server.png");
      }
      messageBuilder.addEmbeds(embedbuilder.build());

      return messageBuilder.build();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

      OptionMapping opts;
      switch (event.getName()) {
        case "info":

          if (parent.infoProvider == null) {
            event.reply("The DisChat implementation didn't provide a hook for this feature").setEphemeral(true)
                .queue();
            return;
          }

          event.reply(infoDialog()).setEphemeral(true).queue();

          return;
        case "setup":
          if (!event.isFromGuild()) {
            event.reply("Please run this command in a guild channel").queue();
            return;
          }
          if (!event.getUser().getId().equals(info.getOwner().getId())) {
            event.reply("Only the bot owner can run this command").setEphemeral(true).queue();
            return;
          }
          opts = event.getOption("sure");
          Boolean sure = opts != null && opts.getAsBoolean();

          if (channel != null && !sure) {
            event.reply("Are you sure you want to do this?\nMake the sure option true to confirm it.")
                .setEphemeral(true).queue();
            return;
          }
          parent.data.channel = event.getChannel().getId();
          parent.data.save();
          setChannel(parent.data.channel);
          event.reply("Channel has been set!").queue();

          break;

        case "link":
          Long user = event.getUser().getIdLong();
          opts = event.getOption("code");
          if (opts == null) {
            event.reply("Please provide the code paramenter. Run /linkme ingame to get that code")
                .setEphemeral(true).queue();
            return;
          }

          String code = event.getOption("code").getAsString();
          if (parent.data.acceptLink(user, code)) {
            event.reply("Linked account!").setEphemeral(true).queue();
          } else {
            event.reply("Could not link account. Run /linkme ingame").setEphemeral(true).queue();
          }
          break;
        case "set-status-message":
          if (!event.getUser().getId().equals(info.getOwner().getId())) {
            event.reply("Only the bot owner can run this command").setEphemeral(true).queue();
            return;
          }
          if (parent.infoProvider == null) {
            event.reply("The DisChat implementation didn't provide a hook for this feature").setEphemeral(true)
                .queue();
            return;
          }
          event.reply("Will create message").setEphemeral(true).queue();

          event.getChannel().sendMessage(infoDialog()).queue((message) -> {

            data.infoMessage = message.getChannelId() + "-" + message.getId();
            data.save();
            PersistentStatus.schedule(parent, message, config.statusUpdateInterval);
          });

          return;
        case "update":
          event.reply("Updating slash commands").setEphemeral(true)
              .queue();
          parent.loadCommands(event.getGuild()::upsertCommand);
          return;
        default:
          if (event.isGuildCommand())
            event.getGuild().deleteCommandById(event.getCommandId());
          else
            parent.jda.deleteCommandById(event.getCommandId());

          event.reply("Unknown command? - Will remove it").setEphemeral(true).queue();
      }

    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
      if (!active)
        return;
      if (!event.isFromGuild())
        return;
      if (event.isWebhookMessage())
        return;

      String content = event.getMessage().getContentDisplay();
      List<StickerItem> stickers = event.getMessage().getStickers();
      List<Attachment> attachments = event.getMessage().getAttachments();

      if (content.length() < 1 && stickers.size() < 1 && attachments.size() < 1)
        return;

      Member member = event.getMember();

      if (member.getId().equals(info.getOwner().getId()) && channel == null && content.equals("!setup")) {
        try {
          setChannel(event.getChannel().getId());

          loadCommands(member.getGuild()::upsertCommand);

          parent.data.channel = event.getChannel().getId();
          parent.data.save();
          event.getChannel().sendMessage("Channel has been set!").queue();

        } catch (PermissionException exception) {
          event.getChannel().sendMessage("Missing required permission :(").queue();
        }
      }

      if (parent.channel == null || event.getChannel().getIdLong() != parent.channel.getIdLong())
        return;

      Chater chater = new Chater(member.getIdLong(), member.getEffectiveName());
      chater.minecraftID = parent.data.DiscordToMinecraft.get(member.getIdLong());
      List<Link> links = new ArrayList<>();
      int index = 0;
      for (StickerItem sticker : stickers) {
        Link link = new Link();
        link.link = sticker.getIconUrl();
        link.name = sticker.getName();
        link.id = index++;
        links.add(link);
      }

      for (Attachment attachment : attachments) {
        Link link = new Link();
        link.link = attachment.getUrl();
        link.name = attachment.getFileName();
        link.id = index++;
        links.add(link);

      }
      this.parent.onRespond.accept(chater, content, links);
    }
  }

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

  public Core(Path path, ChatConsumer onChat) {
    this(path, onChat, null);
  }

  public Core(Path path, ChatConsumer onChat, InfoProvider serverInfo) {
    this.onRespond = onChat;
    this.infoProvider = serverInfo;
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

    channel.sendMessageEmbeds(new EmbedBuilder().setAuthor(String.format(text, args.toArray()), link, pfp)
        .setColor(Color.decode(color)).build())
        .queue();
  }

  public void sendJoin(Chater chater) {
    PersistentStatus.runUpdate();
    if (!active || !config.joinMessages) {
      return;
    }
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

  public void sendAdvancement(Chater chater, String namespace, String category, String advancement) {
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
    var color = Constants.AdvancementColorDict.getOrDefault(category, "#0000ff");

    var titleNS = new NamespaceContainer(namespace, advancement + ".title");
    var descriptionNS = new NamespaceContainer(namespace, advancement + ".desc");
    channel.sendMessageEmbeds(
        new EmbedBuilder()
            .setAuthor(name, link, pfp)
            .setTitle(lexicon.retrieve(titleNS))
            .setDescription(lexicon.retrieve(descriptionNS))
            .setColor(Color.decode(color)).build())
        .queue();
  }

  public void updateIcon() {
    if (!active || channel == null || icon == null || !config.useGuildIcon)
      return;
    Thread.startVirtualThread(
        () -> {
          try {
            icon.accept(this.channel.getGuild().getIcon().downloadToPath().get());
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
