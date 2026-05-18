package com.yourmod.teamsystem.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yourmod.teamsystem.client.ClientTeamData;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class I18n {
    private static Map<String, String> ru;
    private static Map<String, String> en;
    private static boolean loaded = false;

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        ru = loadLang("ru_ru");
        en = loadLang("en_us");
    }

    private static Map<String, String> loadLang(String lang) {
        Map<String, String> map = new HashMap<>();
        try {
            var is = I18n.class.getResourceAsStream("/assets/teamsystem/lang/" + lang + ".json");
            if (is != null) {
                JsonObject obj = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                for (var entry : obj.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static String get(String key, Object... args) {
        ensureLoaded();
        Map<String, String> lang = "ru".equals(ClientTeamData.language) ? ru : en;
        String template = lang.getOrDefault(key, key);
        if (args.length > 0) {
            return String.format(template, args);
        }
        return template;
    }
}
