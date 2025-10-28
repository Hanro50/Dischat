package za.net.hanro50.dischat.core;

import java.io.File;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ApplicationInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import za.net.hanro50.dischat.core.ChatConsumer.Link;

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

  public class MessageReceiveListener extends ListenerAdapter {

    Core parent;

    private MessageReceiveListener(Core parent) {
      this.parent = parent;
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
      parent.loadCommands(event.getGuild());
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {

      OptionMapping opts;
      switch (event.getName()) {
        case "info":
          if (!event.isFromGuild()
              || (channel != null && channel.getGuild().getIdLong() != event.getGuild().getIdLong())) {
            event.reply("This command may only be ran in the guild the bot is linked to").queue();
            return;
          }

          event.reply("WIP\n").setEphemeral(true).queue();
          break;
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

          loadCommands(member.getGuild());

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

  public void loadCommands(Guild guild) {
    guild.upsertCommand("link", "link your account").addOption(OptionType.STRING, "code",
        "Link you minecraft account to your discord one").queue();
    guild.upsertCommand("info", "get server info").queue();
    guild.upsertCommand("setup", "Setup the bot again")
        .addOption(OptionType.BOOLEAN, "sure", "Are you sure you really wanna do this?").queue();
  }

  public void setLexicon(Lexicon lexicon) {
    this.lexicon = lexicon;
  }

  public Core(Path path, ChatConsumer onChat) {

    this.onRespond = onChat;
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
    }

    jda = JDABuilder.createLight(config.token, GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_WEBHOOKS, GatewayIntent.GUILD_MEMBERS)
        .addEventListeners(new MessageReceiveListener(this))
        .build();

    try {
      jda.awaitReady();
      jda.upsertCommand("setup", "Setup the bot again")
          .addOption(OptionType.BOOLEAN, "sure", "Are you sure you really wanna do this?").queue();
      jda.upsertCommand("link", "link your account").addOption(OptionType.STRING, "code",
          "Link you minecraft account to your discord one").queue();
      jda.upsertCommand("info", "get server info").queue();
      info = jda.retrieveApplicationInfo().complete();

      setChannel(data.channel);

    } catch (InterruptedException e) {
      e.printStackTrace();
    }

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
    if (!active || !config.joinMessages) {
      return;
    }
    chater.discordID = data.MinecraftToDiscord.get(chater.minecraftID);
    sendEmbed(chater, lexicon.retrieve(NamespaceContainer.literal("multiplayer.player.joined")), "#04ff00");
  }

  public void sendLeave(Chater chater) {
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
    var color = Constants.getAdvancementColor(category);

    var id = "advancements." + category + "." + advancement;

    var titleNS = new NamespaceContainer(namespace, id + ".title");
    var descriptionNS = new NamespaceContainer(namespace, id + ".description");
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
    if (jda != null)
      try {
        jda.awaitShutdown();
      } catch (InterruptedException e) {

        e.printStackTrace();
      }
  }

}
