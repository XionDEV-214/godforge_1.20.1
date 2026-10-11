package com.eisiadev.godforge;

import com.eisiadev.godforge.machine.ForgeOfGodsMachine;
import com.eisiadev.godforge.structure.GodforgeBlocks;
import com.eisiadev.godforge.structure.GodforgePatterns;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GodforgeMachines {

    private GodforgeMachines() {}

    public static GTRegistrate REGISTRATE;

    public static MultiblockMachineDefinition FORGE_OF_THE_GODS;

    public static void init() {
        FORGE_OF_THE_GODS = registerForgeOfTheGods();
    }

    private static MultiblockMachineDefinition registerForgeOfTheGods() {
        return REGISTRATE
                .multiblock("forge_of_the_gods", ForgeOfGodsMachine::new)
                .langValue("Forge of the Gods")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GodforgeRecipeTypes.FORGE_OF_THE_GODS)
                .recipeModifiers(
                        GTRecipeModifiers.PARALLEL_HATCH,
                        GTRecipeModifiers.BATCH_MODE,
                        GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.NON_PERFECT_OVERCLOCK))
                .appearanceBlock(GodforgeBlocks::shielding)
                .hasBER(true)
                .tooltips(
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.0"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.1"))
                .pattern(GodforgePatterns::full)
                .register();
    }
}
