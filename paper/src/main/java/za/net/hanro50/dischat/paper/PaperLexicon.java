package za.net.hanro50.dischat.paper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import za.net.hanro50.dischat.core.Constants;
import za.net.hanro50.dischat.core.Constants.MapContainer;
import za.net.hanro50.dischat.lang.Lexicon;

public class PaperLexicon extends Lexicon {
  public PaperLexicon(String version, String code) {
    super(version, code);

    if (!code.equals("en_us"))
      return;
    BufferedReader reader = null;
    try {
      InputStream is = Bukkit.class.getResourceAsStream("/assets/minecraft/lang/en_us.json");
      reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
      var lines = reader.lines().collect(Collectors.joining("\n"));
      var mc = Constants.GSON.fromJson(lines, MapContainer.class).getMap();
      info.put("minecraft", mc);
      Constants.LOGGER.warn(lines);
    } finally {
      if (reader != null)
        try {
          reader.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
    }

  }
}
