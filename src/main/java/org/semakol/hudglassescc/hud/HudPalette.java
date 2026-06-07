package org.semakol.hudglassescc.hud;

public final class HudPalette {
    private HudPalette() {}

    public static final int[] DEFAULT = new int[]{
            0xF0F0F0, // 0  white
            0xF2B233, // 1  orange
            0xE57FD8, // 2  magenta
            0x99B2F2, // 3  light blue
            0xDEDE6C, // 4  yellow
            0x7FCC19, // 5  lime
            0xF2B2CC, // 6  pink
            0x4C4C4C, // 7  gray
            0x999999, // 8  light gray
            0x4C99B2, // 9  cyan
            0xB266E5, // 10 purple
            0x3366CC, // 11 blue
            0x7F664C, // 12 brown
            0x57A64E, // 13 green
            0xCC4C4C, // 14 red
            0x111111  // 15 black
    };

    public static char colorToChar(int idx) {
        return idx < 10 ? (char) ('0' + idx) : (char) ('a' + idx - 10);
    }

    public static int parseHex(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
