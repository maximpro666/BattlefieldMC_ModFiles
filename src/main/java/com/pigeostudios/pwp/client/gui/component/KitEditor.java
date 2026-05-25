package com.pigeostudios.pwp.client.gui.component;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.network.KitAdminRequestPacket;
import com.pigeostudios.pwp.network.KitAdminSavePacket;
import com.pigeostudios.pwp.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class KitEditor {

    private JsonObject kitClassesObj = null;
    private String kitSelectedClassId = null;
    private String kitSelectedKitId = null;
    private boolean kitEditActive = false;
    private String kitEditField = null;
    private StringBuilder kitEditBuffer = new StringBuilder();
    private int kitEditCursor = 0;
    private long kitEditBlink = 0;
    private String kitConfigRequested = "";
    private String kitLastJson = "";

    public void activate() {
        kitEditActive = false;
        kitEditField = null;
        if (ClientTeamData.kitConfigEditJson.isEmpty() && kitConfigRequested.isEmpty()) {
            requestKitConfig();
        } else if (!ClientTeamData.kitConfigEditJson.isEmpty()) {
            parseJson();
        }
    }

    public void requestKitConfig() {
        PacketHandler.CHANNEL.sendToServer(new KitAdminRequestPacket());
        kitConfigRequested = "pending";
    }

    private void parseJson() {
        String json = ClientTeamData.kitConfigEditJson;
        if (json == null) json = "";
        if (json.equals(kitLastJson) && kitClassesObj != null) return;
        kitLastJson = json;
        kitConfigRequested = "";
        if (json.isEmpty()) {
            kitClassesObj = null;
            return;
        }
        try {
            JsonElement el = JsonParser.parseString(json);
            kitClassesObj = (el != null && el.isJsonObject())
                ? el.getAsJsonObject().getAsJsonObject("classes") : null;
        } catch (Exception e) {
            kitClassesObj = null;
        }
    }

    public boolean handleClick(double mx, double my, int btn, int navW, int contentX, int contentY, int contentW, int contentH) {
        if (btn != 0) return false;
        int leftW = 180;
        int leftX = contentX;
        int rightX = contentX + leftW + 8;

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

        int btnY = contentY + contentH - 28;
        int btnW = 100;
        int btnH = 22;
        if (mx >= rightX && mx <= rightX + btnW && my >= btnY && my <= btnY + btnH) {
            saveToServer();
            return true;
        }
        if (mx >= rightX + btnW + 6 && mx <= rightX + btnW + 6 + btnW && my >= btnY && my <= btnY + btnH) {
            requestKitConfig();
            return true;
        }

        JsonObject classObj = (kitSelectedClassId != null && kitClassesObj != null)
            ? kitClassesObj.getAsJsonObject(kitSelectedClassId) : null;
        JsonObject kitsObj = (classObj != null && classObj.has("kits"))
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

        if (kitSelectedKitId != null && kitsObj != null && kitsObj.has(kitSelectedKitId)) {
            if (kitEditActive) {
                confirmEdit();
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
                        String currentVal = getFieldValue(kitsObj.getAsJsonObject(kitSelectedKitId), fk);
                        startEdit(kitSelectedClassId, kitSelectedKitId, fk, currentVal);
                        return true;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(int key, int scan, int mods) {
        if (!kitEditActive) return false;
        if (key == 257 || key == 335) {
            confirmEdit();
            return true;
        }
        if (key == 256) {
            cancelEdit();
            return true;
        }
        if (key == 259) {
            if (kitEditCursor > 0) {
                kitEditBuffer.deleteCharAt(kitEditCursor - 1);
                kitEditCursor--;
                kitEditBlink = System.currentTimeMillis();
            }
            return true;
        }
        if (key == 261) {
            if (kitEditCursor < kitEditBuffer.length()) {
                kitEditBuffer.deleteCharAt(kitEditCursor);
                kitEditBlink = System.currentTimeMillis();
            }
            return true;
        }
        if (key == 263) {
            if (kitEditCursor > 0) { kitEditCursor--; kitEditBlink = System.currentTimeMillis(); }
            return true;
        }
        if (key == 262) {
            if (kitEditCursor < kitEditBuffer.length()) { kitEditCursor++; kitEditBlink = System.currentTimeMillis(); }
            return true;
        }
        if (key == 268) {
            kitEditCursor = 0; kitEditBlink = System.currentTimeMillis();
            return true;
        }
        if (key == 269) {
            kitEditCursor = kitEditBuffer.length(); kitEditBlink = System.currentTimeMillis();
            return true;
        }
        return true;
    }

    public boolean charTyped(char ch, int mods) {
        if (!kitEditActive) return false;
        if (ch >= ' ' && ch != 127) {
            kitEditBuffer.insert(kitEditCursor, ch);
            kitEditCursor++;
            kitEditBlink = System.currentTimeMillis();
        }
        return true;
    }

    public void render(GuiGraphics g, int x, int y, int w, int h, int mx, int my, float fadeAlpha, Font font) {
        if (kitClassesObj == null && !ClientTeamData.kitConfigEditJson.isEmpty()) {
            parseJson();
        }

        final int leftW = 180;
        final int leftX = x;
        final int leftY = y + 14;
        final int rightX = x + leftW + 8;
        final int rightW = w - leftW - 8;

        g.fill(leftX, leftY, leftX + leftW, y + h - 10,
            AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0x88)));

        g.drawString(font, "CLASSES", leftX + 4, leftY + 2,
            AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));

        int iy = leftY + 16;
        if (kitClassesObj != null) {
            for (var entry : kitClassesObj.entrySet()) {
                String cId = entry.getKey();
                JsonObject cObj = entry.getValue().getAsJsonObject();
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

        if (kitSelectedClassId != null && kitClassesObj != null) {
            JsonObject classObj = kitClassesObj.getAsJsonObject(kitSelectedClassId);
            if (classObj != null) {
                String cDn = classObj.has("display_name") ? classObj.get("display_name").getAsString() : kitSelectedClassId;
                g.drawString(font, "Class: " + cDn, rightX, y + 14,
                    AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));

                final int kitListY = y + 36;
                int ry = kitListY;
                JsonObject kitsObj = classObj.has("kits") ? classObj.getAsJsonObject("kits") : null;
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

                int editorY = Math.max(ry + 4, y + h - 260);
                int editorH = y + h - 28 - 4 - editorY;
                if (editorH > 20 && kitSelectedKitId != null && kitsObj != null && kitsObj.has(kitSelectedKitId)) {
                    JsonObject kitObj = kitsObj.getAsJsonObject(kitSelectedKitId);
                    int editorW = rightW;
                    g.fill(rightX, editorY, rightX + editorW, editorY + editorH,
                        AnimationHelper.withAlpha(UITheme.BG_SURFACE, (int)(fadeAlpha * 0x66)));

                    if (kitEditActive && kitEditField != null &&
                        kitEditField.startsWith(kitSelectedClassId + "." + kitSelectedKitId + ".")) {
                        renderEditor(g, kitObj, rightX + 4, editorY + 2, editorW - 8, fadeAlpha, font);
                    } else {
                        renderFields(g, kitObj, rightX + 4, editorY + 2, editorW - 8, mx, my, fadeAlpha, font);
                    }
                }
            }
        }

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

    private String getFieldValue(JsonObject kitObj, String fieldKey) {
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
                        List<String> list = new ArrayList<>();
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

    private void startEdit(String classId, String kitId, String field, String currentValue) {
        kitEditActive = true;
        kitEditField = classId + "." + kitId + "." + field;
        kitEditBuffer = new StringBuilder(currentValue != null ? currentValue : "");
        kitEditCursor = kitEditBuffer.length();
        kitEditBlink = System.currentTimeMillis();
    }

    private void confirmEdit() {
        if (!kitEditActive || kitEditField == null) return;
        String path = kitEditField;
        String newValue = kitEditBuffer.toString();

        try {
            String[] parts = path.split("\\.", 3);
            if (parts.length < 3) { cancelEdit(); return; }
            String classId = parts[0];
            String kitId = parts[1];
            String fieldPath = parts[2];

            if (kitClassesObj == null || !kitClassesObj.has(classId)) { cancelEdit(); return; }
            JsonObject classObj = kitClassesObj.getAsJsonObject(classId);
            if (!classObj.has("kits")) { cancelEdit(); return; }
            JsonObject kitsObj = classObj.getAsJsonObject("kits");
            if (!kitsObj.has(kitId)) { cancelEdit(); return; }
            JsonObject kitObj = kitsObj.getAsJsonObject(kitId);

            if (fieldPath.equals("display_name")) {
                kitObj.addProperty("display_name", newValue);
            } else if (fieldPath.equals("description")) {
                kitObj.addProperty("description", newValue);
            } else if (fieldPath.equals("requirements.rank")) {
                try {
                    int rank = Integer.parseInt(newValue.trim());
                    JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new JsonObject();
                    reqObj.addProperty("rank", rank);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.equals("requirements.sp_cost")) {
                try {
                    int sp = Integer.parseInt(newValue.trim());
                    JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new JsonObject();
                    reqObj.addProperty("sp_cost", sp);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.equals("requirements.bc_cost")) {
                try {
                    int bc = Integer.parseInt(newValue.trim());
                    JsonObject reqObj = kitObj.has("requirements")
                        ? kitObj.getAsJsonObject("requirements")
                        : new JsonObject();
                    reqObj.addProperty("bc_cost", bc);
                    kitObj.add("requirements", reqObj);
                } catch (NumberFormatException ignored) {}
            } else if (fieldPath.startsWith("weapons.")) {
                String cat = fieldPath.substring("weapons.".length());
                JsonObject weaponsObj = kitObj.has("weapons")
                    ? kitObj.getAsJsonObject("weapons")
                    : new JsonObject();
                JsonArray arr = new JsonArray();
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
        kitSelectedKitId = null;
    }

    private void cancelEdit() {
        kitEditActive = false;
        kitEditField = null;
    }

    private void saveToServer() {
        if (kitClassesObj == null) return;
        try {
            JsonObject root = new JsonObject();
            root.add("classes", kitClassesObj);
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(root);
            PacketHandler.CHANNEL.sendToServer(new KitAdminSavePacket(json));
        } catch (Exception ignored) {}
    }

    private void renderFields(GuiGraphics g, JsonObject kitObj, int x, int y, int w, int mx, int my, float fadeAlpha, Font font) {
        int fy = y;
        int colW = w / 2;

        String dn = kitObj.has("display_name") ? kitObj.get("display_name").getAsString() : "";
        String desc = kitObj.has("description") ? kitObj.get("description").getAsString() : "";
        int rankVal = 1;
        JsonObject reqObj = kitObj.has("requirements") ? kitObj.getAsJsonObject("requirements") : null;
        if (reqObj != null && reqObj.has("rank")) rankVal = reqObj.get("rank").getAsInt();

        drawEditableField(g, "Name:", dn, x, fy, colW, mx, my, fadeAlpha, font);
        fy += 16;
        drawEditableField(g, "Desc:", desc, x, fy, colW, mx, my, fadeAlpha, font);
        fy += 16;
        drawEditableField(g, "Rank:", String.valueOf(rankVal), x, fy, colW, mx, my, fadeAlpha, font);
        fy += 16;

        String[] weaponCats = {"primary", "secondary", "special", "grenade"};
        JsonObject weaponsObj = kitObj.has("weapons") ? kitObj.getAsJsonObject("weapons") : null;
        if (weaponsObj != null) {
            fy += 4;
            g.drawString(font, "Weapons:", x, fy,
                AnimationHelper.withAlpha(UITheme.ACCENT_DIM, (int)(fadeAlpha * 200)));
            fy += 16;
            for (String cat : weaponCats) {
                String val = "";
                if (weaponsObj.has(cat)) {
                    List<String> list = new ArrayList<>();
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
                drawEditableField(g, cat + ":", truncated.toString(), x, fy, colW, mx, my, fadeAlpha, font);
                fy += 16;
            }
        }

        JsonObject attLimits = kitObj.has("attachment_limits") ?
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
                JsonObject limitsObj = weaponEntry.getValue().getAsJsonObject();
                for (var catEntry : limitsObj.entrySet()) {
                    String catName = catEntry.getKey();
                    List<String> items = new ArrayList<>();
                    for (var el : catEntry.getValue().getAsJsonArray()) items.add(el.getAsString());
                    String line = "    " + catName + ": " + String.join(", ", items);
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

    private void renderEditor(GuiGraphics g, JsonObject kitObj, int x, int y, int w, float fadeAlpha, Font font) {
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

        String displayText = kitEditBuffer.toString();
        String beforeCursor = displayText.substring(0, Math.min(kitEditCursor, displayText.length()));
        String afterCursor = displayText.substring(Math.min(kitEditCursor, displayText.length()));
        int textX = x + 4;
        int textY = y + 36;

        g.fill(x + 2, textY - 2, x + w - 4, textY + font.lineHeight + 2,
            AnimationHelper.withAlpha(UITheme.BG_PANEL, (int)(fadeAlpha * 0xAA)));

        g.drawString(font, beforeCursor, textX, textY,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 255)));
        int cursorX = textX + font.width(beforeCursor);

        if ((System.currentTimeMillis() - kitEditBlink) % 1000 < 500) {
            g.fill(cursorX, textY, cursorX + 1, textY + font.lineHeight,
                AnimationHelper.withAlpha(UITheme.ACCENT, (int)(fadeAlpha * 255)));
        }

        g.drawString(font, afterCursor, cursorX + 2, textY,
            AnimationHelper.withAlpha(UITheme.TEXT_PRIMARY, (int)(fadeAlpha * 180)));

        g.drawString(font, "Enter = confirm, Esc = cancel", x + 4, textY + font.lineHeight + 6,
            AnimationHelper.withAlpha(UITheme.TEXT_MUTED, (int)(fadeAlpha * 150)));
    }

    private void drawEditableField(GuiGraphics g, String label, String value, int x, int y, int maxW, int mx, int my, float fadeAlpha, Font font) {
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
}
