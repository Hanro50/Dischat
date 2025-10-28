package za.net.hanro50.dischat.neoforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.neoforged.fml.ModList;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class NeoForgeLexicon extends Lexicon {
  public NeoForgeLexicon(String version, String code) {
    super(version, code);
  }

  protected void decode(Path path, String origin) throws IOException {
    String data = Files.readString(path);
    Constants.LOGGER.info("Loaded " + data);
    var mc = Constants.GSON.fromJson("{\"map\":" + data + "}", LanguageInfo.class);
    info.put(origin, mc);
  }

  public String retrieve(NamespaceContainer namespace) {
    if (this.info.containsKey(namespace.origin))
      return super.retrieve(namespace);
    Constants.LOGGER.info("Loading language file from " + namespace.origin);
    var modFile = ModList.get().getModFileById(namespace.origin);

    try {
      var resource = modFile.getFile().getSecureJar().getPath("/assets/" + namespace.origin + "/lang/" +
          this.lang + ".json");
      decode(resource, namespace.origin);
      return super.retrieve(namespace);
    } catch (IOException error) {
    }
    try {
      var resource = modFile.getFile().getSecureJar().getPath("/assets/" + namespace.origin + "/lang/en_us.json");
      decode(resource, namespace.origin);
      return super.retrieve(namespace);
    } catch (IOException error) {
    }

    return super.retrieve(namespace);
  }
}
