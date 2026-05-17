package com.yourmod.teamsystem.client.gui.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourmod.teamsystem.client.ClientTeamData;
import com.yourmod.teamsystem.client.PlayerListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4f;

import java.util.UUID;

public class CustomNametagRenderer {
    public static void render(PoseStack poseStack, MultiBufferSource bufferSource, Player player, int packedLight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        UUID uuid = player.getUUID();
        PlayerListEntry entry = ClientTeamData.playerDataMap.get(uuid);
        if (entry == null) return;

        double dist = mc.player.distanceToSqr(player);
        if (dist > 4096.0) return;

        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        float height = player.getBbHeight() + 0.5f;

        Font font = mc.font;
        String rankLine = entry.rank() != null ? entry.rank() : "";
        String callsignLine = entry.callsign() != null ? entry.callsign() : player.getScoreboardName();
        String healthLine = String.format("%.0f\u2764", player.getHealth());

        float maxWidth = Math.max(font.width(rankLine), Math.max(font.width(callsignLine), font.width(healthLine)));
        float pad = 2.0f;
        float bw = maxWidth + pad * 2;
        float bh = (font.lineHeight + 1) * 3 + pad * 2;
        float bx = -bw / 2;
        float by = -bh;

        poseStack.pushPose();
        poseStack.translate(0, height, 0);
        poseStack.mulPose(dispatcher.cameraOrientation());
        float s = 0.025f;
        poseStack.scale(-s, -s, s);

        Matrix4f mat = poseStack.last().pose();
        int bgAlpha = 140 << 24;

        VertexConsumer builder = bufferSource.getBuffer(RenderType.textBackground());
        builder.vertex(mat, bx, by + bh, 0).color(0.1f, 0.1f, 0.1f, 0.7f).endVertex();
        builder.vertex(mat, bx + bw, by + bh, 0).color(0.1f, 0.1f, 0.1f, 0.7f).endVertex();
        builder.vertex(mat, bx + bw, by, 0).color(0.1f, 0.1f, 0.1f, 0.7f).endVertex();
        builder.vertex(mat, bx, by, 0).color(0.1f, 0.1f, 0.1f, 0.7f).endVertex();

        font.drawInBatch(rankLine, bx + pad, by + pad, 0xFFFFAA, false, mat, bufferSource, Font.DisplayMode.SEE_THROUGH, bgAlpha, packedLight);
        font.drawInBatch(callsignLine, bx + pad, by + pad + font.lineHeight + 1, 0xFFFFFFFF, false, mat, bufferSource, Font.DisplayMode.SEE_THROUGH, bgAlpha, packedLight);
        font.drawInBatch(healthLine, bx + pad, by + pad + (font.lineHeight + 1) * 2, 0xFFFF5555, false, mat, bufferSource, Font.DisplayMode.SEE_THROUGH, bgAlpha, packedLight);

        poseStack.popPose();
    }
}
