package com.eisiadev.godforge;

import com.eisiadev.godforge.recipe.GodforgeRecipeGenerator;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(GodforgeMod.MODID)
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GodforgeMod {

    public static final String MODID = "godforge";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public GodforgeMod(FMLJavaModLoadingContext context) {
        LOGGER.info("[Godforge] Loading...");

        GodforgeMachines.REGISTRATE = GTRegistrate.create(MODID);

        IEventBus modBus = context.getModEventBus();
        GodforgeMachines.REGISTRATE.registerRegistrate();

        modBus.addGenericListener(GTRecipeType.class,
                (GTCEuAPI.RegisterEvent<ResourceLocation, GTRecipeType> event) ->
                        GodforgeRecipeTypes.init());

        modBus.addGenericListener(MachineDefinition.class,
                (GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) ->
                        GodforgeMachines.init());

        modBus.addListener(this::onBuildCreativeTab);

        MinecraftForge.EVENT_BUS.addListener(GodforgeRecipeGenerator::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(GodforgeRecipeGenerator::onTagsUpdated);

        if (FMLEnvironment.dist.isClient()) {
            com.eisiadev.godforge.client.GodforgeClient.init(modBus);
        }

        LOGGER.info("[Godforge] Loading complete.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private void onBuildCreativeTab(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation gtMachineTabId = ResourceLocation.fromNamespaceAndPath("gtceu", "machine");
        if (!event.getTabKey().location().equals(gtMachineTabId)) return;
        if (GodforgeMachines.FORGE_OF_THE_GODS != null) {
            event.accept(GodforgeMachines.FORGE_OF_THE_GODS.getItem());
        }
        if (GodforgeMachines.MODULE_OF_THE_GODS != null) {
            event.accept(GodforgeMachines.MODULE_OF_THE_GODS.getItem());
        }
    }
}
