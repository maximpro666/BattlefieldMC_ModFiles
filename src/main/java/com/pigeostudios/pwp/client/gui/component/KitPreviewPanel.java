package com.pigeostudios.pwp.client.gui.component;

import com.mojang.blaze3d.vertex.PoseStack;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.data.ItemResolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

public class KitPreviewPanel {

    public static final int PREVIEW_W = 190;
    private static final int PREVIEW_RENDER_H = 110;
    private static final String[] SLOT_ICONS = {"\uD83D\uDD2B", "\uD83D\uDD27", "\uD83D\uDC8A", "\uD83D\uDCA3"};

    private final List<String> slotKeys;
    private final Map<String, List<String>> weaponOptions;
    private final Map<String, String> selections;
    private final FontAccessor font;

    private String activeSlot;
    private String previewItem;

    public KitPreviewPanel(List<String> slotKeys, Map<String, List<String>> weaponOptions,
                           Map<String, String> selections, String initialSlot) {
        this.slotKeys = slotKeys;
        this.weaponOptions = weaponOptions;
        this.selections = selections;
        this.activeSlot = initialSlot;
        this.previewItem = selections.get(initialSlot);
        this.font = new FontAccessor();
    }

    public String getActiveSlot() { return activeSlot; }
    public String getPreviewItem() { return previewItem; }
    public void setPreviewItem(String item) { this.previewItem = item; }
    public void setActiveSlot(String slot) {
        this.activeSlot = slot;
        this.previewItem = selections.get(slot);
    }

    public void render(GuiGraphics g, int x, int y, int h, int mx, int my, float fade, int alpha) {
        g.fill(x, y, x + PREVIEW_W, y + h,
                AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xDD)));
        g.fill(x + PREVIEW_W - 1, y, x + PREVIEW_W, y + h,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA)));

        renderRenderArea(g, x, y, fade);
        renderSlotList(g, x, y, mx, my, fade, alpha);
    }

    private void renderRenderArea(GuiGraphics g, int px, int py, float fade) {
        int rx = px + 8;
        int ry = py + 10;
        g.fill(rx, ry, rx + PREVIEW_W - 16, ry + PREVIEW_RENDER_H,
                AnimationHelper.withAlpha(UITheme.BG_SLOT, (int)(fade * 0xDD)));
        g.fill(rx, ry, rx + PREVIEW_W - 16, ry + PREVIEW_RENDER_H,
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
            int cy = ry + PREVIEW_RENDER_H / 2 - 4;
            pose.translate(cx, cy, 0);
            pose.scale(scale, scale, 1);
            g.renderItem(stack, -8, -8);
            pose.popPose();

            String displayName = stack.getHoverName().getString();
            font.drawCentered(g, displayName, rx + (PREVIEW_W - 16) / 2, ry + PREVIEW_RENDER_H + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fade * 0xFF)));
            font.drawCentered(g, currentId, rx + (PREVIEW_W - 16) / 2, ry + PREVIEW_RENDER_H + 16,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 200)));
        } else {
            font.drawCentered(g, "\uD83D\uDD2B", rx + (PREVIEW_W - 16) / 2, ry + PREVIEW_RENDER_H / 2 - 8,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
            String noItem = currentId.isEmpty() ? "No item" : currentId.replace("_", " ").toUpperCase();
            font.drawCentered(g, noItem, rx + (PREVIEW_W - 16) / 2, ry + PREVIEW_RENDER_H / 2 + 10,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
        }

        g.fill(rx, ry + PREVIEW_RENDER_H + 28, rx + PREVIEW_W - 16, ry + PREVIEW_RENDER_H + 30,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xCC)));
    }

    private void renderSlotList(GuiGraphics g, int px, int py, int mx, int my, float fade, int alpha) {
        int ry = py + 10;
        int sy = ry + PREVIEW_RENDER_H + 38;
        int iconIdx = 0;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            String icon = iconIdx < SLOT_ICONS.length ? SLOT_ICONS[iconIdx] : "\u2022";
            iconIdx++;
            boolean isActive = key.equals(activeSlot);
            boolean hover = mx >= px + 4 && mx <= px + PREVIEW_W - 4 && my >= sy && my <= sy + 28;

            int slotBg = isActive
                ? AnimationHelper.blendColors(0x00000000, UITheme.ACCENT, 0.12f)
                : hover ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xCC))
                : 0x00000000;
            int slotBorder = isActive
                ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, alpha)
                : hover ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * 0x66))
                : 0x00000000;

            if (slotBg != 0) {
                g.fill(px + 4, sy, px + PREVIEW_W - 4, sy + 28, slotBg);
            }
            if (slotBorder != 0) {
                g.fill(px + 4, sy, px + PREVIEW_W - 4, sy + 1, slotBorder);
                g.fill(px + 4, sy + 27, px + PREVIEW_W - 4, sy + 28, slotBorder);
                g.fill(px + PREVIEW_W - 5, sy, px + PREVIEW_W - 4, sy + 28, slotBorder);
            }
            if (isActive) {
                g.fill(px + 4, sy, px + 7, sy + 28, AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
            }

            font.drawString(g, icon + " " + key.toUpperCase(), px + 14, sy + 8,
                    AnimationHelper.withAlpha(isActive ? UITheme.ACCENT : UITheme.TEXT_MUTED,
                        (int)(fade * (isActive ? 255 : 180))));
            String weap = selections.getOrDefault(key, "").replace("_", " ").toUpperCase();
            if (weap.length() > 12) weap = weap.substring(0, 12);
            font.drawString(g, weap, px + 14, sy + 18,
                    AnimationHelper.withAlpha(isActive ? UITheme.TEXT_PRIMARY : UITheme.TEXT_SECONDARY, alpha));
            sy += 32;
        }
    }

    public String handleSlotClick(double mx, double my, int py) {
        int ry = py + 10;
        int sy = ry + PREVIEW_RENDER_H + 38;
        for (String key : slotKeys) {
            if (!weaponOptions.containsKey(key)) continue;
            if (my >= sy && my <= sy + 28) {
                activeSlot = key;
                previewItem = selections.get(key);
                return key;
            }
            sy += 32;
        }
        return null;
    }

    private static class FontAccessor {
        private final net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        void drawString(GuiGraphics g, String text, int x, int y, int color) {
            g.drawString(font, text, x, y, color);
        }
        void drawCentered(GuiGraphics g, String text, int cx, int y, int color) {
            g.drawString(font, text, cx - font.width(text) / 2, y, color);
        }
    }
}