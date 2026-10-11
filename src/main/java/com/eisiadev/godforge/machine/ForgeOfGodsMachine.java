package com.eisiadev.godforge.machine;

import com.eisiadev.godforge.structure.GodforgeBlocks;
import com.eisiadev.godforge.structure.GodforgeLayout;
import com.eisiadev.godforge.structure.GodforgePatterns;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

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

    private int detectedRings = 1;
    private int unpoweredChecks = 0;
    private boolean editing = false;
    @Nullable
    private TickableSubscription tickSubscription;

    private static final int POWER_OFF_GRACE_CHECKS = 3;

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
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (tickSubscription != null) {
            unsubscribe(tickSubscription);
            tickSubscription = null;
        }
        unpoweredChecks = 0;
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
