package za.net.hanro50.dischat;

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

}
