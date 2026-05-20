package com.yourmod.teamsystem.client.gui.scoreboard.data;

import net.minecraft.resources.ResourceLocation;

public class RankDefinition {
    private static final ResourceLocation RANK_SPRITESHEET = new ResourceLocation("teamsystem", "textures/gui/ranks/ranks.png");

    private static final RankDefinition[] RANKS = {
        new RankDefinition("Pvt", "Private", 0xFF506070, 0),
        new RankDefinition("PFC", "Private First Class", 0xFF586878, 1),
        new RankDefinition("Cpl", "Corporal", 0xFF607080, 2),
        new RankDefinition("Sgt", "Sergeant", 0xFF708090, 3),
        new RankDefinition("SSgt", "Staff Sergeant", 0xFF7890A0, 4),
        new RankDefinition("Lt", "Lieutenant", 0xFFA080E0, 5),
        new RankDefinition("Cpt", "Captain", 0xFFB890E8, 6),
        new RankDefinition("Maj", "Major", 0xFFC8A0F0, 7),
        new RankDefinition("Col", "Colonel", 0xFFE8750A, 8),
        new RankDefinition("Gen", "General", 0xFFC8A050, 9)
    };

    public final String shortName;
    public final String fullName;
    public final ResourceLocation iconTexture;
    public final int color;
    public final int sortWeight;

    public RankDefinition(String shortName, String fullName, int color, int sortWeight) {
        this.shortName = shortName;
        this.fullName = fullName;
        this.iconTexture = RANK_SPRITESHEET;
        this.color = color;
        this.sortWeight = sortWeight;
    }

    public static RankDefinition get(int rankId) {
        if (rankId >= 0 && rankId < RANKS.length) {
            return RANKS[rankId];
        }
        return RANKS[0];
    }

    public int getIconVOffset() {
        return sortWeight * 16;
    }

    public static int count() {
        return RANKS.length;
    }
}
