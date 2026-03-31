package za.net.hanro50.dischat.core;

import java.util.TimerTask;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;

import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.awt.Color;
import java.time.Instant;
import java.util.Timer;

public class PersistentStatus extends TimerTask {
  private Core parent;
  private Message message;
  private static Timer timer;
  private static PersistentStatus instance;
  private static boolean pause;

  private PersistentStatus(Core parent, Message message) {
    this.parent = parent;
    this.message = message;
  }

  public static void kill() {
    if (timer != null)
      timer.cancel();

    if (instance != null)
      instance.cancel();
  }

  public static void schedule(Core parent, Message message, float delay) {
    kill();
    instance = new PersistentStatus(parent, message);
    timer = new Timer();
    Constants.LOGGER.debug("Initiating timer");

    timer.scheduleAtFixedRate(instance, 0, Math.max(Math.round(delay * 1000), 1));
  }

  public static void runUpdate() {

    if (instance == null)
      return;
    if (pause)
      pause = false;

    Thread.startVirtualThread(() -> {
      try {
        // Delay so the server has time to update.
        Thread.sleep(500);
      } catch (Exception e) {
      }
      instance.run();
    });

  }

  @Override
  public void run() {
    if (pause || parent.infoProvider == null || !parent.active)
      return;
    var info = parent.infoProvider.accept();

    if (info == null)
      return;

    var color = "#1591EA";
    var embedbuilder = new EmbedBuilder();
    var messageBuilder = new MessageEditBuilder();

    embedbuilder.setTitle("Server status");
    embedbuilder.setColor(Color.decode(color));

    var detail = "";

    detail += "Online players: " + info.onlinePlayerCount + "/" + info.maxPlayers + '\n';
    detail += "TPS: " + info.tps + '\n';
    detail += "Last Updated: <t:" + Instant.now().getEpochSecond() + ":S>";

    if (info.onlinePlayerCount <= 0) {
      Constants.LOGGER.debug("Hybernating status updates");
      pause = true;
      detail += " (paused)";
    }

    embedbuilder.setDescription(detail);

    if (info.icon != null) {
      messageBuilder.setFiles(FileUpload.fromData(info.icon, "server.png"));
      embedbuilder.setThumbnail("attachment://server.png");
    }

    messageBuilder.setEmbeds(embedbuilder.build());

    Constants.LOGGER.debug("Refreshing timer");
    message.editMessage(messageBuilder.build()).queue();

  }
}
