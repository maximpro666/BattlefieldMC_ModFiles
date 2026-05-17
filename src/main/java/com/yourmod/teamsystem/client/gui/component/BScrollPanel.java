package com.yourmod.teamsystem.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public class BScrollPanel extends BPanel {
    protected int scrollOffset;
    protected int contentHeight;

    public BScrollPanel(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(x, y, x + width, y + height, backgroundColor);
        if (drawBorder) {
            graphics.fill(x, y, x + 1, y + height, borderColor);
            graphics.fill(x + width - 1, y, x + width, y + height, borderColor);
            graphics.fill(x, y, x + width, y + 1, borderColor);
            graphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        }
        enableScissor();
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        for (BComponent child : children) {
            child.render(graphics, mouseX, mouseY + scrollOffset, partialTick);
        }
        graphics.pose().popPose();
        disableScissor();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int maxScroll = Math.max(0, contentHeight - height);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollDelta * 10));
        return true;
    }

    private void enableScissor() {
        var window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        int scX = (int) (x * scale);
        int scY = (int) (window.getScreenHeight() - (y + height) * scale);
        int scW = (int) Math.ceil(width * scale);
        int scH = (int) Math.ceil(height * scale);
        RenderSystem.enableScissor(scX, scY, scW, scH);
    }

    private void disableScissor() {
        RenderSystem.disableScissor();
    }

    public void setContentHeight(int contentHeight) {
        this.contentHeight = contentHeight;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }
}
