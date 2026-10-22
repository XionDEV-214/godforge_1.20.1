package com.eisiadev.godforge.machine;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ModuleOfGodsMachine extends MultiblockControllerMachine {

    public ModuleOfGodsMachine(IMachineBlockEntity holder) {
        super(holder);
    }
}
