package com.pigeostudios.pwp.client.gui.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.OptionalDouble;

public class RenderTypes extends RenderStateShard {

    private RenderTypes(String name, Runnable setup, Runnable clear) {
        super(name, setup, clear);
    }

    public static final RenderType LINES_DEPTH = RenderType.create(
        "lines_depth",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        256,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(ITEM_ENTITY_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .setDepthTestState(LEQUAL_DEPTH_TEST)
            .createCompositeState(false)
    );

    public static RenderType entityCutoutNoDepth(ResourceLocation tex) {
        return RenderType.create("entity_cutout_no_depth",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            true,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_CUTOUT_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(NO_DEPTH_TEST)
                .createCompositeState(false)
        );
    }

    public static RenderType eyesNoDepth(ResourceLocation tex) {
        return RenderType.create("eyes_no_depth",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_EYES_SHADER)
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setDepthTestState(NO_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(false)
        );
    }
}
