package za.net.hanro50.dischat.neoforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import net.neoforged.fml.ModList;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Constants.MapContainer;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class NeoForgeLexicon extends Lexicon {
  public NeoForgeLexicon(String version, String code) {
    super(version, code);
  }

  protected Map<String, String> decode(Path path, String origin) throws IOException {
    return Constants.GSON.fromJson(Files.readString(path), MapContainer.class).getMap();
  }

  public String retrieve(NamespaceContainer namespace) {
    if (this.info.containsKey(namespace.origin))
      return super.retrieve(namespace);
    Constants.LOGGER.info("Loading language file from " + namespace.origin);
    var modFile = ModList.get().getModFileById(namespace.origin);
    Map<String, String> result = new HashMap<>();
    try {
      var resource = modFile.getFile().getSecureJar().getPath("/assets/" + namespace.origin + "/lang/en_us.json");
      result.putAll(decode(resource, namespace.origin));
    } catch (IOException error) {
    }
    try {
      var resource = modFile.getFile().getSecureJar().getPath("/assets/" + namespace.origin + "/lang/" +
          this.lang + ".json");
      result.putAll(decode(resource, namespace.origin));
    } catch (IOException error) {
    }
    super.info.put(namespace.origin, result);
    return super.retrieve(namespace);
  }
}
