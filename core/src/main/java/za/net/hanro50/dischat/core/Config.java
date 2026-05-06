package za.net.hanro50.dischat.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.annotations.Expose;

public class Config {
  @Expose
  public Boolean useGuildIcon = false;
  @Expose
  public Boolean joinMessages = true;
  @Expose
  public Boolean leaveMessages = true;
  @Expose
  public Boolean deathMessages = true;
  @Expose
  public Boolean advancementMessages = true;
  @Expose
  public Boolean silentEmbeds = true;
  @Expose
  public String token = "";
  @Expose
  public String lang = "en_gb";
  @Expose
  public float statusUpdateInterval = 62.512f;
  @Expose
  String DO_NOT_CHANGE_VERSION = "1.0.7";

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

      for (Field field : Config.class.getDeclaredFields()) {
        if (field.getName().equals("DO_NOT_CHANGE_VERSION") || !field.isAnnotationPresent(Expose.class)) {
          Constants.LOGGER.debug("Config deserialization: Skipping field: " + field.getName());
          continue;
        }
        try {
          field.set(result, field.get(config));
        } catch (IllegalArgumentException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }

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
