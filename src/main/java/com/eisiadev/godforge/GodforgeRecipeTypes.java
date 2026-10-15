package com.eisiadev.godforge;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

public final class GodforgeRecipeTypes {

    private GodforgeRecipeTypes() {}

    public static GTRecipeType FORGE_OF_THE_GODS;

    public static void init() {
        FORGE_OF_THE_GODS = GTRecipeTypes.register("forge_of_the_gods", GTRecipeTypes.MULTIBLOCK)
                .setMaxIOSize(9, 9, 9, 9)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_REPLICATOR, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR);
    }
}