package com.eisiadev.godforge.structure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GodforgeLayout {

    private GodforgeLayout() {}

    public enum Ring { ABSENT, SOLID, AIR }

    public static final int AISLES = 187, ROWS = 29, COLS = 127;
    public static final int CTRL_AISLE = 1, CTRL_ROW = 14, CTRL_COL = 63;
    public static final int STAR_DISTANCE = 122;

    private static final int[][] OFFSETS = {
            { 63, 14, 1 },
            { 63, 14, -59 },
            { 55, 11, -67 },
            { 47, 13, -76 }
    };

    public static String[][] ringSource(int ring) {
        return switch (ring) {
            case 1 -> ForgeOfGodsStructureString.FIRST_RING;
            case 2 -> ForgeOfGodsRingsStructureString.SECOND_RING;
            case 3 -> ForgeOfGodsRingsStructureString.THIRD_RING;
            default -> throw new IllegalArgumentException("ring " + ring);
        };
    }

    public static int[] ringOffset(int ring) {
        return OFFSETS[ring];
    }

    private static int gridAisle(int ring, int a) { return CTRL_AISLE - OFFSETS[ring][2] + a; }
    private static int gridRow(int ring, int r) { return CTRL_ROW - OFFSETS[ring][1] + r; }
    private static int gridCol(int ring, int c) { return CTRL_COL - OFFSETS[ring][0] + c; }

    public static char[][][] build(Ring r1, Ring r2, Ring r3) {
        if (r1 == Ring.ABSENT) throw new IllegalArgumentException("ring 1 is mandatory");
        char[][][] g = new char[AISLES][ROWS][COLS];
        for (char[][] a : g) for (char[] r : a) java.util.Arrays.fill(r, ' ');

        place(g, 0, ForgeOfGodsStructureString.BEAM_SHAFT, Ring.SOLID);
        place(g, 1, ForgeOfGodsStructureString.FIRST_RING, r1);
        if (r2 != Ring.ABSENT) place(g, 2, ForgeOfGodsRingsStructureString.SECOND_RING, r2);
        if (r3 != Ring.ABSENT) place(g, 3, ForgeOfGodsRingsStructureString.THIRD_RING, r3);

        for (int a = 0; a < AISLES; a++) {
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    char ch = g[a][r][c];
                    if (ch == '~') g[a][r][c] = 'S';
                    else if (ch == 'D' && a == 0) g[a][r][c] = 'P';
                }
            }
        }
        return g;
    }

    private static void place(char[][][] g, int piece, String[][] src, Ring state) {
        for (int a = 0; a < src.length; a++) {
            for (int r = 0; r < src[a].length; r++) {
                String row = src[a][r];
                for (int c = 0; c < row.length(); c++) {
                    char ch = row.charAt(c);
                    if (ch == ' ') continue;
                    int ga = gridAisle(piece, a), gr = gridRow(piece, r), gc = gridCol(piece, c);
                    if (g[ga][gr][gc] != ' ') {
                        throw new IllegalStateException("piece " + piece + " overlaps at " + ga + "," + gr + "," + gc);
                    }
                    g[ga][gr][gc] = (state == Ring.AIR) ? 'L' : ch;
                }
            }
        }
    }

    public static String[] aisleRows(char[][][] grid, int aisle) {
        String[] rows = new String[ROWS];
        for (int r = 0; r < ROWS; r++) rows[r] = new String(grid[aisle][r]);
        return rows;
    }

    public record Cell(int back, int down, int left, char letter) {}

    private static final Map<Integer, List<Cell>> RING_CELLS = new HashMap<>();

    public static synchronized List<Cell> ringCells(int ring) {
        List<Cell> cached = RING_CELLS.get(ring);
        if (cached == null) {
            String[][] src = ringSource(ring);
            List<Cell> out = new ArrayList<>();
            for (int a = 0; a < src.length; a++) {
                for (int r = 0; r < src[a].length; r++) {
                    String row = src[a][r];
                    for (int c = 0; c < row.length(); c++) {
                        char ch = row.charAt(c);
                        if (ch == ' ') continue;
                        out.add(new Cell(gridAisle(ring, a) - CTRL_AISLE, gridRow(ring, r) - CTRL_ROW,
                                gridCol(ring, c) - CTRL_COL, ch));
                    }
                }
            }
            RING_CELLS.put(ring, out);
            cached = out;
        }
        return cached;
    }
}
