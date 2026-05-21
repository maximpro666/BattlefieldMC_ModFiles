package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BScrollPanel;
import com.pigeostudios.pwp.client.gui.component.ClassCard;
import com.pigeostudios.pwp.client.gui.component.SortControl;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.LockChecker;
import com.pigeostudios.pwp.data.LockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClassSelectionScreen extends Screen {

    private static final int COLS = 3;
    private static final int CARD_W = 140;
    private static final int CARD_H = 112;
    private static final int GAP = 10;

    private final List<KitConfig.ClassConfig> classes = new ArrayList<>();
    private final List<String> classKeys = new ArrayList<>();
    private final List<LockState> lockStates = new ArrayList<>();
    private final List<String> lockReasons = new ArrayList<>();
    private float[] hoverState;
    private float fadeAlpha = 0f;
    private long openTime;
    private int tickCount;
    private BScrollPanel scrollPanel;
    private SortControl sortControl = new SortControl();

    public ClassSelectionScreen() {
        super(Component.literal("Class Selection"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        tickCount = 0;
        loadClasses();

        int panelW = COLS * (CARD_W + GAP) + GAP;
        int panelH = height - 100;
        int panelX = width / 2 - panelW / 2;
        int panelY = 50;

        scrollPanel = new BScrollPanel(panelX, panelY, panelW, panelH);
        int rows = classes.isEmpty() ? 1 : (classes.size() + COLS - 1) / COLS;
        scrollPanel.setContentHeight(rows * (CARD_H + GAP) + GAP);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void loadClasses() {
        classes.clear();
        classKeys.clear();
        lockStates.clear();
        lockReasons.clear();

        KitConfig cfg = KitConfig.get();
        if (cfg == null || cfg.classes == null) return;

        com.pigeostudios.pwp.core.Team localTeam = com.pigeostudios.pwp.client.ClientTeamData.getLocalPlayerTeam();
        if (localTeam == com.pigeostudios.pwp.core.Team.SPECTATOR) {
            hoverState = new float[0];
            return;
        }

        LockChecker.Context ctx = new LockChecker.Context();
        ctx.playerRank = com.pigeostudios.pwp.client.ClientTeamData.localPlayerRank;
        ctx.playerTeam = localTeam.name();
        ctx.playerSP = com.pigeostudios.pwp.client.ClientTeamData.localPlayerWC;
        ctx.playerBC = com.pigeostudios.pwp.client.ClientTeamData.localPlayerBC;

        for (Map.Entry<String, KitConfig.ClassConfig> entry : cfg.classes.entrySet()) {
            if (entry.getValue().kits == null) continue;

            boolean hasUsableKit = false;
            for (KitConfig.KitDef kit : entry.getValue().kits.values()) {
                if (kit.requirements == null) { hasUsableKit = true; break; }
                if (kit.requirements.team != null && !kit.requirements.team.equalsIgnoreCase(localTeam.name())) continue;
                hasUsableKit = true;
                break;
            }
            if (!hasUsableKit) continue;

            classes.add(entry.getValue());
            classKeys.add(entry.getKey());

            boolean anyAvailable = false;
            String lockReason = null;
            LockState worst = LockState.AVAILABLE;

            for (KitConfig.KitDef kit : entry.getValue().kits.values()) {
                LockState ks = LockChecker.checkKit(kit, ctx);
                if (ks == LockState.AVAILABLE) { anyAvailable = true; break; }
                if (ks.ordinal() > worst.ordinal()) {
                    worst = ks;
                    String detail;
                    if (ks == LockState.LOCKED_COST) {
                        detail = kit.requirements != null && kit.requirements.sp_cost > 0 && ctx.playerSP < kit.requirements.sp_cost
                                ? "SP: need " + kit.requirements.sp_cost
                                : "BC: need " + (kit.requirements != null ? kit.requirements.bc_cost : 0);
                    } else {
                        detail = String.valueOf(kit.requirements != null ? kit.requirements.rank : 0);
                    }
                    lockReason = ks.tooltip(detail);
                }
            }

            lockStates.add(anyAvailable ? LockState.AVAILABLE : worst);
            lockReasons.add(lockReason != null ? lockReason : "");
        }
        applySort(sortControl.getMode());
        hoverState = new float[classes.size()];
    }

    private void applySort(SortControl.SortMode mode) {
        int n = classes.size();
        if (n <= 1) return;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> {
            switch (mode) {
                case NAME: {
                    String na = classes.get(a).display_name != null ? I18n.localize(classes.get(a).display_name) : classKeys.get(a);
                    String nb = classes.get(b).display_name != null ? I18n.localize(classes.get(b).display_name) : classKeys.get(b);
                    return na.compareToIgnoreCase(nb);
                }
                case AVAILABILITY:
                    return Boolean.compare(!lockStates.get(a).isSelectable(), !lockStates.get(b).isSelectable());
                default:
                    return 0;
            }
        });
        List<KitConfig.ClassConfig> sc = new ArrayList<>();
        List<String> sk = new ArrayList<>();
        List<LockState> sl = new ArrayList<>();
        List<String> sr = new ArrayList<>();
        for (int i : idx) {
            sc.add(classes.get(i));
            sk.add(classKeys.get(i));
            sl.add(lockStates.get(i));
            sr.add(lockReasons.get(i));
        }
        classes.clear(); classes.addAll(sc);
        classKeys.clear(); classKeys.addAll(sk);
        lockStates.clear(); lockStates.addAll(sl);
        lockReasons.clear(); lockReasons.addAll(sr);
        hoverState = new float[n];
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        tickCount++;
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        // Header
        int panelW = COLS * (CARD_W + GAP) + GAP;
        int panelX = width / 2 - panelW / 2;

        // Back + title
        g.drawString(font, "SELECT YOUR CLASS", panelX, 18,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));

        int sortBtnX = panelX + font.width("SELECT YOUR CLASS") + 16;
        int sortBtnY = 18 - (font.lineHeight + 4) / 2;
        sortControl.render(g, sortBtnX, sortBtnY, mx, my, fadeAlpha);

        g.drawString(font, classes.size() + " classes available", panelX + panelW - font.width(classes.size() + " classes available"), 18,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));

        if (classes.isEmpty()) {
            String msg = com.pigeostudios.pwp.client.ClientTeamData.getLocalPlayerTeam()
                == com.pigeostudios.pwp.core.Team.SPECTATOR
                ? "Select a team first"
                : "No classes available";
            int mw = font.width(msg);
            g.drawString(font, msg, width / 2 - mw / 2, height / 2,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        } else {
            int panelY = 34;
            scrollPanel.render(g);
            float scrollOff = scrollPanel.getScrollOffset();
            g.enableScissor(panelX, panelY, panelX + panelW, panelY + scrollPanel.getHeight());

            for (int i = 0; i < classes.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + GAP + col * (CARD_W + GAP);
                int cy = panelY + GAP + row * (CARD_H + GAP) - Math.round(scrollOff);

                if (cy + CARD_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

                boolean hov = mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H;
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.12f);

                ClassCard.draw(g, cx, cy, classes.get(i), classKeys.get(i),
                        lockStates.get(i), lockReasons.get(i),
                        hoverState[i], fadeAlpha);
            }
            g.disableScissor();
        }

        super.render(g, mx, my, pt);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int sortBtnX = (width / 2 - (COLS * (CARD_W + GAP) + GAP) / 2) + font.width("SELECT YOUR CLASS") + 16;
        int sortBtnY = 18 - (font.lineHeight + 4) / 2;
        if (sortControl.handleClick(mx, my, sortBtnX, sortBtnY)) {
            sortControl.cycleMode();
            applySort(sortControl.getMode());
            return true;
        }

        if (classes.isEmpty()) return super.mouseClicked(mx, my, btn);

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return super.mouseClicked(mx, my, btn);

        int panelW = COLS * (CARD_W + GAP) + GAP;
        int panelX = width / 2 - panelW / 2;
        int panelY = 34;
        float scrollOff = scrollPanel.getScrollOffset();

        for (int i = 0; i < classes.size(); i++) {
            if (!lockStates.get(i).isSelectable()) continue;
            int col = i % COLS;
            int row = i / COLS;
            int cx = panelX + GAP + col * (CARD_W + GAP);
            int cy = panelY + GAP + row * (CARD_H + GAP) - Math.round(scrollOff);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                Minecraft.getInstance().setScreen(new KitSelectionScreen(classKeys.get(i)));
                return true;
            }
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
            if (cfg != null && cfg.classes != null && !cfg.classes.isEmpty()) {
                init();
                return;
            }
        }
        scrollPanel.tick();
    }

    @Override
    public void onClose() {
        SpawnScreenHelper.reopen();
    }
}
