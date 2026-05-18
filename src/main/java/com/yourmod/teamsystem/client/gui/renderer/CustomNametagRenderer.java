package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import com.yourmod.teamsystem.core.Rank;
import com.yourmod.teamsystem.core.Team;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

public class CustomNametagRenderer {
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Player player, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID playerUUID = player.getUUID();
        PlayerListEntry pData = ClientTeamData.getPlayerData(playerUUID);
        if (pData == null) return;

        int rank = pData.rank();
        String callsign = pData.callsign();
        boolean isDowned = pData.isDowned();
        int teamOrdinal = pData.teamOrdinal();

        Rank rankObj = Rank.fromOrdinal(rank);
        boolean isRussian = ClientTeamData.getLocalPlayerTeam() == Team.RUSSIA;
        String prefix = rankObj.getPrefix(isRussian);

        int color = teamOrdinal == 0 ? 0x4488FF : (teamOrdinal == 1 ? 0xFF4444 : 0x888888);
        if (isDowned) color = 0xFF4444;

        String display = prefix + " " + callsign;
        if (isDowned) display = "[X] " + display;

        poseStack.pushPose();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        double dist = dispatcher.distanceToSqr(player);
        if (dist > 4096) { // 64 blocks
            poseStack.popPose();
            return;
        }

        float scale = 0.026F;
        poseStack.translate(0, player.getBbHeight() + 0.5, 0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        poseStack.scale(-scale, -scale, scale);

        Matrix4f mat = poseStack.last().pose();
        var font = mc.font;
        int halfWidth = font.width(display) / 2;
        int bgColor = (int) (0.25 * 255) << 24;

        font.drawInBatch(display, -halfWidth, 0, color, false, mat, bufferSource, net.minecraft.client.gui.Font.DisplayMode.SEE_THROUGH, bgColor, packedLight);
        poseStack.popPose();
    }
}
