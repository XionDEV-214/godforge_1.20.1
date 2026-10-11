package com.eisiadev.godforge.structure;

public final class StructureUtil {

    private StructureUtil() {}

    public static String[][] appendStringArrays(String[][] a, String[][] b) {
        String[][] out = new String[a.length + b.length][];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static String[][] replaceLetters(String[][] source, String replacement) {
        char c = replacement.charAt(0);
        String[][] out = new String[source.length][];
        for (int a = 0; a < source.length; a++) {
            out[a] = new String[source[a].length];
            for (int r = 0; r < source[a].length; r++) {
                char[] row = source[a][r].toCharArray();
                for (int i = 0; i < row.length; i++) {
                    if (row[i] != ' ') row[i] = c;
                }
                out[a][r] = new String(row);
            }
        }
        return out;
    }
}
