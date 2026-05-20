package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.gui.I18n;
import com.yourmod.teamsystem.client.gui.UITheme;

import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.KitSelectPacket;
import com.yourmod.teamsystem.client.gui.component.BScrollPanel;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;

public class KitSelectionScreen extends Screen {

    private static final int COLOR_BG       = UITheme.BG_SCREEN;
    private static final int COLOR_CARD     = UITheme.BG_SURFACE;
    private static final int COLOR_SELECTED = UITheme.ACCENT;
    private static final int COLOR_BORDER   = UITheme.NAMETAG_SPECTATOR;
    private static final int COLOR_TEXT     = UITheme.TEXT_PRIMARY;
    private static final int COLOR_SUBTEXT  = UITheme.TEXT_SECONDARY;

    private static final int COLS    = 3;
    private static final int CELL_W  = 130;
    private static final int CELL_H  = 80;
    private static final int PADDING = 10;

    private final String classId;
    private final List<Map.Entry<String, KitConfig.KitDef>> kitEntries = new ArrayList<>();

    private String selectedKitId = null;
    private float fadeAlpha    = 0f;
    private long openTime;
    private BScrollPanel scrollPanel;
    private com.yourmod.teamsystem.client.gui.component.BButton confirmButton;
    private com.yourmod.teamsystem.client.gui.component.BButton customizeButton;
    private com.yourmod.teamsystem.client.gui.component.BButton backButton;
    private float[] hoverState;

    public KitSelectionScreen(String classId) {
        super(Component.literal("Kit Selection"));
        this.classId = classId;
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        loadKits();
        hoverState = new float[kitEntries.size()];

        int panelW = COLS * (CELL_W + PADDING) + PADDING;
        int panelH = height - 80;
        int panelX = width / 2 - panelW / 2;
        int panelY = 40;

        scrollPanel = new BScrollPanel(panelX, panelY, panelW, panelH);
        int rows = kitEntries.isEmpty() ? 1 : (kitEntries.size() + COLS - 1) / COLS;
        scrollPanel.setContentHeight(rows * (CELL_H + PADDING) + PADDING);

        int btnY = height - 28;
        backButton = addRenderableWidget(new com.yourmod.teamsystem.client.gui.component.BButton(
            width / 2 - 190, btnY, 60, 20,
            Component.literal("\u2190 Back"), btn -> backToClasses()
        ));
        confirmButton = addRenderableWidget(new com.yourmod.teamsystem.client.gui.component.BButton(
            width / 2 - 60, btnY, 120, 20,
            Component.literal("Select Kit"), btn -> confirmSelection()
        ));
        customizeButton = addRenderableWidget(new com.yourmod.teamsystem.client.gui.component.BButton(
            width / 2 + 66, btnY, 120, 20,
            Component.literal("Customize"), btn -> customizeKit()
        ));
    }

    private void loadKits() {
        kitEntries.clear();
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return;
        List<Map.Entry<String, KitConfig.KitDef>> affordable = new ArrayList<>();
        List<Map.Entry<String, KitConfig.KitDef>> locked = new ArrayList<>();
        for (Map.Entry<String, KitConfig.KitDef> entry : cl.kits.entrySet()) {
            KitConfig.KitDef kit = entry.getValue();
            if (kit.requirements == null ||
                (kit.requirements.sp_cost <= com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP &&
                 kit.requirements.bc_cost <= com.yourmod.teamsystem.client.ClientTeamData.localPlayerBC)) {
                affordable.add(entry);
            } else {
                locked.add(entry);
            }
        }
        kitEntries.addAll(affordable);
        kitEntries.addAll(locked);
    }

    private boolean canAffordSelected() {
        if (selectedKitId == null) return false;
        KitConfig cfg = KitConfig.get();
        if (cfg == null) return false;
        KitConfig.ClassConfig cl = cfg.classes.get(classId);
        if (cl == null) return false;
        KitConfig.KitDef kit = cl.kits.get(selectedKitId);
        if (kit == null || kit.requirements == null) return true;
        if (kit.requirements.sp_cost > 0 && com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP < kit.requirements.sp_cost) return false;
        if (kit.requirements.bc_cost > 0 && com.yourmod.teamsystem.client.ClientTeamData.localPlayerBC < kit.requirements.bc_cost) return false;
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

    private void customizeKit() {
        if (selectedKitId != null) {
            Minecraft.getInstance().setScreen(new KitCustomizationScreen(classId, selectedKitId));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        fadeAlpha = Math.min(1f, (System.currentTimeMillis() - openTime) / 250f);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(COLOR_BG, (int)(fadeAlpha * 0xCC)));

        String title = "SELECT KIT";
        int tw = font.width(title);
        g.drawString(font, title, width / 2 - tw / 2, 14, AnimationHelper.withAlpha(COLOR_SELECTED, (int)(fadeAlpha * 255)));

        if (!kitEntries.isEmpty()) {
            int panelW = COLS * (CELL_W + PADDING) + PADDING;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;

            scrollPanel.render(g);

            float scrollOff = scrollPanel.getScrollOffset();
            g.enableScissor(panelX, panelY, panelX + panelW, panelY + scrollPanel.getHeight());

            for (int i = 0; i < kitEntries.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + PADDING + col * (CELL_W + PADDING);
                int cy = panelY + PADDING + row * (CELL_H + PADDING) - (int)scrollOff;

                if (cy + CELL_H < panelY || cy > panelY + scrollPanel.getHeight()) continue;

                boolean hov = mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H;
                hoverState[i] = AnimationHelper.lerp(hoverState[i], hov ? 1f : 0f, 0.15f);

                boolean sel = kitEntries.get(i).getKey().equals(selectedKitId);
                drawKitCell(g, cx, cy, kitEntries.get(i), sel, hoverState[i], fadeAlpha);
            }

            g.disableScissor();
        } else {
            String none = "No kits available";
            int nw = font.width(none);
            g.drawString(font, none, width / 2 - nw / 2, height / 2, AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(fadeAlpha * 200)));
        }

        confirmButton.active = selectedKitId != null && canAffordSelected();
        confirmButton.visible = selectedKitId != null;
        customizeButton.active = selectedKitId != null;
        customizeButton.visible = selectedKitId != null;
        super.render(g, mx, my, pt);
    }

    private void drawKitCell(GuiGraphics g, int x, int y, Map.Entry<String, KitConfig.KitDef> entry, boolean selected, float hover, float alpha) {
        KitConfig.KitDef kit = entry.getValue();
        int bgColor  = selected ? AnimationHelper.blendColors(UITheme.BG_SURFACE, UITheme.ACCENT, 0.15f)
                                : AnimationHelper.withAlpha(COLOR_CARD, (int)(alpha * 0xDD));
        int brdColor = selected ? AnimationHelper.withAlpha(COLOR_SELECTED, (int)(alpha * 255))
                                : AnimationHelper.withAlpha(COLOR_BORDER, (int)(alpha * (0x44 + hover * 0xBB)));

        g.fill(x, y, x + CELL_W, y + CELL_H, bgColor);
        g.fill(x, y, x + CELL_W, y + 1, brdColor);
        g.fill(x, y + CELL_H - 1, x + CELL_W, y + CELL_H, brdColor);
        g.fill(x, y, x + 1, y + CELL_H, brdColor);
        g.fill(x + CELL_W - 1, y, x + CELL_W, y + CELL_H, brdColor);

        String display = kit.display_name != null ? I18n.localize(kit.display_name).toUpperCase() : entry.getKey().toUpperCase();
        int tw = font.width(display);
        g.drawString(font, display,
            x + CELL_W / 2 - tw / 2, y + CELL_H / 2 - 8,
            AnimationHelper.withAlpha(COLOR_TEXT, (int)(alpha * 255)));

        if (kit.description != null && !kit.description.isEmpty()) {
            g.drawCenteredString(font, I18n.localize(kit.description), x + CELL_W / 2, y + CELL_H / 2 + 8,
                AnimationHelper.withAlpha(COLOR_SUBTEXT, (int)(alpha * 180)));
        }

        if (selected) {
            String sel = "\u2713 SELECTED";
            int sw = font.width(sel);
            g.drawString(font, sel, x + CELL_W / 2 - sw / 2, y + CELL_H / 2 + 22,
                AnimationHelper.withAlpha(COLOR_SELECTED, (int)(alpha * 255)));
        }

        if (kit.requirements != null) {
            int lineY = y + 4;
            if (kit.requirements.sp_cost > 0) {
                boolean canAfford = com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP >= kit.requirements.sp_cost;
                int c = canAfford ? AnimationHelper.withAlpha(0xFFAA00, (int)(alpha * 255))
                                  : AnimationHelper.withAlpha(0xFF4444, (int)(alpha * 255));
                String s = "SP " + kit.requirements.sp_cost;
                int cw = font.width(s);
                g.drawString(font, s, x + CELL_W - cw - 4, lineY, c);
                lineY += 10;
            }
            if (kit.requirements.bc_cost > 0) {
                boolean canAfford = com.yourmod.teamsystem.client.ClientTeamData.localPlayerBC >= kit.requirements.bc_cost;
                int c = canAfford ? AnimationHelper.withAlpha(0x55FF55, (int)(alpha * 255))
                                  : AnimationHelper.withAlpha(0xFF4444, (int)(alpha * 255));
                String s = "BC " + kit.requirements.bc_cost;
                int cw = font.width(s);
                g.drawString(font, s, x + CELL_W - cw - 4, lineY, c);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!kitEntries.isEmpty()) {
            int panelW = COLS * (CELL_W + PADDING) + PADDING;
            int panelX = width / 2 - panelW / 2;
            int panelY = 40;
            float scrollOff = scrollPanel.getScrollOffset();

            for (int i = 0; i < kitEntries.size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                int cx = panelX + PADDING + col * (CELL_W + PADDING);
                int cy = panelY + PADDING + row * (CELL_H + PADDING) - (int)scrollOff;
                if (mx >= cx && mx <= cx + CELL_W && my >= cy && my <= cy + CELL_H) {
                    if (btn == 1) {
                        selectedKitId = kitEntries.get(i).getKey();
                        customizeKit();
                    } else {
                        selectedKitId = kitEntries.get(i).getKey();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void backToClasses() {
        Minecraft.getInstance().setScreen(new ClassSelectionScreen());
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollPanel.onScroll((int)mx, (int)my, delta);
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

    @Override
    public boolean isPauseScreen() { return false; }
}
