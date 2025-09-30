package za.net.hanro50.dischat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;

import com.google.gson.annotations.Expose;

public final class Data {
    @Expose
    public String channel;
    @Expose
    public HashMap<Long, String> DiscordToMinecraft = new HashMap<>();
    public HashMap<String, Long> MinecraftToDiscord = new HashMap<>();
    public HashMap<String, String> LinkPlayer = new HashMap<>();
    protected File file;

    public Data() {
    }

    public Data(File file) {
        this.file = file;
    }

    public static Data deserialize(File file) {
        if (!file.exists())
            return new Data(file);
        try {
            Path path = Path.of(file.toURI());

            String info = Files.readString(path);

            Data data = Constants.GSON.fromJson(info, Data.class);

            final Data result = new Data(file);

            result.channel = data.channel;
            result.DiscordToMinecraft.putAll(data.DiscordToMinecraft);
            data.DiscordToMinecraft.forEach((v, k) -> result.MinecraftToDiscord.put(k, v));
            Constants.LOGGER.info(data.DiscordToMinecraft.size() + "");
            return result;
        } catch (IOException exception) {
            exception.printStackTrace();
        }

        return new Data(file);
    }

    public void requestLink(String uuid, Consumer<String> callback) {
        String code;
        do {
            code = String.format("%06d", (Math.abs(Constants.random.nextInt()) % 1000000));
        } while (LinkPlayer.keySet().contains(code));

        LinkPlayer.put(code, uuid);
        callback.accept(code);
    }

    public boolean unlink(String uuid) {
        if (!MinecraftToDiscord.containsKey(uuid))
            return false;
        Long discord = MinecraftToDiscord.get(uuid);

        MinecraftToDiscord.remove(uuid);
        DiscordToMinecraft.remove(discord);
        save();
        return true;
    }

    public boolean unlink(Long discord) {
        if (!DiscordToMinecraft.containsKey(discord))
            return false;
        String uuid = DiscordToMinecraft.get(discord);

        MinecraftToDiscord.remove(uuid);
        DiscordToMinecraft.remove(discord);
        save();
        return true;
    }

    public void save() {
        try {
            String json = Constants.GSON.toJson(this);
            BufferedWriter writer = new BufferedWriter(new FileWriter(file.getAbsolutePath()));
            writer.write(json);
            writer.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public boolean acceptLink(long discordID, String code) {
        if (!LinkPlayer.containsKey(code))
            return false;
        String uuid = LinkPlayer.get(code);
        DiscordToMinecraft.put(discordID, uuid);
        MinecraftToDiscord.put(uuid, discordID);
        save();
        return true;
    }

}
