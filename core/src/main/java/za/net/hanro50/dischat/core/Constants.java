package za.net.hanro50.dischat.core;

import java.net.http.HttpClient;
import java.security.SecureRandom;

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

  public static Core core;

  public static String getAdvancementColor(String category) {

    switch (category) {
      case "adventure":
        return "#10A5FA";
      case "end":
        return "#800080";
      case "husbandry":
        return "#7e7e7e";
      case "nether":
        return "#aa1515";
      case "story":
        return "#fe8738";
      default:
        return "#0000ff";
    }
  }
}
