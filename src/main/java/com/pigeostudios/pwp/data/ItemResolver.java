package com.pigeostudios.pwp.data;

import com.pigeostudios.pwp.PWP;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;

public class ItemResolver {

    private static final Set<String> warnedIds = new HashSet<>();
    private static final Set<String> tacZNamespaces = Set.of("tacz", "maxstuff", "daffas_arsenal");
    private static Item cachedTacZGunItem;

    private static Item getTacZGunItem() {
        if (cachedTacZGunItem == null) {
            cachedTacZGunItem = BuiltInRegistries.ITEM.get(new ResourceLocation("tacz", "modern_kinetic_gun"));
        }
        return cachedTacZGunItem;
    }

    public static ItemStack resolve(String weaponId) {
        if (weaponId == null || weaponId.isEmpty()) return ItemStack.EMPTY;

        ResourceLocation rl;
        try {
            rl = new ResourceLocation(weaponId);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }

        boolean isTacZ = tacZNamespaces.contains(rl.getNamespace());

        Item directItem = BuiltInRegistries.ITEM.get(rl);
        if (directItem != null && directItem != Items.AIR) {
            ItemStack stack = new ItemStack(directItem);
            stack.getOrCreateTag().putString("GunFireMode", "AUTO");
            return stack;
        }

        // Fallback for TaCZ gun pack items: use tacz:modern_kinetic_gun with GunId NBT
        if (isTacZ) {
            Item tacZGun = getTacZGunItem();
            if (tacZGun != null && tacZGun != Items.AIR) {
                ItemStack stack = new ItemStack(tacZGun);
                CompoundTag tag = new CompoundTag();
                tag.putString("GunId", weaponId);
                tag.putString("GunFireMode", "AUTO");
                stack.setTag(tag);
                return stack;
            }
        }

        if (warnedIds.add(weaponId)) {
            PWP.LOGGER.warn("Cannot resolve weapon ID: {} (not registered on this side)", weaponId);
        }
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
