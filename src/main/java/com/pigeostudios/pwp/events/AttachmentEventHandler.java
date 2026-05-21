package com.pigeostudios.pwp.events;

import com.pigeostudios.pwp.PWP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles TACZ weapon attachment NBT persistence.
 * TACZ stores full gun data (attachments, accessories) in ItemStack NBT.
 * On kit equip → attachments are saved per (playerUUID, kitName).
 * On kit re-equip → saved attachments are restored to the gun ItemStack.
 *
 * Integration note: This is invoked by KitManager when a player selects a kit.
 * No TACZ classes are imported — we only touch ItemStack NBT generically.
 */
public class AttachmentEventHandler {

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
    }

    public static void saveGunAttachments(ServerPlayer player, String kitName, ItemStack gunStack) {
        if (gunStack.isEmpty() || !gunStack.hasTag()) return;
        CompoundTag gunNBT = gunStack.getTag();
        if (gunNBT == null || gunNBT.isEmpty()) return;

        Map<String, CompoundTag> saved = PWP.getTeamManager()
            .getOrCreatePlayerData(player.getUUID()).getSavedAttachments();

        String key = kitName + "_" + gunStack.getDescriptionId();
        saved.put(key, gunNBT.copy());
        PWP.getTeamManager().setDirty();
    }

    public static void restoreGunAttachments(ServerPlayer player, String kitName, ItemStack gunStack) {
        if (gunStack.isEmpty()) return;

        Map<String, CompoundTag> saved = PWP.getTeamManager()
            .getOrCreatePlayerData(player.getUUID()).getSavedAttachments();

        String key = kitName + "_" + gunStack.getDescriptionId();
        CompoundTag savedNBT = saved.get(key);
        if (savedNBT != null) {
            gunStack.setTag(savedNBT.copy());
        }
    }

    public static void clearPlayerAttachments(ServerPlayer player) {
        PWP.getTeamManager()
            .getOrCreatePlayerData(player.getUUID()).getSavedAttachments().clear();
        PWP.getTeamManager().setDirty();
    }

    public static void clearKitAttachments(ServerPlayer player, String kitName) {
        Map<String, CompoundTag> saved = PWP.getTeamManager()
            .getOrCreatePlayerData(player.getUUID()).getSavedAttachments();
        saved.entrySet().removeIf(e -> e.getKey().startsWith(kitName + "_"));
        PWP.getTeamManager().setDirty();
    }
}
