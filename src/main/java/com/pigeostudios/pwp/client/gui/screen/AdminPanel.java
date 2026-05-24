package com.pigeostudios.pwp.client.gui.screen;

import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.component.BButton;
import com.pigeostudios.pwp.client.gui.component.AnimationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;



public class AdminPanel extends Screen {

    private enum Tab { MATCH, MAPS, KITS, TEAMS, CONFIG, PUNISHMENTS, REPORTS, TICKETS }

    private static final int NAV_COLLAPSED = 50;
    private static final int NAV_EXPANDED  = 160;
    private static final int NAV_ITEM_H    = 44;
    private static final int HEADER_H      = 40;
    private static final int QUICK_BAR_H   = 36;

    private static final String[] TAB_ICONS  = {"\u26A1", "\uD83D\uDDFA\uFE0F", "\u2694\uFE0F", "\uD83D\uDC65", "\u2699\uFE0F", "\uD83D\uDD28", "\uD83D\uDCE2", "\uD83C\uDFAB"};
    private static final String[] TAB_LABELS = {"Match", "Maps", "Kits", "Teams", "Config", "Punishments", "Reports", "Tickets"};

    private Tab currentTab = Tab.MATCH;
    private float fadeAlpha = 0f;
    private long openTime;
    private float navHover = 0f;
    private int activeNavIdx = 0;

    private int ticketScroll = 0;

    // ===== Kit Editor State =====
    private com.google.gson.JsonObject kitClassesObj = null;
    private String kitSelectedClassId = null;
    private String kitSelectedKitId = null;
    private boolean kitEditActive = false;
    private String kitEditField = null;       // e.g. "display_name", "description", "requirements.rank", "weapons.primary"
    private StringBuilder kitEditBuffer = new StringBuilder();
    private int kitEditCursor = 0;
    private long kitEditBlink = 0;
    private String kitConfigRequested = "";   // non-empty when we sent a request
    private String kitLastJson = "";           // tracks last parsed JSON to avoid re-parsing

    public AdminPanel() {
        super(Component.literal("Admin Panel"));
    }

    @Override
    protected void init() {
        openTime = System.currentTimeMillis();
        int navW = NAV_COLLAPSED;
        int qy = HEADER_H;
        int btnH = 24;
        int btnY = qy + (QUICK_BAR_H - btnH) / 2;
        int x = navW + 8;

        addRenderableWidget(new BButton(x, btnY, 100, btnH,
            Component.literal("\u25B6 START"), btn -> sendAdminCommand("start"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 104, btnY, 100, btnH,
            Component.literal("\u21BB RESTART"), btn -> sendAdminCommand("restart"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 208, btnY, 90, btnH,
            Component.literal("\u25A0 STOP"), btn -> sendAdminCommand("stop"),
            BButton.Variant.GHOST));
        addRenderableWidget(new BButton(x + 302, btnY, 120, btnH,
            Component.literal("\uD83D\uDDF3 VOTE"), btn -> sendAdminCommand("forcevote"),
            BButton.Variant.GHOST));
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        float elapsed = (System.currentTimeMillis() - openTime) / 250f;
        fadeAlpha = Math.min(1f, elapsed);

        g.fill(0, 0, width, height, AnimationHelper.withAlpha(UITheme.BG_SCREEN, (int)(fadeAlpha * 0xCC)));

        boolean hoveringNav = mx < NAV_EXPANDED + 10;
        navHover = AnimationHelper.lerp(navHover, hoveringNav ? 1f : 0f, 0.1f);
        int navW = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);

        drawNavBar(g, mx, my, navW);
        drawHeader(g, navW);
        drawQuickBarBg(g, navW);

        int contentX = navW + 10;
        int contentY = HEADER_H + QUICK_BAR_H + 10;
        int contentW = width - navW - 20;
        int contentH = height - contentY - 10;

        switch (currentTab) {
            case MATCH -> renderMatchTab(g, contentX, contentY, contentW, contentH);
            case MAPS  -> renderMapsTab(g, contentX, contentY, contentW, contentH);
            case KITS  -> renderKitsTab(g, contentX, contentY, contentW, contentH, mx, my);
            case TEAMS -> renderTeamsTab(g, contentX, contentY, contentW, contentH);
            case CONFIG -> renderConfigTab(g, contentX, contentY, contentW, contentH);
            case PUNISHMENTS -> renderPunishmentsTab(g, contentX, contentY, contentW, contentH);
            case REPORTS -> renderReportsTab(g, contentX, contentY, contentW, contentH);
            case TICKETS -> renderTicketsTab(g, contentX, contentY, contentW, contentH, mx, my);
        }

        super.render(g, mx, my, pt);
    }

    private void drawNavBar(GuiGraphics g, int mx, int my, int navW) {
        g.fill(0, 0, navW, height, AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xDD)));
        g.fill(navW - 1, 0, navW, height, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 180)));

        int startY = HEADER_H + 10;

        for (int i = 0; i < TAB_ICONS.length; i++) {
            int iy = startY + i * (NAV_ITEM_H + 4);
            boolean active = i == currentTab.ordinal();
            boolean hov = mx >= 0 && mx <= navW && my >= iy && my <= iy + NAV_ITEM_H;

            if (active) {
                g.fill(0, iy, 3, iy + NAV_ITEM_H, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
            }

            int bg = active
                ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x66))
                : (hov ? AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 0x33)) : 0);
            if (bg != 0) {
                g.fill(4, iy, navW - 2, iy + NAV_ITEM_H, bg);
            }

            g.drawCenteredString(font, TAB_ICONS[i], navW / 2, iy + (NAV_ITEM_H - font.lineHeight) / 2,
                active ? AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255))
                       : AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

            if (navHover > 0.1f) {
                int labelAlpha = (int)(fadeAlpha * 255 * navHover);
                g.drawString(font, TAB_LABELS[i], 52, iy + (NAV_ITEM_H - font.lineHeight) / 2,
                    AnimationHelper.withAlpha(active ? UITheme.ACCENT : UITheme.TEXT_PRIMARY, labelAlpha));
            }
        }
    }

    private void drawHeader(GuiGraphics g, int navW) {
        g.fill(navW, 0, width, HEADER_H, AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0xDD)));
        g.fill(navW, HEADER_H - 1, width, HEADER_H, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));

        String title = "ADMIN PANEL";
        int tw = font.width(title);
        g.drawString(font, title, navW + 12, (HEADER_H - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
    }

    private void drawQuickBarBg(GuiGraphics g, int navW) {
        int qy = HEADER_H;
        g.fill(navW, qy, width, qy + QUICK_BAR_H, AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x44)));
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        int navW = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);
        int startY = HEADER_H + 10;
        for (int i = 0; i < TAB_ICONS.length; i++) {
            int iy = startY + i * (NAV_ITEM_H + 4);
            if (mx >= 0 && mx <= navW && my >= iy && my <= iy + NAV_ITEM_H) {
                currentTab = Tab.values()[i];
                activeNavIdx = i;
                if (currentTab == Tab.KITS) {
                    kitEditActive = false;
                    kitEditField = null;
                    if (ClientTeamData.kitConfigEditJson.isEmpty() && kitConfigRequested.isEmpty()) {
                        requestKitConfig();
                    } else if (!ClientTeamData.kitConfigEditJson.isEmpty()) {
                        kitParseJson();
                    }
                }
                return true;
            }
        }
        if (currentTab == Tab.KITS) {
            if (kitHandleClick(mx, my, btn)) return true;
        }
        if (currentTab == Tab.TICKETS) {
            var tickets = ClientTeamData.ticketList;
            int tx = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover) + 4;
            int ty = HEADER_H + QUICK_BAR_H + 10;
            int listX = tx + 4;
            int listY = ty + 36;
            int rowH = 20;
            int vis = Math.max(1, (height - listY - 24) / rowH);
            for (int i = ticketScroll; i < Math.min(ticketScroll + vis, tickets.size()); i++) {
                int ry = listY + (i - ticketScroll) * rowH;
                if (mx >= listX - 4 && mx <= listX + width - tx - 20 && my >= ry && my <= ry + rowH) {
                    Minecraft.getInstance().setScreen(new TicketScreen(tickets.get(i).id));
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (currentTab == Tab.KITS && kitEditActive) {
            if (key == 257 || key == 335) { // Enter
                kitConfirmEdit();
                return true;
            }
            if (key == 256) { // Escape
                kitCancelEdit();
                return true;
            }
            if (key == 259) { // Backspace
                if (kitEditCursor > 0) {
                    kitEditBuffer.deleteCharAt(kitEditCursor - 1);
                    kitEditCursor--;
                    kitEditBlink = System.currentTimeMillis();
                }
                return true;
            }
            if (key == 261) { // Delete
                if (kitEditCursor < kitEditBuffer.length()) {
                    kitEditBuffer.deleteCharAt(kitEditCursor);
                    kitEditBlink = System.currentTimeMillis();
                }
                return true;
            }
            if (key == 263) { // Left
                if (kitEditCursor > 0) { kitEditCursor--; kitEditBlink = System.currentTimeMillis(); }
                return true;
            }
            if (key == 262) { // Right
                if (kitEditCursor < kitEditBuffer.length()) { kitEditCursor++; kitEditBlink = System.currentTimeMillis(); }
                return true;
            }
            if (key == 268) { // Home
                kitEditCursor = 0; kitEditBlink = System.currentTimeMillis();
                return true;
            }
            if (key == 269) { // End
                kitEditCursor = kitEditBuffer.length(); kitEditBlink = System.currentTimeMillis();
                return true;
            }
            // Don't propagate other keys during edit
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean charTyped(char ch, int mods) {
        if (currentTab == Tab.KITS && kitEditActive) {
            if (ch >= ' ' && ch != 127) {
                kitEditBuffer.insert(kitEditCursor, ch);
                kitEditCursor++;
                kitEditBlink = System.currentTimeMillis();
            }
            return true;
        }
        return super.charTyped(ch, mods);
    }

    // ===== Kit Editor =====

    private void requestKitConfig() {
        com.pigeostudios.pwp.network.PacketHandler.CHANNEL.sendToServer(
            new com.pigeostudios.pwp.network.KitAdminRequestPacket());
        kitConfigRequested = "pending";
    }

    private void kitParseJson() {
        String json = ClientTeamData.kitConfigEditJson;
        if (json == null) json = "";
        if (json.equals(kitLastJson) && kitClassesObj != null) return; // skip if unchanged
        kitLastJson = json;
        kitConfigRequested = "";
        if (json.isEmpty()) {
            kitClassesObj = null;
            return;
        }
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            kitClassesObj = (el != null && el.isJsonObject())
                ? el.getAsJsonObject().getAsJsonObject("classes") : null;
        } catch (Exception e) {
            kitClassesObj = null;
        }
    }

    private boolean kitHandleClick(double mx, double my, int btn) {
        if (btn != 0) return false;
        int navW = (int)(NAV_COLLAPSED + (NAV_EXPANDED - NAV_COLLAPSED) * navHover);
        int contentX = navW + 10;
        int contentY = HEADER_H + QUICK_BAR_H + 10;
        int contentW = width - navW - 20;
        int contentH = height - contentY - 10;
        int leftW = 180;
        int leftX = contentX;
        int rightX = contentX + leftW + 8;

        // 1. Left panel class list
        int iy = contentY + 30;
        if (kitClassesObj != null) {
            for (var entry : kitClassesObj.entrySet()) {
                if (mx >= leftX && mx <= leftX + leftW && my >= iy && my <= iy + 18) {
                    kitSelectedClassId = entry.getKey();
                    kitSelectedKitId = null;
                    kitEditActive = false;
                    kitEditField = null;
                    return true;
                }
                iy += 20;
            }
        }

        // 2. Bottom Save/Request buttons
        int btnY = contentY + contentH - 28;
        int btnW = 100;
        int btnH = 22;
        if (mx >= rightX && mx <= rightX + btnW && my >= btnY && my <= btnY + btnH) {
            kitSaveToServer();
            return true;
        }
        if (mx >= rightX + btnW + 6 && mx <= rightX + btnW + 6 + btnW && my >= btnY && my <= btnY + btnH) {
            requestKitConfig();
            return true;
        }

        // 3. Kit list click
        com.google.gson.JsonObject classObj = (kitSelectedClassId != null && kitClassesObj != null)
            ? kitClassesObj.getAsJsonObject(kitSelectedClassId) : null;
        com.google.gson.JsonObject kitsObj = (classObj != null && classObj.has("kits"))
            ? classObj.getAsJsonObject("kits") : null;
        if (kitsObj != null) {
            int ry = contentY + 52;
            for (var entry : kitsObj.entrySet()) {
                if (mx >= rightX && mx <= rightX + (contentW - leftW - 8) && my >= ry && my <= ry + 18) {
                    kitSelectedKitId = entry.getKey();
                    kitEditActive = false;
                    kitEditField = null;
                    return true;
                }
                ry += 20;
            }
        }

        // 4. Editor field clicks (when a kit is selected)
        if (kitSelectedKitId != null && kitsObj != null && kitsObj.has(kitSelectedKitId)) {
            if (kitEditActive) {
                // If currently editing, clicking anywhere confirms and restarts for new field
                kitConfirmEdit();
            }
            int numKits = kitsObj.size();
            int ry2 = contentY + 52 + numKits * 20;
            int editorY = Math.max(ry2 + 4, contentY + contentH - 260);

            if (mx >= rightX + 4 && mx <= rightX + (contentW - leftW - 4) && my >= editorY + 2 && my <= editorY + 150) {
            String[] fieldKeys = {"display_name", "description", "requirements.rank", "requirements.sp_cost", "requirements.bc_cost",
                null, "weapons.primary", "weapons.secondary", "weapons.special", "weapons.grenade"};
            int[] yOffsets = {2, 18, 34, 50, 66, 86, 102, 118, 134, 150};

                for (int i = 0; i < fieldKeys.length; i++) {
                    int yStart = editorY + yOffsets[i];
                    int yEnd = (i + 1 < yOffsets.length) ? editorY + yOffsets[i + 1] : yStart + 16;
                    if (my >= yStart && my < yEnd && fieldKeys[i] != null) {
                        String fk = fieldKeys[i];
                        String currentVal = kitGetFieldValue(kitsObj.getAsJsonObject(kitSelectedKitId), fk);
                        kitStartEdit(kitSelectedClassId, kitSelectedKitId, fk, currentVal);
                        return true;
                    }
                }
                return true; // click in editor but not on a field
            }
        }
        return false;
    }

    private String kitGetFieldValue(com.google.gson.JsonObject kitObj, String fieldKey) {
        switch (fieldKey) {
            case "display_name":
                return kitObj.has("display_name") ? kitObj.get("display_name").getAsString() : "";
            case "description":
                return kitObj.has("description") ? kitObj.get("description").getAsString() : "";
            case "requirements.rank": {
                if (kitObj.has("requirements") && kitObj.getAsJsonObject("requirements").has("rank"))
                    return String.valueOf(kitObj.getAsJsonObject("requirements").get("rank").getAsInt());
                return "1";
            }
            case "requirements.sp_cost": {
                if (kitObj.has("requirements") && kitObj.getAsJsonObject("requirements").has("sp_cost"))
                    return String.valueOf(kitObj.getAsJsonObject("requirements").get("sp_cost").getAsInt());
                return "0";
            }
            case "requirements.bc_cost": {
                if (kitObj.has("requirements") && kitObj.getAsJsonObject("requirements").has("bc_cost"))
                    return String.valueOf(kitObj.getAsJsonObject("requirements").get("bc_cost").getAsInt());
                return "0";
            }
            default: {
                if (fieldKey.startsWith("weapons.")) {
                    String cat = fieldKey.substring("weapons.".length());
                    if (kitObj.has("weapons") && kitObj.getAsJsonObject("weapons").has(cat)) {
                        java.util.List<String> list = new java.util.ArrayList<>();
                        for (var el : kitObj.getAsJsonObject("weapons").getAsJsonArray(cat))
                            list.add(el.getAsString());
                        return String.join(", ", list);
                    }
                    return "";
                }
                return "";
            }
        }
    }

    private void kitStartEdit(String classId, String kitId, String field, String currentValue) {
        kitEditActive = true;
        kitEditField = classId + "." + kitId + "." + field;
        kitEditBuffer = new StringBuilder(currentValue != null ? currentValue : "");
        kitEditCursor = kitEditBuffer.length();
        kitEditBlink = System.currentTimeMillis();
    }

    private void kitConfirmEdit() {
        if (!kitEditActive || kitEditField == null) return;
        // Parse the path: classId.kitId.rest.of.path
        String path = kitEditField;
        String newValue = kitEditBuffer.toString();

        // Navigate to the correct JsonElement and set the value
        try {
            String[] parts = path.split("\\.", 3);
            if (parts.length < 3) { kitCancelEdit(); return; }
            String classId = parts[0];
            String kitId = parts[1];
            String fieldPath = parts[2];

            if (kitClassesObj == null || !kitClassesObj.has(classId)) { kitCancelEdit(); return; }
            com.google.gson.JsonObject classObj = kitClassesObj.getAsJsonObject(classId);
            if (!classObj.has("kits")) { kitCancelEdit(); return; }
            com.google.gson.JsonObject kitsObj = classObj.getAsJsonObject("kits");
            if (!kitsObj.has(kitId)) { kitCancelEdit(); return; }
            com.google.gson.JsonObject kitObj = kitsObj.getAsJsonObject(kitId);

            if (fieldPath.equals("display_name")) {
                kitObj.addProperty("display_name", newValue);
            } else if (fieldPath.equals("description")) {
                kitObj.addProperty("description", newValue);
            } else if (fieldPath.equals("requirements.rank")) {
                try {
                    int rank = Integer.parseInt(newValue.trim());
                    com.google.gson.JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new com.google.gson.JsonObject();
                    reqObj.addProperty("rank", rank);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.equals("requirements.sp_cost")) {
                try {
                    int sp = Integer.parseInt(newValue.trim());
                    com.google.gson.JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new com.google.gson.JsonObject();
                    reqObj.addProperty("sp_cost", sp);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.equals("requirements.bc_cost")) {
                try {
                    int bc = Integer.parseInt(newValue.trim());
                    com.google.gson.JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new com.google.gson.JsonObject();
                    reqObj.addProperty("bc_cost", bc);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.startsWith("weapons.")) {
                String cat = fieldPath.substring("weapons.".length());
                com.google.gson.JsonObject weaponsObj = kitObj.has("weapons")
                    ? kitObj.getAsJsonObject("weapons")
                    : new com.google.gson.JsonObject();
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (String s : newValue.split(",")) {
                    String trimmed = s.trim();
                    if (!trimmed.isEmpty()) arr.add(trimmed);
                }
                weaponsObj.add(cat, arr);
                kitObj.add("weapons", weaponsObj);
            }
        } catch (Exception ignored) {}

        kitEditActive = false;
        kitEditField = null;
        kitSelectedKitId = null; // Deselect to show updated state
    }

    private void kitCancelEdit() {
        kitEditActive = false;
        kitEditField = null;
    }

    private void kitSaveToServer() {
        if (kitClassesObj == null) return;
        try {
            com.google.gson.JsonObject root = new com.google.gson.JsonObject();
            root.add("classes", kitClassesObj);
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(root);
            com.pigeostudios.pwp.network.PacketHandler.CHANNEL.sendToServer(
                new com.pigeostudios.pwp.network.KitAdminSavePacket(json));
        } catch (Exception ignored) {}
    }

    private void sendAdminCommand(String action) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;
        switch (action) {
            case "start" -> mc.player.connection.sendCommand("game start");
            case "stop" -> mc.player.connection.sendCommand("game end");
            case "restart" -> mc.player.connection.sendCommand("game countdown 30");
            case "forcevote" -> mc.player.connection.sendCommand("game start");
        }
    }

    private void renderMatchTab(GuiGraphics g, int x, int y, int w, int h) {
        int cardH = 100;
        g.fill(x, y, x + w, y + cardH, AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0xDD)));
        g.fill(x, y, x + w, y + 3, AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 200)));

        int ly = y + 12;
        drawLValue(g, x + 12, ly, "Status", phaseName(ClientTeamData.getGamePhase()), UITheme.STATUS_OK);
        ly += 18;
        drawLValue(g, x + 12, ly, "Map", ClientTeamData.getCurrentMapName(), UITheme.TEXT_PRIMARY);
        ly += 18;
        drawLValue(g, x + 12, ly, "Timer", formatTime(ClientTeamData.matchTimeSeconds), UITheme.TEXT_PRIMARY);

        int tx = x + w / 2;
        g.drawString(font, "NATO: " + ClientTeamData.getNatoTickets(), tx, y + 12,
            AnimationHelper.withAlpha(UITheme.TEAM_NATO, (int)(fadeAlpha * 255)));
        g.drawString(font, "RUSSIA: " + ClientTeamData.getRussiaTickets(), tx, y + 30,
            AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, (int)(fadeAlpha * 255)));
        g.drawString(font, "Players: " + (ClientTeamData.playerDataMap != null ? ClientTeamData.playerDataMap.size() : 0),
            tx, y + 48, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
    }

    private void renderMapsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "MAP MANAGEMENT", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        drawLValue(g, x, cy, "Current Map", ClientTeamData.getCurrentMapName(), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x, cy, "Phase", phaseName(ClientTeamData.getGamePhase()), UITheme.TEXT_SECONDARY);

        cy += 30;
        g.drawString(font, "Vote Settings", x, cy,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
        cy += 18;
        drawLValue(g, x, cy, "Vote Duration", "30s", UITheme.TEXT_SECONDARY);
        cy += 18;
        drawLValue(g, x, cy, "Maps Per Vote", "4", UITheme.TEXT_SECONDARY);

        cy += 30;
        g.drawString(font, "Use /pwp admin addmap <name> <path>", x, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
    }

    private void renderKitsTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.drawString(font, "KIT EDITOR", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        final int leftW = 180;
        final int leftX = x;
        final int leftY = y + 14;
        final int rightX = x + leftW + 8;
        final int rightW = w - leftW - 8;

        // ---- LEFT PANEL: Class list ----
        g.fill(leftX, leftY, leftX + leftW, y + h - 10,
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0x88)));

        g.drawString(font, "CLASSES", leftX + 4, leftY + 2,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));

        int iy = leftY + 16;
        if (kitClassesObj == null && !ClientTeamData.kitConfigEditJson.isEmpty()) {
            kitParseJson();
        }

        if (kitClassesObj != null) {
            for (var entry : kitClassesObj.entrySet()) {
                String cId = entry.getKey();
                com.google.gson.JsonObject cObj = entry.getValue().getAsJsonObject();
                String displayName = cObj.has("display_name") ? cObj.get("display_name").getAsString() : cId;

                boolean selected = cId.equals(kitSelectedClassId);
                boolean hov = mx >= leftX && mx <= leftX + leftW && my >= iy && my <= iy + 18;

                if (selected || hov) {
                    int bg = selected ? AnimationHelper.withAlpha(UITheme.ACCENT, 0x44)
                                     : AnimationHelper.withAlpha(UITheme.BORDER, 0x33);
                    g.fill(leftX, iy, leftX + leftW, iy + 18, bg);
                }

                g.drawString(font, displayName, leftX + 6, iy + 4,
                    AnimationHelper.withAlpha(selected ? UITheme.ACCENT : UITheme.TEXT_PRIMARY,
                        (int)(fadeAlpha * (selected ? 255 : 220))));
                iy += 20;
            }
        } else {
            g.drawString(font, "No config loaded", leftX + 6, iy + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
            iy += 16;
            g.drawString(font, "Click Request below", leftX + 6, iy + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
        }

        // ---- RIGHT PANEL: Kit list + editor below ----
        if (kitSelectedClassId != null && kitClassesObj != null) {
            com.google.gson.JsonObject classObj = kitClassesObj.getAsJsonObject(kitSelectedClassId);
            if (classObj != null) {
                String cDn = classObj.has("display_name") ? classObj.get("display_name").getAsString() : kitSelectedClassId;
                g.drawString(font, "Class: " + cDn, rightX, y + 14,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

                final int kitListY = y + 36;
                int ry = kitListY;
                com.google.gson.JsonObject kitsObj = classObj.has("kits") ? classObj.getAsJsonObject("kits") : null;
                if (kitsObj != null) {
                    g.drawString(font, "Kits:", rightX, ry,
                        AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
                    ry += 16;

                    for (var entry : kitsObj.entrySet()) {
                        String kId = entry.getKey();
                        String kDn = entry.getValue().getAsJsonObject().has("display_name")
                            ? entry.getValue().getAsJsonObject().get("display_name").getAsString() : kId;
                        boolean selected = kId.equals(kitSelectedKitId);
                        if (selected) {
                            g.fill(rightX, ry, rightX + rightW, ry + 18,
                                AnimationHelper.withAlpha(UITheme.ACCENT, 0x33));
                        }
                        g.drawString(font, (selected ? "\u25B6 " : "  ") + kDn, rightX + 4, ry + 4,
                            AnimationHelper.withAlpha(selected ? UITheme.ACCENT : UITheme.TEXT_PRIMARY,
                                (int)(fadeAlpha * (selected ? 255 : 220))));
                        ry += 20;
                    }
                } else {
                    g.drawString(font, "No kits defined", rightX, ry,
                        AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
                }

                // ---- Kit editor section (below kit list, above buttons) ----
                int editorY = Math.max(ry + 4, y + h - 260);
                int editorH = y + h - 28 - 4 - editorY;
                if (editorH > 20 && kitSelectedKitId != null && kitsObj != null && kitsObj.has(kitSelectedKitId)) {
                    com.google.gson.JsonObject kitObj = kitsObj.getAsJsonObject(kitSelectedKitId);
                    int editorW = rightW;
                    g.fill(rightX, editorY, rightX + editorW, editorY + editorH,
                        AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0x66)));

                    if (kitEditActive && kitEditField != null &&
                        kitEditField.startsWith(kitSelectedClassId + "." + kitSelectedKitId + ".")) {
                        renderKitEditor(g, kitObj, rightX + 4, editorY + 2, editorW - 8, mx, my);
                    } else {
                        renderKitFields(g, kitObj, rightX + 4, editorY + 2, editorW - 8, mx, my);
                    }
                }
            }
        }

        // ---- Bottom buttons ----
        int btnY = y + h - 28;
        int btnW = 100;
        int btnH = 22;

        boolean saveHov = mx >= rightX && mx <= rightX + btnW && my >= btnY && my <= btnY + btnH;
        g.fill(rightX, btnY, rightX + btnW, btnY + btnH,
            AnimationHelper.withAlpha(saveHov ? UITheme.ACCENT : UITheme.BG_SURFACE, (int)(fadeAlpha * 200)));
        g.drawCenteredString(font, "\u2714 SAVE", rightX + btnW / 2, btnY + (btnH - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

        int reqX = rightX + btnW + 6;
        boolean reqHov = mx >= reqX && mx <= reqX + btnW && my >= btnY && my <= btnY + btnH;
        g.fill(reqX, btnY, reqX + btnW, btnY + btnH,
            AnimationHelper.withAlpha(reqHov ? UITheme.ACCENT : UITheme.BG_SURFACE, (int)(fadeAlpha * 200)));
        g.drawCenteredString(font, "\u21BB REQUEST", reqX + btnW / 2, btnY + (btnH - font.lineHeight) / 2,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));
    }

    private void renderKitFields(GuiGraphics g, com.google.gson.JsonObject kitObj, int x, int y, int w, int mx, int my) {
        int fy = y;
        int colW = w / 2;

        String dn = kitObj.has("display_name") ? kitObj.get("display_name").getAsString() : "";
        String desc = kitObj.has("description") ? kitObj.get("description").getAsString() : "";
        int rankVal = 1;
        com.google.gson.JsonObject reqObj = kitObj.has("requirements") ? kitObj.getAsJsonObject("requirements") : null;
        if (reqObj != null && reqObj.has("rank")) rankVal = reqObj.get("rank").getAsInt();

        drawEditableField(g, "Name:", dn, x, fy, colW, mx, my);
        fy += 16;
        drawEditableField(g, "Desc:", desc, x, fy, colW, mx, my);
        fy += 16;
        drawEditableField(g, "Rank:", String.valueOf(rankVal), x, fy, colW, mx, my);
        fy += 16;

        String[] weaponCats = {"primary", "secondary", "special", "grenade"};
        com.google.gson.JsonObject weaponsObj = kitObj.has("weapons") ? kitObj.getAsJsonObject("weapons") : null;
        if (weaponsObj != null) {
            fy += 4;
            g.drawString(font, "Weapons:", x, fy,
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
            fy += 16;
            for (String cat : weaponCats) {
                String val = "";
                if (weaponsObj.has(cat)) {
                    java.util.List<String> list = new java.util.ArrayList<>();
                    for (var el : weaponsObj.getAsJsonArray(cat)) list.add(el.getAsString());
                    val = String.join(", ", list);
                }
                StringBuilder truncated = new StringBuilder();
                if (font.width(val) > colW) {
                    for (int i = 0; i < val.length(); i++) {
                        if (font.width(val.substring(0, i + 1)) > colW - 8) break;
                        truncated.append(val.charAt(i));
                    }
                    truncated.append("...");
                } else {
                    truncated.append(val);
                }
                drawEditableField(g, cat + ":", truncated.toString(), x, fy, colW, mx, my);
                fy += 16;
            }
        }

        // Attachment limits section
        com.google.gson.JsonObject attLimits = kitObj.has("attachment_limits") ?
            kitObj.getAsJsonObject("attachment_limits") : null;
        if (attLimits != null && attLimits.size() > 0) {
            fy += 4;
            g.drawString(font, "Attachment Limits:", x, fy,
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 180)));
            fy += 16;
            int maxWidth = w - 10;
            for (var weaponEntry : attLimits.entrySet()) {
                String weaponId = weaponEntry.getKey();
                g.drawString(font, "  " + weaponId + ":", x, fy,
                    AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 180)));
                fy += 14;
                com.google.gson.JsonObject limitsObj = weaponEntry.getValue().getAsJsonObject();
                for (var catEntry : limitsObj.entrySet()) {
                    String cat = catEntry.getKey();
                    java.util.List<String> items = new java.util.ArrayList<>();
                    for (var el : catEntry.getValue().getAsJsonArray()) items.add(el.getAsString());
                    String line = "    " + cat + ": " + String.join(", ", items);
                    int lineColor = AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 160));
                    if (font.width(line) > maxWidth) {
                        g.drawString(font, line.substring(0, Math.min(line.length(), 40)), x, fy, lineColor);
                    } else {
                        g.drawString(font, line, x, fy, lineColor);
                    }
                    fy += 14;
                }
            }
        }
    }

    private void renderKitEditor(GuiGraphics g, com.google.gson.JsonObject kitObj, int x, int y, int w, int mx, int my) {
        // Extract the field suffix from kitEditField
        if (kitEditField == null) return;
        String[] parts = kitEditField.split("\\.", 3);
        if (parts.length < 3) return;
        String fieldSuffix = parts[2];

        g.fill(x, y, x + w, y + 120,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x66)));

        g.drawString(font, "Editing: " + fieldSuffix, x + 4, y + 4,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        g.drawString(font, "Value:", x + 4, y + 20,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

        // Draw the edit buffer with cursor
        String displayText = kitEditBuffer.toString();
        String beforeCursor = displayText.substring(0, Math.min(kitEditCursor, displayText.length()));
        String afterCursor = displayText.substring(Math.min(kitEditCursor, displayText.length()));
        int textX = x + 4;
        int textY = y + 36;

        // Background for text input area
        g.fill(x + 2, textY - 2, x + w - 4, textY + font.lineHeight + 2,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xAA)));

        g.drawString(font, beforeCursor, textX, textY,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));
        int cursorX = textX + font.width(beforeCursor);

        // Blinking cursor
        if ((System.currentTimeMillis() - kitEditBlink) % 1000 < 500) {
            g.fill(cursorX, textY, cursorX + 1, textY + font.lineHeight,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        }

        g.drawString(font, afterCursor, cursorX + 2, textY,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 180)));

        g.drawString(font, "Enter = confirm, Esc = cancel", x + 4, textY + font.lineHeight + 6,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
    }

    private void drawEditableField(GuiGraphics g, String label, String value, int x, int y, int maxW, int mx, int my) {
        String text = label + " " + value;
        int color;
        if (mx >= x && mx <= x + Math.min(font.width(text) + 8, maxW) && my >= y && my <= y + 14) {
            color = AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255));
            g.fill(x, y, x + Math.min(font.width(text) + 8, maxW), y + 14,
                AnimationHelper.withAlpha(UITheme.ACCENT, 0x22));
        } else {
            color = AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 220));
        }
        g.drawString(font, label, x, y,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        int lw = font.width(label);
        String displayVal = value;
        if (font.width(displayVal) > maxW - lw - 8) {
            for (int i = 0; i < displayVal.length(); i++) {
                if (font.width(displayVal.substring(0, i + 1)) > maxW - lw - 12) {
                    displayVal = displayVal.substring(0, i) + "...";
                    break;
                }
            }
        }
        g.drawString(font, displayVal, x + lw + 2, y, color);
    }

    private void renderTeamsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "TEAM SETTINGS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        int natoCount = 0, rusCount = 0;
        if (ClientTeamData.playerTeamMap != null) {
            for (var entry : ClientTeamData.playerTeamMap.entrySet()) {
                if (entry.getValue() == 0) natoCount++;
                else if (entry.getValue() == 1) rusCount++;
            }
        }

        g.drawString(font, "NATO: " + natoCount + " players | Tickets: " + ClientTeamData.getNatoTickets(),
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEAM_NATO, (int)(fadeAlpha * 255)));
        cy += 20;
        g.drawString(font, "RUSSIA: " + rusCount + " players | Tickets: " + ClientTeamData.getRussiaTickets(),
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEAM_RUSSIA, (int)(fadeAlpha * 255)));
        cy += 20;
        g.drawString(font, "Total: " + (natoCount + rusCount) + " players",
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));

        if (ClientTeamData.playerDataMap != null) {
            cy += 30;
            g.drawString(font, "Player List:", x, cy,
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
            cy += 16;
            for (var entry : ClientTeamData.playerDataMap.entrySet()) {
                if (cy > y + h - 20) break;
                var ple = entry.getValue();
                String teamTag = "[" + (ple.teamOrdinal() == 0 ? "NATO" : ple.teamOrdinal() == 1 ? "RUS" : "SPECT") + "]";
                String line = teamTag + " " + (ple.callsign() != null ? ple.callsign() : "Unknown");
                g.drawString(font, line, x + 8, cy,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
                cy += 14;
            }
        }
    }

    private void renderConfigTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "SERVER CONFIG", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        cy += 10;
        drawLValue(g, x + 8, cy, "Language", ClientTeamData.language, UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "Max Tickets", String.valueOf(ClientTeamData.maxTickets), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Scale", String.format("%.1f", ClientTeamData.guiScale), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Opacity", String.format("%.1f", ClientTeamData.guiOpacity), UITheme.TEXT_PRIMARY);
        cy += 18;
        drawLValue(g, x + 8, cy, "GUI Volume", String.format("%.1f", ClientTeamData.guiVolume), UITheme.TEXT_PRIMARY);

        cy += 30;
        g.drawString(font, "Config editing: /pwp admin config <key> <value>",
            x + 8, cy, AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
    }

    private void renderPunishmentsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "PUNISHMENTS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));
        cy += 16;
        g.drawString(font, "Commands:", x + 8, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        cy += 18;
        String[] cmds = {
            "§6/warnchat <player> <reason>",
            "§6/warngame <player> <reason>",
            "§6/warnvoice <player> <reason>",
            "§6/warngeneral <player> <reason>",
            "§6/kick <player> <reason>",
            "§6/mute <player> <hours> <reason>",
            "§6/voicemute <player> <hours> <reason>",
            "§6/tempban <player> <hours> <reason>",
            "§6/ban <player> <reason>",
            "§6/unban <uuid>",
            "§6/unmute <player>",
            "§6/punishments <player>",
            "§6/staff add/remove/list"
        };
        for (String cmd : cmds) {
            if (cy > y + h - 10) break;
            g.drawString(font, cmd, x + 12, cy,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cy += 16;
        }
    }

    private void renderTicketsTab(GuiGraphics g, int x, int y, int w, int h, int mx, int my) {
        g.drawString(font, "TICKETS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));

        var tickets = ClientTeamData.ticketList;
        int headerY = y + 18;
        g.fill(x, headerY, x + w, headerY + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));

        int[] cw = {32, 100, 80, 70, 80};
        String[] headers = {"ID", "Reporter", "Target", "Type", "Status"};
        int hx = x + 6;
        for (int i = 0; i < cw.length; i++) {
            g.drawString(font, "\u00A78" + headers[i], hx, headerY + 6,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 180)));
            hx += cw[i];
        }

        // scrollable list area
        int listX = x + 4;
        int listY = headerY + 18;
        int listW = w - 8;
        int listH = h - (listY - y) - 14;

        g.fill(listX - 4, listY, listX + listW + 4, listY + listH,
            AnimationHelper.withAlpha(UITheme.BG_BLACK, (int)(fadeAlpha * 0x33)));

        int rowH = 20;
        int vis = Math.max(1, listH / rowH);
        int maxScroll = Math.max(0, tickets.size() - vis);
        if (ticketScroll > maxScroll) ticketScroll = maxScroll;

        for (int i = ticketScroll; i < Math.min(ticketScroll + vis, tickets.size()); i++) {
            var t = tickets.get(i);
            int ry = listY + (i - ticketScroll) * rowH;
            boolean hov = mx >= listX && mx <= listX + listW && my >= ry && my <= ry + rowH;

            if (hov) {
                g.fill(listX, ry, listX + listW, ry + rowH,
                    AnimationHelper.withAlpha(UITheme.ACCENT_GHOST, (int)(fadeAlpha * 255)));
            }
            g.fill(listX, ry + rowH - 1, listX + listW, ry + rowH,
                AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 40)));

            int cx2 = listX + 2;
            g.drawString(font, "\u00A7f#" + t.id, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[0];

            String rep = t.reporterName;
            if (rep.startsWith("UUID:")) rep = rep.substring(5);
            if (rep.length() > 10) rep = rep.substring(0, 8) + "..";
            g.drawString(font, "\u00A7e" + rep, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[1];

            String target = t.targetName.length() > 10 ? t.targetName.substring(0, 8) + ".." : t.targetName;
            g.drawString(font, "\u00A7f" + target, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cx2 += cw[2];

            g.drawString(font, "\u00A77" + t.type, cx2, ry + 4,
                AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
            cx2 += cw[3];

            // assigned badge
            if (t.assignedTo != null && !t.assignedTo.isEmpty()) {
                String a = t.assignedTo.length() > 8 ? t.assignedTo.substring(0, 6) + ".." : t.assignedTo;
                g.drawString(font, "\u00A76\u2691" + a, cx2, ry + 4,
                    AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 220)));
            } else {
                g.drawString(font, "\u00A78\u2713 open", cx2, ry + 4,
                    AnimationHelper.withAlpha(UITheme.STATUS_OK, (int)(fadeAlpha * 180)));
            }
        }

        if (tickets.isEmpty()) {
            g.drawCenteredString(font, "\u00A77No tickets", listX + listW / 2, listY + listH / 2 - 4,
                AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 200)));
        }

        // scrollbar hint
        if (maxScroll > 0) {
            float pct = (float) ticketScroll / maxScroll;
            int barH = Math.max(8, listH * vis / Math.max(1, tickets.size()));
            int barY = listY + (int)((listH - barH) * pct);
            g.fill(listX + listW + 2, barY, listX + listW + 4, barY + barH,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 150)));
        }
    }

    private void renderReportsTab(GuiGraphics g, int x, int y, int w, int h) {
        g.drawString(font, "REPORTS", x, y,
            AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        int cy = y + 20;
        g.fill(x, cy, x + w, cy + 1, AnimationHelper.withAlpha(UITheme.BORDER, (int)(fadeAlpha * 150)));
        cy += 16;
        g.drawString(font, "Commands:", x + 8, cy,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        cy += 18;
        String[] cmds = {
            "§6/report <player> <type> <desc>",
            "§6/reports",
            "§6/report view <id>",
            "§6/report claim <id>",
            "§6/report dismiss <id> <reason>",
            "§6/report punish <id> <punishment_id>",
            "",
            "§7Report types:",
            " TEAM_KILL, CHEATING, TOXIC_CHAT, VOICE_ABUSE,",
            " GRIEFING, EXPLOITING, AFK, INAPPROPRIATE_NAME,",
            " TEAM_STACKING, COALITION, OTHER"
        };
        for (String cmd : cmds) {
            if (cy > y + h - 10) break;
            g.drawString(font, cmd, x + 12, cy,
                AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 200)));
            cy += 16;
        }
    }

    private void drawLValue(GuiGraphics g, int x, int y, String label, String value, int valueColor) {
        g.drawString(font, label + ":", x, y,
            AnimationHelper.withAlpha(UITheme.TEXT_SECONDARY, (int)(fadeAlpha * 200)));
        int lw = font.width(label + ":  ");
        g.drawString(font, value, x + lw, y,
            AnimationHelper.withAlpha(valueColor, (int)(fadeAlpha * 255)));
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }

    private String phaseName(int phase) {
        return switch (phase) {
            case 0 -> "LOBBY";
            case 1 -> "VOTING";
            case 2 -> "PLAYING";
            case 3 -> "ENDED";
            default -> "UNKNOWN";
        };
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (currentTab == Tab.TICKETS) {
            int listH = height - HEADER_H - QUICK_BAR_H - 10 - 36 - 14;
            int rowH = 20;
            int vis = Math.max(1, listH / rowH);
            int maxScroll = Math.max(0, ClientTeamData.ticketList.size() - vis);
            ticketScroll = (int) Math.max(0, Math.min(maxScroll, ticketScroll - delta * 3));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
