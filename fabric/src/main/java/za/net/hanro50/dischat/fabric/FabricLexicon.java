package za.net.hanro50.dischat.fabric;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.FabricLoader;
import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Lexicon;
import za.net.hanro50.dischat.core.NamespaceContainer;

public class FabricLexicon extends Lexicon {

  protected void decode(Path path, String origin) {
    try {
      String data = Files.readString(path);

      var mc = Constants.GSON.fromJson("{\"map\":" + data + "}", LanguageInfo.class);
      info.put(origin, mc);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public FabricLexicon(String version, String code) {
    super(version, code);
  }

  public String retrieve(NamespaceContainer namespace) {
    if (this.info.containsKey(namespace.origin))
      return super.retrieve(namespace);

    var optional = FabricLoader.getInstance().getModContainer(namespace.origin);
    if (!optional.isPresent()) {
      Constants.LOGGER.warn("COULD NOT FIND MOD CONTAINER FOR " + namespace.origin);
      return super.retrieve(namespace);
    }
    var container = optional.get();

    var f = container.findPath("/assets/" + namespace.origin + "/lang/" + this.lang + ".json");
    if (f.isPresent()) {
      decode(f.get(), namespace.origin);
      return super.retrieve(namespace);
    }

    f = container.findPath("/assets/" + namespace.origin + "/lang/en_us.json");
    if (f.isPresent())
      decode(f.get(), namespace.origin);
    return super.retrieve(namespace);
  }
}
