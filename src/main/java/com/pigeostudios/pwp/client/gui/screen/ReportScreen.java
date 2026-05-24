package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.report.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ReportScreen extends Screen {

    private float fadeAlpha = 0f;
    private float slideIn = 0f;
    private long openTime;

    private String selectedTargetName;
    private ReportType selectedType;
    private String statusMsg = "";
    private long statusTime = 0;
    private final List<Player> onlinePlayers;

    private BButton sendBtn;

    public ReportScreen() {
        super(Component.literal("Report"));
        Minecraft mc = Minecraft.getInstance();
        this.onlinePlayers = mc.level != null
            ? mc.level.players().stream().filter(p -> p != mc.player).collect(Collectors.toList())
            : List.of();
        if (!onlinePlayers.isEmpty()) selectedTargetName = onlinePlayers.get(0).getScoreboardName();
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int cx = width / 2;

        sendBtn = new BButton(cx - 40, height / 2 + 80, 80, 22,
            Component.literal("SEND"), btn -> send(), BButton.Variant.PRIMARY);
        addRenderableWidget(sendBtn);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);
        slideIn = AnimationHelper.lerp(slideIn, 1f, 0.18f);

        int alphaBg = (int)(fadeAlpha * 0xDD);
        g.fill(0, 0, width, height, alphaBg << 24 | 0x000000);

        int pw = 260, ph = Math.max(200, Math.min(260, height - 80));
        int cx = width / 2, cy = height / 2;
        int px = cx - pw / 2, py = Math.max(20, cy - ph / 2);

        float off = (1f - AnimationHelper.easeOutCubic(slideIn)) * 30f;
        g.pose().pushPose();
        g.pose().translate(0, off, 0);

        g.fill(px, py, px + pw, py + ph,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xEE)));
        g.fill(px, py, px + pw, py + 3,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int ba = (int)(fadeAlpha * 0xAA);
        g.fill(px, py, px + pw, py + 1, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px, py + ph - 1, px + pw, py + ph, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px, py, px + 1, py + ph, AnimationHelper.withAlpha(UITheme.BORDER, ba));
        g.fill(px + pw - 1, py, px + pw, py + ph, AnimationHelper.withAlpha(UITheme.BORDER, ba));

        g.drawCenteredString(font, "\u00A76REPORT PLAYER", cx, py + 16,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        g.fill(cx - 40, py + 30, cx + 40, py + 32,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0x88)));

        int fy = py + 44;

        // Player
        boolean phov = mx >= px + 12 && mx <= px + pw - 12 && my >= fy && my <= fy + 24;
        g.drawString(font, "\u00A77Player:", px + 12, fy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        g.fill(px + 12, fy + 12, px + pw - 12, fy + 24,
            AnimationHelper.withAlpha(phov ? UITheme.BORDER : UITheme.BG_SLOT, (int)(fadeAlpha * 0x66)));
        g.drawString(font, "\u00A7f" + (selectedTargetName != null ? selectedTargetName : ""), px + 16, fy + 13,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 220)));

        // Type
        fy += 32;
        g.drawString(font, "\u00A77Type:", px + 12, fy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        fy += 12;
        for (ReportType rt : ReportType.values()) {
            boolean sel = rt == selectedType;
            boolean thov = mx >= px + 12 && mx <= px + pw - 12 && my >= fy && my <= fy + 14;
            if (sel) g.fill(px + 12, fy, px + pw - 12, fy + 14,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0x44)));
            else if (thov) g.fill(px + 12, fy, px + pw - 12, fy + 14,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x33)));
            g.drawString(font, (sel ? "\u00A7a" : "\u00A77") + rt.getDisplayName(),
                px + 16, fy + 1, AnimationHelper.withAlpha(sel ? 0xFF50B050 : UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 220)));
            fy += 16;
        }

        g.pose().popPose();

        if (!statusMsg.isEmpty() && System.currentTimeMillis() - statusTime < 3000) {
            g.drawCenteredString(font, statusMsg, cx, py + ph - 14,
                AnimationHelper.withAlpha(0xFFFFFFFF, (int)(fadeAlpha * 255)));
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int pw = 260, ph = Math.max(200, Math.min(260, height - 80));
        int cx = width / 2, cy = height / 2;
        int px = cx - pw / 2, py = Math.max(20, cy - ph / 2);

        int fy = py + 44;

        // Player click
        if (mx >= px + 12 && mx <= px + pw - 12 && my >= fy + 12 && my <= fy + 24) {
            if (onlinePlayers.isEmpty()) return true;
            int idx = onlinePlayers.indexOf(selectedTargetName);
            idx = (idx + 1) % onlinePlayers.size();
            if (idx < 0) idx = 0;
            selectedTargetName = onlinePlayers.get(idx).getScoreboardName();
            return true;
        }

        // Type click
        int ty = fy + 44;
        for (ReportType rt : ReportType.values()) {
            if (mx >= px + 12 && mx <= px + pw - 12 && my >= ty && my <= ty + 14) {
                selectedType = rt;
                return true;
            }
            ty += 16;
        }

        return super.mouseClicked(mx, my, btn);
    }

    private void send() {
        if (selectedTargetName == null || selectedType == null) {
            statusMsg = "\u00A7cSelect player and type";
            statusTime = System.currentTimeMillis();
            return;
        }
        com.pigeostudios.pwp.network.PacketHandler.CHANNEL.sendToServer(
            new com.pigeostudios.pwp.network.ReportSubmitPacket(
                selectedTargetName, selectedType.name(), ""));
        statusMsg = "\u00A7aReport sent";
        statusTime = System.currentTimeMillis();
        selectedType = null;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
