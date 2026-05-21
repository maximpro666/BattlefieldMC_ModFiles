package com.pigeostudios.pwp.client.gui.component;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.data.TaczAttachmentResolver;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.ItemResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;

import java.util.*;

public class KitConfigPanel {

    private static final int SLOT_H = 34;
    private static final int SLOT_GAP = 6;
    private static final int FOOTER_H = 40;
    private static final int HEADER_H = 36;
    private static final int PREVIEW_W = 190;

    private final String classId;
    private final String kitId;
    private final List<String> slotKeys;
    private final Map<String, List<String>> weaponOptions;
    private final Map<String, Integer> selectionIndex;
    private final Map<String, String> selections;
    private final List<String> armorKeys;
    private final Map<String, List<String>> armorOptions;
    private final Map<String, Integer> armorIndex;
    private final Map<String, String> armorSelections;
    private final Map<String, String> attachmentSelections;
    private final Runnable onDataChanged;

    private String activeSlot;
    private BScrollPanel scrollPanel;
    private float fade;
    private int alpha;
    private int mouseX, mouseY;
    private final Map<String, String> displayNames = new HashMap<>();

    private String displayName(String id) {
        if (id == null || id.isEmpty()) return "\u2014";
        return displayNames.computeIfAbsent(id, k -> {
            ItemStack stack = ItemResolver.resolve(k);
            if (!stack.isEmpty()) return stack.getHoverName().getString();
            return k.replace("_", " ").toUpperCase();
        });
    }

    private static final String[] ICONS = {"\uD83D\uDD2B", "\uD83D\uDD27", "\uD83D\uDC8A", "\uD83D\uDCA3"};

    public KitConfigPanel(String classId, String kitId,
                          List<String> slotKeys,
                          Map<String, List<String>> weaponOptions,
                          Map<String, Integer> selectionIndex,
                          Map<String, String> selections,
                          List<String> armorKeys,
                          Map<String, List<String>> armorOptions,
                          Map<String, Integer> armorIndex,
                          Map<String, String> armorSelections,
                          Map<String, String> attachmentSelections,
                          String activeSlot,
                          Runnable onDataChanged) {
        this.classId = classId;
        this.kitId = kitId;
        this.slotKeys = slotKeys;
        this.weaponOptions = weaponOptions;
        this.selectionIndex = selectionIndex;
        this.selections = selections;
        this.armorKeys = armorKeys;
        this.armorOptions = armorOptions;
        this.armorIndex = armorIndex;
        this.armorSelections = armorSelections;
        this.attachmentSelections = attachmentSelections;
        this.activeSlot = activeSlot;
        this.onDataChanged = onDataChanged;
    }

    public String getActiveSlot() { return activeSlot; }
    public void setActiveSlot(String slot) { this.activeSlot = slot; }
    public void setFade(float f) { this.fade = f; }
    public void setAlpha(int a) { this.alpha = a; }
    public void setMouse(int mx, int my) { this.mouseX = mx; this.mouseY = my; }

    public void init(int width, int height) {
        int configX = PREVIEW_W + 14;
        int configW = width - configX - 14;
        int scrollTop = HEADER_H + 8;
        int scrollH = height - HEADER_H - FOOTER_H - 16;
        scrollPanel = new BScrollPanel(configX, scrollTop, configW, scrollH);
        recalcContent();
    }

    public void tick() {
        if (scrollPanel != null) scrollPanel.tick();
    }

    public boolean handleMouseScroll(double mx, double my, double delta) {
        if (scrollPanel != null && mx >= PREVIEW_W + 8) {
            scrollPanel.onScroll((int) mx, (int) my, delta);
            return true;
        }
        return false;
    }

    public int recalcContent() {
        if (scrollPanel == null) return 0;
        int h = 0;
        h += 10;
        h += weaponOptions.size() * (SLOT_H + SLOT_GAP);
        String activeWeapon = selections.getOrDefault(activeSlot, "");
        if (hasAttachments(activeWeapon)) {
            h += 20;
            h += getAttachmentCategories(activeWeapon).size() * 28;
        }
        if (!armorOptions.isEmpty()) {
            h += 20;
            h += armorOptions.size() * (SLOT_H + SLOT_GAP);
        }
        scrollPanel.setContentHeight(h);
        return h;
    }

    public void render(GuiGraphics g, int width, int height) {
        int cx = PREVIEW_W + 8;
        int cw = width - cx - 8;
        int cy = HEADER_H + 8;
        int ch = height - HEADER_H - FOOTER_H - 16;
        var font = Minecraft.getInstance().font;

        g.enableScissor(cx, cy, cx + cw, cy + ch);
        scrollPanel.render(g);

        float soff = scrollPanel.getScrollOffset();
        int curY = cy - (int) soff + 4;

        // Section header left implicit — slot names suffice
        curY += 4;

        int iconIdx = 0;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            renderWeaponRow(g, cx + 8, curY, cw - 16, key, ICONS[Math.min(iconIdx, 3)], font);
            curY += SLOT_H + SLOT_GAP;
            iconIdx++;
        }

        curY += SLOT_GAP;

        String activeWeapon = selections.getOrDefault(activeSlot, "");
        if (hasAttachments(activeWeapon)) {
            curY += 4;

            List<String> cats = getAttachmentCategories(activeWeapon);
            for (String cat : cats) {
                List<String> opts = getAttachmentOptions(activeWeapon, cat);
                String sel = attachmentSelections.get(activeWeapon + ":" + cat);
                if (sel == null && !opts.isEmpty()) {
                    sel = opts.get(0);
                    attachmentSelections.put(activeWeapon + ":" + cat, sel);
                }

                g.drawString(font, cat.toUpperCase() + ":", cx + 10, curY + 4,
                        AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fade * 200)));

                int chipX = cx + 16 + font.width(cat.toUpperCase() + ": ");
                for (String opt : opts) {
                    boolean chipSel = opt.equals(sel);
                    String optDisplay = opt.replace("_", " ").toUpperCase();
                    int chipW = font.width(optDisplay) + 16;
                    int chipH = 22;
                    boolean chipHov = mouseX >= chipX && mouseX <= chipX + chipW && mouseY >= curY && mouseY <= curY + chipH;

                    int chipBg = chipSel ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xDD))
                            : chipHov ? AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fade * 0xCC))
                            : AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0x88));
                    int chipBrd = chipSel ? AnimationHelper.withAlpha(UITheme.ACCENT, alpha)
                            : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * (chipHov ? 0x88 : 0x44)));

                    g.fill(chipX, curY, chipX + chipW, curY + chipH, chipBg);
                    g.fill(chipX, curY, chipX + chipW, curY + 1, chipBrd);
                    g.fill(chipX, curY + chipH - 1, chipX + chipW, curY + chipH, chipBrd);
                    g.fill(chipX + chipW - 1, curY, chipX + chipW, curY + chipH, chipBrd);

                    g.drawString(font, optDisplay, chipX + 8, curY + 6,
                            chipSel ? 0xFFFFFFFF : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));

                    chipX += chipW + 4;
                }
                curY += 26;
            }
            curY += 6;
        }

        if (!armorOptions.isEmpty()) {
            curY += 4;
            for (String key : armorKeys) {
                if (!armorOptions.containsKey(key)) continue;
                renderArmorRow(g, cx + 8, curY, cw - 16, key, font);
                curY += SLOT_H + SLOT_GAP;
            }
        }

        g.disableScissor();
    }

    private void renderWeaponRow(GuiGraphics g, int x, int y, int w, String key, String icon, net.minecraft.client.gui.Font font) {
        boolean active = key.equals(activeSlot);
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + SLOT_H;

        int bg = active ? AnimationHelper.blendColors(UITheme.BG_SLOT, UITheme.ACCENT, 0.12f)
                : AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fade * 0xDD));
        g.fill(x, y, x + w, y + SLOT_H, bg);
        g.fill(x, y, x + w, y + 1, AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.BORDER, (int)(fade * (active ? alpha : 0x66))));
        g.fill(x, y + SLOT_H - 1, x + w, y + SLOT_H, AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.BORDER, (int)(fade * (active ? alpha : 0x66))));

        g.drawString(font, icon, x + 8, y + 8,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
        g.drawString(font, key, x + 28, y + 6,
                AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_MUTED, (int)(fade * 180)));

        String sel = selections.getOrDefault(key, "");
        String display = sel.isEmpty() ? "" : displayName(sel);
        g.drawString(font, display, x + 28, y + 18,
                AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, alpha));

        List<String> opts = weaponOptions.get(key);
        if (opts != null && opts.size() > 1) {
            int arrowY = y + SLOT_H / 2 - 4;
            int leftX = x + w - 28;
            g.drawString(font, "\u2039", leftX, arrowY,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 200)));
            g.drawString(font, "\u203A", leftX + 12, arrowY,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 200)));
        }
    }

    private void renderArmorRow(GuiGraphics g, int x, int y, int w, String key, net.minecraft.client.gui.Font font) {
        int bg = AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fade * 0xDD));
        g.fill(x, y, x + w, y + SLOT_H, bg);
        g.fill(x, y, x + w, y + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x66)));
        g.fill(x, y + SLOT_H - 1, x + w, y + SLOT_H, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0x66)));

        // Render actual item icon
        String sel = armorSelections.getOrDefault(key, "");
        ItemStack stack = ItemStack.EMPTY;
        if (!sel.isEmpty()) {
            stack = ItemResolver.resolve(sel);
        }
        if (!stack.isEmpty()) {
            g.renderItem(stack, x + 8, y + 6);
        } else {
            g.drawString(font, "\u2022", x + 12, y + 10,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
        }

        g.drawString(font, key, x + 32, y + 6,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 180)));
        String display = sel.isEmpty() ? "" : displayName(sel);
        g.drawString(font, display, x + 32, y + 18,
                AnimationHelper.withAlpha(sel.isEmpty() ? UITheme.TEXT_MUTED : UITheme.TEXT_PRIMARY, alpha));

        List<String> opts = armorOptions.get(key);
        if (opts != null && opts.size() > 1) {
            int arrowY = y + SLOT_H / 2 - 4;
            int leftX = x + w - 28;
            g.drawString(font, "\u2039", leftX, arrowY,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 200)));
            g.drawString(font, "\u203A", leftX + 12, arrowY,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 200)));
        }
    }

    public String handleConfigClick(double mx, double my, int width, int height) {
        int cx = PREVIEW_W + 8;
        int cw = width - cx - 8;
        int cy = HEADER_H + 8;
        int ch = height - HEADER_H - FOOTER_H - 16;
        if (!(mx >= cx && mx <= cx + cw && my >= cy && my <= cy + ch)) return null;

        float soff = scrollPanel.getScrollOffset();
        int curY = cy - (int) soff + 4;

        curY += 4;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            int sx = PREVIEW_W + 16;
            int sy = curY;
            int sw = cw - 16;
            if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + SLOT_H) {
                int leftX = sx + sw - 28;
                if (mx >= leftX && mx <= leftX + 10) {
                    cycleWeapon(key, -1);
                } else if (mx >= leftX + 12 && mx <= leftX + 22) {
                    cycleWeapon(key, 1);
                } else {
                    activeSlot = key;
                }
                if (onDataChanged != null) onDataChanged.run();
                return activeSlot;
            }
            curY += SLOT_H + SLOT_GAP;
        }

        curY += SLOT_GAP;
        String activeWeapon = selections.getOrDefault(activeSlot, "");
        if (hasAttachments(activeWeapon)) {
            curY += 4;
            List<String> cats = getAttachmentCategories(activeWeapon);
            for (String cat : cats) {
                List<String> opts = getAttachmentOptions(activeWeapon, cat);
                int chipX = PREVIEW_W + 24 + Minecraft.getInstance().font.width(cat.toUpperCase() + ": ");
                for (String opt : opts) {
                    String optDisplay = opt.replace("_", " ").toUpperCase();
                    int chipW = Minecraft.getInstance().font.width(optDisplay) + 16;
                    int chipH = 22;
                    if (mx >= chipX && mx <= chipX + chipW && my >= curY && my <= curY + chipH) {
                        attachmentSelections.put(activeWeapon + ":" + cat, opt);
                        if (onDataChanged != null) onDataChanged.run();
                        return "attachment";
                    }
                    chipX += chipW + 4;
                }
                curY += 26;
            }
            curY += 6;
        }

        if (!armorOptions.isEmpty()) {
            curY += 4;
            for (String key : armorKeys) {
                if (!armorOptions.containsKey(key)) continue;
                int sx = PREVIEW_W + 16;
                int sy = curY;
                int sw = cw - 16;
                if (mx >= sx && mx <= sx + sw && my >= sy && my <= sy + SLOT_H) {
                    int leftX = sx + sw - 28;
                    if (mx >= leftX && mx <= leftX + 10) {
                        cycleArmor(key, -1);
                    } else if (mx >= leftX + 12 && mx <= leftX + 22) {
                        cycleArmor(key, 1);
                    }
                    if (onDataChanged != null) onDataChanged.run();
                    return "armor";
                }
                curY += SLOT_H + SLOT_GAP;
            }
        }

        return null;
    }

    private String cycleWeapon(String slot, int dir) {
        List<String> opts = weaponOptions.get(slot);
        if (opts == null || opts.size() <= 1) return selections.getOrDefault(slot, "");
        int idx = selectionIndex.getOrDefault(slot, 0);
        idx = (idx + dir + opts.size()) % opts.size();
        selectionIndex.put(slot, idx);
        String sel = opts.get(idx);
        selections.put(slot, sel);
        if (slot.equals(activeSlot)) {
            recalcContent();
        }
        return sel;
    }

    private String cycleArmor(String slot, int dir) {
        List<String> opts = armorOptions.get(slot);
        if (opts == null || opts.isEmpty()) return "";
        int idx = armorIndex.getOrDefault(slot, 0);
        idx = (idx + dir + opts.size()) % opts.size();
        armorIndex.put(slot, idx);
        String sel = opts.get(idx);
        armorSelections.put(slot, sel);
        return sel;
    }

    private boolean hasAttachments(String weaponId) {
        if (TaczAttachmentResolver.hasAttachments(weaponId)) return true;
        KitConfig.KitDef kit = getKit();
        return kit != null && kit.attachment_limits != null && kit.attachment_limits.containsKey(weaponId);
    }

    private List<String> getAttachmentCategories(String weaponId) {
        List<String> tacz = TaczAttachmentResolver.getCategories(weaponId);
        if (!tacz.isEmpty()) return tacz;
        KitConfig.KitDef kit = getKit();
        if (kit == null || kit.attachment_limits == null) return List.of();
        var limits = kit.attachment_limits.get(weaponId);
        if (limits == null) return List.of();
        List<String> cats = new ArrayList<>();
        if (limits.scope != null && !limits.scope.isEmpty()) cats.add("scope");
        if (limits.barrel != null && !limits.barrel.isEmpty()) cats.add("barrel");
        if (limits.grip != null && !limits.grip.isEmpty()) cats.add("grip");
        if (limits.magazine != null && !limits.magazine.isEmpty()) cats.add("magazine");
        if (limits.ammo != null && !limits.ammo.isEmpty()) cats.add("ammo");
        if (limits.muzzle != null && !limits.muzzle.isEmpty()) cats.add("muzzle");
        if (limits.underbarrel != null && !limits.underbarrel.isEmpty()) cats.add("underbarrel");
        return cats;
    }

    private List<String> getAttachmentOptions(String weaponId, String category) {
        List<String> tacz = TaczAttachmentResolver.getOptions(weaponId, category);
        if (!tacz.isEmpty()) return tacz;
        KitConfig.KitDef kit = getKit();
        if (kit == null || kit.attachment_limits == null) return List.of();
        var limits = kit.attachment_limits.get(weaponId);
        if (limits == null) return List.of();
        return limits.forCategory(category);
    }

    private KitConfig.KitDef getKit() {
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return null;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return null;
        return cl.kits.get(kitId);
    }
}