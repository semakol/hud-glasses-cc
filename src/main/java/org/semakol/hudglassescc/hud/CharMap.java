package org.semakol.hudglassescc.hud;

/**
 * Maps CC byte values (0..255) into the Private Use Area so they reference
 * CC's own font texture via the {@code hudglassescc:hud_term} bitmap font.
 * Chars {@code >= 256} pass through unchanged so any genuine Unicode in the
 * input (e.g. {@code "═"} typed in UTF-8 Lua source) renders via the vanilla
 * font fallback inside the same font JSON.
 */
public final class CharMap {
    private CharMap() {}

    public static final int PUA_BASE = 0xE000;

    public static char remap(char c) {
        return c < 256 ? (char) (PUA_BASE | c) : c;
    }
}
