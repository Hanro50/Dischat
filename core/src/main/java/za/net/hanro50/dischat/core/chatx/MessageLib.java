package za.net.hanro50.dischat.core.chatx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import za.net.hanro50.dischat.core.Chater;
import za.net.hanro50.dischat.core.Constants;

public class MessageLib {
  static private PlainText parseLink(String token) {

    token = token.substring(1);
    token = token.substring(0, token.length() - 2);

    var parts = token.split("\\]\\(");

    if (parts.length != 2 || parts[0].startsWith("http"))
      return new LinkText(token, false);

    return new LinkText(parts[0], parts[1], false);

  }

  static public Message build(Chater chater, net.dv8tion.jda.api.entities.Message base) {

    List<PlainText> items = new ArrayList<>();
    var raw = base.getContentRaw();

    var mentions = base.getMentions();
    String markdownLinkRegex = "\\[([^\\]]+)\\]\\((https?://[^\\s)]+)\\)";
    String discordRegex = "<#[0-9]+>|<@&?[0-9]+>|<a?:[a-zA-Z0-9_]+:[0-9]+>";
    String urlRegex = "https?://[a-zA-Z0-9./?=&_-]+";
    Pattern pattern = Pattern.compile("(" + markdownLinkRegex + "|" + discordRegex + "|" + urlRegex + ")");
    Matcher matcher = pattern.matcher(raw);

    var replacements = new HashMap<String, PlainText>();

    for (var emote : mentions.getCustomEmojis())
      replacements.put(emote.getAsMention(), new LinkText(":" + emote.getName() + ":", emote.getImageUrl(), false));

    for (var channel : mentions.getChannels())
      replacements.put(channel.getAsMention(), new LinkText("#" + channel.getName(), channel.getJumpUrl(), false));

    for (var role : mentions.getRoles())
      replacements.put(role.getAsMention(), new ColorText("@" + role.getName(), role.getColor()));

    for (var member : mentions.getMembers()) {
      var m = new Chater(member.getIdLong(), member.getEffectiveName());
      m.minecraftID = Constants.core.data.DiscordToMinecraft.get(member.getIdLong());
      replacements.put(member.getAsMention(), new Mention(m, member.getColor()));
    }

    int lastIndex = 0;

    while (matcher.find()) {
      if (matcher.start() > lastIndex)
        items.add(new PlainText(raw.substring(lastIndex, matcher.start())));

      String token = matcher.group();
      if (token.startsWith("[") && token.endsWith(")"))
        items.add(parseLink(token));
      else if (token.startsWith("http://") || token.startsWith("https://"))
        items.add(new LinkText(token, false));
      else
        items.add(replacements.getOrDefault(token, new ColorText(token, Color.red)));

      lastIndex = matcher.end();
    }
    var lastBit = raw.substring(lastIndex);
    if (lastBit.length() > 0)
      items.add(new PlainText(lastBit));

    for (var sticker : base.getStickers()) {
      var link = sticker.getIconUrl();
      if (link.endsWith(".gif"))
        link.substring(0, link.length() - 4);
      items.add(new LinkText("[" + sticker.getName() + "]", link, Constants.core.config.printStickers));
    }

    for (var attachment : base.getAttachments())
      items.add(new LinkText("[" + attachment.getFileName() + "]", attachment.getProxyUrl(), false));

    return new Message(chater, items);
  }
}
