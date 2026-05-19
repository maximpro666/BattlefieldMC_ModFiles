package com.yourmod.teamsystem.data;

import com.yourmod.teamsystem.TeamSystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Auto-detects TACZ attachments for guns using TACZ's API via reflection.
 * No manual attachment_limits config needed.
 */
public class TaczAttachmentResolver {

    private static boolean checked = false;
    private static boolean taczAvailable = false;

    private static Class<?> iGunClass;
    private static Class<?> cgpClass;
    private static Class<?> gunIndexClass;
    private static Class<?> gunDataClass;
    private static Class<?> attachmentItemClass;
    private static Class<?> attachmentCategoryClass;

    private static final Map<String, Map<String, List<String>>> cache = new HashMap<>();

    private static boolean init() {
        if (checked) return taczAvailable;
        checked = true;
        try {
            iGunClass = Class.forName("com.tacz.guns.api.item.IGun");
            cgpClass = Class.forName("com.tacz.guns.resource.CommonGunPack");
            gunIndexClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.GunIndex");
            gunDataClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.GunData");
            attachmentItemClass = Class.forName("com.tacz.guns.api.item.attachment.AttachmentItem");
            attachmentCategoryClass = Class.forName("com.tacz.guns.api.item.attachment.AttachmentCategory");
            taczAvailable = true;
        } catch (ClassNotFoundException e) {
            taczAvailable = false;
        }
        return taczAvailable;
    }

    public static Map<String, List<String>> getAttachments(String weaponId) {
        if (!init()) return Map.of();

        Map<String, List<String>> cached = cache.get(weaponId);
        if (cached != null) return cached;

        try {
            Map<String, List<String>> result = detectForWeapon(weaponId);
            cache.put(weaponId, result);
            return result;
        } catch (Exception e) {
            TeamSystem.LOGGER.debug("TACZ auto-detect failed for {}: {}", weaponId, e.getMessage());
            cache.put(weaponId, Map.of());
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> detectForWeapon(String weaponId) throws Exception {
        ItemStack stack = ItemResolver.resolve(weaponId);
        if (stack.isEmpty()) return Map.of();

        Method getIGunOrNull = iGunClass.getMethod("getIGunOrNull", ItemStack.class);
        Object iGun = getIGunOrNull.invoke(null, stack);
        if (iGun == null) return Map.of();

        Method getGunId = iGunClass.getMethod("getGunId", ItemStack.class);
        ResourceLocation gunId = (ResourceLocation) getGunId.invoke(iGun, stack);
        if (gunId == null) return Map.of();

        Method getInstance = cgpClass.getMethod("getInstance");
        Object cgp = getInstance.invoke(null);

        Method getGunIndex = cgpClass.getMethod("getGunIndex", ResourceLocation.class);
        Object gunIndex = getGunIndex.invoke(cgp, gunId);
        if (gunIndex == null) return Map.of();

        Method getData = gunIndexClass.getMethod("getData");
        Object gunData = getData.invoke(gunIndex);
        if (gunData == null) return Map.of();

        Method getAllowed = gunDataClass.getMethod("getAllowAttachmentTypes");
        Collection<?> allowedTypes = (Collection<?>) getAllowed.invoke(gunData);
        if (allowedTypes == null || allowedTypes.isEmpty()) return Map.of();

        Map<String, List<String>> result = new HashMap<>();
        for (Object type : allowedTypes) {
            String catName = type.toString().toLowerCase(Locale.ROOT);
            List<String> items = getAttachmentItemsForCategory(catName);
            if (!items.isEmpty()) {
                result.put(catName, items);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<String> getAttachmentItemsForCategory(String categoryName) throws Exception {
        List<String> result = new ArrayList<>();

        Object targetCategory = null;
        for (Object constant : attachmentCategoryClass.getEnumConstants()) {
            if (constant.toString().equalsIgnoreCase(categoryName)) {
                targetCategory = constant;
                break;
            }
        }
        if (targetCategory == null) return result;

        Method getType = attachmentItemClass.getMethod("getType");

        Set<ResourceLocation> keys = BuiltInRegistries.ITEM.keySet();
        for (ResourceLocation itemId : keys) {
            if (!"tacz".equals(itemId.getNamespace())) continue;
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item == null || item == Items.AIR) continue;
            if (!attachmentItemClass.isInstance(item)) continue;

            Object type = getType.invoke(item);
            if (type == targetCategory || type.equals(targetCategory)) {
                result.add(itemId.toString());
            }
        }
        return result;
    }

    public static boolean hasAttachments(String weaponId) {
        return !getAttachments(weaponId).isEmpty();
    }

    public static List<String> getCategories(String weaponId) {
        return new ArrayList<>(getAttachments(weaponId).keySet());
    }

    public static List<String> getOptions(String weaponId, String category) {
        return getAttachments(weaponId).getOrDefault(category, List.of());
    }
}
