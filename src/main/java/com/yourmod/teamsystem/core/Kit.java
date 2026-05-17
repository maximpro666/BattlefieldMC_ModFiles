package com.yourmod.teamsystem.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import com.google.gson.*;

import java.util.*;

public class Kit {
    public static class IndexedItem {
        public int slot;
        public ItemStack stack;

        public IndexedItem(int slot, ItemStack stack) {
            this.slot = slot;
            this.stack = stack;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("slot", slot);
            CompoundTag saveTag = new CompoundTag();
            stack.save(saveTag);
            obj.addProperty("nbt", saveTag.toString());
            return obj;
        }

        public static IndexedItem fromJson(JsonObject obj) {
            try {
                int slot = obj.get("slot").getAsInt();
                String nbtString = obj.get("nbt").getAsString();
                CompoundTag nbt = TagParser.parseTag(nbtString);
                ItemStack stack = ItemStack.of(nbt);
                return new IndexedItem(slot, stack);
            } catch (Exception e) { return null; }
        }
    }

    private String name;
    private String displayName;
    private Team team;
    private int minRankOrdinal;
    private int cooldownSeconds;
    private List<IndexedItem> items;

    public Kit(String name, String displayName, Team team, int minRankOrdinal, int cooldownSeconds, List<IndexedItem> items) {
        this.name = name;
        this.displayName = displayName;
        this.team = team;
        this.minRankOrdinal = Math.max(0, minRankOrdinal);
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Team getTeam() { return team; }
    public int getMinRankOrdinal() { return minRankOrdinal; }
    public int getCooldownSeconds() { return cooldownSeconds; }
    public List<IndexedItem> getItems() { return items; }
    public void setItems(List<IndexedItem> items) { this.items = items != null ? items : new ArrayList<>(); }

    public void applyToPlayer(ServerPlayer player) {
        Inventory inv = player.getInventory();
        inv.clearContent();
        player.getInventory().armor.set(0, ItemStack.EMPTY);
        player.getInventory().armor.set(1, ItemStack.EMPTY);
        player.getInventory().armor.set(2, ItemStack.EMPTY);
        player.getInventory().armor.set(3, ItemStack.EMPTY);
        player.getInventory().offhand.set(0, ItemStack.EMPTY);

        KitManager.clearCuriosSlots(player);

        for (IndexedItem ii : items) {
            if (ii.stack.isEmpty()) continue;
            ItemStack copy = ii.stack.copy();
            if (ii.slot >= 0 && ii.slot < 36) {
                inv.setItem(ii.slot, copy);
            } else if (ii.slot == 40) {
                inv.offhand.set(0, copy);
            } else if (ii.slot >= 100 && ii.slot <= 103) {
                EquipmentSlot eq = EquipmentSlot.values()[ii.slot - 100 + 2];
                player.setItemSlot(eq, copy);
            } else if (ii.slot >= 200) {
                KitManager.setCuriosSlot(player, ii.slot - 200, copy);
            } else {
                player.addItem(copy);
            }
        }
        player.inventoryMenu.broadcastChanges();
    }

    public static Kit fromPlayerInventory(String name, String displayName, Team team, int minRankOrdinal, ServerPlayer player) {
        List<IndexedItem> items = new ArrayList<>();
        Inventory inv = player.getInventory();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) items.add(new IndexedItem(i, stack));
        }
        ItemStack offhand = inv.offhand.get(0);
        if (!offhand.isEmpty()) items.add(new IndexedItem(40, offhand));
        for (int i = 0; i < 4; i++) {
            ItemStack armor = inv.armor.get(i);
            if (!armor.isEmpty()) items.add(new IndexedItem(100 + i, armor));
        }
        List<ItemStack> curios = KitManager.getCuriosSlots(player);
        for (int i = 0; i < curios.size(); i++) {
            if (!curios.get(i).isEmpty()) items.add(new IndexedItem(200 + i, curios.get(i)));
        }
        return new Kit(name, displayName, team, minRankOrdinal, 0, items);
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("displayName", displayName);
        obj.addProperty("team", team.getName());
        obj.addProperty("minRankOrdinal", minRankOrdinal);
        obj.addProperty("cooldownSeconds", cooldownSeconds);
        JsonArray itemsArray = new JsonArray();
        for (IndexedItem ii : items) {
            if (!ii.stack.isEmpty()) itemsArray.add(ii.toJson());
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
        List<IndexedItem> items = new ArrayList<>();
        JsonArray itemsArray = obj.getAsJsonArray("items");
        for (JsonElement elem : itemsArray) {
            try {
                IndexedItem ii = IndexedItem.fromJson(elem.getAsJsonObject());
                if (ii != null && !ii.stack.isEmpty()) items.add(ii);
            } catch (Exception e) { /* Skip invalid items */ }
        }
        return new Kit(name, displayName, team, minRankOrdinal, cooldownSeconds, items);
    }
}
