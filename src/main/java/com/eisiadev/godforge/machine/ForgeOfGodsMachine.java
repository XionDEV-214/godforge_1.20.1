package com.eisiadev.godforge.machine;

import com.eisiadev.godforge.GodforgeRecipeTypes;
import com.eisiadev.godforge.structure.GodforgeBlocks;
import com.eisiadev.godforge.structure.GodforgeLayout;
import com.eisiadev.godforge.structure.GodforgeModules;
import com.eisiadev.godforge.structure.GodforgePatterns;

import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.content.ContentModifier;
import com.gregtechceu.gtceu.api.recipe.modifier.ModifierFunction;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.recipe.modifier.RecipeModifier;
import com.gregtechceu.gtceu.common.data.GTRecipeModifiers;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ForgeOfGodsMachine extends WorkableElectricMultiblockMachine implements IMachineLife {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ForgeOfGodsMachine.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private boolean animating = false;

    @Persisted
    @DescSynced
    private int ringCount = 1;

    @Persisted
    private boolean flippedAtForm = false;

    @Persisted
    @DescSynced
    private int moduleCount = 0;

    private int detectedRings = 1;
    private int unpoweredChecks = 0;
    private boolean editing = false;
    @Nullable
    private TickableSubscription tickSubscription;

    private static final int POWER_OFF_GRACE_CHECKS = 3;

    private static final Set<ForgeOfGodsMachine> LOADED_SERVER = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ForgeOfGodsMachine> LOADED_CLIENT = Collections.newSetFromMap(new WeakHashMap<>());

    private static final RecipeModifier OVERCLOCK =
            GTRecipeModifiers.ELECTRIC_OVERCLOCK.apply(OverclockingLogic.NON_PERFECT_OVERCLOCK);

    public ForgeOfGodsMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public boolean isAnimating() {
        return animating;
    }

    public int getRingCount() {
        return ringCount;
    }

    public int getModuleCount() {
        return moduleCount;
    }

    @Override
    public BlockPattern getPattern() {
        return GodforgePatterns.get(getDefinition(), animating ? ringCount : 3, animating);
    }

    @Override
    public boolean checkPattern() {
        MultiblockMachineDefinition definition = getDefinition();
        if (animating) {
            return GodforgePatterns.get(definition, ringCount, true).checkPatternAt(getMultiblockState(), false);
        }
        for (int n = 3; n >= 1; n--) {
            if (GodforgePatterns.get(definition, n, false).checkPatternAt(getMultiblockState(), false)) {
                detectedRings = n;
                return true;
            }
        }
        return false;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        flippedAtForm = isFlipped();
        if (!animating) {
            ringCount = detectedRings;
        }
        if (!isRemote() && tickSubscription == null) {
            tickSubscription = subscribeServerTick(this::tickPower);
        }
        if (!isRemote()) {
            updateModules();
        }
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (tickSubscription != null) {
            unsubscribe(tickSubscription);
            tickSubscription = null;
        }
        unpoweredChecks = 0;
        if (!editing && moduleCount != 0) {
            moduleCount = 0;
            markDirty();
        }
        if (!editing && animating && getLevel() instanceof ServerLevel level) {
            restoreRingsAfterBreak(level);
        }
    }

    @Override
    public void onMachineRemoved() {
        if (!editing && animating && getLevel() instanceof ServerLevel level) {
            restoreRingsAfterBreak(level);
        }
    }

    private void tickPower() {
        if (isRemote() || getOffsetTimer() % 20 != 0 || !isFormed()) return;

        updateModules();

        boolean powered = hasPower();
        if (powered) {
            unpoweredChecks = 0;
            if (!animating) setAnimating(true);
        } else if (animating && ++unpoweredChecks >= POWER_OFF_GRACE_CHECKS) {
            unpoweredChecks = 0;
            setAnimating(false);
        }
    }

    private boolean hasPower() {
        EnergyContainerList containers = getEnergyContainer();
        return containers.getEnergyStored() > 0;
    }

    private void setAnimating(boolean on) {
        if (!(getLevel() instanceof ServerLevel level)) return;
        if (animating == on) return;
        if (!areRingChunksLoaded(level)) return;

        MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(level);
        mwsd.removeMapping(getMultiblockState());
        editing = true;
        try {
            placeRings(level, on);
        } finally {
            editing = false;
        }
        animating = on;
        markDirty();

        if (checkPatternWithLock()) {
            setFlipped(getMultiblockState().isNeededFlip());
            onStructureFormed();
            mwsd.addMapping(getMultiblockState());
        } else {
            setFlipped(false);
            onStructureInvalid();
            mwsd.addAsyncLogic(this);
        }
    }

    private void restoreRingsAfterBreak(ServerLevel level) {
        if (!areRingChunksLoaded(level)) return;
        editing = true;
        try {
            placeRings(level, false);
        } finally {
            editing = false;
        }
        animating = false;
        markDirty();
        MultiblockWorldSavedData mwsd = MultiblockWorldSavedData.getOrCreate(level);
        mwsd.removeMapping(getMultiblockState());
        mwsd.addAsyncLogic(this);
    }

    private void placeRings(ServerLevel level, boolean air) {
        Direction front = getFrontFacing();
        Direction upwards = getUpwardsFacing();
        boolean flipped = flippedAtForm;
        Direction back = RelativeDirection.BACK.getRelative(front, upwards, flipped);
        Direction up = RelativeDirection.UP.getRelative(front, upwards, flipped);
        Direction left = RelativeDirection.LEFT.getRelative(front, upwards, flipped);

        BlockPos origin = getPos();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Map<Character, BlockState> states = new HashMap<>();
        BlockState airState = Blocks.AIR.defaultBlockState();

        for (int ring = 1; ring <= ringCount; ring++) {
            for (GodforgeLayout.Cell cell : GodforgeLayout.ringCells(ring)) {
                BlockState target;
                if (air) {
                    target = airState;
                } else {
                    target = states.computeIfAbsent(cell.letter(), ch -> {
                        Block b = GodforgeBlocks.forLetter(ch);
                        return b == null ? airState : b.defaultBlockState();
                    });
                }
                pos.set(origin).move(back, cell.back()).move(up, -cell.down()).move(left, cell.left());
                if (level.getBlockState(pos) != target) {
                    level.setBlock(pos, target, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        loadedSet(isRemote()).add(this);
    }

    @Override
    public void onUnload() {
        super.onUnload();
        loadedSet(isRemote()).remove(this);
    }

    private static Set<ForgeOfGodsMachine> loadedSet(boolean client) {
        return client ? LOADED_CLIENT : LOADED_SERVER;
    }

    public static List<ForgeOfGodsMachine> loaded(Level level) {
        List<ForgeOfGodsMachine> list = new ArrayList<>();
        for (ForgeOfGodsMachine machine : loadedSet(level.isClientSide)) {
            if (machine.getLevel() == level && !machine.isInValid()) list.add(machine);
        }
        return list;
    }

    public GodforgeModules.Frame moduleFrame() {
        return GodforgeModules.Frame.of(getPos(), getFrontFacing(), getUpwardsFacing());
    }

    public void updateModules() {
        if (!(getLevel() instanceof ServerLevel level) || !isFormed()) return;
        int count = GodforgeModules.countModules(level, moduleFrame());
        if (count >= 0 && count != moduleCount) {
            moduleCount = count;
            markDirty();
        }
    }

    public static @NotNull ModifierFunction moduleModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof ForgeOfGodsMachine forge) || !forge.isFormed()) return ModifierFunction.IDENTITY;
        if (!GodforgeRecipeTypes.isExoticMode(recipe.getType())) return ModifierFunction.IDENTITY;
        int modules = forge.getModuleCount();
        if (modules <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .durationMultiplier(exoticDurationMultiplier(modules))
                .outputModifier(ContentModifier.multiplier(exoticOutputMultiplier(modules)))
                .build();
    }

    public static @NotNull ModifierFunction parallelModifier(@NotNull MetaMachine machine, @NotNull GTRecipe recipe) {
        if (!(machine instanceof ForgeOfGodsMachine forge) || !forge.isFormed()) return ModifierFunction.IDENTITY;
        int hatch = forge.getParallelHatch().map(IParallelHatch::getCurrentParallel).orElse(1);
        int limit = parallelLimit(hatch, recipe.getType(), forge.getModuleCount());
        int parallels = ParallelLogic.getParallelAmount(machine, recipe, limit);
        if (parallels <= 1) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .modifyAllContents(ContentModifier.multiplier(parallels))
                .eutMultiplier(parallels)
                .parallels(parallels)
                .build();
    }

    public static @NotNull ModifierFunction overclockModifier(@NotNull MetaMachine machine,
                                                             @NotNull GTRecipe recipe) {
        return OVERCLOCK.getModifier(machine, recipe);
    }

    public static @NotNull ModifierFunction moduleOverclockModifier(@NotNull MetaMachine machine,
                                                                   @NotNull GTRecipe recipe) {
        if (!(machine instanceof ForgeOfGodsMachine forge) || !forge.isFormed()) return ModifierFunction.IDENTITY;
        if (!GodforgeRecipeTypes.isGeneratedMode(recipe.getType())) return ModifierFunction.IDENTITY;
        int modules = forge.getModuleCount();
        if (modules <= 0) return ModifierFunction.IDENTITY;
        return ModifierFunction.builder()
                .durationMultiplier(moduleDurationMultiplier(modules))
                .build();
    }

    public static double moduleDurationMultiplier(int modules) {
        return Math.pow(2.0, -(double) modules / GodforgeModules.MAX_MODULES);
    }

    public static double moduleSpeedMultiplier(int modules) {
        return 1.0 / moduleDurationMultiplier(modules);
    }

    public static int parallelLimit(int hatchParallel, GTRecipeType type, int modules) {
        long limit = Math.max(1, hatchParallel);
        if (GodforgeRecipeTypes.isGeneratedMode(type)) {
            limit *= Math.max(1, modules);
        }
        return (int) Math.min(Integer.MAX_VALUE, limit);
    }

    public static double exoticDurationMultiplier(int modules) {
        return 1.0 + 0.5 * modules;
    }

    public static int exoticOutputMultiplier(int modules) {
        return 1 + modules;
    }

    private static String formatMultiplier(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        super.addDisplayText(textList);
        if (!isFormed()) return;
        textList.add(Component.translatable("godforge.multiblock.modules", moduleCount,
                GodforgeModules.MAX_MODULES));
        if (moduleCount < GodforgeModules.MAX_MODULES) {
            textList.add(Component.translatable("godforge.multiblock.modules.hint")
                    .withStyle(ChatFormatting.YELLOW));
        }
        GTRecipeType type = getRecipeType();
        if (GodforgeRecipeTypes.isExoticMode(type)) {
            textList.add(Component.translatable("godforge.multiblock.exotic_bonus",
                    formatMultiplier(exoticDurationMultiplier(moduleCount)), exoticOutputMultiplier(moduleCount)));
        } else if (GodforgeRecipeTypes.isGeneratedMode(type)) {
            int hatch = getParallelHatch().map(IParallelHatch::getCurrentParallel).orElse(1);
            textList.add(Component.translatable("godforge.multiblock.parallel_bonus",
                    Math.max(1, moduleCount), parallelLimit(hatch, type, moduleCount),
                    String.format("%.2f", moduleSpeedMultiplier(moduleCount))));
        }
    }

    private boolean areRingChunksLoaded(ServerLevel level) {
        BlockPos center = getStarCenterPos();
        int r = 64;
        return level.hasChunksAt(center.offset(-r, -r, -r), center.offset(r, r, r));
    }

    public BlockPos getStarCenterPos() {
        Direction back = RelativeDirection.BACK.getRelative(getFrontFacing(), getUpwardsFacing(), false);
        return getPos().relative(back, GodforgeLayout.STAR_DISTANCE);
    }
}
