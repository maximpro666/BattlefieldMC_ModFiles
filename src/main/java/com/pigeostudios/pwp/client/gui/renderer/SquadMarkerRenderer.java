package com.pigeostudios.pwp.client.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pigeostudios.pwp.PWP;
import com.pigeostudios.pwp.client.ClientTeamData;
import com.pigeostudios.pwp.client.PlayerListEntry;
import com.pigeostudios.pwp.client.gui.UITheme;
import com.pigeostudios.pwp.client.gui.VisualsConfig;
import com.pigeostudios.pwp.core.Team;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SquadMarkerRenderer {

    private static final double PROXIMITY_FADE_DIST = 3.0;
    private static final double ANGLE_COS = Math.cos(Math.toRadians(3.0));
    private static final int LEADER_COLOR_DEFAULT = 0xFFFFD700;
    private boolean loggedOnce = false;

    public void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Player localPlayer = mc.player;
        if (localPlayer == null) return;

        VisualsConfig cfg = VisualsConfig.get();
        if (cfg.squadMarker == null) { PWP.LOGGER.info("[SquadMarker] cfg.squadMarker is null!"); return; }
        if (!cfg.squadMarker.enabled) { PWP.LOGGER.info("[SquadMarker] disabled in config"); return; }

        Team localTeam = ClientTeamData.getLocalPlayerTeam();
        if (localTeam == Team.SPECTATOR) { PWP.LOGGER.info("[SquadMarker] local team is SPECTATOR"); return; }

        if (mc.level == null) return;
        int playerCount = 0;
        int teamMatchCount = 0;

        Vec3 camPos = camera.getPosition();

        for (Player player : mc.level.players()) {
            if (player == localPlayer) continue;
            if (player.isSpectator()) continue;
            playerCount++;

            PlayerListEntry entry = ClientTeamData.playerDataMap.get(player.getUUID());
            if (entry == null) { PWP.LOGGER.info("[SquadMarker] no PlayerListEntry for {}", player.getName().getString()); continue; }

            if (entry.teamOrdinal() != localTeam.ordinal()) continue;
            teamMatchCount++;

            double dist = player.distanceTo(localPlayer);

            float alpha = (float) cfg.squadMarker.opacity;

            if (dist < PROXIMITY_FADE_DIST) {
                alpha *= (float) (dist / PROXIMITY_FADE_DIST);
            }

            if (alpha < 0.01f) { PWP.LOGGER.info("[SquadMarker] alpha too low: {}", alpha); continue; }

            Vec3 targetPos = player.getEyePosition(partialTick);
            Vec3 toTarget = targetPos.subtract(camPos).normalize();
            Vector3f lv = camera.getLookVector();
            Vec3 lookVec = new Vec3(lv.x, lv.y, lv.z);
            double dot = toTarget.dot(lookVec);
            if (dot > ANGLE_COS) { PWP.LOGGER.info("[SquadMarker] hidden by crosshair check, dot={}", dot); continue; }

            Team playerTeam = Team.fromOrdinal(entry.teamOrdinal());
            int teamColor = playerTeam == Team.NATO ? UITheme.TEAM_NATO : UITheme.TEAM_RUSSIA;

            int color;
            String mySquad = ClientTeamData.localPlayerSquad;
            boolean inSquad = mySquad != null && !mySquad.isEmpty();
            if (inSquad && isPlayerInMySquad(entry, mySquad)) {
                if (entry.isSquadLeader()) {
                    color = cfg.squadMarker.leaderColor != 0 ? cfg.squadMarker.leaderColor : LEADER_COLOR_DEFAULT;
                    if (!loggedOnce) PWP.LOGGER.info("[SquadMarker] {} is squad LEADER", player.getName().getString());
                } else {
                    color = cfg.squadMarker.color != 0 ? cfg.squadMarker.color : teamColor;
                }
            } else {
                color = teamColor;
            }

            if (!loggedOnce) {
                PWP.LOGGER.info("[SquadMarker] RENDERING marker for {} at dist={}, color={:#X}", player.getName().getString(), dist, color);
                loggedOnce = true;
            }

            renderSquare(poseStack, bufferSource, player, camPos, camera, partialTick, cfg.squadMarker.size, color, alpha);
        }

        if (!loggedOnce) {
            PWP.LOGGER.info("[SquadMarker] checked {} other players, {} team matches", playerCount, teamMatchCount);
        }
    }

    private boolean isPlayerInMySquad(PlayerListEntry entry, String mySquad) {
        String ps = entry.squadName() != null && !entry.squadName().isEmpty() ? entry.squadName() : entry.squad();
        return ps != null && mySquad.equals(ps);
    }

    private void renderSquare(PoseStack poseStack, MultiBufferSource bufferSource, Player player,
                               Vec3 camPos, Camera camera, float partialTick, double size, int color, float alpha) {
        double wy = player.yo + (player.getY() - player.yo) * partialTick;
        double headY = wy + player.getBbHeight() + 0.55;
        double px = player.xo + (player.getX() - player.xo) * partialTick;
        double pz = player.zo + (player.getZ() - player.zo) * partialTick;
        double dx = px - camPos.x;
        double dz = pz - camPos.z;
        double dy = headY - camPos.y;
        double actualDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (actualDist < 0.01) return;

        float scale = (float) (0.025 * Math.max(1.0, actualDist / 15.0));

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.translate(0, 0, (float) (-actualDist * 0.0001));
        poseStack.scale(-scale, -scale, scale);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();

        float s = (float) size * 10f;
        float half = s / 2f;
        int renderColor = (Math.min(0xFF, (int) (alpha * 255)) << 24) | (color & 0x00FFFFFF);
        float r = ((renderColor >> 16) & 0xFF) / 255f;
        float g = ((renderColor >> 8) & 0xFF) / 255f;
        float b = (renderColor & 0xFF) / 255f;
        float a = ((renderColor >> 24) & 0xFF) / 255f;

        vc.vertex(mat, -half, -half, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, half, -half, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, half, half, 0).color(r, g, b, a).endVertex();
        vc.vertex(mat, -half, half, 0).color(r, g, b, a).endVertex();

        poseStack.popPose();
    }
}
