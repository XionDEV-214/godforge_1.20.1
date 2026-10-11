package com.eisiadev.godforge;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.common.data.GTSoundEntries;

import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;

import net.minecraft.resources.ResourceLocation;

public final class GodforgeRecipeTypes {

    private GodforgeRecipeTypes() {}

    public static GTRecipeType FORGE_OF_THE_GODS;

    public static void init() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gtceu", "forge_of_the_gods");
        FORGE_OF_THE_GODS = new GTRecipeType(id, "forge_of_the_gods")
                .setMaxIOSize(9, 9, 9, 9)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_REPLICATOR, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.REPLICATOR);

        GTRegistries.RECIPE_TYPES.register(id, FORGE_OF_THE_GODS);
    }
}
