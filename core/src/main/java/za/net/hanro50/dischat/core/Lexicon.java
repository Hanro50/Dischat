package za.net.hanro50.dischat.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.annotations.Expose;

import za.net.hanro50.dischat.core.Constants.MapContainer;

public class Lexicon {

  protected Map<String, Map<String, String>> info = new HashMap<>();

  protected String lang = "en_us";

  static public class Manifests {
    static public class Latest {
      @Expose
      String release;
      @Expose
      String snapshot;
    }

    static public class Version {
      @Expose
      String id;
      @Expose
      String url;
    }

    @Expose
    Latest latest;
    @Expose
    Version[] versions;
  }

  static public class GameInfo {
    static public class AssetIndex {
      @Expose
      String url;
    }

    @Expose
    AssetIndex assetIndex;
  }

  static public class Assets {
    static public class ObjectData {
      @Expose
      String hash;
      @Expose
      int size;
    }

    @Expose
    public Map<String, ObjectData> objects = new HashMap<>();
  }

  protected Lexicon() {
  }

  public String retrieve(NamespaceContainer namespace) {

    var language = info.get(namespace.origin);
    if (language == null) {
      Constants.LOGGER.warn("Could not find language for origin " + namespace.origin);
      return namespace.path;
    }
    String[] arr = namespace.path.split("\\.");
    ArrayList<String> lst = new ArrayList<>();
    for (String a : arr)
      lst.add(a);

    do {
      var key = String.join(".", lst);
      if (language.containsKey(key))
        return language.get(key);
      lst.removeLast();
    } while (lst.size() > 0);
    Constants.LOGGER.warn("Could not find " + namespace.origin + "::" + namespace.path);

    return namespace.path;
  }

  public Lexicon(String version, String code) {
    this.lang = code;
    try {
      var manifestRequest = HttpRequest.newBuilder()
          .uri(URI.create("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"))
          .header("Content-Type", "application/json")
          .GET()
          .build();
      var manifestsResponse = Constants.HTTP_CLIENT.send(manifestRequest, HttpResponse.BodyHandlers.ofString())
          .body();
      var manifests = Constants.GSON.fromJson(manifestsResponse, Manifests.class);
      Manifests.Version v;
      FindLang: {
        for (int i = 0; i < manifests.versions.length; i++) {
          v = manifests.versions[i];
          if (v.id.equals(version))
            break FindLang;
        }
        Constants.LOGGER.warn("COULD NOT FIND MANIFEST. DEFAULTING TO FIRST ENTRY :<");
        v = manifests.versions[0];
      }

      var gameInfoRequest = HttpRequest.newBuilder()
          .uri(URI.create(v.url))
          .header("Content-Type", "application/json")
          .GET()
          .build();
      var gameInfoResponse = Constants.HTTP_CLIENT.send(gameInfoRequest, HttpResponse.BodyHandlers.ofString())
          .body();
      var gameInfo = Constants.GSON.fromJson(gameInfoResponse, GameInfo.class);

      var assetIndexRequest = HttpRequest.newBuilder()
          .uri(URI.create(gameInfo.assetIndex.url))
          .header("Content-Type", "application/json")
          .GET()
          .build();
      var assetIndexResponse = Constants.HTTP_CLIENT
          .send(assetIndexRequest, HttpResponse.BodyHandlers.ofString())
          .body();

      var assets = Constants.GSON.fromJson(assetIndexResponse, Assets.class);

      var data = assets.objects.get("minecraft/lang/" + code + ".json");

      if (data == null) {
        Constants.LOGGER.warn("SELECTED LANGUAGE IS INVALID");
        data = assets.objects.get("minecraft/lang/en_gb.json");
      }
      var languageJsonRequest = HttpRequest.newBuilder()
          .uri(URI.create(
              "https://resources.download.minecraft.net/" + data.hash.substring(0, 2) + "/" + data.hash))
          .header("Content-Type", "application/json")
          .GET()
          .build();
      var languageJsonResponse = Constants.HTTP_CLIENT
          .send(languageJsonRequest, HttpResponse.BodyHandlers.ofString())
          .body();
      Constants.LOGGER.info("Lang url: https://resources.download.minecraft.net/" + data.hash.substring(0, 2)
          + "/" + data.hash);
      var mc = (Map<String, String>) Constants.GSON.fromJson(languageJsonResponse,
          MapContainer.class).getMap();
      info.put("minecraft", mc);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}
