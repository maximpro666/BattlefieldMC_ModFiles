package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.data.LockChecker;
import com.yourmod.teamsystem.data.LockState;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassSelectionScreen extends Screen {

    private static final int COLS   = 3;
    private static final int CARD_W = 140;
    private static final int CARD_H = 100;
    private static final int GAP    = 12;

    private final List<KitConfig.ClassConfig> classes = new ArrayList<>();
    private final List<LockState> lockStates = new ArrayList<>();
    private final List<String> lockTooltips = new ArrayList<>();
    private float[] hoverState;
    private float fadeAlpha = 0f;
    private long openTime;
    private BScrollPanel scrollPanel;
    private String selectedClassId = "";

    public ClassSelectionScreen() {
        super(Component.literal("Class Selection"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        loadClasses();

        int panelW = COLS * (CARD_W + GAP) + GAP;
        int panelH = height - 100;
        int panelX = width / 2 - panelW / 2;
        int panelY = 50;

        scrollPanel = new BScrollPanel(panelX, panelY, panelW, panelH);
        int rows = classes.isEmpty() ? 1 : (classes.size() + COLS - 1) / COLS;
        scrollPanel.setContentHeight(rows * (CARD_H + GAP) + GAP);
    }

    private void loadClasses() {
        classes.clear();
        lockStates.clear();
        lockTooltips.clear();

        KitConfig cfg = KitConfig.get();
        Map<String, KitConfig.ClassConfig> classMap = (cfg != null) ? cfg.classes : null;

        if (classMap != null && !classMap.isEmpty()) {
            LockChecker.Context ctx = new LockChecker.Context();
            ctx.playerRank = ClientTeamData.localPlayerRank;
            ctx.playerTeam = ClientTeamData.getLocalPlayerTeam().name();

            for (Map.Entry<String, KitConfig.ClassConfig> entry : classMap.entrySet()) {
                classes.add(entry.getValue());

                boolean anyKitAvailable = false;
                String lockReason = null;
                LockState worstLock = LockState.AVAILABLE;

                for (KitConfig.KitDef kit : entry.getValue().kits.values()) {
                    LockState ks = LockChecker.checkKit(kit, ctx);
                    if (ks == LockState.AVAILABLE) {
                        anyKitAvailable = true;
                        break;
                    }
                    if (ks.ordinal() > worstLock.ordinal()) {
                        worstLock = ks;
                        lockReason = ks.tooltip(String.valueOf(kit.requirements.rank));
                    }
                }

                if (!anyKitAvailable) {
                    lockStates.add(worstLock);
                    lockTooltips.add(lockReason != null ? lockReason : "");
                } else {
                    lockStates.add(LockState.AVAILABLE);
                    lockTooltips.add("");
                }
            }
        }
        hoverState = new float[classes.size()];
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        String title = "SELECT CLASS";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, 18,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        if (classes.isEmpty()) {
            String msg = "No classes available";
            int mw = font.width(msg);
            g.drawString(font, msg, width / 2 - mw / 2, height / 2,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        } else {
            int panelX = width / 2 - (COLS * (CARD_W + GAP) + GAP) / 2;
            int panelY = 50;

            scrollPanel.render(g);
            float scrollOff = scrollPanel.getScrollOffset();
            g.enableScissor(panelX, panelY, panelX + scrollPanel.getWidth(), panelY + scrollPanel.getHeight());

            for (int i = 0; i < classes.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + GAP + col * (CARD_W + GAP);
                int cy = panelY + GAP + row * (CARD_H + GAP) - (int) scrollOff;

                if (cy + CARD_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

                boolean hov = mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H;
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);

                drawClassCard(g, cx, cy, classes.get(i),
                    lockStates.get(i), lockTooltips.get(i),
                    hoverState[i], fadeAlpha, i);
            }
            g.disableScissor();
        }

        super.render(g, mx, my, pt);
    }

    private void drawClassCard(GuiGraphics g, int x, int y, KitConfig.ClassConfig cl,
                                LockState lockState, String lockTooltip,
                                float hover, float alpha, int index) {
        boolean locked = !lockState.isSelectable();

        int bgColor = locked ?
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(alpha * 0x55)) :
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(alpha * (0xDD + hover * 0x22)));
        int borderColor;
        if (locked) {
            borderColor = AnimationHelper.withAlpha(UITheme.BORDER, (int)(alpha * 0x66));
        } else if (hover > 0.01f) {
            borderColor = AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * (0x77 + hover * 0x88)));
        } else {
            borderColor = AnimationHelper.withAlpha(UITheme.BORDER, (int)(alpha * 0xAA));
        }

        g.fill(x, y, x + CARD_W, y + CARD_H, bgColor);
        g.fill(x, y, x + CARD_W, y + 1, borderColor);
        g.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, borderColor);
        g.fill(x, y, x + 1, y + CARD_H, borderColor);
        g.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, borderColor);

        if (!locked) {
            g.fill(x, y, x + CARD_W, y + 3,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(alpha * 255)));
        }

        if (cl.icon != null && !cl.icon.isEmpty()) {
            g.drawCenteredString(font, cl.icon, x + CARD_W / 2, y + 14,
                locked ? AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 180))
                       : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(alpha * 255)));
        }

        String displayName = cl.display_name != null ? I18n.localize(cl.display_name).toUpperCase() : "CLASS";
        int dw = font.width(displayName);
        g.drawString(font, displayName, x + CARD_W / 2 - dw / 2, y + CARD_H / 2 - 4,
            locked ? AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 180))
                   : AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(alpha * 255)));

        int kitCount = cl.kits != null ? cl.kits.size() : 0;
        String sub = kitCount + " kit" + (kitCount != 1 ? "s" : "");
        g.drawCenteredString(font, sub, x + CARD_W / 2, y + CARD_H / 2 + 10,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(alpha * 180)));

        if (locked) {
            g.fill(x, y, x + CARD_W, y + CARD_H,
                AnimationHelper.withAlpha(0xFF000000, (int)(alpha * 0x99)));
            g.drawCenteredString(font, "\uD83D\uDD12", x + CARD_W / 2, y + CARD_H - 32,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(alpha * 200)));

            if (!lockTooltip.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(x + CARD_W / 2f, y + CARD_H - 20, 0);
                g.pose().scale(0.7f, 0.7f, 1.0f);
                g.drawCenteredString(font, lockTooltip, 0, 0,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(alpha * 180)));
                g.pose().popPose();
            }
        }

        if (hover > 0.01f && !locked) {
            g.fill(x + 1, y + 3, x + CARD_W - 1, y + 4,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(hover * 80)));
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (classes.isEmpty()) return super.mouseClicked(mx, my, btn);

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return super.mouseClicked(mx, my, btn);

        int panelX = width / 2 - (COLS * (CARD_W + GAP) + GAP) / 2;
        int panelY = 50;
        float scrollOff = scrollPanel.getScrollOffset();

        int i = 0;
        for (Map.Entry<String, KitConfig.ClassConfig> entry : cfg.classes.entrySet()) {
            if (i >= lockStates.size()) break;
            if (!lockStates.get(i).isSelectable()) { i++; continue; }
            int col = i % COLS;
            int row = i / COLS;
            int cx = panelX + GAP + col * (CARD_W + GAP);
            int cy = panelY + GAP + row * (CARD_H + GAP) - (int) scrollOff;
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                Minecraft.getInstance().setScreen(new KitSelectionScreen(entry.getKey()));
                return true;
            }
            i++;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollPanel.onScroll((int) mx, (int) my, delta);
        return true;
    }

    @Override
    public void tick() {
        if (classes.isEmpty()) {
            KitConfig cfg = KitConfig.get();
            if (cfg != null && !cfg.classes.isEmpty()) {
                init();
                return;
            }
        }
        scrollPanel.tick();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
