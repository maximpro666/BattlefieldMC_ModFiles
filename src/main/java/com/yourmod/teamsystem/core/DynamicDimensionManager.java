package com.yourmod.teamsystem.core;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class DynamicDimensionManager {

    private static final ResourceLocation MAP_DIMENSION_ID = new ResourceLocation("teamsystem", "map");
    private static final ResourceKey<Level> MAP_DIMENSION_KEY = ResourceKey.create(Registries.DIMENSION, MAP_DIMENSION_ID);

    public static ResourceKey<Level> getDimKey() {
        return MAP_DIMENSION_KEY;
    }
}
