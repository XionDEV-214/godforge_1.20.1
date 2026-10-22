package com.eisiadev.godforge.structure;

import com.gregtechceu.gtceu.common.data.GTBlocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.Nullable;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GodforgeBlocks {

    private GodforgeBlocks() {}

    public static final String STELLAR_ENERGY = "stellar_energy_casing";
    public static final String MAGNETIC_CONFINEMENT = "transcendentally_amplified_magnetic_confinement_casing";
    public static final String GUIDANCE = "celestial_matter_guidance_casing";
    public static final String SHIELDING = "singularity_reinforced_stellar_shielding_casing";
    public static final String GRAVITON_REMOTE = "godforge_graviton_casing_1";
    public static final String GRAVITON_MEDIAL = "godforge_graviton_casing_2";
    public static final String GRAVITON_CENTRAL = "godforge_graviton_casing_3";

    public static Block casing(String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("gtceu", path);
        if (!ForgeRegistries.BLOCKS.containsKey(id)) {
            throw new IllegalStateException("[Godforge] Block '" + id + "' is not registered. " +
                    "Check that godforge_casing.js (startup script) is loaded.");
        }
        return ForgeRegistries.BLOCKS.getValue(id);
    }

    public static Block shielding() { return casing(SHIELDING); }
    public static Block guidance() { return casing(GUIDANCE); }
    public static Block magnetic() { return casing(MAGNETIC_CONFINEMENT); }

    public static final String ETERNAL_COIL = "eternal_coil_block";

    public static Block moduleCore() { return casing(ETERNAL_COIL); }

    public static final ResourceLocation SHIELDING_MODEL = ResourceLocation.fromNamespaceAndPath("gtceu",
            "block/" + SHIELDING);

    @Nullable
    public static Block forModuleLetter(char letter) {
        return switch (letter) {
            case 'A' -> moduleCore();
            case 'B', 'C', '~' -> shielding();
            case 'D', 'E' -> guidance();
            case 'F' -> magnetic();
            case 'G' -> casing(STELLAR_ENERGY);
            default -> null;
        };
    }

    @Nullable
    public static Block forLetter(char letter) {
        return switch (letter) {
            case 'A', 'E' -> magnetic();
            case 'B', 'J' -> shielding();
            case 'C', 'D', 'P' -> guidance();
            case 'F' -> casing(STELLAR_ENERGY);
            case 'G' -> casing(GRAVITON_REMOTE);
            case 'K' -> casing(GRAVITON_MEDIAL);
            case 'I' -> casing(GRAVITON_CENTRAL);
            case 'H' -> GTBlocks.FUSION_GLASS.get();
            case 'L' -> Blocks.AIR;
            default -> null;
        };
    }
}
