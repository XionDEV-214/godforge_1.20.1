package com.eisiadev.godforge.structure;

import com.eisiadev.godforge.structure.GodforgeLayout.Ring;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GodforgePatterns {

    private GodforgePatterns() {}

    private static final BlockPattern[] SOLID = new BlockPattern[4];
    private static final BlockPattern[] AIR = new BlockPattern[4];

    public static synchronized BlockPattern get(MultiblockMachineDefinition definition, int rings, boolean animate) {
        BlockPattern[] cache = animate ? AIR : SOLID;
        if (cache[rings] == null) {
            Ring state = animate ? Ring.AIR : Ring.SOLID;
            Ring r2 = rings >= 2 ? state : Ring.ABSENT;
            Ring r3 = rings >= 3 ? state : Ring.ABSENT;
            cache[rings] = build(definition, state, r2, r3);
        }
        return cache[rings];
    }

    public static BlockPattern full(MultiblockMachineDefinition definition) {
        return get(definition, 3, false);
    }

    private static BlockPattern build(MultiblockMachineDefinition definition, Ring r1, Ring r2, Ring r3) {
        char[][][] grid = GodforgeLayout.build(r1, r2, r3);

        FactoryBlockPattern builder = FactoryBlockPattern.start(
                RelativeDirection.LEFT, RelativeDirection.DOWN, RelativeDirection.BACK);
        for (int a = 0; a < GodforgeLayout.AISLES; a++) {
            builder.aisle(GodforgeLayout.aisleRows(grid, a));
        }

        TraceabilityPredicate ports = Predicates.autoAbilities(definition.getRecipeTypes())
                .or(Predicates.autoAbilities(false, false, true));

        builder.where('S', Predicates.controller(Predicates.blocks(definition.getBlock())))
                .where('A', Predicates.blocks(GodforgeBlocks.magnetic()).or(ports))
                .where('P', Predicates.blocks(GodforgeBlocks.guidance()).or(ports))
                .where('L', Predicates.air());
        for (char letter : "BCDEFGHIJK".toCharArray()) {
            builder.where(letter, Predicates.blocks(GodforgeBlocks.forLetter(letter)));
        }
        return builder.build();
    }
}
