package com.yourmod.teamsystem.client.gui.widget;

import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.data.LockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class BCard extends AbstractWidget {
    private final String iconText;
    private final String title;
    private final String subtitle;
    private LockState lockState;
    private String lockTooltip;

    private long creationTime;
    private float staggerDelay;
    private float hoverProgress = 0f;
    private Runnable onClickAction;

    private int accentColor = UITheme.ACCENT;
    private boolean showAccentBorder = true;

    public BCard(int x, int y, int width, int height, String iconText, String title, String subtitle) {
        super(x, y, width, height, Component.literal(title));
        this.iconText = iconText;
        this.title = title;
        this.subtitle = subtitle;
        this.lockState = LockState.AVAILABLE;
        this.lockTooltip = "";
        this.creationTime = System.currentTimeMillis();
        this.staggerDelay = 0f;
    }

    public BCard setLockState(LockState lockState, String tooltip) {
        this.lockState = lockState;
        this.lockTooltip = tooltip;
        return this;
    }

    public BCard setOnClick(Runnable action) {
        this.onClickAction = action;
        return this;
    }

    public BCard setStaggerDelay(float delayMs) {
        this.staggerDelay = delayMs;
        return this;
    }

    public BCard setAccentColor(int color) {
        this.accentColor = color;
        return this;
    }

    public BCard setShowAccentBorder(boolean show) {
        this.showAccentBorder = show;
        return this;
    }

    public LockState getLockState() {
        return lockState;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long elapsed = System.currentTimeMillis() - creationTime;
        float fadeProgress = Math.min(1.0f, Math.max(0f, (elapsed - staggerDelay) / 250f));
        if (fadeProgress < 1.0f) {
            fadeProgress = AnimationHelper.easeOutCubic(fadeProgress);
        }

        boolean locked = !lockState.isSelectable();

        if (isHovered() && !locked) {
            hoverProgress = Math.min(1.0f, hoverProgress + 0.15f);
        } else {
            hoverProgress = Math.max(0f, hoverProgress - 0.1f);
        }

        float scale = 1.0f + (hoverProgress * 0.05f);

        graphics.pose().pushPose();
        graphics.pose().translate(getX() + width / 2f, getY() + height / 2f, 0);
        graphics.pose().scale(scale, scale, 1.0f);
        graphics.pose().translate(-width / 2f, -height / 2f, 0);

        int baseAlpha = (int)(fadeProgress * 255);

        int bgColor = locked ?
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(baseAlpha * 0.35f)) :
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, baseAlpha);
        graphics.fill(0, 0, width, height, bgColor);

        int borderColor;
        if (locked) {
            borderColor = AnimationHelper.withAlpha(UITheme.BORDER, (int)(baseAlpha * 0.5f));
        } else if (showAccentBorder && hoverProgress > 0) {
            borderColor = AnimationHelper.blendColors(
                UITheme.BORDER, accentColor, hoverProgress);
            borderColor = AnimationHelper.withAlpha(borderColor, baseAlpha);
        } else {
            borderColor = AnimationHelper.withAlpha(UITheme.BORDER, baseAlpha);
        }

        graphics.fill(0, 0, width, 1, borderColor);
        graphics.fill(0, height - 1, width, height, borderColor);
        graphics.fill(0, 0, 1, height, borderColor);
        graphics.fill(width - 1, 0, width, height, borderColor);

        if (showAccentBorder && !locked) {
            int accentBarColor = AnimationHelper.withAlpha(accentColor, baseAlpha);
            graphics.fill(0, 0, width, 3, accentBarColor);
        }

        if (iconText != null && !iconText.isEmpty()) {
            int iconColor = locked ?
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, baseAlpha) :
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, baseAlpha);
            graphics.drawCenteredString(Minecraft.getInstance().font, iconText,
                width / 2, 12, iconColor);
        }

        int titleColor = locked ?
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, baseAlpha) :
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, baseAlpha);
        graphics.drawCenteredString(Minecraft.getInstance().font, title,
            width / 2, height / 2 - 4, titleColor);

        if (subtitle != null && !subtitle.isEmpty()) {
            int subtitleColor = locked ?
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(baseAlpha * 0.7f)) :
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, baseAlpha);
            graphics.pose().pushPose();
            graphics.pose().translate(width / 2f, height / 2f + 6, 0);
            graphics.pose().scale(0.8f, 0.8f, 1.0f);
            graphics.drawCenteredString(Minecraft.getInstance().font, subtitle, 0, 0, subtitleColor);
            graphics.pose().popPose();
        }

        if (locked) {
            graphics.fill(0, 0, width, height,
                AnimationHelper.withAlpha(0xFF000000, (int)(baseAlpha * 0.6f)));

            graphics.drawCenteredString(Minecraft.getInstance().font, "\uD83D\uDD12",
                width / 2, height - 24,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, baseAlpha));

            if (lockTooltip != null && !lockTooltip.isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(width / 2f, height - 14, 0);
                graphics.pose().scale(0.7f, 0.7f, 1.0f);
                graphics.drawCenteredString(Minecraft.getInstance().font, lockTooltip, 0, 0,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, baseAlpha));
                graphics.pose().popPose();
            }
        }

        graphics.pose().popPose();
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (lockState.isSelectable() && onClickAction != null) {
            onClickAction.run();
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
