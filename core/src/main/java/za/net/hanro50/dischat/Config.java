package za.net.hanro50.dischat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.annotations.Expose;

public class Config {
  @Expose
  public Boolean joinMessages = true;
  @Expose
  public Boolean leaveMessages = true;
  @Expose
  public Boolean deathMessages = true;
  @Expose
  public Boolean advancementMessages = true;
  @Expose
  public String token = "";

  @Expose
  public String lang = "en_gb";

  @Expose
  String DO_NOT_CHANGE_VERSION = "1.0.3";

  public static Config deserialize(File file) {
    final Config result = new Config();
    if (!file.exists()) {
      result.create(file);
      return result;
    }
    try {
      Path path = Path.of(file.toURI());

      String info = Files.readString(path);

      Config config = Constants.GSON.fromJson(info, Config.class);

      result.token = config.token;
      result.lang = config.lang;

      result.advancementMessages = config.advancementMessages;
      result.deathMessages = config.deathMessages;
      result.leaveMessages = config.leaveMessages;
      result.joinMessages = config.joinMessages;

      if (!config.DO_NOT_CHANGE_VERSION.equals(result.DO_NOT_CHANGE_VERSION)) {
        Constants.LOGGER.info("UPDATING CONFIG!");
        result.create(file);
      }
      return result;
    } catch (IOException exception) {
      exception.printStackTrace();
    }

    return result;
  }

  private void create(File file) {
    try {
      String json = Constants.GSON.toJson(this);
      BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
      writer.write(json);
      writer.close();
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }
}
