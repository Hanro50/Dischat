package za.net.hanro50.dischat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.gson.annotations.Expose;

public class WebhookObjects {
    @Expose()
    public String content;
    @Expose
    public String username;
    @Expose
    public String avatar_url;

    public WebhookObjects() {
    }

    public WebhookObjects(Chater chater, String content) {
        this.content = content;
        this.username = chater.name;
        this.avatar_url = "https://mc-heads.net/avatar/" + chater.minecraftID.toString();
    }

    public void send(String webhook) {
        Constants.LOGGER.info(Constants.GSON.toJson(this));
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhook)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Constants.GSON.toJson(this)))
                .build();
        try {
            Constants.LOGGER.info(Constants.client.send(request, HttpResponse.BodyHandlers.ofString()).body());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
