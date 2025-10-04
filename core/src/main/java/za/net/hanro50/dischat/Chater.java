package za.net.hanro50.dischat;

public class Chater {
    public String minecraftID;
    public String name;
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
