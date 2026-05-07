package za.net.hanro50.dischat.core;

import com.google.gson.annotations.Expose;

public class Chater {
  @Expose
  public String minecraftID;
  @Expose
  public String name;
  @Expose
  public Long discordID;

  public Chater(String uuid, String name) {
    this.minecraftID = uuid;
    this.name = name;
  }

  public Chater(Long id, String name) {
    this.discordID = id;
    this.name = name;
  }
}
