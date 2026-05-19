package com.yourmod.teamsystem.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.data.ItemResolver;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSavePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class KitCustomizationScreen extends Screen {

    private static final int PREVIEW_W = 200;
    private static final int SLOT_H = 44;
    private static final int SLOT_GAP = 8;
    private static final int FOOTER_H = 44;
    private static final int HEADER_H = 36;
    private static final int PREVIEW_RENDER_H = 140;
    private static final int COL_GAP = 4;
    private static final int CONFIG_PAD = 14;

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
    private boolean dataChanged = false;
    private String previewItem;

    private float fadeAlpha = 0f;
    private long openTime;
    private int mouseX, mouseY;
    private BScrollPanel scrollPanel;
    private com.yourmod.teamsystem.client.gui.component.BButton saveButton;
    private int panelX, panelY, panelW, panelH, configX, configW;

    private static final int ARROW_W = 18;
    private static final int ARROW_H = 18;

    public KitCustomizationScreen(String classId, String kitId) {
        super(Component.literal("Kit Customization"));
        this.classId = classId;
        this.kitId = kitId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();

        panelW = Math.min(width - 40, 780);
        panelH = height - 40;
        panelX = width / 2 - panelW / 2;
        panelY = 20;
        configX = panelX + PREVIEW_W + COL_GAP;
        configW = panelW - PREVIEW_W - COL_GAP;

        int scrollH = panelH - HEADER_H - FOOTER_H - 16;
        scrollPanel = new BScrollPanel(configX, panelY + HEADER_H + 8, configW, scrollH);

        loadData();

        int btnY = panelY + panelH - FOOTER_H + 4;
        addRenderableWidget(new com.yourmod.teamsystem.client.gui.component.BButton(
            panelX + 10, btnY, 90, 20,
            Component.literal("\u2190 Back"), btn -> onClose(), false
        ));
        saveButton = addRenderableWidget(new com.yourmod.teamsystem.client.gui.component.BButton(
            panelX + panelW - 130, btnY, 120, 20,
            Component.literal("Save & Deploy"), btn -> saveAndDeploy()
        ));
    }

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

        activeSlot = selections.containsKey("Primary") ? "Primary" : "Secondary";
        previewItem = selections.getOrDefault(activeSlot, null);

        resetScrollContent();
    }

    private void resetScrollContent() {
        int contentH = 0;
        contentH += slotKeys.size() * (SLOT_H + SLOT_GAP) + SLOT_GAP;
        String activeWeapon = selections.getOrDefault(activeSlot, "");
        boolean hasAttachments = hasAttachmentLimits(activeWeapon);
        if (hasAttachments) {
            contentH += 80 + getAttachmentCategories(activeWeapon).size() * 30;
        }
        if (!armorOptions.isEmpty()) {
            contentH += 20 + armorOptions.size() * (SLOT_H + SLOT_GAP);
        }
        scrollPanel.setContentHeight(contentH);
    }

    private boolean hasAttachmentLimits(String weaponId) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return false;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return false;
        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return false;
        return kit.attachment_limits.containsKey(weaponId);
    }

    private List<String> getAttachmentCategories(String weaponId) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return List.of();
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return List.of();
        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return List.of();
        KitConfig.AttachmentLimit limits = kit.attachment_limits.get(weaponId);
        if (limits == null) return List.of();
        List<String> cats = new ArrayList<>();
        if (!limits.scope.isEmpty()) cats.add("scope");
        if (!limits.barrel.isEmpty()) cats.add("barrel");
        if (!limits.grip.isEmpty()) cats.add("grip");
        if (!limits.magazine.isEmpty()) cats.add("magazine");
        if (!limits.ammo.isEmpty()) cats.add("ammo");
        if (!limits.muzzle.isEmpty()) cats.add("muzzle");
        if (!limits.underbarrel.isEmpty()) cats.add("underbarrel");
        return cats;
    }

    private List<String> getAttachmentOptions(String weaponId, String category) {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return List.of();
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return List.of();
        KitConfig.KitDef kit = cl.kits.get(kitId);
        if (kit == null) return List.of();
        KitConfig.AttachmentLimit limits = kit.attachment_limits.get(weaponId);
        if (limits == null) return List.of();
        return limits.forCategory(category);
    }

    private String getAttachmentSelection(String weaponId, String category) {
        String key = weaponId + ":" + category;
        return attachmentSelections.get(key);
    }

    private void setAttachmentSelection(String weaponId, String category, String value) {
        String key = weaponId + ":" + category;
        attachmentSelections.put(key, value);
    }

    private String cycleArmor(String slot, int direction) {
        List<String> options = armorOptions.get(slot);
        if (options == null || options.isEmpty()) return "";
        int idx = armorIndex.getOrDefault(slot, 0);
        idx = (idx + direction + options.size()) % options.size();
        armorIndex.put(slot, idx);
        String sel = options.get(idx);
        armorSelections.put(slot, sel);
        dataChanged = true;
        return sel;
    }

    private String cycleWeapon(String slot, int direction) {
        List<String> options = weaponOptions.get(slot);
        if (options == null || options.isEmpty()) return "";
        int idx = selectionIndex.getOrDefault(slot, 0);
        idx = (idx + direction + options.size()) % options.size();
        selectionIndex.put(slot, idx);
        String sel = options.get(idx);
        selections.put(slot, sel);
        if (slot.equals(activeSlot)) {
            previewItem = sel;
            resetScrollContent();
        }
        dataChanged = true;
        return sel;
    }

    private String cycleAttachment(String weaponId, String category, int direction) {
        List<String> options = getAttachmentOptions(weaponId, category);
        if (options.isEmpty()) return "";
        String current = getAttachmentSelection(weaponId, category);
        int idx = current != null ? options.indexOf(current) : -1;
        if (idx < 0) idx = 0;
        else idx = (idx + direction + options.size()) % options.size();
        String sel = options.get(idx);
        setAttachmentSelection(weaponId, category, sel);
        dataChanged = true;
        return sel;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        mouseX = mx;
        mouseY = my;
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xDD)));

        renderHeader(g);
        renderPreviewPanel(g);
        renderConfigPanel(g);
        super.render(g, mx, my, pt);
    }

    private void renderHeader(GuiGraphics g) {
        KitConfig cfg = KitConfig.get();
        String title;
        if (cfg != null && cfg.classes.containsKey(classId)) {
            KitConfig.ClassConfig cl = cfg.classes.get(classId);
            KitConfig.KitDef k = cl.kits.get(kitId);
            if (k != null && k.display_name != null) {
                title = I18n.localize(k.display_name).toUpperCase();
            } else {
                title = kitId.toUpperCase();
            }
        } else {
            title = kitId.toUpperCase();
        }
        int tw = font.width(title);
        g.drawString(font, title, panelX + panelW / 2 - tw / 2, panelY + 10,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
    }

    private void renderPreviewPanel(GuiGraphics g) {
        int px = panelX;
        int py = panelY + HEADER_H;
        int ph = panelH - HEADER_H - FOOTER_H;

        g.fill(px, py, px + PREVIEW_W, py + ph,
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0xDD)));
        g.fill(px + PREVIEW_W - 1, py, px + PREVIEW_W, py + ph,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xAA)));

        int renderY = py + 10;
        int renderH = PREVIEW_RENDER_H;
        g.fill(px + 4, renderY, px + PREVIEW_W - 4, renderY + renderH,
            AnimationHelper.withAlpha(0xFF000000, (int)(fadeAlpha * 0x66)));

        String currentId = previewItem != null ? previewItem : "";
        ItemStack previewStack = ItemStack.EMPTY;
        if (!currentId.isEmpty()) {
            previewStack = ItemResolver.resolve(currentId);
        }

        if (!previewStack.isEmpty()) {
            PoseStack pose = g.pose();
            pose.pushPose();
            float scale = 3.0f;
            int renderCX = px + PREVIEW_W / 2;
            int renderCY = renderY + renderH / 2 - 10;
            pose.translate(renderCX, renderCY, 0);
            pose.scale(scale, scale, 1);
            g.renderItem(previewStack, -8, -8);
            pose.popPose();

            String displayName = previewStack.getHoverName().getString();
            int dw = font.width(displayName);
            g.drawString(font, displayName, px + PREVIEW_W / 2 - dw / 2, renderY + renderH + 2,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

            String subInfo = currentId;
            int sw = font.width(subInfo);
            g.drawString(font, subInfo, px + PREVIEW_W / 2 - sw / 2, renderY + renderH + 14,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 200)));
        } else {
            String noItem = "No item";
            int nw = font.width(noItem);
            g.drawString(font, noItem, px + PREVIEW_W / 2 - nw / 2, renderY + renderH / 2 - 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
        }

        int slotStartY = renderY + renderH + 30;
        int slotIdx = 0;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            int sy = slotStartY + slotIdx * (SLOT_H + 4);
            boolean active = key.equals(activeSlot);
            boolean hover = mouseX >= px + 4 && mouseX <= px + PREVIEW_W - 4
                && mouseY >= sy && mouseY <= sy + SLOT_H;
            renderSlotCard(g, px + 4, sy, PREVIEW_W - 8, key, active, hover);
            slotIdx++;
        }
    }

    private void renderSlotCard(GuiGraphics g, int x, int y, int w, String slotKey, boolean active, boolean hover) {
        int bg = active ? AnimationHelper.blendColors(UITheme.BG_SLOT, UITheme.ACCENT, 0.12f)
                        : AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fadeAlpha * 0xDD));
        int brd = active ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255))
                         : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * (hover ? 0xAA : 0x55)));

        g.fill(x, y, x + w, y + SLOT_H, bg);
        g.fill(x, y, x + w, y + 1, brd);
        g.fill(x, y + SLOT_H - 1, x + w, y + SLOT_H, brd);
        g.fill(x, y, x + 1, y + SLOT_H, brd);
        g.fill(x + w - 1, y, x + w, y + SLOT_H, brd);

        if (active) {
            g.fill(x, y, x + 3, y + SLOT_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        }

        String sel = selections.getOrDefault(slotKey, "");
        String displayName = sel.isEmpty() ? "-" : ItemResolver.getDisplayName(sel);

        g.drawString(font, slotKey, x + 8, y + 6,
            AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 255)));
        g.drawString(font, displayName, x + 8, y + 22,
            AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

        if (hover) {
            String nav = "\u25C0 \u25B6";
            int nw = font.width(nav);
            g.drawString(font, nav, x + w - nw - 6, y + SLOT_H / 2 - 4,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));
        }
    }

    private void renderConfigPanel(GuiGraphics g) {
        int cx = configX;
        int cy = panelY + HEADER_H + 8;
        int cw = configW;
        int ch = panelH - HEADER_H - FOOTER_H - 16;

        g.enableScissor(cx, cy, cx + cw, cy + ch);
        scrollPanel.render(g);

        float soff = scrollPanel.getScrollOffset();
        int curY = cy - (int) soff + 4;

        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            renderConfigSlot(g, cx + CONFIG_PAD, curY, cw - CONFIG_PAD * 2, key);
            curY += SLOT_H + SLOT_GAP;
        }

        curY += SLOT_GAP;

        String activeWeapon = selections.getOrDefault(activeSlot, "");
        boolean hasAttachments = hasAttachmentLimits(activeWeapon);
        if (hasAttachments) {
            int secY = curY;
            g.drawString(font, "ATTACHMENTS", cx + CONFIG_PAD, secY,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
            curY += 12;

            List<String> cats = getAttachmentCategories(activeWeapon);
            for (String cat : cats) {
                List<String> opts = getAttachmentOptions(activeWeapon, cat);
                String sel = getAttachmentSelection(activeWeapon, cat);
                if (sel == null && !opts.isEmpty()) {
                    sel = opts.get(0);
                    setAttachmentSelection(activeWeapon, cat, sel);
                }

                g.drawString(font, capitalize(cat) + ":", cx + CONFIG_PAD, curY + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

                int chipX = cx + CONFIG_PAD + font.width(capitalize(cat) + ": ") + 8;
                for (String opt : opts) {
                    boolean selected = opt.equals(sel);
                    int chipW = font.width(formatAttachName(opt)) + 16;
                    int chipH = 22;

                    boolean chipHov = mouseX >= chipX && mouseX <= chipX + chipW
                        && mouseY >= curY && mouseY <= curY + chipH;

                    int chipBg = selected ? AnimationHelper.withAlpha(UITheme.ACCENT, 0xDD)
                        : chipHov ? AnimationHelper.withAlpha(UITheme.BG_SLOT, 0xAA)
                        : AnimationHelper.withAlpha(UITheme.BG_SURFACE, 0x88);
                    int chipTxt = selected ? 0xFFFFFFFF
                        : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255));

                    g.fill(chipX, curY, chipX + chipW, curY + chipH, chipBg);
                    if (!selected) {
                        g.fill(chipX, curY, chipX + chipW, curY + 1, AnimationHelper.withAlpha(UITheme.BORDER, 0x66));
                        g.fill(chipX, curY + chipH - 1, chipX + chipW, curY + chipH, AnimationHelper.withAlpha(UITheme.BORDER, 0x66));
                    }
                    if (selected) {
                        g.fill(chipX, curY, chipX + chipW, curY + 1, 0xFFFFFFFF);
                    }

                    g.drawString(font, formatAttachName(opt), chipX + 8, curY + 6,
                        chipTxt);

                    chipX += chipW + 4;
                }
                curY += 26;
            }
            curY += 8;
        }

        if (!armorOptions.isEmpty()) {
            g.drawString(font, "ARMOR", cx + CONFIG_PAD, curY,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
            curY += 12;

            for (String key : armorKeys) {
                if (!armorOptions.containsKey(key)) continue;
                renderConfigArmorRow(g, cx + CONFIG_PAD, curY, cw - CONFIG_PAD * 2, key);
                curY += SLOT_H + SLOT_GAP;
            }
        }

        g.disableScissor();
    }

    private void renderConfigSlot(GuiGraphics g, int x, int y, int w, String slotKey) {
        boolean active = slotKey.equals(activeSlot);
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + SLOT_H;

        int bg = active ? AnimationHelper.blendColors(UITheme.BG_SLOT, UITheme.ACCENT, 0.12f)
                        : AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fadeAlpha * 0xDD));
        int brd = active ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255))
                         : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * (hover ? 0xAA : 0x55)));

        g.fill(x, y, x + w, y + SLOT_H, bg);
        g.fill(x, y, x + w, y + 1, brd);
        g.fill(x, y + SLOT_H - 1, x + w, y + SLOT_H, brd);

        if (active) {
            g.fill(x, y, x + 3, y + SLOT_H,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        }

        // Slot label
        g.drawString(font, slotKey, x + 10, y + 6,
            AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 255)));

        // Weapon name
        String sel = selections.getOrDefault(slotKey, "");
        String displayName = sel.isEmpty() ? "-" : ItemResolver.getDisplayName(sel);
        g.drawString(font, displayName, x + 10, y + 22,
            AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

        // Arrows
        String left = "\u25C0";
        int lw = font.width(left);
        int arrowX = x + w - 36;
        g.drawString(font, left, arrowX, y + SLOT_H / 2 - 4,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));
        g.drawString(font, "\u25B6", arrowX + 18, y + SLOT_H / 2 - 4,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));
    }

    private void renderConfigArmorRow(GuiGraphics g, int x, int y, int w, String armorKey) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + SLOT_H;

        g.fill(x, y, x + w, y + SLOT_H,
            AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fadeAlpha * 0xDD)));
        g.fill(x, y, x + w, y + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * (hover ? 0xAA : 0x55))));
        g.fill(x, y + SLOT_H - 1, x + w, y + SLOT_H,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * (hover ? 0xAA : 0x55))));

        g.drawString(font, armorKey, x + 10, y + 6,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 255)));

        String sel = armorSelections.getOrDefault(armorKey, "");
        String displayName = sel.isEmpty() ? "None" : ItemResolver.getDisplayName(sel);
        g.drawString(font, displayName, x + 10, y + 22,
            AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

        String left = "\u25C0";
        int arrowX = x + w - 36;
        g.drawString(font, left, arrowX, y + SLOT_H / 2 - 4,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));
        g.drawString(font, "\u25B6", arrowX + 18, y + SLOT_H / 2 - 4,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);
        int cx = configX;
        int cw = configW;
        float soff = scrollPanel.getScrollOffset();
        int baseY = panelY + HEADER_H + 8;
        int scrollContentStart = baseY - (int) soff + 4;

        // Click on config panel slot cards
        int curY = scrollContentStart;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) { curY += SLOT_H + SLOT_GAP; continue; }
            int sx = cx + CONFIG_PAD;
            int sy = curY;
            if (mx >= sx && mx <= sx + cw - CONFIG_PAD * 2 && my >= sy && my <= sy + SLOT_H) {
                int arrowX = sx + cw - CONFIG_PAD * 2 - 36;
                if (mx >= arrowX && mx <= arrowX + ARROW_W) {
                    cycleWeapon(key, -1);
                } else if (mx >= arrowX + 18 && mx <= arrowX + 36) {
                    cycleWeapon(key, 1);
                } else {
                    activeSlot = key;
                    previewItem = selections.get(key);
                    resetScrollContent();
                }
                return true;
            }
            curY += SLOT_H + SLOT_GAP;
        }

        // Click on armor rows
        curY += SLOT_GAP;
        String activeWeapon = selections.getOrDefault(activeSlot, "");
        if (hasAttachmentLimits(activeWeapon)) {
            curY += 20 + getAttachmentCategories(activeWeapon).size() * 26 + 8;
        }
        if (!armorOptions.isEmpty()) {
            curY += 12;
            for (String key : armorKeys) {
                if (!armorOptions.containsKey(key)) { curY += SLOT_H + SLOT_GAP; continue; }
                int sx = cx + CONFIG_PAD;
                int sy = curY;
                if (mx >= sx && mx <= sx + cw - CONFIG_PAD * 2 && my >= sy && my <= sy + SLOT_H) {
                    int arrowX = sx + cw - CONFIG_PAD * 2 - 36;
                    if (mx >= arrowX && mx <= arrowX + ARROW_W) {
                        cycleArmor(key, -1);
                    } else if (mx >= arrowX + 18 && mx <= arrowX + 36) {
                        cycleArmor(key, 1);
                    }
                    return true;
                }
                curY += SLOT_H + SLOT_GAP;
            }
        }

        // Click on attachment chips
        curY = scrollContentStart + 4;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            curY += SLOT_H + SLOT_GAP;
        }
        curY += SLOT_GAP;
        if (hasAttachmentLimits(activeWeapon)) {
            curY += 12;
            List<String> cats = getAttachmentCategories(activeWeapon);
            for (String cat : cats) {
                List<String> opts = getAttachmentOptions(activeWeapon, cat);
                int chipX = cx + CONFIG_PAD + font.width(capitalize(cat) + ": ") + 8;
                for (String opt : opts) {
                    int chipW = font.width(formatAttachName(opt)) + 16;
                    int chipH = 22;
                    if (mx >= chipX && mx <= chipX + chipW && my >= curY && my <= curY + chipH) {
                        setAttachmentSelection(activeWeapon, cat, opt);
                        dataChanged = true;
                        return true;
                    }
                    chipX += chipW + 4;
                }
                curY += 26;
            }
        }

        // Click on preview panel slot cards
        int renderY = panelY + HEADER_H + 10;
        int slotStartY = renderY + PREVIEW_RENDER_H + 30;
        int slotIdx = 0;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            int sy = slotStartY + slotIdx * (SLOT_H + 4);
            if (mx >= panelX + 4 && mx <= panelX + PREVIEW_W - 4 && my >= sy && my <= sy + SLOT_H) {
                activeSlot = key;
                previewItem = selections.get(key);
                resetScrollContent();
                return true;
            }
            slotIdx++;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx >= configX && mx <= configX + configW) {
            scrollPanel.onScroll((int) mx, (int) my, delta);
        }
        return true;
    }

    @Override
    public void tick() {
        scrollPanel.tick();
    }

    private void saveAndDeploy() {
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
        PacketHandler.CHANNEL.sendToServer(new KitSavePacket(classId + ":" + kitId, sb.toString()));
        onClose();
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String formatAttachName(String name) {
        if (name == null || name.isEmpty()) return name;
        String spaced = name.replace('_', ' ');
        if (spaced.isEmpty()) return spaced;
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = true;
        for (char c : spaced.toCharArray()) {
            if (c == ' ') {
                sb.append(c);
                nextUpper = true;
            } else if (nextUpper) {
                sb.append(Character.toUpperCase(c));
                nextUpper = false;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(new KitSelectionScreen(classId));
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
