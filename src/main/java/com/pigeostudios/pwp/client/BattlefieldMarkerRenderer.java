package com.pigeostudios.pwp.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.pigeostudios.pwp.core.MarkerData;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = "pwp", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BattlefieldMarkerRenderer {

    private static final float MAX_RENDER_DISTANCE = 1000.0f;
    private static final float MIN_SCALE = 0.1f;
    private static final float MAX_SCALE = 1.5f;
    private static final float TEXT_DISTANCE = 50.0f;
    private static final Map<String, Double> smoothProgress = new HashMap<>();

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        var poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();
        Team localTeam = ClientTeamData.getLocalPlayerTeam();
        boolean isSpectator = localTeam == Team.SPECTATOR;

        MultiBufferSource.BufferSource textBuffer = MultiBufferSource.immediate(new BufferBuilder(256));

        poseStack.pushPose();

        for (MarkerData marker : ClientTeamData.tacticalMarkers) {
            if (isSpectator || marker.getTeamOrdinal() < 0 || marker.getTeamOrdinal() == localTeam.ordinal()) {
                renderTacticalMarker(poseStack, marker, cameraPos, mc, textBuffer);
            }
        }

        for (FOBData fob : ClientTeamData.fobs) {
            if (isSpectator || fob.teamOrdinal() == localTeam.ordinal()) {
                renderFOB(poseStack, fob, cameraPos, mc, textBuffer);
            }
        }

        int[] natoBase = ClientTeamData.getNatoBasePos();
        int[] russiaBase = ClientTeamData.getRussiaBasePos();
        if (natoBase != null && (isSpectator || localTeam == Team.NATO)) {
            renderBase(poseStack, natoBase, 0x5555FF, cameraPos, mc, textBuffer);
        }
        if (russiaBase != null && (isSpectator || localTeam == Team.RUSSIA)) {
            renderBase(poseStack, russiaBase, 0xFF5555, cameraPos, mc, textBuffer);
        }

        for (CapturePointData cp : ClientTeamData.capturePoints) {
            renderCapturePoint(poseStack, cp, cameraPos, mc, textBuffer);
        }

        poseStack.popPose();

        textBuffer.endBatch();
    }

    private static void renderTacticalMarker(PoseStack poseStack, MarkerData marker, Vec3 cameraPos, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        Vec3 pos = worldToCamera(marker.getX(), marker.getY(), marker.getZ(), cameraPos);
        double distance = pos.length();
        if (distance > MAX_RENDER_DISTANCE) return;

        String letter = getMarkerLetter(marker);
        int color = getMarkerColor(marker);

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        float yRot = (float) Math.atan2(pos.x, pos.z);
        poseStack.mulPose(Axis.YP.rotation(yRot));
        float scale = calculateScale(distance);
        poseStack.scale(scale, scale, scale);

        renderLetterIcon(poseStack, letter, color, mc, textBuffer);

        if (distance < TEXT_DISTANCE) {
            renderText(poseStack, marker.getLabel(), distance, mc, textBuffer);
        }
        poseStack.popPose();
    }

    private static void renderFOB(PoseStack poseStack, FOBData fob, Vec3 cameraPos, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        Vec3 pos = worldToCamera(fob.x(), fob.y(), fob.z(), cameraPos);
        double distance = pos.length();
        if (distance > MAX_RENDER_DISTANCE) return;

        int color = fob.teamOrdinal() == Team.NATO.ordinal() ? 0x5555FF : 0xFF5555;

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        float yRot = (float) Math.atan2(pos.x, pos.z);
        poseStack.mulPose(Axis.YP.rotation(yRot));
        float scale = calculateScale(distance);
        poseStack.scale(scale, scale, scale);

        renderLetterIcon(poseStack, "F", color, mc, textBuffer);

        if (distance < TEXT_DISTANCE) {
            renderText(poseStack, "FOB: " + fob.name(), distance, mc, textBuffer);
        }
        poseStack.popPose();
    }

    private static void renderBase(PoseStack poseStack, int[] basePos, int color, Vec3 cameraPos, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        Vec3 pos = worldToCamera(basePos[0] + 0.5, basePos[1] + 1.5, basePos[2] + 0.5, cameraPos);
        double distance = pos.length();
        if (distance > MAX_RENDER_DISTANCE) return;

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        float yRot = (float) Math.atan2(pos.x, pos.z);
        poseStack.mulPose(Axis.YP.rotation(yRot));
        float scale = calculateScale(distance) * 1.5f;
        poseStack.scale(scale, scale, scale);

        renderLetterIcon(poseStack, "B", color, mc, textBuffer);

        if (distance < TEXT_DISTANCE) {
            renderText(poseStack, "Base", distance, mc, textBuffer);
        }
        poseStack.popPose();
    }

    private static void renderCapturePoint(PoseStack poseStack, CapturePointData cp, Vec3 cameraPos, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        Vec3 pos = worldToCamera(cp.x(), cp.y(), cp.z(), cameraPos);
        double distance = pos.length();
        if (distance > MAX_RENDER_DISTANCE) return;

        int color = 0xFFAA00;
        if (cp.ownerTeamOrdinal() >= 0) {
            color = cp.ownerTeamOrdinal() == Team.NATO.ordinal() ? 0x5555FF : 0xFF5555;
        }

        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        float yRot = (float) Math.atan2(pos.x, pos.z);
        poseStack.mulPose(Axis.YP.rotation(yRot));
        float scale = calculateScale(distance);
        poseStack.scale(scale, scale, scale);

        renderLetterIcon(poseStack, getFirstLetter(cp.name()), color, mc, textBuffer);

        if (cp.capturingTeamOrdinal() >= 0) {
            renderCaptureProgress(poseStack, cp.progress(), cp.capturingTeamOrdinal(), mc, textBuffer);
        }

        if (distance < TEXT_DISTANCE) {
            renderText(poseStack, cp.name(), distance, mc, textBuffer);
        }
        poseStack.popPose();
    }

    private static void renderLetterIcon(PoseStack poseStack, String letter, int color, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();

        poseStack.pushPose();
        poseStack.translate(0, -0.15, 0);
        poseStack.scale(-0.03f, -0.03f, 0.03f);

        Matrix4f matrix = poseStack.last().pose();
        int lw = mc.font.width(letter);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, -lw / 2f - 3, -4, 0).color(0, 0, 0, 0.6f).endVertex();
        buffer.vertex(matrix, -lw / 2f - 3, 10, 0).color(0, 0, 0, 0.6f).endVertex();
        buffer.vertex(matrix, lw / 2f + 3, 10, 0).color(0, 0, 0, 0.6f).endVertex();
        buffer.vertex(matrix, lw / 2f + 3, -4, 0).color(0, 0, 0, 0.6f).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        mc.font.drawInBatch(letter, -lw / 2f, 0, color, false,
                matrix, textBuffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        textBuffer.endBatch();

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static void renderCaptureProgress(PoseStack poseStack, double progress, int capturingTeamOrdinal, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        float target = (float) Math.min(1.0, Math.max(0.0, progress / 100.0));
        String key = "cp_" + capturingTeamOrdinal;
        float current = smoothProgress.containsKey(key) ? smoothProgress.get(key).floatValue() : target;
        current += (target - current) * 0.15f;
        smoothProgress.put(key, (double) current);

        int barColor = capturingTeamOrdinal == Team.NATO.ordinal() ? 0x5555FF : 0xFF5555;
        float r = ((barColor >> 16) & 0xFF) / 255.0f;
        float g = ((barColor >> 8) & 0xFF) / 255.0f;
        float b = (barColor & 0xFF) / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();

        Matrix4f matrix = poseStack.last().pose();
        float barW = 0.4f;
        float barH = 0.04f;
        float barY = -0.55f;

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, -barW, barY, 0).color(0, 0, 0, 0.7f).endVertex();
        buffer.vertex(matrix, -barW, barY - barH, 0).color(0, 0, 0, 0.7f).endVertex();
        buffer.vertex(matrix, barW, barY - barH, 0).color(0, 0, 0, 0.7f).endVertex();
        buffer.vertex(matrix, barW, barY, 0).color(0, 0, 0, 0.7f).endVertex();

        float fillW = barW * 2 * current;
        buffer.vertex(matrix, -barW, barY, 0).color(r, g, b, 0.9f).endVertex();
        buffer.vertex(matrix, -barW, barY - barH, 0).color(r, g, b, 0.9f).endVertex();
        buffer.vertex(matrix, -barW + fillW, barY - barH, 0).color(r, g, b, 0.9f).endVertex();
        buffer.vertex(matrix, -barW + fillW, barY, 0).color(r, g, b, 0.9f).endVertex();

        BufferUploader.drawWithShader(buffer.end());

        String pctText = String.format("%.0f%%", progress);
        int tw = mc.font.width(pctText);
        poseStack.pushPose();
        poseStack.translate(0, barY - barH - 0.06f, 0);
        poseStack.scale(-0.02f, -0.02f, 0.02f);
        matrix = poseStack.last().pose();
        mc.font.drawInBatch(pctText, -tw / 2f, 0, 0xFFFFFF, false,
                matrix, textBuffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);
        textBuffer.endBatch();
        poseStack.popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderText(PoseStack poseStack, String text, double distance, Minecraft mc, MultiBufferSource.BufferSource textBuffer) {
        poseStack.pushPose();
        poseStack.translate(0, 0.7, 0);
        poseStack.scale(-0.02f, -0.02f, 0.02f);

        String distanceText = String.format("%.0fm", distance);
        int textWidth = mc.font.width(text);
        int distWidth = mc.font.width(distanceText);
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, -textWidth / 2f - 2, -2, 0).color(0, 0, 0, 0.5f).endVertex();
        buffer.vertex(matrix, -textWidth / 2f - 2, 10, 0).color(0, 0, 0, 0.5f).endVertex();
        buffer.vertex(matrix, textWidth / 2f + 2, 10, 0).color(0, 0, 0, 0.5f).endVertex();
        buffer.vertex(matrix, textWidth / 2f + 2, -2, 0).color(0, 0, 0, 0.5f).endVertex();
        BufferUploader.drawWithShader(buffer.end());

        mc.font.drawInBatch(text, -textWidth / 2f, 0, 0xFFFFFF, false,
                matrix, textBuffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);

        poseStack.translate(0, 12, 0);
        matrix = poseStack.last().pose();
        mc.font.drawInBatch(distanceText, -distWidth / 2f, 0, 0xAAAAAA, false,
                matrix, textBuffer,
                net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, 15728880);

        textBuffer.endBatch();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private static Vec3 worldToCamera(double wx, double wy, double wz, Vec3 cameraPos) {
        return new Vec3(wx + 0.5 - cameraPos.x, wy + 1.5 - cameraPos.y, wz + 0.5 - cameraPos.z);
    }

    private static float calculateScale(double distance) {
        float scale = MAX_SCALE / (1.0f + (float) distance / 100.0f);
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    private static String getMarkerLetter(MarkerData marker) {
        return switch (marker.getType()) {
            case ATTACK -> "A";
            case DEFEND -> "D";
            case OBSERVE -> "O";
            default -> getFirstLetter(marker.getLabel());
        };
    }

    private static String getFirstLetter(String str) {
        if (str == null || str.isEmpty()) return "?";
        return String.valueOf(Character.toUpperCase(str.charAt(0)));
    }

    private static int getMarkerColor(MarkerData marker) {
        return switch (marker.getType()) {
            case ATTACK -> 0xFF4444;
            case DEFEND -> 0x44FF44;
            case OBSERVE -> 0xFF44FF;
            default -> 0xFFFFFF;
        };
    }
}
