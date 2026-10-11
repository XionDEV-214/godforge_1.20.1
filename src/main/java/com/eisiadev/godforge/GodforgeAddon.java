package com.eisiadev.godforge;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@GTAddon
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GodforgeAddon implements IGTAddon {

    @Override
    public GTRegistrate getRegistrate() {
        return GodforgeMachines.REGISTRATE;
    }

    @Override
    public void initializeAddon() {
        GodforgeMod.LOGGER.info("[Godforge] Addon initialized.");
    }

    @Override
    public String addonModId() {
        return GodforgeMod.MODID;
    }
}
