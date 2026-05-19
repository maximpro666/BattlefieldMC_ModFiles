package com.yourmod.teamsystem.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.client.gui.component.AnimationHelper;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.network.OpenSpawnSelectionScreenPacket.BeaconInfo;
import com.yourmod.teamsystem.network.OpenSpawnSelectionScreenPacket.SquadmateInfo;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.RespawnAtPointPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpawnSelectionScreen extends Screen {

    private static final int LEFT_PANEL_RATIO  = 60;
    private static final int CARD_HEIGHT       = 60;
    private static final int CARD_MARGIN       = 6;
    private static final int SECTION_HEADER_H  = 22;
    private static final int PANEL_PAD         = 14;
    private static final int ACCENT_BAR_W      = 3;
    private static final int ICON_SIZE         = 22;
    private static final int SCROLL_SPEED      = 12;
    private static final float FADE_DURATION_MS   = 250f;
    private static final int   STAGGER_TICKS      = 3;

    private final List<SquadmateInfo>    squadmates;
    private final List<FOBData>          fobs;
    private final List<BeaconInfo>       beacons;
    private final int                    teamOrdinal;
    private final String                 selectedKit;

    private final List<SpawnCard> cards = new ArrayList<>();
    private long   openTimeMs;
    private int    tickCount;
    private int    scrollOffset;
    private int    maxScroll;
    private float[] cardHover;

    public SpawnSelectionScreen(List<SquadmateInfo> squadmates,
                                List<FOBData> fobs,
                                List<BeaconInfo> beacons,
                                int teamOrdinal,
                                String selectedKit) {
        super(Component.literal("Spawn Selection"));
        this.squadmates  = squadmates  != null ? squadmates  : List.of();
        this.fobs        = fobs        != null ? fobs        : List.of();
        this.beacons     = beacons     != null ? beacons     : List.of();
        this.teamOrdinal = teamOrdinal;
        this.selectedKit = selectedKit != null ? selectedKit : "";
    }

    @Override
    protected void init() {
        super.init();
        openTimeMs = System.currentTimeMillis();
        tickCount  = 0;
        scrollOffset = 0;
        buildCardList();
        cardHover = new float[cards.size()];
        computeMaxScroll();
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void buildCardList() {
        cards.clear();
        cards.add(new SpawnCard(SpawnType.BASE, "Main Base", "Main Base",
                new UUID(0, 0), teamOrdinal, 0));
        for (SquadmateInfo sm : squadmates) {
            cards.add(new SpawnCard(SpawnType.SQUADMATE, sm.callsign(), sm.callsign(),
                    sm.uuid(), sm.teamOrdinal(), sm.cooldownTicks()));
        }
        for (FOBData fob : fobs) {
            int cd = 0;
            cards.add(new SpawnCard(SpawnType.FOB, fob.name(), fob.name(),
                    new UUID(0, 0), fob.teamOrdinal(), cd));
        }
        for (BeaconInfo b : beacons) {
            cards.add(new SpawnCard(SpawnType.BEACON, b.name(), b.name(),
                    new UUID(0, 0), b.teamOrdinal(), 0));
        }
    }

    private void computeMaxScroll() {
        int leftPanelW = leftPanelWidth();
        int contentH   = computeContentHeight();
        int visibleH   = height - PANEL_PAD * 2;
        maxScroll = Math.max(0, contentH - visibleH);
    }

    private int computeContentHeight() {
        int h = 0;
        SpawnType last = null;
        for (SpawnCard c : cards) {
            if (c.type != last) { h += SECTION_HEADER_H + CARD_MARGIN; last = c.type; }
            h += CARD_HEIGHT + CARD_MARGIN;
        }
        return h;
    }

    @Override
    public void tick() {
        tickCount++;
        for (SpawnCard c : cards) {
            if (c.cooldownTicks > 0) c.cooldownTicks--;
            if (c.type == SpawnType.SQUADMATE) {
                Map<java.util.UUID, Integer> statuses = ClientTeamData.squadmateStatuses;
                if (statuses != null && statuses.containsKey(c.targetUUID)) {
                    c.cooldownTicks = Math.max(c.cooldownTicks, statuses.get(c.targetUUID));
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        float fade = Math.min(1f, (System.currentTimeMillis() - openTimeMs) / FADE_DURATION_MS);
        int leftW  = leftPanelWidth();
        int rightX = leftW;
        int rightW = width - leftW;
        gfx.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fade * 0xCC)));
        renderLeftPanel(gfx, mouseX, mouseY, fade, leftW);
        gfx.fill(leftW, PANEL_PAD, leftW + 1, height - PANEL_PAD,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fade * 0x55)));
        renderRightPanel(gfx, rightX, rightW, mouseX, mouseY, fade);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void renderLeftPanel(GuiGraphics gfx, int mouseX, int mouseY, float fade, int panelW) {
        int x    = PANEL_PAD;
        int maxW = panelW - PANEL_PAD * 2;
        int y    = PANEL_PAD - scrollOffset;
        RenderSystem.enableScissor(0, 0, panelW, height);
        SpawnType lastType = null;
        int cardIndex = 0;
        for (SpawnCard card : cards) {
            if (card.type != lastType) {
                if (y > -SECTION_HEADER_H && y < height + SECTION_HEADER_H) {
                    renderSectionHeader(gfx, x, y, maxW, card.type.label, fade);
                }
                y += SECTION_HEADER_H + CARD_MARGIN;
                lastType = card.type;
            }
            float progress = Math.min(1f, (tickCount - cardIndex * (float) STAGGER_TICKS) / 12f);
            progress = Math.max(0f, AnimationHelper.easeOutCubic(progress));
            int slideX = x - (int)((1f - progress) * (panelW * 0.4f));
            boolean hovered = mouseX >= x && mouseX < x + maxW && mouseY >= y && mouseY < y + CARD_HEIGHT;
            cardHover[cardIndex] = AnimationHelper.lerp(cardHover[cardIndex], hovered ? 1f : 0f, 0.15f);
            if (y > -CARD_HEIGHT && y < height + CARD_HEIGHT) {
                renderSpawnCard(gfx, slideX, y, maxW, card, cardHover[cardIndex], fade);
            }
            y += CARD_HEIGHT + CARD_MARGIN;
            cardIndex++;
        }
        RenderSystem.disableScissor();
    }

    private void renderSectionHeader(GuiGraphics gfx, int x, int y, int w, String label, float fade) {
        int alpha = (int)(fade * 0xFF);
        gfx.fill(x, y + SECTION_HEADER_H / 2, x + w, y + SECTION_HEADER_H / 2 + 1,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha / 3));
        gfx.drawString(font, label, x, y + (SECTION_HEADER_H - 8) / 2,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha), false);
    }

    private void renderSpawnCard(GuiGraphics gfx, int x, int y, int w,
                                  SpawnCard card, float hover, float fade) {
        boolean available = card.cooldownTicks == 0;
        int alpha = (int)(fade * 0xFF);
        int bgColor = AnimationHelper.withAlpha(UITheme.BG_SURFACE,
                (int)(alpha * (0.85f + 0.15f * hover)));
        gfx.fill(x, y, x + w, y + CARD_HEIGHT, bgColor);
        int barColor = available
                ? AnimationHelper.withAlpha(UITheme.STATUS_OK, alpha)
                : AnimationHelper.withAlpha(UITheme.MAP_COOLDOWN, alpha);
        gfx.fill(x, y, x + ACCENT_BAR_W, y + CARD_HEIGHT, barColor);
        int iconX = x + ACCENT_BAR_W + 10;
        int iconY = y + (CARD_HEIGHT - ICON_SIZE) / 2;
        renderTypeIcon(gfx, card.type, iconX, iconY, card.teamColor(), alpha);
        int textX   = iconX + ICON_SIZE + 8;
        int nameY   = y + 12;
        int statusY = y + 12 + font.lineHeight + 4;
        gfx.drawString(font, card.displayName, textX, nameY,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha), false);
        String statusText;
        int    statusColor;
        if (available) {
            statusText  = "Ready";
            statusColor = UITheme.STATUS_OK;
        } else {
            int secs   = (card.cooldownTicks + 19) / 20;
            statusText = "Under fire \u2014 " + secs + "s";
            statusColor = UITheme.MAP_COOLDOWN;
        }
        gfx.drawString(font, statusText, textX, statusY,
                AnimationHelper.withAlpha(statusColor, alpha), false);
        if (available) {
            int btnW   = 50;
            int btnX   = x + w - btnW - 8;
            int btnY   = y + (CARD_HEIGHT - 16) / 2;
            int btnBg  = AnimationHelper.withAlpha(UITheme.ACCENT,
                    (int)(alpha * (0.6f + 0.4f * hover)));
            gfx.fill(btnX, btnY, btnX + btnW, btnY + 16, btnBg);
            gfx.drawCenteredString(font, "SPAWN", btnX + btnW / 2, btnY + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
        }
    }

    private void renderTypeIcon(GuiGraphics gfx, SpawnType type, int x, int y, int color, int alpha) {
        int c = AnimationHelper.withAlpha(color, alpha);
        switch (type) {
            case BASE -> gfx.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, c);
            case SQUADMATE -> {
                int pad = 4;
                gfx.fill(x + pad, y,       x + ICON_SIZE - pad, y + ICON_SIZE,       c);
                gfx.fill(x,       y + pad, x + ICON_SIZE,       y + ICON_SIZE - pad, c);
            }
            case FOB -> {
                int mid = ICON_SIZE / 2;
                gfx.fill(x + mid / 2, y,        x + mid + mid / 2, y + ICON_SIZE, c);
                gfx.fill(x,          y + mid / 2, x + ICON_SIZE,   y + mid + mid / 2, c);
            }
            case BEACON -> {
                int s = 4;
                gfx.fill(x + s, y,        x + ICON_SIZE - s, y + ICON_SIZE, c);
                gfx.fill(x,     y + s,    x + ICON_SIZE,     y + ICON_SIZE - s, c);
            }
        }
    }

    private void renderRightPanel(GuiGraphics gfx, int rx, int rw, int mouseX, int mouseY, float fade) {
        int alpha = (int)(fade * 0xFF);
        int x     = rx + PANEL_PAD;
        int w     = rw - PANEL_PAD * 2;
        int y     = PANEL_PAD + 10;
        String teamName  = teamOrdinal == 0 ? "NATO" : "RUSSIA";
        int    teamColor = teamOrdinal == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
        gfx.fill(x, y, x + w, y + 40, AnimationHelper.withAlpha(UITheme.BG_SURFACE, alpha));
        gfx.fill(x, y, x + ACCENT_BAR_W, y + 40, AnimationHelper.withAlpha(teamColor, alpha));
        gfx.drawString(font, "TEAM", x + ACCENT_BAR_W + 8, y + 6,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha), false);
        gfx.drawString(font, teamName, x + ACCENT_BAR_W + 8, y + 18,
                AnimationHelper.withAlpha(teamColor, alpha), false);
        y += 46 + 30;
        gfx.fill(x, y, x + w, y + 40, AnimationHelper.withAlpha(UITheme.BG_SURFACE, alpha));
        gfx.fill(x, y, x + ACCENT_BAR_W, y + 40, AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
        gfx.drawString(font, "KIT", x + ACCENT_BAR_W + 8, y + 6,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha), false);
        gfx.drawString(font, selectedKit.isEmpty() ? "Default" : selectedKit,
                x + ACCENT_BAR_W + 8, y + 18,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha), false);
        y += 46;
        renderSmallButton(gfx, x, y, w, "CUSTOMIZE LOADOUT", alpha);
    }

    private void renderSmallButton(GuiGraphics gfx, int x, int y, int w, String label, int alpha) {
        gfx.fill(x, y, x + w, y + 22,
                AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(alpha * 0.8f)));
        gfx.drawCenteredString(font, label, x + w / 2, y + 7,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);
        int leftW = leftPanelWidth();
        if (mx >= leftW) {
            handleRightPanelClick((int) mx, (int) my, leftW);
            return true;
        }
        int x    = PANEL_PAD;
        int maxW = leftW - PANEL_PAD * 2;
        int y    = PANEL_PAD - scrollOffset;
        SpawnType lastType = null;
        for (SpawnCard card : cards) {
            if (card.type != lastType) { y += SECTION_HEADER_H + CARD_MARGIN; lastType = card.type; }
            if (mx >= x && mx < x + maxW && my >= y && my < y + CARD_HEIGHT) {
                if (card.cooldownTicks == 0) {
                    sendRespawnPacket(card);
                }
                return true;
            }
            y += CARD_HEIGHT + CARD_MARGIN;
        }
        return super.mouseClicked(mx, my, btn);
    }

    private void handleRightPanelClick(int mx, int my, int rx) {
        int x = rx + PANEL_PAD;
        int y = PANEL_PAD + 10 + 46 + 30;
        if (mx >= x && my >= y && my < y + 22) {
            minecraft.setScreen(new ClassSelectionScreen());
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < leftPanelWidth()) {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll,
                    scrollOffset - delta * SCROLL_SPEED));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256 && !cards.isEmpty() && cards.get(0).cooldownTicks == 0) {
            sendRespawnPacket(cards.get(0));
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void sendRespawnPacket(SpawnCard card) {
        String type = card.type.packetType;
        PacketHandler.CHANNEL.sendToServer(
                new RespawnAtPointPacket(type, card.targetUUID, card.targetName));
    }

    private int leftPanelWidth() {
        return width * LEFT_PANEL_RATIO / 100;
    }

    private enum SpawnType {
        BASE("MAIN BASE",          RespawnAtPointPacket.TYPE_BASE),
        SQUADMATE("SQUADMATES",    RespawnAtPointPacket.TYPE_SQUADMATE),
        FOB("FOBs",                RespawnAtPointPacket.TYPE_FOB),
        BEACON("RESPAWN BEACONS",  RespawnAtPointPacket.TYPE_BEACON);

        final String label;
        final String packetType;
        SpawnType(String label, String packetType) {
            this.label      = label;
            this.packetType = packetType;
        }
    }

    private static class SpawnCard {
        final SpawnType type;
        final String    displayName;
        final String    targetName;
        final UUID      targetUUID;
        final int       team;
        int             cooldownTicks;

        SpawnCard(SpawnType type, String displayName, String targetName,
                  UUID targetUUID, int team, int cooldownTicks) {
            this.type          = type;
            this.displayName   = displayName;
            this.targetName    = targetName;
            this.targetUUID    = targetUUID;
            this.team          = team;
            this.cooldownTicks = cooldownTicks;
        }

        int teamColor() {
            return team == 0 ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;
        }
    }
}
