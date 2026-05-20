package com.yourmod.teamsystem.data;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class ItemResolver {

    public static ItemStack resolve(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) return ItemStack.EMPTY;

        ResourceLocation rl;
        try {
            rl = new ResourceLocation(weaponId);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }

        Item directItem = BuiltInRegistries.ITEM.get(rl);
        if (directItem != null && directItem != Items.AIR) {
            return new ItemStack(directItem);
        }

        TeamSystem.LOGGER.warn("Cannot resolve weapon ID: {}", weaponId);
        return ItemStack.EMPTY;
    }

    /**
     * Returns human-readable display name for a weapon ID.
     * Falls back to the ID itself if the item cannot be resolved.
     */
    public static String getDisplayName(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) return "";
        ItemStack stack = resolve(weaponId);
        if (!stack.isEmpty()) {
            Component name = stack.getHoverName();
            if (name != null) return name.getString();
        }
        // Fallback: return the part after colon
        int colon = weaponId.indexOf(':');
        return colon >= 0 ? weaponId.substring(colon + 1) : weaponId;
    }
}
