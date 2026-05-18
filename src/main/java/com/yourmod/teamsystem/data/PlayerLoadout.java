package com.yourmod.teamsystem.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlayerLoadout {

    public String player_uuid;
    public String classId;
    public String kitId;
    public LoadoutSlots loadout = new LoadoutSlots();
    public String vehicle;

    public static class WeaponSlot {
        public String id;
        public Map<String, String> attachments = new HashMap<>();

        public WeaponSlot() {}
        public WeaponSlot(String id) { this.id = id; }
    }

    public static class LoadoutSlots {
        public WeaponSlot primary;
        public WeaponSlot secondary;
        public String special;
        public String grenade;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public void save(Path worldDir) {
        try {
            Path dir  = worldDir.resolve("teamsystem/playerloadouts");
            Files.createDirectories(dir);
            Path file = dir.resolve(player_uuid + ".json");
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PlayerLoadout load(Path worldDir, String uuid) {
        Path file = worldDir.resolve("teamsystem/playerloadouts/" + uuid + ".json");
        if (Files.exists(file)) {
            try {
                String json = Files.readString(file);
                PlayerLoadout pl = GSON.fromJson(json, PlayerLoadout.class);
                if (pl != null) return pl;
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        PlayerLoadout pl = new PlayerLoadout();
        pl.player_uuid = uuid;
        return pl;
    }
}
