package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.I18n;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import com.pigeostudios.pwp.client.gui.component.BScrollPanel;
import com.pigeostudios.pwp.client.gui.component.KitCard;
import com.pigeostudios.pwp.client.gui.component.SortControl;
import com.pigeostudios.pwp.data.ItemResolver;
import com.pigeostudios.pwp.data.KitConfig;
import com.pigeostudios.pwp.data.LockChecker;
import com.pigeostudios.pwp.data.LockState;
import com.pigeostudios.pwp.network.PacketHandler;
import com.pigeostudios.pwp.network.KitSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KitSelectionScreen extends Screen {

    private static final int LEFT_RATIO = 58;
    private static final int CARD_W = 130;
    private static final int CARD_H = 86;
    private static final int GAP = 8;

    private final String classId;
    private final List<Map.Entry<String, KitConfig.KitDef>> kitEntries = new ArrayList<>();
    private final List<LockState> lockStates = new ArrayList<>();
    private final List<String> lockReasons = new ArrayList<>();
    private String selectedKitId = null;
    private float fadeAlpha = 0f;
    private long openTime;
    private int tickCount;
    private float[] hoverState;
    private BScrollPanel scrollPanel;
    private SortControl sortControl = new SortControl();
    private final Map<String, String> displayNames = new HashMap<>();

    private String displayName(String id) {
        if (id == null || id.isEmpty()) return "\u2014";
        return displayNames.computeIfAbsent(id, k -> {
            ItemStack stack = ItemResolver.resolve(k);
            if (!stack.isEmpty()) return stack.getHoverName().getString();
            return k.replace("_", " ");
        });
    }

    public KitSelectionScreen(String classId) {
        super(Component.literal("Kit Selection"));
        this.classId = classId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        tickCount = 0;
        loadKits();
        applySort(sortControl.getMode());
        hoverState = new float[kitEntries.size()];

        int leftW = width * LEFT_RATIO / 100;
        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = 50;
        int panelH = height - 80;

        scrollPanel = new BScrollPanel(panelX, panelY, gridW, panelH);
        int rows = kitEntries.isEmpty() ? 1 : (kitEntries.size() + cols - 1) / cols;
        scrollPanel.setContentHeight(rows * (CARD_H + GAP) + GAP);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void loadKits() {
        kitEntries.clear();
        lockStates.clear();
        lockReasons.clear();

        com.pigeostudios.pwp.core.Team team = com.pigeostudios.pwp.client.ClientTeamData.getLocalPlayerTeam();
        if (team == com.pigeostudios.pwp.core.Team.SPECTATOR) return;

        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null || cl.kits == null) return;

        LockChecker.Context ctx = new LockChecker.Context();
        ctx.playerRank = com.pigeostudios.pwp.client.ClientTeamData.localPlayerRank;
        ctx.playerTeam = team.name();
        ctx.playerSP = com.pigeostudios.pwp.client.ClientTeamData.localPlayerWC;
        ctx.playerBC = com.pigeostudios.pwp.client.ClientTeamData.localPlayerBC;

        for (Map.Entry<String, KitConfig.KitDef> entry : cl.kits.entrySet()) {
            kitEntries.add(entry);
            LockState ls = LockChecker.checkKit(entry.getValue(), ctx);
            lockStates.add(ls);
            String reason = "";
            if (!ls.isSelectable()) {
                var req = entry.getValue().requirements;
                if (ls == LockState.LOCKED_RANK) {
                    reason = "Rank " + (req != null ? req.rank : 0) + " required";
                } else if (ls == LockState.LOCKED_COST) {
                    if (req != null && req.sp_cost > 0 && ctx.playerSP < req.sp_cost)
                        reason = req.sp_cost + " SP required";
                    else if (req != null && req.bc_cost > 0 && ctx.playerBC < req.bc_cost)
                        reason = req.bc_cost + " BC required";
                    else
                        reason = "Cost required";
                } else {
                    reason = "Locked";
                }
            }
            lockReasons.add(reason);
        }
        selectedKitId = !kitEntries.isEmpty() ? kitEntries.get(0).getKey() : null;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        tickCount++;
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);
        int alpha = (int)(fadeAlpha * 0xFF);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        int leftW = width * LEFT_RATIO / 100;

        renderHeader(g, mx, my, alpha);
        renderLeftPanel(g, mx, my, fadeAlpha, alpha, leftW);
        renderDivider(g, leftW, fadeAlpha, alpha);
        renderRightPanel(g, leftW, mx, my, fadeAlpha, alpha);

        super.render(g, mx, my, pt);
    }

    private void renderHeader(GuiGraphics g, int mx, int my, int alpha) {
        KitConfig cfg = KitConfig.get();
        KitConfig.ClassConfig cl = cfg != null ? cfg.classes.get(classId) : null;
        String className = cl != null && cl.display_name != null ? I18n.localize(cl.display_name).toUpperCase() : classId.toUpperCase();

        int available = 0;
        for (var ls : lockStates) {
            if (ls.isSelectable()) available++;
        }

        int x = 16;
        if (cl != null && cl.icon != null && !cl.icon.isEmpty()) {
            g.drawString(font, cl.icon, x, 14, UITheme.TEXT_PRIMARY);
            x += font.width(cl.icon) + 8;
        }
        g.drawString(font, className, x, 14, UITheme.ACCENT);
        x += font.width(className) + 8;
        g.drawString(font, "\u2014 SELECT KIT", x, 14,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
        x += font.width("\u2014 SELECT KIT") + 16;

        int sortBtnY = 14 - (font.lineHeight + 4) / 2 + 2;
        sortControl.render(g, x, sortBtnY, mx, my, fadeAlpha);

        String count = available + "/" + kitEntries.size() + " available";
        g.drawString(font, count, width - font.width(count) - 16, 14,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
    }

    private void renderDivider(GuiGraphics g, int x, float fade, int alpha) {
        g.fill(x, 42, x + 1, height - 10, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA)));
    }

    // ── LEFT PANEL (kit grid) ───────────────────────────

    private KitConfig.ClassConfig getClassConfig() {
        KitConfig cfg = KitConfig.get();
        return cfg != null ? cfg.classes.get(classId) : null;
    }

    private void renderLeftPanel(GuiGraphics g, int mx, int my, float fade, int alpha, int leftW) {
        if (kitEntries.isEmpty()) {
            String none = "No kits available";
            g.drawString(font, none, (leftW - font.width(none)) / 2, height / 2,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
            return;
        }

        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = 50;

        scrollPanel.render(g);
        float scrollOff = scrollPanel.getScrollOffset();
        g.enableScissor(panelX, panelY, panelX + gridW, panelY + scrollPanel.getHeight());

        for (int i = 0; i < kitEntries.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            int cx = panelX + col * colW;
            int cy = panelY + row * (CARD_H + GAP) - Math.round(scrollOff);

            if (cy + CARD_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

            boolean hov = mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H;
            hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.12f);

            boolean sel = kitEntries.get(i).getKey().equals(selectedKitId);
            KitCard.draw(g, cx, cy, kitEntries.get(i).getValue(), kitEntries.get(i).getKey(),
                lockStates.get(i), lockReasons.get(i), sel, hoverState[i], fade);

            // Equipped badge overlay
            String equippedId = com.pigeostudios.pwp.client.gui.screen.SpawnScreenHelper.getLastSelectedKit();
            if (equippedId != null && equippedId.equals(classId + ":" + kitEntries.get(i).getKey())) {
                String badge = "EQUIPPED";
                int bw = font.width(badge);
                int bx = cx + KitCard.CARD_W - bw - 16;
                int by = cy + 3;
                int bh = 14;
                g.fill(bx - 2, by, bx + bw + 6, by + bh,
                        AnimationHelper.withAlpha(0xFF1A1A1A, (int)(fade * 0xEE)));
                g.fill(bx - 2, by, bx + bw + 6, by + 1,
                        AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xCC)));
                g.drawString(font, badge, bx, by + 4, UITheme.ACCENT);
            }
        }
        g.disableScissor();

        // Hint
        String hint = "Right-click to select & customize";
        g.drawString(font, hint, (leftW - font.width(hint)) / 2, height - 20,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 120)));
    }



    // ── RIGHT PANEL (kit details) ───────────────────────

    private void renderRightPanel(GuiGraphics g, int rx, int mx, int my, float fade, int alpha) {
        int x = rx + 14;
        int w = width - rx - 14;
        int y = 50;

        KitConfig.KitDef active = null;
        String activeKey = null;
        int activeIdx = 0;
        for (int i = 0; i < kitEntries.size(); i++) {
            if (kitEntries.get(i).getKey().equals(selectedKitId)) {
                active = kitEntries.get(i).getValue();
                activeKey = kitEntries.get(i).getKey();
                activeIdx = i;
                break;
            }
        }
        if (active == null) {
            String msg = "Select a kit to preview";
            g.drawString(font, msg, x + (w - font.width(msg)) / 2, height / 2,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 150)));
            return;
        }

        boolean locked = !lockStates.get(activeIdx).isSelectable();

        // Kit name + description
        String name = active.display_name != null ? I18n.localize(active.display_name).toUpperCase() : activeKey.toUpperCase();
        g.drawString(font, name, x, y,
                AnimationHelper.withAlpha(locked ? UITheme.TEXT_MUTED : UITheme.ACCENT, alpha));
        y += 12;

        if (active.description != null && !active.description.isEmpty()) {
            g.drawString(font, I18n.localize(active.description), x, y,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fade * 200)));
            y += 14;
        }

        // Accent line
        g.fill(x, y, x + Math.min(w, 200), y + 2,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * 0xCC)));
        y += 12;

        if (locked) {
            g.drawString(font, "\uD83D\uDD12 " + lockReasons.get(activeIdx), x, y,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fade * 200)));
            return;
        }

        // Weapon slots
        String[][] weaponSlots = {
                {"Primary", active.weapons != null && active.weapons.primary != null && !active.weapons.primary.isEmpty() ? active.weapons.primary.get(0) : null},
                {"Secondary", active.weapons != null && active.weapons.secondary != null && !active.weapons.secondary.isEmpty() ? active.weapons.secondary.get(0) : null},
                {"Special", active.weapons != null && active.weapons.special != null && !active.weapons.special.isEmpty() ? active.weapons.special.get(0) : null},
                {"Grenade", active.weapons != null && active.weapons.grenade != null && !active.weapons.grenade.isEmpty() ? active.weapons.grenade.get(0) : null},
        };

        int iconX = x;
        for (var slot : weaponSlots) {
            if (slot[1] == null) continue;
            g.drawString(font, displayName(slot[1]), iconX, y,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
            y += 12;
        }

        // Buttons
        int btnY = height - 48;
        int btnW = (w - 8) / 2;

        // Select button
        boolean selHov = mx >= x && mx < x + btnW && my >= btnY && my < btnY + 26;
        int selBg = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, selHov ? 1f : 0f);
        g.fill(x, btnY, x + btnW, btnY + 26, AnimationHelper.withAlpha(selBg, alpha));
        g.fill(x, btnY, x + 2, btnY + 26, AnimationHelper.withAlpha(0x33000000, alpha));
        String selTxt = "Deploy Kit";
        g.drawString(font, selTxt, x + btnW / 2 - font.width(selTxt) / 2, btnY + 9, 0xFFFFFFFF);

        // Customize button
        int btnX2 = x + btnW + 8;
        boolean custHov = mx >= btnX2 && mx < btnX2 + btnW && my >= btnY && my < btnY + 26;
        int custBg = custHov ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD)) : 0x00000000;
        if (custBg != 0) g.fill(btnX2, btnY, btnX2 + btnW, btnY + 26, custBg);
        int custBorder = custHov ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, alpha) : AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA));
        g.fill(btnX2, btnY, btnX2 + btnW, btnY + 1, custBorder);
        g.fill(btnX2, btnY + 25, btnX2 + btnW, btnY + 26, custBorder);
        g.fill(btnX2 + btnW - 1, btnY, btnX2 + btnW, btnY + 26, custBorder);
        g.fill(btnX2, btnY, btnX2 + 2, btnY + 26,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fade * (0x44 + (custHov ? 0x44 : 0)))));
        String custTxt = "Customize \u2192";
        g.drawString(font, custTxt, btnX2 + btnW / 2 - font.width(custTxt) / 2, btnY + 9,
                AnimationHelper.withAlpha(custHov ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, alpha));
    }

    // ── MOUSE ───────────────────────────────────────────

    private void applySort(SortControl.SortMode mode) {
        int n = kitEntries.size();
        if (n <= 1) return;
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) idx[i] = i;
        java.util.Arrays.sort(idx, (a, b) -> {
            switch (mode) {
                case NAME:
                    return kitEntries.get(a).getKey().compareToIgnoreCase(kitEntries.get(b).getKey());
                case AVAILABILITY:
                    return Boolean.compare(!lockStates.get(a).isSelectable(), !lockStates.get(b).isSelectable());
                default:
                    return 0;
            }
        });
        List<Map.Entry<String, KitConfig.KitDef>> ek = new ArrayList<>();
        List<LockState> sl = new ArrayList<>();
        List<String> sr = new ArrayList<>();
        for (int i : idx) {
            ek.add(kitEntries.get(i));
            sl.add(lockStates.get(i));
            sr.add(lockReasons.get(i));
        }
        kitEntries.clear(); kitEntries.addAll(ek);
        lockStates.clear(); lockStates.addAll(sl);
        lockReasons.clear(); lockReasons.addAll(sr);
        hoverState = new float[n];
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int sortBtnX = 16;
        KitConfig cfg = KitConfig.get();
        KitConfig.ClassConfig cl = cfg != null ? cfg.classes.get(classId) : null;
        if (cl != null && cl.icon != null && !cl.icon.isEmpty()) {
            sortBtnX += font.width(cl.icon) + 8;
        }
        if (cl != null && cl.display_name != null) {
            sortBtnX += font.width(I18n.localize(cl.display_name).toUpperCase()) + 8;
        } else {
            sortBtnX += font.width(classId.toUpperCase()) + 8;
        }
        sortBtnX += font.width("\u2014 SELECT KIT") + 16;
        int sortBtnY = 14 - (font.lineHeight + 4) / 2 + 2;
        if (sortControl.handleClick(mx, my, sortBtnX, sortBtnY)) {
            sortControl.cycleMode();
            applySort(sortControl.getMode());
            return true;
        }

        if (kitEntries.isEmpty()) return super.mouseClicked(mx, my, btn);

        int leftW = width * LEFT_RATIO / 100;

        // Right panel buttons
        if (mx >= leftW) {
            return handleRightPanelClick(mx, my, btn, leftW);
        }

        // Left panel grid
        int colW = CARD_W + GAP;
        int cols = Math.max(1, (leftW - GAP) / colW);
        int gridW = cols * colW;
        int panelX = (leftW - gridW) / 2;
        int panelY = 50;
        float scrollOff = scrollPanel.getScrollOffset();

        for (int i = 0; i < kitEntries.size(); i++) {
            if (!lockStates.get(i).isSelectable()) continue;
            int col = i % cols;
            int row = i / cols;
            int cx = panelX + col * colW;
            int cy = panelY + row * (CARD_H + GAP) - Math.round(scrollOff);
            if (mx >= cx && mx <= cx + CARD_W && my >= cy && my <= cy + CARD_H) {
                if (btn == 1) {
                    selectedKitId = kitEntries.get(i).getKey();
                    openCustomize();
                } else {
                    selectedKitId = kitEntries.get(i).getKey();
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    private boolean handleRightPanelClick(double mx, double my, int btn, int rx) {
        if (selectedKitId == null) return true;
        int x = rx + 14;
        int w = width - rx - 14;
        int btnW = (w - 8) / 2;
        int btnY = height - 48;

        if (mx >= x && mx < x + btnW && my >= btnY && my < btnY + 26) {
            confirmSelection();
            return true;
        }
        int btnX2 = x + btnW + 8;
        if (mx >= btnX2 && mx < btnX2 + btnW && my >= btnY && my < btnY + 26) {
            openCustomize();
            return true;
        }
        return true;
    }

    private void confirmSelection() {
        if (selectedKitId != null) {
            String packageId = classId + ":" + selectedKitId;
            PacketHandler.CHANNEL.sendToServer(new KitSelectPacket(packageId));
            SpawnScreenHelper.updateSelectedKit(packageId);
            onClose();
        }
    }

    private void openCustomize() {
        if (selectedKitId != null) {
            Minecraft.getInstance().setScreen(new KitCustomizationScreen(classId, selectedKitId));
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollPanel.onScroll((int) mx, (int) my, delta);
        return true;
    }

    @Override
    public void tick() {
        scrollPanel.tick();
    }

    @Override
    public void onClose() {
        SpawnScreenHelper.reopen();
    }
}
