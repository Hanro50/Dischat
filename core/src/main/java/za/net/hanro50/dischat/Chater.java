package za.net.hanro50.dischat;

public class Chater {
    String minecraftID;
    String name;
    Long discordID;

    Chater(String uuid, String name) {
        this.minecraftID = uuid;
        this.name = name;
    }

    Chater(Long id, String name) {
        this.discordID = id;
        this.name = name;
    }
}
