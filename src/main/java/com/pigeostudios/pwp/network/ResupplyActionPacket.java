package com.pigeostudios.pwp.network;

import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.ammo.*;
import com.pigeostudios.pwp.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

import static com.pigeostudios.pwp.core.ChatHelper.*;

public class ResupplyActionPacket {

    public enum Action { RESUPPLY_AMMO, BUY_ROCKET }

    private final Action action;
    private final int slotIndex; // -1 = all (rocket/backward compat), 0-8 = specific hotbar slot

    public ResupplyActionPacket(Action action, int slotIndex) {
        this.action = action;
        this.slotIndex = slotIndex;
    }

    public ResupplyActionPacket(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.slotIndex = buf.readInt();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeInt(slotIndex);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            if (!RateLimiter.checkAndThrottle(player)) return;
            if (!PacketValidator.checkAndReject(player, PacketValidator.requireTeamPlayable(player))) return;

            BattlefieldRuntime rt = BattlefieldRuntime.getInstance();
            AmmoService ammoService = rt.getAmmoService();
            IAmmoProvider provider = ammoService.getBestProvider(player);

            if (action == Action.RESUPPLY_AMMO) {
                handleResupplyAmmo(player, rt, slotIndex);
            } else if (action == Action.BUY_ROCKET) {
                handleBuyRocket(player, rt, provider);
            }
        });
        return true;
    }

    private static final int RESUPPLY_AMMO_COST = 5;

    private static void handleResupplyAmmo(ServerPlayer player, BattlefieldRuntime rt, int slotIndex) {
        if (RESUPPLY_AMMO_COST > 0) {
            if (!rt.deductBC(player.getUUID(), RESUPPLY_AMMO_COST)) {
                player.displayClientMessage(error("Not enough BC! Need " + RESUPPLY_AMMO_COST + " BC for resupply"), false);
                return;
            }
            rt.syncBC(player);
        }

        boolean refilled = false;
        List<String> refilledWeapons = new ArrayList<>();

        Set<String> hotbarCalibers = getHotbarCalibers(player);

        int startSlot = (slotIndex >= 0) ? slotIndex : 0;
        int endSlot = (slotIndex >= 0) ? slotIndex + 1 : 9;

        for (int i = startSlot; i < endSlot; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            // TACZ gun — refill ammo box
            if (id.equals("tacz:modern_kinetic_gun") && stack.getTag() != null) {
                String gunId = stack.getTag().getString("GunId");
                String caliber = TACZ_CALIBER_MAP.get(gunId);
                if (caliber != null) {
                    ItemStack ammoBox = findSuitableAmmoBox(player, caliber, hotbarCalibers);
                    if (ammoBox == null) {
                        ammoBox = createItem("tacz:ammo_box");
                        if (!ammoBox.isEmpty()) {
                            CompoundTag tag = ammoBox.getOrCreateTag();
                            tag.putString("AmmoId", caliber);
                            tag.putInt("AmmoCount", 240);
                            tag.putInt("Level", 1);
                            if (!player.getInventory().add(ammoBox)) {
                                player.drop(ammoBox, false);
                            }
                            refilled = true;
                            refilledWeapons.add(getSimpleGunName(gunId));
                        }
                    } else {
                        CompoundTag tag = ammoBox.getOrCreateTag();
                        tag.putString("AmmoId", caliber);
                        tag.putInt("AmmoCount", 240);
                        tag.putInt("Level", 1);
                        refilled = true;
                        refilledWeapons.add(getSimpleGunName(gunId));
                    }
                }
                continue;
            }

            // SuperbWarfare standard ammo
            ItemStack ammo = getAmmoForWeapon(id);
            if (!ammo.isEmpty()) {
                int existing = countItems(player, BuiltInRegistries.ITEM.getKey(ammo.getItem()).toString());
                if (existing < 2) {
                    ItemStack give = ammo.copy();
                    if (!player.getInventory().add(give)) {
                        player.drop(give, false);
                    }
                    refilled = true;
                    String name = id.contains(":") ? id.substring(id.indexOf(':') + 1).replace('_', ' ') : id;
                    refilledWeapons.add(name);
                }
            }
        }

        if (refilled) {
            player.inventoryMenu.broadcastChanges();
            String weaponList = String.join(", ", refilledWeapons);
            player.displayClientMessage(accent("Resupplied: " + weaponList + " (" + RESUPPLY_AMMO_COST + " BC)"), false);
        } else {
            player.displayClientMessage(error("No weapons in hotbar needing ammo!"), false);
        }
    }

    private static Set<String> getHotbarCalibers(ServerPlayer player) {
        Set<String> calibers = new HashSet<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals("tacz:modern_kinetic_gun") && stack.getTag() != null) {
                String gunId = stack.getTag().getString("GunId");
                String cal = TACZ_CALIBER_MAP.get(gunId);
                if (cal != null) calibers.add(cal);
            }
        }
        return calibers;
    }

    private static ItemStack findSuitableAmmoBox(ServerPlayer player, String neededCaliber, Set<String> hotbarCalibers) {
        // First pass: exact caliber match
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("tacz:ammo_box")) {
                CompoundTag tag = stack.getTag();
                if (tag != null && neededCaliber.equals(tag.getString("AmmoId"))) {
                    return stack;
                }
            }
        }
        // Second pass: any box whose caliber is not needed by another hotbar gun
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("tacz:ammo_box")) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    String existing = tag.getString("AmmoId");
                    if (existing.isEmpty() || !hotbarCalibers.contains(existing)) {
                        return stack;
                    }
                }
            }
        }
        return null;
    }

    private static String getSimpleGunName(String gunId) {
        String path = gunId.contains(":") ? gunId.substring(gunId.indexOf(':') + 1) : gunId;
        return path.replace('_', ' ');
    }

    private static void handleBuyRocket(ServerPlayer player, BattlefieldRuntime rt, IAmmoProvider provider) {
        if (provider == null) {
            player.displayClientMessage(error("You must be near an ammo provider!"), false);
            return;
        }
        if (!provider.providesPaidAmmo()) {
            player.displayClientMessage(error("This provider does not sell rockets!"), false);
            return;
        }

        boolean isBase = isAtTeamBase(player);
        String weaponId = findRocketWeapon(player);
        if (weaponId == null) {
            player.displayClientMessage(error("Hold a rocket launcher in your hotbar!"), false);
            return;
        }

        RocketDef def = ROCKET_MAP.get(weaponId);
        if (def == null) return;

        AmmoCooldownManager cdm = rt.getAmmoCooldownManager();
        if (!cdm.canRequest(player.getUUID(), def.catalogId, isBase)) {
            long remain = cdm.getRemainingCooldown(player.getUUID(), def.catalogId, isBase) / 1000;
            player.displayClientMessage(error("Wait " + remain + "s before buying more " + def.label + " rockets!"), false);
            return;
        }

        if (countItems(player, def.ammoItemId) >= def.maxCount) {
            player.displayClientMessage(error("You already have enough " + def.label + " rockets! (max " + def.maxCount + ")"), false);
            return;
        }

        ItemStack rocket = createItem(def.ammoItemId);
        if (rocket.isEmpty()) {
            player.displayClientMessage(error("Failed to create rocket item!"), false);
            return;
        }

        int cost = provider.getAmmoCost(def.catalogId);
        if (cost > 0) {
            if (!rt.deductBC(player.getUUID(), cost)) {
                player.displayClientMessage(error("Not enough BC! Need " + cost + " BC for 1x " + def.label + " rocket"), false);
                return;
            }
            rt.syncBC(player);
        }

        if (!player.getInventory().add(rocket)) {
            player.drop(rocket, false);
            player.displayClientMessage(accent("Inventory full! Rocket dropped at your feet."), false);
        } else {
            player.inventoryMenu.broadcastChanges();
        }

        cdm.setCooldown(player.getUUID(), def.catalogId);
        String location = isBase ? "base" : "capture point";
        player.displayClientMessage(accent("Purchased 1x " + def.label + " rocket for " + cost + " BC at " + location + "!"), false);
    }

    // ─── helpers ───

    private static boolean isAtTeamBase(ServerPlayer player) {
        GameManager game = PWP.getGameManager();
        if (game == null) return false;
        MapConfig map = game.getCurrentMap();
        if (map == null || !map.hasTeamSpawns()) return false;
        Team team = PWP.getTeamManager().getOrCreatePlayerData(player.getUUID()).getTeam();
        int[] spawn = team == Team.NATO ? map.getNatoSpawn() : map.getRussiaSpawn();
        if (spawn == null) return false;
        double dx = player.getX() - (spawn[0] + 0.5);
        double dz = player.getZ() - (spawn[2] + 0.5);
        double radius = map.getBaseRadius();
        return dx * dx + dz * dz <= radius * radius;
    }

    private static String findRocketWeapon(ServerPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                if (ROCKET_MAP.containsKey(id)) return id;
            }
        }
        return null;
    }

    private static int countItems(ServerPlayer player, String itemId) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static ItemStack createItem(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(rl);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(item);
            }
        } catch (Exception e) {}
        return ItemStack.EMPTY;
    }

    private static ItemStack getAmmoForWeapon(String id) {
        String[] noAmmo = {"hand_grenade", "rgo_grenade", "c4_bomb", "taser",
            "repair_tool", "m18_smoke_grenade", "claymore_mine", "blu_43_mine", "lunge_mine", "m_79"};
        for (String p : noAmmo) {
            if (id.endsWith(p)) return ItemStack.EMPTY;
        }
        if (!id.startsWith("superbwarfare:") && !id.startsWith("tacz:")) return ItemStack.EMPTY;

        if (id.equals("superbwarfare:rpg")) return createItem("superbwarfare:rpg_rocket_standard");
        if (id.equals("superbwarfare:javelin")) return createItem("superbwarfare:javelin_missile");
        if (id.equals("superbwarfare:igla_9k38")) return createItem("superbwarfare:medium_anti_air_missile");

        String path = id.startsWith("superbwarfare:") ? id.substring("superbwarfare:".length()) : "";
        boolean handgun = path.contains("glock") || path.contains("m_1911") || path.contains("mp_443")
            || path.contains("deagle") || path.contains("cz75");
        boolean smg = path.contains("mp_5") || path.contains("vector");
        boolean shotgun = path.contains("m_870") || path.contains("aa_12");
        boolean sniper = path.contains("awm") || path.contains("mosin") || path.contains("ntw")
            || path.contains("m_98b") || path.contains("svd") || path.contains("sks")
            || path.contains("bocek") || path.contains("k_98") || path.contains("mk_14")
            || path.contains("marlin") || path.contains("hunting_rifle");
        boolean rifle = path.contains("m_4") || path.contains("hk_416") || path.contains("ak_47")
            || path.contains("ak_12") || path.contains("qbz") || path.contains("rpk")
            || path.contains("m_60") || path.contains("m_2_hb") || path.contains("minigun");

        if (handgun || smg) return createItem("superbwarfare:handgun_ammo_box");
        if (shotgun) return createItem("superbwarfare:shotgun_ammo_box");
        if (sniper) return createItem("superbwarfare:sniper_ammo_box");
        if (rifle) return createItem("superbwarfare:rifle_ammo_box");

        return ItemStack.EMPTY;
    }

    private static final Map<String, String> TACZ_CALIBER_MAP = new LinkedHashMap<>();
    static {
        TACZ_CALIBER_MAP.put("tacz:m4a1", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:hk416a5", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:hk416d", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:scar_l", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:m16a4", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:m16a1", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:spr15hb", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:aug", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:g36k", "tacz:556x45");
        TACZ_CALIBER_MAP.put("tacz:ak47", "tacz:762x39");
        TACZ_CALIBER_MAP.put("tacz:rpk", "tacz:762x39");
        TACZ_CALIBER_MAP.put("tacz:type_81", "tacz:762x39");
        TACZ_CALIBER_MAP.put("tacz:sks_tactical", "tacz:762x39");
        TACZ_CALIBER_MAP.put("tacz:qbz_95", "tacz:58x42");
        TACZ_CALIBER_MAP.put("tacz:qbz_191", "tacz:58x42");
        TACZ_CALIBER_MAP.put("tacz:mk14", "tacz:308");
        TACZ_CALIBER_MAP.put("tacz:scar_h", "tacz:308");
        TACZ_CALIBER_MAP.put("tacz:hk_g3", "tacz:308");
        TACZ_CALIBER_MAP.put("tacz:fn_fal", "tacz:308");
        TACZ_CALIBER_MAP.put("tacz:fn_evolys", "tacz:308");
        TACZ_CALIBER_MAP.put("tacz:ai_awp", "tacz:338");
        TACZ_CALIBER_MAP.put("tacz:m107", "tacz:50bmg");
        TACZ_CALIBER_MAP.put("tacz:m95", "tacz:50bmg");
        TACZ_CALIBER_MAP.put("tacz:m700", "tacz:30_06");
        TACZ_CALIBER_MAP.put("tacz:lonetrail", "tacz:30_06");
        TACZ_CALIBER_MAP.put("tacz:kar98", "tacz:792x57");
        TACZ_CALIBER_MAP.put("tacz:hk_mp5a5", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:uzi", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:b93r", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:vector45", "tacz:45acp");
        TACZ_CALIBER_MAP.put("tacz:ump45", "tacz:45acp");
        TACZ_CALIBER_MAP.put("tacz:p90", "tacz:57x28");
        TACZ_CALIBER_MAP.put("tacz:glock_17", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:cz75", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:m9a4", "tacz:9mm");
        TACZ_CALIBER_MAP.put("tacz:p320", "tacz:45acp");
        TACZ_CALIBER_MAP.put("tacz:m1911", "tacz:45acp");
        TACZ_CALIBER_MAP.put("tacz:deagle", "tacz:50ae");
        TACZ_CALIBER_MAP.put("tacz:aa12", "tacz:12g");
        TACZ_CALIBER_MAP.put("tacz:m1014", "tacz:12g");
        TACZ_CALIBER_MAP.put("tacz:m249", "tacz:556x45");
    }

    private record RocketDef(String catalogId, String ammoItemId, int maxCount, String label) {}

    private static final Map<String, RocketDef> ROCKET_MAP = new LinkedHashMap<>();
    static {
        ROCKET_MAP.put("superbwarfare:rpg", new RocketDef("superbwarfare:rpg_rocket", "superbwarfare:rpg_rocket_standard", 3, "RPG"));
        ROCKET_MAP.put("superbwarfare:javelin", new RocketDef("superbwarfare:javelin_missile", "superbwarfare:javelin_missile", 1, "Javelin"));
        ROCKET_MAP.put("superbwarfare:igla_9k38", new RocketDef("superbwarfare:igla_missile", "superbwarfare:medium_anti_air_missile", 2, "Igla"));
        ROCKET_MAP.put("superbwarfare:tbg", new RocketDef("superbwarfare:tbg_rocket", "superbwarfare:tbg_rocket", 3, "TBG"));
        ROCKET_MAP.put("superbwarfare:rpo_a", new RocketDef("superbwarfare:rpg_rocket", "superbwarfare:rpg_rocket_standard", 3, "RPG"));
        ROCKET_MAP.put("superbwarfare:cg_pea", new RocketDef("superbwarfare:atgm_missile", "superbwarfare:atgm_missile", 2, "ATGM"));
    }
}
