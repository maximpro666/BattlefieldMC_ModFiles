package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import com.google.gson.*;

import java.util.ArrayList;
import java.util.List;

public class Kit {
    private String name;
    private String displayName;
    private Team team;
    private int minRankOrdinal;
    private int cooldownSeconds;
    private List<ItemStack> items;

    public Kit(String name, String displayName, Team team, int minRankOrdinal, int cooldownSeconds, List<ItemStack> items) {
        this.name = name;
        this.displayName = displayName;
        this.team = team;
        this.minRankOrdinal = Math.max(0, minRankOrdinal);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Team getTeam() {
        return team;
    }

    public int getMinRankOrdinal() {
        return minRankOrdinal;
    }

    public int getCooldownSeconds() {
        return cooldownSeconds;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void setItems(List<ItemStack> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("displayName", displayName);
        obj.addProperty("team", team.getName());
        obj.addProperty("minRankOrdinal", minRankOrdinal);
        obj.addProperty("cooldownSeconds", cooldownSeconds);

        JsonArray itemsArray = new JsonArray();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                JsonObject itemObj = new JsonObject();
                CompoundTag saveTag = new CompoundTag();
                item.save(saveTag);
                itemObj.addProperty("nbt", saveTag.toString());
                itemsArray.add(itemObj);
            }
        }
        obj.add("items", itemsArray);

        return obj;
    }

    public static Kit fromJson(JsonObject obj) {
        String name = obj.get("name").getAsString();
        String displayName = obj.get("displayName").getAsString();
        Team team = Team.fromString(obj.get("team").getAsString());
        int minRankOrdinal = obj.get("minRankOrdinal").getAsInt();
        int cooldownSeconds = obj.get("cooldownSeconds").getAsInt();

        List<ItemStack> items = new ArrayList<>();
        JsonArray itemsArray = obj.getAsJsonArray("items");
        for (JsonElement elem : itemsArray) {
            try {
                JsonObject itemObj = elem.getAsJsonObject();
                String nbtString = itemObj.get("nbt").getAsString();
                CompoundTag nbt = TagParser.parseTag(nbtString);
                ItemStack stack = ItemStack.of(nbt);
                if (!stack.isEmpty()) {
                    items.add(stack);
                }
            } catch (Exception e) {
                // Skip invalid items
            }
        }

        return new Kit(name, displayName, team, minRankOrdinal, cooldownSeconds, items);
    }
}
