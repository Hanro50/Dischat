package za.net.hanro50.dischat.neoforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarResource;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Constants.MapContainer;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class NeoForgeLexicon extends Lexicon {
  protected Map<String, String> decode(JarResource resource, String origin) {
    String data;
    try {
      data = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
      return Constants.GSON.fromJson(data, MapContainer.class).getMap();
    } catch (IOException e) {
      e.printStackTrace();
    }

    return new HashMap<>();

  }

  public NeoForgeLexicon(String version, String code) {
    super(version, code);
  }

  public String retrieve(NamespaceContainer namespace) {
    if (this.info.containsKey(namespace.origin))
      return super.retrieve(namespace);
    Constants.LOGGER.info("Loading language file from " + namespace.origin);
    var modFile = ModList.get().getModFileById(namespace.origin);
    Map<String, String> result = new HashMap<>();

    var resource = modFile.getFile().getContents().get("/assets/" + namespace.origin + "/lang/" +
        this.lang + ".json");
    if (resource != null) {
      result.putAll(decode(resource, namespace.origin));
      return super.retrieve(namespace);
    }

    resource = modFile.getFile().getContents().get("/assets/" + namespace.origin + "/lang/en_us.json");
    if (resource != null)
      result.putAll(decode(resource, namespace.origin));
    super.info.put(namespace.origin, result);
    return super.retrieve(namespace);
  }
}
