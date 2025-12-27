package za.net.hanro50.dischat.neoforge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarResource;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Constants.MapContainer;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class NeoForgeLexicon extends Lexicon {

  protected Map<String, String> decode(JarResource path, String origin) throws IOException {
    return Constants.GSON.fromJson(Files.readString(path), MapContainer.class).getMap();
  }

  public NeoForgeLexicon(String version, String code) {
    super(version, code);
  }

  public String retrieve(NamespaceContainer namespace) {
    if (this.info.containsKey(namespace.origin))
      return super.retrieve(namespace);
    Constants.LOGGER.info("Loading language file from " + namespace.origin);
    var modFile = ModList.get().getModFileById(namespace.origin);

    var resource = modFile.getFile().getContents().get("/assets/" + namespace.origin + "/lang/" +
        this.lang + ".json");
    if (resource != null) {
      decode(resource, namespace.origin);
      return super.retrieve(namespace);
    }

    resource = modFile.getFile().getContents().get("/assets/" + namespace.origin + "/lang/en_us.json");
    if (resource != null)
      decode(resource, namespace.origin);
    return super.retrieve(namespace);
  }
}
