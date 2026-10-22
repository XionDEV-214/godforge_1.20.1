package com.eisiadev.godforge;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

public final class GodforgeRecipeTypes {

    private GodforgeRecipeTypes() {}

    public static GTRecipeType SMELTERY;
    public static GTRecipeType PLASMAFY;
    public static GTRecipeType DEPLASMAFY;
    public static GTRecipeType FUSION;
    public static GTRecipeType EXOTICIZER;
    public static GTRecipeType FORGE_OF_THE_GODS;

    public static void init() {
        SMELTERY = GTRecipeTypes.register("godforge_smeltery", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(1, 3, 0, 0)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.FURNACE);

        PLASMAFY = GTRecipeTypes.register("godforge_plasmafy", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(0, 0, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);

        DEPLASMAFY = GTRecipeTypes.register("godforge_deplasmafy", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(0, 0, 1, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_ARROW, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.COOLING);

        FUSION = GTRecipeTypes.register("godforge_fusion", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(0, 0, 2, 1)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_FUSION, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC)
                .setOffsetVoltageText(true)
                .setMaxTooltips(4);

        EXOTICIZER = GTRecipeTypes.register("godforge_exoticizer", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(9, 9, 9, 9)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_REPLICATOR, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR);

        FORGE_OF_THE_GODS = GTRecipeTypes.register("forge_of_the_gods", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(9, 9, 9, 9)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_REPLICATOR, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR)
                .setXEIVisible(false);
    }

    public static GTRecipeType[] modes() {
        return new GTRecipeType[] { SMELTERY, PLASMAFY, DEPLASMAFY, FUSION, EXOTICIZER };
    }

    public static boolean isGeneratedMode(GTRecipeType type) {
        return type == SMELTERY || type == PLASMAFY || type == DEPLASMAFY || type == FUSION;
    }

    public static boolean isExoticMode(GTRecipeType type) {
        return type == EXOTICIZER;
    }
}
