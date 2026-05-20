package com.yourmod.teamsystem.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.KitConfigPanel;
import com.yourmod.teamsystem.data.ItemResolver;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSavePacket;
import com.yourmod.teamsystem.network.KitSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KitCustomizationScreen extends Screen {

    private static final int PREVIEW_W = 190;
    private static final int PREVIEW_RENDER_H = 110;
    private static final int FOOTER_H = 40;
    private static final int HEADER_H = 36;

    private final String classId;
    private final String kitId;

    private final List<String> slotKeys = List.of("Primary", "Secondary", "Special", "Grenade");
    private final Map<String, List<String>> weaponOptions = new LinkedHashMap<>();
    private final Map<String, Integer> selectionIndex = new LinkedHashMap<>();
    private final Map<String, String> selections = new LinkedHashMap<>();

    private final List<String> armorKeys = List.of("Helmet", "Chestplate", "Backpack", "Shoulderpads");
    private final Map<String, List<String>> armorOptions = new LinkedHashMap<>();
    private final Map<String, Integer> armorIndex = new LinkedHashMap<>();
    private final Map<String, String> armorSelections = new LinkedHashMap<>();

    private final Map<String, String> attachmentSelections = new LinkedHashMap<>();
    private String activeSlot = "Primary";
    private String previewItem;

    private float fadeAlpha = 0f;
    private long openTime;
    private int tickCount;
    private int mouseX, mouseY;
    private KitConfigPanel kitConfigPanel;
    private final Map<String, String> displayNames = new HashMap<>();

    private String displayName(String id) {
        if (id == null || id.isEmpty()) return "\u2014";
        return displayNames.computeIfAbsent(id, k -> {
            ItemStack stack = ItemResolver.resolve(k);
            if (!stack.isEmpty()) return stack.getHoverName().getString();
            return k.replace("_", " ");
        });
    }

    public KitCustomizationScreen(String classId, String kitId) {
        super(Component.literal("Kit Customization"));
        this.classId = classId;
        this.kitId = kitId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        tickCount = 0;
        loadData();

        kitConfigPanel = new KitConfigPanel(classId, kitId,
                slotKeys, weaponOptions, selectionIndex, selections,
                armorKeys, armorOptions, armorIndex, armorSelections,
                attachmentSelections, activeSlot, null);
        kitConfigPanel.init(width, height);
        kitConfigPanel.setFade(1f);
        kitConfigPanel.setAlpha(0xFF);
        kitConfigPanel.setMouse(0, 0);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void loadData() {
        weaponOptions.clear();
        selectionIndex.clear();
        selections.clear();
        armorOptions.clear();
        armorIndex.clear();
        armorSelections.clear();
        attachmentSelections.clear();

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return;
        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return;

        if (kit.weapons != null) {
            if (kit.weapons.primary != null && !kit.weapons.primary.isEmpty()) {
                weaponOptions.put("Primary", kit.weapons.primary);
                selectionIndex.put("Primary", 0);
                selections.put("Primary", kit.weapons.primary.get(0));
            }
            if (kit.weapons.secondary != null && !kit.weapons.secondary.isEmpty()) {
                weaponOptions.put("Secondary", kit.weapons.secondary);
                selectionIndex.put("Secondary", 0);
                selections.put("Secondary", kit.weapons.secondary.get(0));
            }
            if (kit.weapons.special != null && !kit.weapons.special.isEmpty()) {
                weaponOptions.put("Special", kit.weapons.special);
                selectionIndex.put("Special", 0);
                selections.put("Special", kit.weapons.special.get(0));
            }
            if (kit.weapons.grenade != null && !kit.weapons.grenade.isEmpty()) {
                weaponOptions.put("Grenade", kit.weapons.grenade);
                selectionIndex.put("Grenade", 0);
                selections.put("Grenade", kit.weapons.grenade.get(0));
            }
        }

        if (kit.armor != null) {
            if (kit.armor.helmet != null && !kit.armor.helmet.isEmpty()) {
                armorOptions.put("Helmet", kit.armor.helmet);
                armorIndex.put("Helmet", 0);
                armorSelections.put("Helmet", kit.armor.helmet.get(0));
            }
            if (kit.armor.chestplate != null && !kit.armor.chestplate.isEmpty()) {
                armorOptions.put("Chestplate", kit.armor.chestplate);
                armorIndex.put("Chestplate", 0);
                armorSelections.put("Chestplate", kit.armor.chestplate.get(0));
            }
            if (kit.armor.backpack != null && !kit.armor.backpack.isEmpty()) {
                armorOptions.put("Backpack", kit.armor.backpack);
                armorIndex.put("Backpack", 0);
                armorSelections.put("Backpack", kit.armor.backpack.get(0));
            }
            if (kit.armor.shoulderpads != null && !kit.armor.shoulderpads.isEmpty()) {
                armorOptions.put("Shoulderpads", kit.armor.shoulderpads);
                armorIndex.put("Shoulderpads", 0);
                armorSelections.put("Shoulderpads", kit.armor.shoulderpads.get(0));
            }
        }

        activeSlot = selections.containsKey("Primary") ? "Primary" : selections.keySet().stream().findFirst().orElse("Primary");
        previewItem = selections.getOrDefault(activeSlot, null);
    }

    private KitConfig.KitDef getKit() {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return null;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return null;
        return cl.kits.get(kitId);
    }

    // ── RENDER ─────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        tickCount++;
        mouseX = mx;
        mouseY = my;
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        kitConfigPanel.setFade(fadeAlpha);
        kitConfigPanel.setAlpha(alpha);
        kitConfigPanel.setMouse(mx, my);

        renderFooter(g, mx, my, fadeAlpha, alpha);
        renderPreviewPanel(g, mx, my, fadeAlpha, alpha);
        kitConfigPanel.render(g, width, height);

        super.render(g, mx, my, pt);
    }

    // ── FOOTER ─────────────────────────────────────────

    private void renderFooter(GuiGraphics g, int mx, int my, float fade, int alpha) {
        KitConfig.KitDef kit = getKit();
        String name = kit != null && kit.display_name != null ? I18n.localize(kit.display_name).toUpperCase() : kitId.toUpperCase();

        g.fill(0, 0, width, FOOTER_H,
                AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xDD)));
        g.fill(0, FOOTER_H - 1, width, FOOTER_H,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA)));

        // Back button
        String back = "\u2190 Back";
        boolean backHov = mx >= 10 && mx < 10 + font.width(back) + 20 && my >= 8 && my < 8 + 22;
        if (backHov) {
            g.fill(10, 8, 10 + font.width(back) + 20, 30,
                    AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(fade * 0xCC)));
        }
        g.drawString(font, back, 18, 14,
                AnimationHelper.withAlpha(backHov ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, alpha));

        // Kit name
        g.drawString(font, name, 120, 12,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        g.drawString(font, "customization", 120 + font.width(name) + 6, 12,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180)));

        // Save button
        int btnW = 90;
        int btnX = width - btnW * 2 - 20;
        boolean svHov = mx >= btnX && mx < btnX + btnW && my >= 8 && my < 32;
        int svBg = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, svHov ? 1f : 0f);
        g.fill(btnX, 8, btnX + btnW, 32, AnimationHelper.withAlpha(svBg, alpha));
        g.fill(btnX, 8, btnX + 2, 32, AnimationHelper.withAlpha(0x33000000, alpha));
        String svTxt = "Save";
        g.drawString(font, svTxt, btnX + btnW / 2 - font.width(svTxt) / 2, 17, 0xFF000000);

        // Deploy button
        int dpX = btnX + btnW + 8;
        boolean dpHov = mx >= dpX && mx < dpX + btnW && my >= 8 && my < 32;
        int dpBg = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, dpHov ? 1f : 0f);
        g.fill(dpX, 8, dpX + btnW, 32, AnimationHelper.withAlpha(dpBg, alpha));
        g.fill(dpX, 8, dpX + 2, 32, AnimationHelper.withAlpha(0x33000000, alpha));
        String dpTxt = "Deploy";
        g.drawString(font, dpTxt, dpX + btnW / 2 - font.width(dpTxt) / 2, 17, 0xFF000000);
    }

    // ── PREVIEW PANEL ──────────────────────────────────

    private void renderPreviewPanel(GuiGraphics g, int mx, int my, float fade, int alpha) {
        int px = 0;
        int py = FOOTER_H;
        int ph = height - FOOTER_H;

        g.fill(px, py, px + PREVIEW_W, py + ph,
                AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xDD)));
        g.fill(px + PREVIEW_W - 1, py, px + PREVIEW_W, py + ph,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA)));

        // Render area
        int rx = 8;
        int ry = py + 10;
        int rh = PREVIEW_RENDER_H;
        g.fill(rx, ry, rx + PREVIEW_W - 16, ry + rh,
                AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fade * 0xDD)));
        g.fill(rx, ry, rx + PREVIEW_W - 16, ry + rh,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x44)));

        String currentId = previewItem != null ? previewItem : "";
        ItemStack stack = ItemStack.EMPTY;
        if (!currentId.isEmpty()) {
            stack = ItemResolver.resolve(currentId);
        }

        if (!stack.isEmpty()) {
            PoseStack pose = g.pose();
            pose.pushPose();
            float scale = 3.0f;
            int cx = rx + (PREVIEW_W - 16) / 2;
            int cy = ry + rh / 2 - 4;
            pose.translate(cx, cy, 0);
            pose.scale(scale, scale, 1);
            g.renderItem(stack, -8, -8);
            pose.popPose();

            String displayName = stack.getHoverName().getString();
            int dw = font.width(displayName);
            g.drawString(font, displayName, rx + (PREVIEW_W - 16) / 2 - dw / 2, ry + rh + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
        } else {
            g.drawString(font, "\uD83D\uDD2B", rx + (PREVIEW_W - 16) / 2 - 9, ry + rh / 2 - 8,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
            String noItem = currentId.isEmpty() ? "No item" : currentId.replace("_", " ").toUpperCase();
            g.drawString(font, noItem, rx + (PREVIEW_W - 16) / 2 - font.width(noItem) / 2, ry + rh / 2 + 10,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
        }

        // Accent line under render area
        g.fill(rx, ry + rh + 28, rx + PREVIEW_W - 16, ry + rh + 30,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xCC)));

        // Weapon slot list
        int sy = ry + rh + 38;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            boolean isActive = key.equals(activeSlot);
            boolean hover = mx >= 4 && mx <= PREVIEW_W - 4 && my >= sy && my <= sy + 28;

            int slotBg = isActive ? AnimationHelper.blendColors(0x00000000, UITheme.ACCENT, 0.12f)
                    : hover ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xCC))
                    : 0x00000000;
            int slotBorder = isActive ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, alpha)
                    : hover ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * 0x66))
                    : 0x00000000;

            if (slotBg != 0) {
                g.fill(4, sy, PREVIEW_W - 4, sy + 28, slotBg);
            }
            if (slotBorder != 0) {
                g.fill(4, sy, PREVIEW_W - 4, sy + 1, slotBorder);
                g.fill(4, sy + 27, PREVIEW_W - 4, sy + 28, slotBorder);
                g.fill(PREVIEW_W - 5, sy, PREVIEW_W - 4, sy + 28, slotBorder);
            }
            if (isActive) {
                g.fill(4, sy, 7, sy + 28,
                        AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
            }

            // Slot icon + label
            String icon = switch (key) {
                case "Primary" -> "\uD83D\uDD2B";
                case "Secondary" -> "\uD83D\uDD27";
                case "Special" -> "\uD83D\uDC8A";
                case "Grenade" -> "\uD83D\uDCA3";
                default -> "\u2022";
            };
            g.drawString(font, icon + " " + key, 14, sy + 8,
                    AnimationHelper.withAlpha(isActive ? UITheme.ACCENT : UITheme.TEXT_MUTED, (int)(fade * (isActive ? 255 : 180))));

            // Weapon name
            String weap = displayName(selections.getOrDefault(key, ""));
            if (weap.length() > 18) weap = weap.substring(0, 18);
            g.drawString(font, weap, 14, sy + 18,
                    AnimationHelper.withAlpha(isActive ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY, alpha));

            sy += 32;
        }

        // Armor slots
        if (!armorOptions.isEmpty()) {
            sy += 4;
            g.fill(8, sy, PREVIEW_W - 8, sy + 1,
                    AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x66)));
            sy += 8;
            for (String key : armorKeys) {
                if (!armorOptions.containsKey(key)) continue;
                boolean isActive = key.equals(activeSlot);
                boolean hover = mx >= 4 && mx <= PREVIEW_W - 4 && my >= sy && my <= sy + 24;

                int slotBg = isActive ? AnimationHelper.blendColors(0x00000000, UITheme.ACCENT, 0.12f)
                        : hover ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xCC))
                        : 0x00000000;
                int slotBorder = isActive ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, alpha)
                        : hover ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * 0x66))
                        : 0x00000000;

                if (slotBg != 0) g.fill(4, sy, PREVIEW_W - 4, sy + 24, slotBg);
                if (slotBorder != 0) {
                    g.fill(4, sy, PREVIEW_W - 4, sy + 1, slotBorder);
                    g.fill(4, sy + 23, PREVIEW_W - 4, sy + 24, slotBorder);
                    g.fill(PREVIEW_W - 5, sy, PREVIEW_W - 4, sy + 24, slotBorder);
                }
                if (isActive)
                    g.fill(4, sy, 7, sy + 24, AnimationHelper.withAlpha(UITheme.ACCENT, alpha));

                // Render actual armor item
                String armorSel = armorSelections.getOrDefault(key, "");
                ItemStack armorStack = ItemStack.EMPTY;
                if (!armorSel.isEmpty()) {
                    armorStack = ItemResolver.resolve(armorSel);
                }
                if (!armorStack.isEmpty()) {
                    g.renderItem(armorStack, 14, sy + 3);
                } else {
                    g.drawString(font, "\u2022", 18, sy + 8,
                            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
                }
                g.drawString(font, key, 32, sy + 4,
                        AnimationHelper.withAlpha(isActive ? UITheme.ACCENT : UITheme.TEXT_MUTED, (int)(fade * (isActive ? 255 : 180))));

                String armorVal = displayName(armorSel);
                if (armorVal.length() > 18) armorVal = armorVal.substring(0, 18);
                g.drawString(font, armorVal, 32, sy + 14,
                        AnimationHelper.withAlpha(isActive ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY, alpha));

                sy += 28;
            }
        }
    }

    // ── MOUSE ──────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        // Footer back button
        if (my >= 8 && my <= 32) {
            String back = "\u2190 Back";
            if (mx >= 10 && mx < 10 + font.width(back) + 20) {
                onClose();
                return true;
            }
            // Save button
            int btnW = 90;
            int btnX = width - btnW * 2 - 20;
            if (mx >= btnX && mx < btnX + btnW) {
                saveConfig();
                return true;
            }
            // Deploy button
            int dpX = btnX + btnW + 8;
            if (mx >= dpX && mx < dpX + btnW) {
                saveAndDeploy();
                return true;
            }
        }

        // Preview panel slot clicks (weapons + armor)
        if (mx >= 0 && mx <= PREVIEW_W && my >= FOOTER_H) {
            int ry = FOOTER_H + 10;
            int rh = PREVIEW_RENDER_H;
            int sy = ry + rh + 38;
            for (String key : slotKeys) {
                if (!weaponOptions.containsKey(key)) continue;
                if (my >= sy && my <= sy + 28) {
                    activeSlot = key;
                    previewItem = selections.get(key);
                    kitConfigPanel.setActiveSlot(key);
                    return true;
                }
                sy += 32;
            }
            if (!armorOptions.isEmpty()) {
                sy += 12;
                for (String key : armorKeys) {
                    if (!armorOptions.containsKey(key)) continue;
                    if (my >= sy && my <= sy + 24) {
                        activeSlot = key;
                        previewItem = armorSelections.get(key);
                        kitConfigPanel.setActiveSlot(key);
                        return true;
                    }
                    sy += 28;
                }
            }
        }

        // Config panel interactions
        String configResult = kitConfigPanel.handleConfigClick(mx, my, width, height);
        if (configResult != null) {
            String newActive = kitConfigPanel.getActiveSlot();
            if (!newActive.equals(activeSlot)) {
                activeSlot = newActive;
                previewItem = selections.get(activeSlot);
            }
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (kitConfigPanel != null) {
            kitConfigPanel.handleMouseScroll(mx, my, delta);
        }
        return true;
    }

    @Override
    public void tick() {
        if (kitConfigPanel != null) {
            kitConfigPanel.tick();
        }
    }

    // ── SAVE / DEPLOY ──────────────────────────────────

    private String buildLoadoutJson() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        for (String key : slotKeys) {
            if (!selections.containsKey(key)) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(key).append("\":\"").append(selections.get(key)).append("\"");
        }
        for (String key : armorKeys) {
            if (!armorSelections.containsKey(key)) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(key).append("\":\"").append(armorSelections.get(key)).append("\"");
        }
        if (!attachmentSelections.isEmpty()) {
            if (!first) sb.append(",");
            sb.append("\"Attachments\":{");
            boolean afirst = true;
            for (Map.Entry<String, String> entry : attachmentSelections.entrySet()) {
                if (!afirst) sb.append(",");
                afirst = false;
                sb.append("\"").append(escapeJson(entry.getKey())).append("\":\"")
                        .append(escapeJson(entry.getValue())).append("\"");
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private void saveConfig() {
        String packageId = classId + ":" + kitId;
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(packageId, buildLoadoutJson()));
    }

    private void saveAndDeploy() {
        String packageId = classId + ":" + kitId;
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(packageId, buildLoadoutJson()));
        PacketHandler.CHANNEL.sendToServer(new KitSelectPacket(packageId));
        SpawnScreenHelper.updateSelectedKit(packageId);
        onClose();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(new KitSelectionScreen(classId));
        }
    }
}
