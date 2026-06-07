package org.semakol.hudglassescc.hud;

import org.semakol.hudglassescc.network.HudUpdatePayload;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side terminal buffer. Char + fg + bg per cell.
 * bg value -1 means transparent for that cell.
 *
 * Notes on dirty semantics:
 *  - The "current" pen colors (curFg/curBg) are stored on the buffer but are
 *    NOT shipped in the payload — only per-cell colors are. So changing the
 *    pen color alone does not mark the buffer dirty; the next write/blit will.
 *  - Payloads are cached and invalidated on markDirty(), so repeated reads of
 *    the same buffer state (e.g. resending to a player who just bound) reuse
 *    the same array allocations instead of rebuilding them every tick.
 */
public class HudBuffer {
    public static final int TRANSPARENT = -1;

    public final int width;
    public final int height;
    public final char[][] chars;
    public final byte[][] fg;
    public final byte[][] bg;
    public final int[] palette;

    public int cursorX = 0;
    public int cursorY = 0;
    private byte curFg = 0;       // white
    private byte curBg = TRANSPARENT;
    private boolean cursorBlink = false;

    private volatile boolean dirty = true;
    private volatile HudUpdatePayload cachedPayload;
    /** Monotonic content version. Bumped on every mutation; lets per-viewer
     *  delivery tracking know when a frame changed without a shared dirty flag. */
    private final AtomicLong version = new AtomicLong(1);

    public HudBuffer(int width, int height) {
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
        this.chars = new char[this.height][this.width];
        this.fg = new byte[this.height][this.width];
        this.bg = new byte[this.height][this.width];
        this.palette = HudPalette.DEFAULT.clone();
        clear();
    }

    public boolean isDirty() { return dirty; }

    public void markDirty() {
        dirty = true;
        cachedPayload = null;
        version.incrementAndGet();
    }

    public void clearDirty() { dirty = false; }

    /** Current content version. Changes whenever the buffer is mutated. */
    public long getVersion() { return version.get(); }

    public void clear() {
        for (int y = 0; y < height; y++) {
            Arrays.fill(chars[y], ' ');
            Arrays.fill(fg[y], curFg);
            Arrays.fill(bg[y], curBg);
        }
        markDirty();
    }

    public void clearLine() {
        if (cursorY < 0 || cursorY >= height) return;
        Arrays.fill(chars[cursorY], ' ');
        Arrays.fill(fg[cursorY], curFg);
        Arrays.fill(bg[cursorY], curBg);
        markDirty();
    }

    public void setCursorPos(int x, int y) {
        cursorX = x;
        cursorY = y;
    }

    public void setTextColor(int idx) {
        // Pen-only change; does not affect rendered state until next write.
        curFg = (byte) Math.max(0, Math.min(15, idx));
    }

    public int getTextColor() { return curFg & 0xFF; }

    public void setBackgroundColor(int idx) {
        // Pen-only change; does not affect rendered state until next write.
        if (idx < 0) curBg = TRANSPARENT;
        else curBg = (byte) Math.max(0, Math.min(15, idx));
    }

    public int getBackgroundColor() { return curBg; }

    public boolean getCursorBlink() { return cursorBlink; }

    public void setCursorBlink(boolean blink) { this.cursorBlink = blink; }

    public void write(String text) {
        if (text == null || text.isEmpty()) return;
        if (cursorY < 0 || cursorY >= height) {
            cursorX += text.length();
            return;
        }
        int y = cursorY;
        char[] cRow = chars[y];
        byte[] fRow = fg[y];
        byte[] bRow = bg[y];
        byte cf = curFg;
        byte cb = curBg;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            int x = cursorX + i;
            if (x < 0) continue;
            if (x >= width) break;
            cRow[x] = CharMap.remap(text.charAt(i));
            fRow[x] = cf;
            bRow[x] = cb;
        }
        cursorX += len;
        markDirty();
    }

    public void blit(byte[] text, byte[] fgStr, byte[] bgStr) {
        if (text == null) return;
        int n = text.length;
        if (fgStr == null || bgStr == null || fgStr.length != n || bgStr.length != n) return;
        if (cursorY < 0 || cursorY >= height) { cursorX += n; return; }
        int y = cursorY;
        char[] cRow = chars[y];
        byte[] fRow = fg[y];
        byte[] bRow = bg[y];
        for (int i = 0; i < n; i++) {
            int x = cursorX + i;
            if (x < 0) continue;
            if (x >= width) break;
            cRow[x] = CharMap.remap((char) (text[i] & 0xFF));
            int fi = HudPalette.parseHex((char) (fgStr[i] & 0xFF));
            int bi = HudPalette.parseHex((char) (bgStr[i] & 0xFF));
            if (fi >= 0) fRow[x] = (byte) fi;
            bRow[x] = (byte) (bi >= 0 ? bi : TRANSPARENT);
        }
        cursorX += n;
        markDirty();
    }

    public void blit(String text, String fgStr, String bgStr) {
        if (text == null) return;
        int n = text.length();
        if (fgStr == null || bgStr == null || fgStr.length() != n || bgStr.length() != n) return;
        if (cursorY < 0 || cursorY >= height) { cursorX += n; return; }
        int y = cursorY;
        char[] cRow = chars[y];
        byte[] fRow = fg[y];
        byte[] bRow = bg[y];
        for (int i = 0; i < n; i++) {
            int x = cursorX + i;
            if (x < 0) continue;
            if (x >= width) break;
            cRow[x] = CharMap.remap(text.charAt(i));
            int fi = HudPalette.parseHex(fgStr.charAt(i));
            int bi = HudPalette.parseHex(bgStr.charAt(i));
            if (fi >= 0) fRow[x] = (byte) fi;
            bRow[x] = (byte) (bi >= 0 ? bi : TRANSPARENT);
        }
        cursorX += n;
        markDirty();
    }

    public void scroll(int n) {
        if (n == 0) return;
        if (n >= height || -n >= height) {
            // Scrolled past the buffer — equivalent to clear.
            clear();
            return;
        }
        if (n > 0) {
            // Scroll up (content moves up, blank rows appear at bottom).
            for (int y = 0; y < height - n; y++) {
                System.arraycopy(chars[y + n], 0, chars[y], 0, width);
                System.arraycopy(fg[y + n], 0, fg[y], 0, width);
                System.arraycopy(bg[y + n], 0, bg[y], 0, width);
            }
            for (int y = height - n; y < height; y++) {
                Arrays.fill(chars[y], ' ');
                Arrays.fill(fg[y], curFg);
                Arrays.fill(bg[y], curBg);
            }
        } else {
            int s = -n;
            for (int y = height - 1; y >= s; y--) {
                System.arraycopy(chars[y - s], 0, chars[y], 0, width);
                System.arraycopy(fg[y - s], 0, fg[y], 0, width);
                System.arraycopy(bg[y - s], 0, bg[y], 0, width);
            }
            for (int y = 0; y < s; y++) {
                Arrays.fill(chars[y], ' ');
                Arrays.fill(fg[y], curFg);
                Arrays.fill(bg[y], curBg);
            }
        }
        markDirty();
    }

    public void setPaletteColor(int idx, int rgb) {
        if (idx < 0 || idx > 15) return;
        int newRgb = rgb & 0xFFFFFF;
        if (palette[idx] == newRgb) return;
        palette[idx] = newRgb;
        markDirty();
    }

    public int getPaletteColor(int idx) {
        if (idx < 0 || idx > 15) return 0;
        return palette[idx];
    }

    public HudUpdatePayload toPayload() {
        HudUpdatePayload local = cachedPayload;
        if (local != null) return local;

        String[] lines = new String[height];
        String[] fgs = new String[height];
        String[] bgs = new String[height];
        char[] cl = new char[width];
        char[] fl = new char[width];
        char[] bl = new char[width];
        for (int y = 0; y < height; y++) {
            char[] cRow = chars[y];
            byte[] fRow = fg[y];
            byte[] bRow = bg[y];
            for (int x = 0; x < width; x++) {
                cl[x] = cRow[x];
                fl[x] = HudPalette.colorToChar(fRow[x] & 0xFF);
                byte b = bRow[x];
                bl[x] = b == TRANSPARENT ? '-' : HudPalette.colorToChar(b & 0xFF);
            }
            lines[y] = new String(cl);
            fgs[y] = new String(fl);
            bgs[y] = new String(bl);
        }
        local = new HudUpdatePayload(width, height, lines, fgs, bgs, palette.clone());
        cachedPayload = local;
        return local;
    }
}
