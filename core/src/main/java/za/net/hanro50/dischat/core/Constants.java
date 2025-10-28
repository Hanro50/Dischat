package za.net.hanro50.dischat.core;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Constants {
  public static final String MOD_ID = "dischat";
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
  public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting()
      .create();
  public static final SecureRandom random = new SecureRandom();
  public static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

  public static HashMap<String, String> AdvancementColorDict = new HashMap<>();

  public static Core core;

  static {
    /**
     * How this generally works
     * The format is either one of the following formates
     * format 1 "namespace"
     * format 2 "namespace:category"
     *
     * The category code is mostly reused from vanilla, so
     * a vanilla advancement would have an id of category/advancement.
     *
     * If your advancement id does not contain a "/" in it, format 1 is used.
     * If it does then format 2 is used and the first part (the category) is used as
     * a category
     *
     * "category/advancement" ->
     * split on the slash ->
     * "category" "/" "advancement"->
     * take element 1)
     */

    // Vanilla categories
    AdvancementColorDict.put("minecraft:adventure", "#10A5FA");
    AdvancementColorDict.put("minecraft:end", "#800080");
    AdvancementColorDict.put("minecraft:husbandry", "#7e7e7e");
    AdvancementColorDict.put("minecraft:nether", "#aa1515");
    AdvancementColorDict.put("minecraft:story", "#fe8738");

    // modded advancements
    AdvancementColorDict.put("ae2:main", "#afb9c3");
    AdvancementColorDict.put("apotheosis:progression", "#2f0974");
    AdvancementColorDict.put("apothic_enchanting", "#391d6d");
    AdvancementColorDict.put("apothic_spawners", "#52328d");

    AdvancementColorDict.put("create", "#c76756");
    AdvancementColorDict.put("trading_floor", "#00ac61");

    AdvancementColorDict.put("eather", "#9d90a7");

    AdvancementColorDict.put("farmersdelight:main", "#ad8d54");
    AdvancementColorDict.put("mutantmonsters", "#00ff00");
    AdvancementColorDict.put("twilightforest", "#00eb89ff");
  }
}
