package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.client.gui.UITheme;
import com.yourmod.teamsystem.client.gui.scoreboard.data.RankDefinition;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

public class CustomNametagRenderer {

    private static final int MAX_DIST = 32;
    private static final int ICON_SIZE = 16;
    private static final int BAR_W = 40;
    private static final int BAR_H = 2;
    private static final int LINE_GAP = 2;
    private static final int BAR_OFFSET = 4;

    public void renderNametag(PoseStack poseStack, MultiBufferSource bufferSource,
                                Player player, Camera camera) {
        if (player == Minecraft.getInstance().player) return;
        UUID uuid = player.getUUID();

        Map<UUID, PlayerListEntry> map = ClientTeamData.playerDataMap;
        if (map == null) return;
        PlayerListEntry ple = map.get(uuid);
        if (ple == null) return;

        RankDefinition rankDef = RankDefinition.get(ple.rank());
        String callsign = ple.callsign() != null ? ple.callsign() : player.getName().getString();

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;

        String nick = player.getName().getString();
        float kd = ple.deaths() == 0 ? ple.kills() : (float) ple.kills() / (float) ple.deaths();
        String kdStr = String.format("K/D: %.2f", kd);

        String firstLine = rankDef.shortName + " " + callsign;
        int firstW = font.width(firstLine);
        String secondLine = nick;
        int secondW = font.width(secondLine);
        int kdW = font.width(kdStr);
        int totalW = Math.max(firstW, secondW + kdW + 8);
        totalW = Math.max(totalW, BAR_W);

        Vec3 camPos = camera.getPosition();
        double dx = player.getX() - camPos.x;
        double dy = player.getY() + player.getBbHeight() + 0.5 - camPos.y;
        double dz = player.getZ() - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > MAX_DIST) return;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        int centerX = 0;
        int textY = 0;

        int iconX = centerX - totalW / 2;
        drawIcon(poseStack, iconX, textY - 2, rankDef);

        font.drawInBatch(rankDef.shortName + " " + callsign,
            centerX - firstW / 2f, textY,
            0xFFFFFFFF, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        font.drawInBatch(kdStr,
            centerX + secondW / 2f + 4, textY + 12,
            0xFFB0B0B0, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        font.drawInBatch(nick,
            centerX - totalW / 2f, textY + 12,
            0xFF808080, true,
            poseStack.last().pose(),
            bufferSource,
            Font.DisplayMode.SEE_THROUGH,
            0, 0xF000F0);

        int barX = centerX - BAR_W / 2;
        int barY = textY + font.lineHeight * 2 + LINE_GAP + BAR_OFFSET;
        drawBar(poseStack, barX, barY);

        poseStack.popPose();
    }

    private void drawIcon(PoseStack poseStack, int x, int y, RankDefinition rankDef) {
        RenderSystem.setShaderTexture(0, rankDef.iconTexture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        float u0 = 0f;
        float v0 = rankDef.getIconVOffset() / 160f;
        float u1 = 1f;
        float v1 = (rankDef.getIconVOffset() + ICON_SIZE) / 160f;

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(poseStack.last().pose(), x, y + ICON_SIZE, 0).uv(u0, v1).endVertex();
        buffer.vertex(poseStack.last().pose(), x + ICON_SIZE, y + ICON_SIZE, 0).uv(u1, v1).endVertex();
        buffer.vertex(poseStack.last().pose(), x + ICON_SIZE, y, 0).uv(u1, v0).endVertex();
        buffer.vertex(poseStack.last().pose(), x, y, 0).uv(u0, v0).endVertex();
        tessellator.end();
    }

    private void drawBar(PoseStack poseStack, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder buffer = tessellator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawColoredQuad(buffer, poseStack, x, y, BAR_W, BAR_H, 0x40, 0, 0, 0);
        tessellator.end();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        drawColoredQuad(buffer, poseStack, x, y, BAR_W, BAR_H, 0xFF, 0x80, 0x80, 0x80);
        tessellator.end();
    }

    private void drawColoredQuad(BufferBuilder buffer, PoseStack poseStack, int x, int y, int w, int h, int a, int r, int g, int b) {
        buffer.vertex(poseStack.last().pose(), x, y + h, 0).color(r, g, b, a).endVertex();
        buffer.vertex(poseStack.last().pose(), x + w, y + h, 0).color(r, g, b, a).endVertex();
        buffer.vertex(poseStack.last().pose(), x + w, y, 0).color(r, g, b, a).endVertex();
        buffer.vertex(poseStack.last().pose(), x, y, 0).color(r, g, b, a).endVertex();
    }
}
