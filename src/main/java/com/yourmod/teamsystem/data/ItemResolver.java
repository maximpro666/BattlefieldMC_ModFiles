package com.yourmod.teamsystem.data;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class ItemResolver {

    private static final String TACZ_GUN_ITEM_ID = "tacz:modern_kinetic_gun";

    private static Boolean taczGunItemChecked = false;
    private static Item taczGunItem = null;

    private static Item getTaczGunItem() {
        if (!taczGunItemChecked) {
            taczGunItemChecked = true;
            try {
                Item found = BuiltInRegistries.ITEM.get(new ResourceLocation(TACZ_GUN_ITEM_ID));
                if (found != null && found != Items.AIR) {
                    taczGunItem = found;
                }
            } catch (Exception e) {
                TeamSystem.LOGGER.warn("TACZ modern_kinetic_gun not found, TACZ guns disabled");
            }
        }
        return taczGunItem;
    }

    public static ItemStack resolve(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) return ItemStack.EMPTY;

        ResourceLocation rl;
        try {
            rl = new ResourceLocation(weaponId);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }

        // Try direct registry lookup (Superb Warfare, other mods)
        Item directItem = BuiltInRegistries.ITEM.get(rl);
        if (directItem != null && directItem != Items.AIR) {
            return new ItemStack(directItem);
        }

        // Try as TACZ gun (modern_kinetic_gun with GunId NBT)
        Item taczItem = getTaczGunItem();
        if (taczItem != null) {
            ItemStack stack = new ItemStack(taczItem);
            CompoundTag tag = new CompoundTag();
            tag.putString("GunId", weaponId);
            tag.putString("GunFireMode", "AUTO");
            stack.setTag(tag);
            return stack;
        }

        TeamSystem.LOGGER.warn("Cannot resolve weapon ID: {} (no direct item, no TACZ)", weaponId);
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
