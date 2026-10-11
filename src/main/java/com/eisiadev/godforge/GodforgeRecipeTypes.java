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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(GodforgeMod.MODID, "forge_of_the_gods");
        FORGE_OF_THE_GODS = new GTRecipeType(id, "forge_of_the_gods")
                .setMaxIOSize(16, 16, 16, 16)
                .setEUIO(IO.IN)
                .setProgressBar(GuiTextures.PROGRESS_BAR_FUSION, ProgressTexture.FillDirection.LEFT_TO_RIGHT)
                .setSound(GTSoundEntries.ARC);

        GTRegistries.RECIPE_TYPES.register(id, FORGE_OF_THE_GODS);
    }
}
