package za.net.hanro50.dischat.core;

import java.awt.Color;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import za.net.hanro50.dischat.core.chatx.MessageLib;

public class MessageReceiveListener extends ListenerAdapter {

  Core parent;

  MessageReceiveListener(Core parent) {
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
        if (!event.getUser().getId().equals(parent.info.getOwner().getId())) {
          event.reply("Only the bot owner can run this command").setEphemeral(true).queue();
          return;
        }
        opts = event.getOption("sure");
        Boolean sure = opts != null && opts.getAsBoolean();

        if (parent.channel != null && !sure) {
          event.reply("Are you sure you want to do this?\nMake the sure option true to confirm it.")
              .setEphemeral(true).queue();
          return;
        }
        parent.data.channel = event.getChannel().getId();
        parent.data.save();
        parent.setChannel(parent.data.channel);
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
        if (!event.getUser().getId().equals(parent.info.getOwner().getId())) {
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

          parent.data.infoMessage = message.getChannelId() + "-" + message.getId();
          parent.data.save();
          PersistentStatus.schedule(parent, message, parent.config.statusUpdateInterval);
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
  public void onGuildUpdateIcon(GuildUpdateIconEvent event) {
    Thread.startVirtualThread(() -> parent.updateIcon());
  }

  @Override
  public void onMessageReceived(MessageReceivedEvent event) {
    if (!parent.active)
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

    if (member.getId().equals(parent.info.getOwner().getId()) && parent.channel == null && content.equals("!setup")) {
      try {
        parent.setChannel(event.getChannel().getId());

        parent.loadCommands(member.getGuild()::upsertCommand);

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

    var data = MessageLib.build(chater, event.getMessage());
    this.parent.onRespond.accept(chater, data);
  }
}