package com.eisiadev.godforge.structure;

import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.Predicates;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GodforgeModules {

    private GodforgeModules() {}

    public static final int MAX_MODULES = 16;

    public static final String[][] SHAPE = {
            { "       ", "  BBB  ", " BBBBB ", " BB~BB ", " BBBBB ", "  BBB  ", "       " },
            { "  CCC  ", " CFFFC ", "CFFFFFC", "CFFFFFC", "CFFFFFC", " CFFFC ", "  CCC  " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   G   ", "       ", "       ", "       " } };

    private static final int CENTER = 3;

    public enum Outward { UP, DOWN, LEFT, RIGHT }

    public enum Status { COMPLETE, INCOMPLETE, UNLOADED }

    public record Slot(int back, int down, int left, Outward outward) {}

    public record Cell(int depth, int a, int b, char letter) {}

    public record Frame(BlockPos controller, Direction back, Direction up, Direction left) {

        public static Frame of(BlockPos controller, Direction front, Direction upwards) {
            return new Frame(controller,
                    RelativeDirection.BACK.getRelative(front, upwards, false),
                    RelativeDirection.UP.getRelative(front, upwards, false),
                    RelativeDirection.LEFT.getRelative(front, upwards, false));
        }
    }

    public record Site(int index, BlockPos anchor, Direction out, Direction axisA, Direction axisB) {

        public BlockPos cellPos(Cell cell) {
            return anchor.relative(out, cell.depth()).relative(axisA, cell.a()).relative(axisB, cell.b());
        }

        public boolean contains(BlockPos pos) {
            if (pos.equals(anchor)) return true;
            for (Cell cell : cells()) {
                if (cellPos(cell).equals(pos)) return true;
            }
            return false;
        }
    }

    public record BuildResult(int placed, int obstructed, Map<Block, Integer> missing) {}

    private static List<Slot> slots;
    private static List<Cell> cells;

    public static synchronized List<Slot> slots() {
        if (slots == null) {
            List<Slot> list = new ArrayList<>();
            String[][] shaft = ForgeOfGodsStructureString.BEAM_SHAFT;
            int[] offset = GodforgeLayout.ringOffset(0);
            for (int a = 0; a < shaft.length; a++) {
                for (int r = 0; r < shaft[a].length; r++) {
                    String row = shaft[a][r];
                    for (int c = 0; c < row.length(); c++) {
                        if (row.charAt(c) != 'J') continue;
                        int back = a - offset[2];
                        int down = r - offset[1];
                        int left = c - offset[0];
                        Outward out = Math.abs(down) >= Math.abs(left) ?
                                (down < 0 ? Outward.UP : Outward.DOWN) :
                                (left > 0 ? Outward.LEFT : Outward.RIGHT);
                        list.add(new Slot(back, down, left, out));
                    }
                }
            }
            slots = Collections.unmodifiableList(list);
        }
        return slots;
    }

    public static synchronized List<Cell> cells() {
        if (cells == null) {
            List<Cell> list = new ArrayList<>();
            for (int d = 0; d < SHAPE.length; d++) {
                for (int r = 0; r < SHAPE[d].length; r++) {
                    String row = SHAPE[d][r];
                    for (int c = 0; c < row.length(); c++) {
                        char ch = row.charAt(c);
                        if (ch == ' ' || ch == '~') continue;
                        list.add(new Cell(d, r - CENTER, c - CENTER, ch));
                    }
                }
            }
            cells = Collections.unmodifiableList(list);
        }
        return cells;
    }

    public static List<Site> sites(Frame frame) {
        List<Site> list = new ArrayList<>(MAX_MODULES);
        List<Slot> all = slots();
        for (int i = 0; i < all.size(); i++) {
            Slot slot = all.get(i);
            BlockPos anchor = frame.controller().relative(frame.back(), slot.back())
                    .relative(frame.up(), -slot.down()).relative(frame.left(), slot.left());
            Direction out = switch (slot.outward()) {
                case UP -> frame.up();
                case DOWN -> frame.up().getOpposite();
                case LEFT -> frame.left();
                case RIGHT -> frame.left().getOpposite();
            };
            Direction axisA = (slot.outward() == Outward.UP || slot.outward() == Outward.DOWN) ?
                    frame.left() : frame.up();
            list.add(new Site(i, anchor, out, axisA, frame.back()));
        }
        return list;
    }

    public static Block expected(char letter) {
        Block block = GodforgeBlocks.forModuleLetter(letter);
        if (block == null) throw new IllegalArgumentException("module letter " + letter);
        return block;
    }

    public static Status status(Level level, Site site) {
        if (!level.isLoaded(site.anchor())) return Status.UNLOADED;
        if (!level.getBlockState(site.anchor()).is(GodforgeBlocks.shielding())) return Status.INCOMPLETE;
        for (Cell cell : cells()) {
            BlockPos pos = site.cellPos(cell);
            if (!level.isLoaded(pos)) return Status.UNLOADED;
            if (!level.getBlockState(pos).is(expected(cell.letter()))) return Status.INCOMPLETE;
        }
        return Status.COMPLETE;
    }

    public static int countModules(Level level, Frame frame) {
        int count = 0;
        for (Site site : sites(frame)) {
            Status status = status(level, site);
            if (status == Status.UNLOADED) return -1;
            if (status == Status.COMPLETE) count++;
        }
        return Math.min(count, MAX_MODULES);
    }

    public static BuildResult build(Level level, Player player, Site site) {
        int placed = 0;
        int obstructed = 0;
        Map<Block, Integer> missing = new LinkedHashMap<>();
        boolean creative = player.getAbilities().instabuild;
        for (Cell cell : cells()) {
            BlockPos pos = site.cellPos(cell);
            Block block = expected(cell.letter());
            BlockState current = level.getBlockState(pos);
            if (current.is(block)) continue;
            if (!current.canBeReplaced() || !level.mayInteract(player, pos)) {
                obstructed++;
                continue;
            }
            if (!creative && !takeOne(player.getInventory(), block.asItem())) {
                missing.merge(block, 1, Integer::sum);
                continue;
            }
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
            placed++;
        }
        return new BuildResult(placed, obstructed, missing);
    }

    private static boolean takeOne(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.is(item)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    public static BlockPattern jeiPattern(MultiblockMachineDefinition definition) {
        FactoryBlockPattern builder = FactoryBlockPattern.start(
                RelativeDirection.RIGHT, RelativeDirection.BACK, RelativeDirection.UP);
        for (String[] aisle : SHAPE) {
            builder.aisle(aisle);
        }
        builder.where('~', Predicates.controller(Predicates.blocks(definition.getBlock())));
        for (char ch : "ABCDEFG".toCharArray()) {
            builder.where(ch, Predicates.blocks(expected(ch)));
        }
        return builder.build();
    }
}
