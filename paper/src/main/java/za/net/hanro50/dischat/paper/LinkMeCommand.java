package za.net.hanro50.dischat.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import za.net.hanro50.dischat.core.Constants;

public class LinkMeCommand implements CommandExecutor {

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
      @NotNull String[] args) {
    if (!(sender instanceof Player)) {
      Constants.LOGGER.warn("Can only be ran as a player :(");
      return false;
    }
    Player player = (Player) sender;

    Constants.core.data.requestLink(player.getUniqueId().toString(), (code) -> {

      final TextComponent codeprt = new TextComponent(code);
      codeprt.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code));
      codeprt.setBold(true);
      codeprt.setColor(ChatColor.GREEN);
      player.spigot().sendMessage(
          new TextComponent(new TextComponent("Link code is <"), codeprt,
              new TextComponent(">\nUse the /link command on the bot to complete linking")));
    });
    return true;
  }
}