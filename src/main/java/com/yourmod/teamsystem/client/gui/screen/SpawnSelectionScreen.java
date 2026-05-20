package com.yourmod.teamsystem.client.gui.screen;

import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.FOBData;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.component.*;
import com.yourmod.teamsystem.data.KitConfig;
import com.yourmod.teamsystem.network.OpenSpawnSelectionScreenPacket;
import com.yourmod.teamsystem.network.PacketHandler;
import com.yourmod.teamsystem.network.RespawnAtPointPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.*;
import javax.annotation.Nullable;

public class SpawnSelectionScreen extends Screen {

    private static final int LEFT_RATIO = 55;
    private static final int CARD_H = 52;
    private static final int CARD_GAP = 4;

    private final List<OpenSpawnSelectionScreenPacket.SquadmateInfo> squadmates;
    private final List<FOBData> fobs;
    private final List<OpenSpawnSelectionScreenPacket.BeaconInfo> beacons;
    private final int teamOrdinal;
    private final String selectedKit;

    private final List<SpawnEntry> entries = new ArrayList<>();
    private long openTimeMs;
    private int tickCount;
    private float scrollOffset;
    private float targetScrollOffset;
    private String selectedId;
    private TopBar topBar;

    public SpawnSelectionScreen(List<OpenSpawnSelectionScreenPacket.SquadmateInfo> squadmates,
                                 List<FOBData> fobs,
                                 List<OpenSpawnSelectionScreenPacket.BeaconInfo> beacons,
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
        tickCount = 0;
        scrollOffset = 0;
        topBar = new TopBar();
        buildEntries();
        if (!entries.isEmpty()) selectedId = entries.get(0).id;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void buildEntries() {
        entries.clear();
        int[] basePos = teamOrdinal == 0 ? ClientTeamData.getNatoBasePos() : ClientTeamData.getRussiaBasePos();
        double baseX = basePos != null ? basePos[0] : 0;
        double baseZ = basePos != null ? basePos[2] : 0;
        entries.add(new SpawnEntry("base_main", "MAIN BASE", "BASE", teamOrdinal, 0, true, "Base", null, baseX, baseZ));
        for (var sm : squadmates)
            entries.add(new SpawnEntry("sm_" + sm.uuid(), sm.callsign(), "SQUADMATE", sm.teamOrdinal(), sm.cooldownTicks(), sm.cooldownTicks() == 0, "Squadmate", sm.uuid()));
        for (var fob : fobs)
            entries.add(new SpawnEntry("fob_" + fob.fobId(), fob.name(), "FOB", fob.teamOrdinal(), 0, fob.health() > 0, fob.name(), null, fob.x(), fob.z()));
        for (var b : beacons)
            entries.add(new SpawnEntry("beacon_" + b.name(), b.name(), "BEACON", b.teamOrdinal(), 0, true, "Beacon", null, b.x(), b.z()));
    }

    @Override
    public void tick() {
        tickCount++;
        scrollOffset = AnimationHelper.lerp(scrollOffset, targetScrollOffset, 0.15f);
        if (Math.abs(scrollOffset - targetScrollOffset) < 0.5f) scrollOffset = targetScrollOffset;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float fade = Math.min(1f, (System.currentTimeMillis() - openTimeMs) / 250f);
        int alpha = (int)(fade * 0xFF);
        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fade * 0xCC)));

        topBar.render(g, width, "SPAWN SELECTION",
            com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP,
            com.yourmod.teamsystem.client.ClientTeamData.localPlayerBC,
            com.yourmod.teamsystem.client.ClientTeamData.localPlayerRank);

        BreadcrumbNav.render(g, width, TopBar.TOP_H, List.of("SPAWN"), alpha);

        int topH = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H;
        int leftW = width * LEFT_RATIO / 100;
        renderLeftPanel(g, mx, my, fade, alpha, leftW, topH);
        g.fill(leftW, topH, leftW + 1, height - StatusBar.STATUS_H,
            AnimationHelper.withAlpha(UITheme.BORDER, (int)(fade * 0xAA)));
        renderRightPanel(g, leftW, mx, my, fade, alpha, topH);

        renderStatusLine(g, fade, alpha);

        super.render(g, mx, my, pt);
    }

    private void renderStatusLine(GuiGraphics g, float fade, int alpha) {
        int y0 = height - StatusBar.STATUS_H;
        g.fill(0, y0, width, height,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fade * 0xEE)));
        g.fill(0, y0, width, y0 + 1,
            AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        String cnt = entries.size() + " spawn points available";
        g.drawString(font, cnt, 10,
            y0 + (StatusBar.STATUS_H - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
    }

    private void renderLeftPanel(GuiGraphics g, int mx, int my, float fade, int alpha, int panelW, int topH) {
        int x = 14;
        int maxW = panelW - 28;
        int y = topH + 6 - (int) scrollOffset;

        int paneH = height - topH - StatusBar.STATUS_H - 8;
        g.enableScissor(0, topH, panelW, topH + paneH);

        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            float progress = Math.min(1f, (tickCount - i * 3f) / 12f);
            progress = Math.max(0f, AnimationHelper.easeOutCubic(progress));
            int slideX = x - (int)((1f - progress) * (panelW * 0.3f));

            boolean hovered = mx >= slideX && mx < slideX + maxW && my >= y && my < y + CARD_H;
            boolean selected = e.id.equals(selectedId);

            if (y > -CARD_H && y < topH + paneH + CARD_H)
                renderSpawnCard(g, slideX, y, maxW, e, hovered, selected, fade, alpha, i);
            y += CARD_H + CARD_GAP;
        }
        g.disableScissor();
    }

    private void renderSpawnCard(GuiGraphics g, int x, int y, int w, SpawnEntry e, boolean hovered, boolean selected, float fade, int alpha, int idx) {
        int bg = selected ? AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(fade * 0xFF))
                : hovered ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD))
                : 0x00000000;
        int border = selected ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * 0xFF))
                : hovered ? AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fade * 0x88))
                : 0x00000000;

        int typeColor = switch (e.type) {
            case "BASE" -> UITheme.TEAM_NATO;
            case "FOB" -> UITheme.ACCENT;
            case "SQUADMATE" -> UITheme.STATUS_OK;
            case "BEACON" -> 0xFF8B5CF6;
            default -> UITheme.TEXT_SECONDARY;
        };
        int leftBar = selected ? UITheme.ACCENT : typeColor;

        if ((bg & 0xFF000000) != 0) g.fill(x, y, x + w, y + CARD_H, bg);
        if ((border & 0xFF000000) != 0) {
            g.fill(x, y, x + w, y + 1, border);
            g.fill(x, y + CARD_H - 1, x + w, y + CARD_H, border);
            g.fill(x + w - 1, y, x + w, y + CARD_H, border);
        }
        g.fill(x, y, x + 3, y + CARD_H,
            AnimationHelper.withAlpha(leftBar, (int)(fade * (selected ? 0xFF : 0x88))));

        com.yourmod.teamsystem.client.gui.component.StatusDot.draw(g, x + 12, y + CARD_H / 2, e.safe ? com.yourmod.teamsystem.client.gui.component.StatusDot.Status.OK : com.yourmod.teamsystem.client.gui.component.StatusDot.Status.DANGER);

        int textX = x + 26;
        g.drawString(font, e.displayName, textX, y + 8,
            AnimationHelper.withAlpha(selected ? UITheme.ACCENT : UITheme.TEXT_PRIMARY, alpha));
        g.drawString(font, e.type, textX, y + 20,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));

        String dist = e.distance;
        int dw = font.width(dist);
        g.drawString(font, dist, x + w - dw - 10, y + 8,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, alpha));
        if (e.cooldown > 0) {
            int secs = e.cooldown / 20;
            String cdText = secs + "s";
            g.drawString(font, cdText, x + w - font.width(cdText) - 10, y + 21,
                AnimationHelper.withAlpha(UITheme.STATUS_WARN, alpha));
        } else if (!e.safe) {
            String contested = "CONTESTED";
            g.drawString(font, contested, x + w - font.width(contested) - 10, y + 21,
                AnimationHelper.withAlpha(UITheme.STATUS_DANGER, alpha));
        }
    }

    private void renderRightPanel(GuiGraphics g, int leftW, int mx, int my, float fade, int alpha, int topH) {
        int rx = leftW + 14;
        int rw = width - rx - 8;
        int y = topH + 8;

        SpawnEntry sel = null;
        for (var e : entries) { if (e.id.equals(selectedId)) { sel = e; break; } }
        if (sel == null) return;

        int typeColor = switch (sel.type) {
            case "BASE" -> UITheme.TEAM_NATO;
            case "FOB" -> UITheme.ACCENT;
            case "SQUADMATE" -> UITheme.STATUS_OK;
            case "BEACON" -> 0xFF8B5CF6;
            default -> UITheme.TEXT_SECONDARY;
        };

        g.fill(rx, y, rx + 10, y + 10, AnimationHelper.withAlpha(typeColor, alpha));
        g.drawString(font, sel.displayName, rx + 18, y + 1,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, alpha));
        y += 22;
        com.yourmod.teamsystem.client.gui.component.AccentLine.draw(g, rx, y, Math.min(rw, 200), fade);
        y += 14;

        String teamName = teamOrdinal == 0 ? "NATO" : "RUSSIA";
        String[][] info = {
            {"TYPE", sel.type},
            {"TEAM", teamName},
            {"DISTANCE", sel.distance},
            {"STATUS", sel.safe ? "SECURE" : "CONTESTED"},
        };
        int cellW = (rw - 8) / 2;
        for (int row = 0; row < 2; row++) {
            int cy = y + row * 44;
            for (int col = 0; col < 2; col++) {
                int idx = row * 2 + col;
                int cx = rx + col * (cellW + 8);
                g.fill(cx, cy, cx + cellW, cy + 40,
                    AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD)));
                g.fill(cx, cy, cx + cellW, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx, cy + 39, cx + cellW, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx, cy, cx + 1, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.fill(cx + cellW - 1, cy, cx + cellW, cy + 40, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
                g.drawString(font, info[idx][0], cx + 8, cy + 4,
                    AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
                int valColor = idx == 3 ? (sel.safe ? UITheme.STATUS_OK : UITheme.STATUS_DANGER) : UITheme.TEXT_PRIMARY;
                g.drawString(font, info[idx][1], cx + 8, cy + 18,
                    AnimationHelper.withAlpha(valColor, alpha));
            }
        }
        y += 88 + 8;

        if (!selectedKit.isEmpty()) {
            g.fill(rx, y, rx + rw, y + 44,
                AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD)));
            g.fill(rx, y, rx + rw, y + 1, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
            g.fill(rx, y + 43, rx + rw, y + 44, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
            g.fill(rx, y, rx + 1, y + 44, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
            g.fill(rx + rw - 1, y, rx + rw, y + 44, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
            g.drawString(font, "CURRENT KIT", rx + 8, y + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, alpha));
            g.drawString(font, selectedKit, rx + 8, y + 20,
                AnimationHelper.withAlpha(UITheme.ACCENT, alpha));
            y += 52;
        }

        y = height - StatusBar.STATUS_H - 38;
        int btnH = 28;
        int btnWSmall = (rw - 8) / 3;
        int btnWBig = btnWSmall * 2;

        // Determine kit cost for deploy button
        String costLabel = null;
        boolean canAfford = true;
        if (!selectedKit.isEmpty() && selectedKit.contains(":")) {
            String[] parts = selectedKit.split(":", 2);
            KitConfig cfg = KitConfig.get();
            if (cfg != null) {
                KitConfig.ClassConfig cl = cfg.classes.get(parts[0]);
                if (cl != null) {
                    KitConfig.KitDef kit = cl.kits.get(parts[1]);
                    if (kit != null && kit.requirements != null) {
                        int sp = kit.requirements.sp_cost;
                        int bc = kit.requirements.bc_cost;
                        if (sp > 0 || bc > 0) {
                            StringBuilder sb = new StringBuilder("КУПИТЬ ЗА");
                            if (sp > 0) { sb.append(" ").append(sp).append("SP"); }
                            if (bc > 0) { if (sp > 0) sb.append(" +"); sb.append(" ").append(bc).append("BC"); }
                            costLabel = "> " + sb.toString();
                            int playerSP = com.yourmod.teamsystem.client.ClientTeamData.localPlayerSP;
                            int playerBC = com.yourmod.teamsystem.client.ClientTeamData.localPlayerBC;
                            canAfford = (sp <= playerSP) && (bc <= playerBC);
                        }
                    }
                }
            }
        }

        boolean clsHov = mx >= rx && mx < rx + btnWSmall && my >= y && my < y + btnH;
        int clsBg = clsHov ? AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fade * 0xDD)) : 0x00000000;
        if ((clsBg & 0xFF000000) != 0) g.fill(rx, y, rx + btnWSmall, y + btnH, clsBg);
        g.fill(rx, y, rx + 2, y + btnH, AnimationHelper.withAlpha(UITheme.BORDER, alpha));
        String clsLabel = "CLASSES";
        g.drawString(font, clsLabel, rx + btnWSmall / 2 - font.width(clsLabel) / 2, y + 9,
            AnimationHelper.withAlpha(clsHov ? UITheme.ACCENT : UITheme.TEXT_SECONDARY, alpha));

        int btnX = rx + btnWSmall + 8;
        boolean spHov = mx >= btnX && mx < btnX + btnWBig && my >= y && my < y + btnH;
        int btnBg;
        if (costLabel != null && !canAfford) {
            btnBg = AnimationHelper.blendColors(0xFF551111, 0xFF331111, spHov ? 1f : 0f);
        } else {
            btnBg = AnimationHelper.blendColors(UITheme.ACCENT, 0xFFFF8C0A, spHov ? 1f : 0f);
        }
        g.fill(btnX, y, btnX + btnWBig, y + btnH,
            AnimationHelper.withAlpha(btnBg, alpha));
        g.fill(btnX, y, btnX + 2, y + btnH, AnimationHelper.withAlpha(0x33000000, alpha));
        String spLabel = costLabel != null ? costLabel : "> SPAWN HERE";
        int spLabelColor = costLabel != null
            ? (canAfford ? UITheme.STATUS_OK : UITheme.STATUS_DANGER)
            : 0xFFFFFFFF;
        g.drawString(font, spLabel, btnX + btnWBig / 2 - font.width(spLabel) / 2, y + 9,
            AnimationHelper.withAlpha(spLabelColor, alpha));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn != 0) return super.mouseClicked(mx, my, btn);

        int leftW = width * LEFT_RATIO / 100;
        int topH = TopBar.TOP_H + BreadcrumbNav.BREADCRUMB_H;
        int paneH = height - topH - StatusBar.STATUS_H - 8;

        if (mx >= leftW) {
            int rx = leftW + 14;
            int rw = width - rx - 8;
            int y = height - StatusBar.STATUS_H - 38;
            int btnH = 28;
            int btnWSmall = (rw - 8) / 3;
            int btnWBig = btnWSmall * 2;

            if (mx >= rx && mx < rx + btnWSmall && my >= y && my < y + btnH) {
                openClasses(); return true;
            }
            if (mx >= rx + btnWSmall + 8 && mx < rx + btnWSmall + 8 + btnWBig && my >= y && my < y + btnH) {
                spawnSelected(); return true;
            }
            return true;
        }

        if (my < topH || my > topH + paneH) return super.mouseClicked(mx, my, btn);

        int x = 14;
        int maxW = leftW - 28;
        int ly = topH + 6 - (int) scrollOffset;

        for (var e : entries) {
            if (mx >= x && mx < x + maxW && my >= ly && my < ly + CARD_H) {
                selectedId = e.id;
                return true;
            }
            ly += CARD_H + CARD_GAP;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (mx < width * LEFT_RATIO / 100) {
            int maxScroll = Math.max(0, entries.size() * (CARD_H + CARD_GAP) + CARD_GAP - (height - TopBar.TOP_H - BreadcrumbNav.BREADCRUMB_H - StatusBar.STATUS_H - 8));
            targetScrollOffset = (int) Math.max(0, Math.min(maxScroll, targetScrollOffset - delta * 12));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) {
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void openClasses() {
        Minecraft.getInstance().setScreen(new ClassSelectionScreen());
    }

    private void spawnSelected() {
        for (var e : entries) {
            if (e.id.equals(selectedId) && e.cooldown == 0) { sendRespawn(e); return; }
        }
    }

    private void sendRespawn(SpawnEntry e) {
        String type = switch (e.type) {
            case "BASE" -> RespawnAtPointPacket.TYPE_BASE;
            case "SQUADMATE" -> RespawnAtPointPacket.TYPE_SQUADMATE;
            case "FOB" -> RespawnAtPointPacket.TYPE_FOB;
            case "BEACON" -> RespawnAtPointPacket.TYPE_BEACON;
            default -> RespawnAtPointPacket.TYPE_BASE;
        };
        UUID targetId = e.squadUuid != null ? e.squadUuid : UUID.randomUUID();
        PacketHandler.CHANNEL.sendToServer(new RespawnAtPointPacket(type, targetId, e.targetName));
    }

    private static class SpawnEntry {
        final String id;
        final String displayName;
        final String type;
        final int team;
        int cooldown;
        boolean safe;
        final String targetName;
        final String distance;
        @Nullable final UUID squadUuid;

        SpawnEntry(String id, String displayName, String type, int team, int cooldown, boolean safe, String targetName) {
            this(id, displayName, type, team, cooldown, safe, targetName, null, 0, 0);
        }

        SpawnEntry(String id, String displayName, String type, int team, int cooldown, boolean safe, String targetName, @Nullable UUID squadUuid) {
            this(id, displayName, type, team, cooldown, safe, targetName, squadUuid, 0, 0);
        }

        SpawnEntry(String id, String displayName, String type, int team, int cooldown, boolean safe, String targetName, @Nullable UUID squadUuid, double x, double z) {
            this.id = id; this.displayName = displayName; this.type = type; this.team = team;
            this.cooldown = cooldown; this.safe = safe; this.targetName = targetName; this.squadUuid = squadUuid;
            if (type.equals("BASE")) {
                this.distance = "\u2014";
            } else {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    double dx = player.getX() - x;
                    double dz = player.getZ() - z;
                    int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                    this.distance = dist + "m";
                } else {
                    this.distance = "?m";
                }
            }
        }
    }
}
