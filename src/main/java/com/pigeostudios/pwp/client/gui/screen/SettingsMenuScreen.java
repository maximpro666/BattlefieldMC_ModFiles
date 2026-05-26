package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.BSlider;
import com.pigeostudios.pwp.client.gui.component.BToggle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SettingsMenuScreen extends Screen {

    private float fadeAlpha = 0f;
    private long openTime;
    private int tickCount = 0;

    private static final int PW = 520;
    private static final int RH = 24;
    private static final int GP = 6;
    private static final int CW = (PW - 48) / 2;
    private static final int CH = 10;
    private static final int CP = 4;

    private static final int SCROLL_SPEED = 16;

    private final float[] secEnter = new float[3];

    private int genCardY, genCardH, genContentY;
    private int dispCardY, dispCardH, dispContentY;
    private int lx, rx, py, phActual;
    private int scrollOffs, maxScroll, footerY;
    private final Map<AbstractWidget, Integer> widgetBaseY = new HashMap<>();

    public SettingsMenuScreen() {
        super(Component.literal(I18n.get("pwp.ui.settings")));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        scrollOffs = 0;
        int cx = width / 2;
        int px = cx - PW / 2;
        py = PY();
        lx = px + 16;
        rx = cx + 8;

        genCardY = py + 30;
        dispCardY = py + 30;
        genContentY = genCardY + CH + CP;
        dispContentY = dispCardY + CH + CP;

        genCardH = CH + CP + RH * 5 + GP * 4 + CP;
        dispCardH = CH + CP + RH * 7 + GP * 6 + CP;

        // │ panel & scroll sizing │
        int contentBottom = Math.max(genCardY + genCardH, dispCardY + dispCardH);
        int neededH = contentBottom - py + 20;
        int maxH = height - py - 10;
        phActual = Math.min(Math.max(neededH, 120), maxH);
        maxScroll = Math.max(0, neededH - phActual);

        footerY = py + phActual - RH - 12;
        int hw = (CW - GP) / 2;

        // ── LEFT: GENERAL ──
        BSlider[] vs = {null};
        vs[0] = new BSlider(lx, genContentY, CW, RH,
            lit(I18n.get("pwp.ui.volume", (int)(ClientTeamData.guiVolume * 100))),
            ClientTeamData.guiVolume,
            v -> { ClientTeamData.guiVolume = v; vs[0].setMessage(lit(I18n.get("pwp.ui.volume", (int)(v * 100)))); });
        addRenderableWidget(vs[0]);

        addRenderableWidget(new BButton(lx, genContentY + (RH + GP), CW, RH,
            lit(I18n.get("pwp.ui.language", ClientTeamData.language.toUpperCase())),
            btn -> {
                String lang = ClientTeamData.language.equals("ru") ? "en" : "ru";
                ClientTeamData.language = lang;
                btn.setMessage(lit(I18n.get("pwp.ui.language", lang.toUpperCase())));
            }));

        BSlider[] ss = {null};
        ss[0] = new BSlider(lx, genContentY + (RH + GP) * 2, CW, RH,
            lit(I18n.get("pwp.ui.gui_scale", (int)(ClientTeamData.guiScale * 100))),
            (ClientTeamData.guiScale - 0.5f) / 1.0f,
            v -> { ClientTeamData.guiScale = 0.5f + v * 1.0f; ss[0].setMessage(lit(I18n.get("pwp.ui.gui_scale", (int)(ClientTeamData.guiScale * 100)))); });
        addRenderableWidget(ss[0]);

        BSlider[] os = {null};
        os[0] = new BSlider(lx, genContentY + (RH + GP) * 3, CW, RH,
            lit(I18n.get("pwp.ui.opacity", (int)(ClientTeamData.guiOpacity * 100))),
            (ClientTeamData.guiOpacity - 0.3f) / 0.7f,
            v -> { ClientTeamData.guiOpacity = 0.3f + v * 0.7f; os[0].setMessage(lit(I18n.get("pwp.ui.opacity", (int)(ClientTeamData.guiOpacity * 100)))); });
        addRenderableWidget(os[0]);

        addRenderableWidget(new BButton(lx, genContentY + (RH + GP) * 4, CW, RH,
            lit(I18n.get("pwp.ui.replay_mod")),
            btn -> openReplayMod()));

        // ── LEFT footer ──
        addRenderableWidget(new BButton(lx, footerY, hw, RH,
            lit(I18n.get("pwp.ui.vanilla_settings")), btn ->
                Minecraft.getInstance().setScreen(new OptionsScreen(this, Minecraft.getInstance().options))));
        addRenderableWidget(new BButton(lx + hw + GP, footerY, hw, RH,
            lit(I18n.get("pwp.ui.back")), btn -> onClose()));

        // ── RIGHT: DISPLAY ──
        addRenderableWidget(new BToggle(rx, dispContentY, CW, RH, lit(I18n.get("pwp.ui.show_compass")), ClientTeamData.showCompass, v -> ClientTeamData.showCompass = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP), CW, RH, lit(I18n.get("pwp.ui.show_ticketbar")), ClientTeamData.showTicketBar, v -> ClientTeamData.showTicketBar = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP) * 2, CW, RH, lit(I18n.get("pwp.ui.show_squad")), ClientTeamData.showSquad, v -> ClientTeamData.showSquad = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP) * 3, CW, RH, lit(I18n.get("pwp.ui.show_vitals")), ClientTeamData.showVitals, v -> ClientTeamData.showVitals = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP) * 4, CW, RH, lit(I18n.get("pwp.ui.show_hotbar")), ClientTeamData.showHotbar, v -> ClientTeamData.showHotbar = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP) * 5, CW, RH, lit(I18n.get("pwp.ui.show_killfeed")), ClientTeamData.showKillFeed, v -> ClientTeamData.showKillFeed = v));
        addRenderableWidget(new BToggle(rx, dispContentY + (RH + GP) * 6, CW, RH, lit(I18n.get("pwp.ui.use_custom_menu")), ClientTeamData.useCustomMenu, v -> ClientTeamData.useCustomMenu = v));

        // ── capture base Y positions & apply scroll ──
        widgetBaseY.clear();
        for (var w : renderables) {
            if (w instanceof AbstractWidget aw) {
                widgetBaseY.put(aw, aw.getY());
            }
        }
        applyScroll();
    }

    private static Component lit(String s) { return Component.literal(s); }
    private static String loc(String s) { return I18n.localize(s); }
    private int PY() { return Math.max(15, (height - Math.min(phActual > 0 ? phActual : 420, height - 25)) / 3); }

    private void openReplayMod() {
        try {
            Class<?> replayClass = Class.forName("com.replaymod.replay.ReplayModReplay");
            Field instanceField = replayClass.getField("instance");
            Object replayMod = instanceField.get(null);

            Class<?> viewerClass = Class.forName("com.replaymod.replay.gui.screen.GuiReplayViewer");
            Constructor<?> ctor = viewerClass.getConstructor(replayClass);
            Object viewer = ctor.newInstance(replayMod);

            Method display = viewerClass.getMethod("display");
            display.invoke(viewer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (maxScroll <= 0) return false;
        int prev = scrollOffs;
        scrollOffs = net.minecraft.util.Mth.clamp(scrollOffs - (int)(delta * SCROLL_SPEED), 0, maxScroll);
        if (scrollOffs != prev) {
            applyScroll();
        }
        return true;
    }

    private void applyScroll() {
        for (var entry : widgetBaseY.entrySet()) {
            int baseY = entry.getValue();
            if (baseY < footerY) {
                entry.getKey().setY(baseY - scrollOffs);
            } else {
                entry.getKey().setY(baseY);
            }
        }
    }

    @Override
    public void tick() {
        tickCount++;
        float base = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        for (int i = 0; i < 3; i++) {
            float d = i * 0.06f;
            secEnter[i] = Math.min(1f, Math.max(0, (base - d) / (1f - d)));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        int cx = width / 2;
        int px = cx - PW / 2;

        g.fill(px, py, px + PW, py + phActual, AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(px, py, px + PW, py + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xFF)));
        g.fill(px, py + phActual - 1, px + PW, py + phActual, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xFF)));
        g.fill(px, py, px + 1, py + phActual, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xFF)));
        g.fill(px + PW - 1, py, px + PW, py + phActual, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xFF)));
        g.fill(px, py, px + PW, py + 3, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0xFF)));

        String title = I18n.get("pwp.ui.settings_uppercase");
        g.drawString(font, title, cx - font.width(title) / 2, py + 8, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0xFF)));

        // ── clip content to the full panel interior ──
        g.enableScissor(px, py, px + PW, py + phActual);

        int so = scrollOffs;
        drawCard(g, lx, genCardY - so, CW, genCardH, loc("GENERAL // ОБЩИЕ"), 0);
        drawUnderline(g, lx, genCardY + CH + 1 - so, CW, 0);

        drawCard(g, rx, dispCardY - so, CW, dispCardH, I18n.get("pwp.ui.hud_elements"), 1);
        drawUnderline(g, rx, dispCardY + CH + 1 - so, CW, 1);

        super.render(g, mx, my, pt);

        g.disableScissor();

        // ── draw a thin accent line just above the footer ──
        g.fill(px, footerY - 1, px + PW, footerY, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x50)));
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h, String header, int idx) {
        float e = secEnter[idx];
        if (e <= 0.01f) return;
        int sx = x + (int)((1f - AnimationHelper.easeOutCubic(e)) * 20);
        int a = (int)(fadeAlpha * 0xDD);

        g.fill(sx, y, sx + w, y + h, AnimationHelper.withAlpha(UITheme.BG_SURFACE, a));
        g.fill(sx, y, sx + w, y + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xB0)));
        g.fill(sx, y + h - 1, sx + w, y + h, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xB0)));
        g.fill(sx, y, sx + 1, y + h, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xB0)));
        g.fill(sx + w - 1, y, sx + w, y + h, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0xB0)));

        g.drawString(font, header, sx + 6, y + 2, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 0xC0)));
    }

    private void drawUnderline(GuiGraphics g, int x, int y, int w, int idx) {
        float e = secEnter[idx];
        if (e <= 0.01f) return;
        int lw = (int)((w - 12) * AnimationHelper.easeOutCubic(e));
        g.fill(x + 6, y, x + 6 + lw, y + 1, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 0xB0)));
    }

    @Override
    public void renderBackground(GuiGraphics g) { }

    @Override
    public void onClose() { super.onClose(); }

    @Override
    public boolean isPauseScreen() { return false; }
}
