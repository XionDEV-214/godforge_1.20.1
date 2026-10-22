package com.eisiadev.godforge;

import com.eisiadev.godforge.machine.ForgeOfGodsMachine;
import com.eisiadev.godforge.machine.ModuleOfGodsItem;
import com.eisiadev.godforge.machine.ModuleOfGodsMachine;
import com.eisiadev.godforge.structure.GodforgeBlocks;
import com.eisiadev.godforge.structure.GodforgeModules;
import com.eisiadev.godforge.structure.GodforgePatterns;

import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.model.generators.ModelFile;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GodforgeMachines {

    private GodforgeMachines() {}

    public static GTRegistrate REGISTRATE;

    public static MultiblockMachineDefinition FORGE_OF_THE_GODS;
    public static MultiblockMachineDefinition MODULE_OF_THE_GODS;

    public static void init() {
        FORGE_OF_THE_GODS = registerForgeOfTheGods();
        MODULE_OF_THE_GODS = registerModuleOfTheGods();
    }

    private static MultiblockMachineDefinition registerForgeOfTheGods() {
        return REGISTRATE
                .multiblock("forge_of_the_gods", ForgeOfGodsMachine::new)
                .langValue("Forge of the Gods")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeTypes(GodforgeRecipeTypes.modes())
                .recipeModifiers(
                        ForgeOfGodsMachine::moduleModifier,
                        ForgeOfGodsMachine::parallelModifier,
                        GTRecipeModifiers.BATCH_MODE,
                        ForgeOfGodsMachine::overclockModifier,
                        ForgeOfGodsMachine::moduleOverclockModifier)
                .appearanceBlock(GodforgeBlocks::shielding)
                .hasBER(true)
                .tooltips(
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.0"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.1"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.2"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.3"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.4"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.5"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.6"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.7"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.8"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.9"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.modules.0"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.modules.1"),
                        Component.translatable("block.godforge.forge_of_the_gods.tooltip.modules.2"))
                .pattern(GodforgePatterns::full)
                .register();
    }

    private static MultiblockMachineDefinition registerModuleOfTheGods() {
        return REGISTRATE
                .multiblock("module_of_the_gods", ModuleOfGodsMachine::new,
                        MetaMachineBlock::new, ModuleOfGodsItem::new, MetaMachineBlockEntity::new)
                .langValue("Module of the Gods")
                .rotationState(RotationState.NON_Y_AXIS)
                .recipeType(GTRecipeTypes.DUMMY_RECIPES)
                .appearanceBlock(GodforgeBlocks::shielding)
                .model((ctx, prov, builder) -> builder.forAllStatesModels(
                        state -> new ModelFile.UncheckedModelFile(GodforgeBlocks.SHIELDING_MODEL)))
                .tooltips(
                        Component.translatable("block.godforge.module_of_the_gods.tooltip.0"),
                        Component.translatable("block.godforge.module_of_the_gods.tooltip.1"),
                        Component.translatable("block.godforge.module_of_the_gods.tooltip.2"),
                        Component.translatable("block.godforge.module_of_the_gods.tooltip.3"),
                        Component.translatable("block.godforge.module_of_the_gods.tooltip.4"))
                .pattern(GodforgeModules::jeiPattern)
                .register();
    }
}
