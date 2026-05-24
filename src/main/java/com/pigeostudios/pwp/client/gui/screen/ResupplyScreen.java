package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.ResupplyActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ResupplyScreen extends Screen {

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 220;

    private float fadeAlpha;
    private long openTime;

    private final List<WeaponEntry> weaponEntries = new ArrayList<>();
    private boolean hasRocketWeapon;

    public ResupplyScreen() {
        super(Component.literal("Resupply"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        fadeAlpha = 0f;

        scanHotbar();

        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;
        int contentY = height / 2 - 30;

        int btnX = panelX + PANEL_W - 110;

        if (!weaponEntries.isEmpty()) {
            for (int i = 0; i < weaponEntries.size(); i++) {
                WeaponEntry entry = weaponEntries.get(i);
                int y = contentY + i * 24;

                if (entry.isRocket) {
                    addRenderableWidget(new BButton(btnX, y, 100, 20,
                        Component.literal("§6Buy Rocket"),
                        btn -> {
                            PacketHandler.CHANNEL.sendToServer(new ResupplyActionPacket(ResupplyActionPacket.Action.BUY_ROCKET, -1));
                            onClose();
                        },
                        BButton.Variant.PRIMARY));
                } else {
                    addRenderableWidget(new BButton(btnX, y, 100, 20,
                        Component.literal("§aResupply"),
                        btn -> {
                            PacketHandler.CHANNEL.sendToServer(new ResupplyActionPacket(ResupplyActionPacket.Action.RESUPPLY_AMMO, entry.slotIndex));
                            onClose();
                        }));
                }
            }
        }

        if (hasRocketWeapon) {
            int rocketBtnY = contentY + weaponEntries.size() * 24 + 4;
            addRenderableWidget(new BButton(btnX, rocketBtnY, 100, 20,
                Component.literal("§6Buy Rocket"),
                btn -> {
                    PacketHandler.CHANNEL.sendToServer(new ResupplyActionPacket(ResupplyActionPacket.Action.BUY_ROCKET, -1));
                    onClose();
                },
                BButton.Variant.PRIMARY));
        }
    }

    private void scanHotbar() {
        weaponEntries.clear();
        hasRocketWeapon = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String[] rocketIds = {"superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
            "superbwarfare:tbg", "superbwarfare:rpo_a", "superbwarfare:cg_pea"};

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

            if (isRocketWeapon(id, rocketIds)) {
                hasRocketWeapon = true;
                String label = getRocketLabel(id);
                weaponEntries.add(new WeaponEntry(stack, id, label, true, i));
            } else if (isConventionalWeapon(id)) {
                String label = getWeaponLabel(id);
                weaponEntries.add(new WeaponEntry(stack, id, label, false, i));
            }
        }

        if (weaponEntries.size() > 4) {
            weaponEntries.subList(4, weaponEntries.size()).clear();
        }
    }

    private boolean isRocketWeapon(String id, String[] rocketIds) {
        for (String rid : rocketIds) {
            if (id.equals(rid)) return true;
        }
        return false;
    }

    private String getRocketLabel(String id) {
        if (id.contains("rpg") || id.contains("rpo")) return "RPG";
        if (id.contains("javelin")) return "Javelin";
        if (id.contains("igla")) return "Igla";
        if (id.contains("tbg")) return "TBG";
        if (id.contains("cg_pea")) return "ATGM";
        return "Rocket";
    }

    private boolean isConventionalWeapon(String id) {
        if (id.startsWith("tacz:")) return true;
        if (id.startsWith("superbwarfare:")) {
            String[] rocketIds = {"superbwarfare:rpg", "superbwarfare:javelin", "superbwarfare:igla_9k38",
                "superbwarfare:tbg", "superbwarfare:rpo_a", "superbwarfare:cg_pea"};
            for (String rid : rocketIds) {
                if (id.equals(rid)) return false;
            }
            return true;
        }
        return false;
    }

    private String getWeaponLabel(String id) {
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        path = path.replace('_', ' ');
        if (path.length() > 18) path = path.substring(0, 17) + "…";
        StringBuilder result = new StringBuilder();
        boolean cap = true;
        for (char c : path.toCharArray()) {
            if (c == ' ') { result.append(c); cap = true; }
            else if (cap) { result.append(Character.toUpperCase(c)); cap = false; }
            else result.append(c);
        }
        return result.toString();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 200f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_OVERLAY, (int)(fadeAlpha * 0xBB)));

        int cx = width / 2;
        int panelX = cx - PANEL_W / 2;
        int panelY = height / 2 - PANEL_H / 2;

        g.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, alpha));
        g.fill(panelX, panelY, panelX + PANEL_W, panelY + 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

        String title = "§6⛁  Resupply";
        g.drawString(font, title, cx - font.width(title) / 2, panelY + 12,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));

        if (weaponEntries.isEmpty() && !hasRocketWeapon) {
            String msg = "§7No weapons detected in hotbar";
            g.drawString(font, msg, cx - font.width(msg) / 2, height / 2 - 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        } else {
            int contentX = panelX + 10;
            int contentY = panelY + 35;

            // Draw weapon icons and names
            for (int i = 0; i < weaponEntries.size(); i++) {
                WeaponEntry entry = weaponEntries.get(i);
                int y = contentY + i * 24;

                // Item icon
                g.renderFakeItem(entry.stack, contentX, y);

                // Weapon name
                String prefix = entry.isRocket ? "§c" : "§f";
                g.drawString(font, prefix + entry.weaponName, contentX + 22, y + 5,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
            }

            if (hasRocketWeapon) {
                int rocketY = contentY + weaponEntries.size() * 24 + 8;
                g.drawString(font, "§6⛭ Rocket launcher detected", contentX + 22, rocketY + 5,
                    AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
            }
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 || keyCode == 82) { // ESC or R
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private record WeaponEntry(ItemStack stack, String id, String weaponName, boolean isRocket, int slotIndex) {}
}
