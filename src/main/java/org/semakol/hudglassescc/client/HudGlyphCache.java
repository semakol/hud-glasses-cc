package org.semakol.hudglassescc.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.ClientConfig;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.hud.CharMap;

import java.util.Arrays;

/**
 * Cached {@link BakedGlyph}s for the 256 HUD font characters.
 *
 * <p>{@code Font#drawInBatch} is fine for a line of chat, but the HUD draws up to
 * 256x128 cells <em>every frame</em>. Per call it allocates a {@code StringRenderOutput}
 * and a {@code Matrix4f} copy, then re-resolves the font set, glyph info and baked
 * glyph, and re-reads the style — all of which are identical for every cell. At
 * 256x96 that's ~25k cells x 60fps, and it shows up as a hard framerate drop.
 *
 * <p>Resolving each glyph once lets the render loop call
 * {@link BakedGlyph#render} directly: no allocation, no lookups, just four vertices.
 *
 * <p>The cache self-heals across resource reloads: {@code FontManager} throws its
 * {@link FontSet}s away and builds new ones, so an identity check on the font set is
 * enough to notice that the baked glyphs (and their now-closed textures) are stale.
 */
public final class HudGlyphCache {
    private static final ResourceLocation FONT_ID = Hudglassescc.id("hud_term");
    private static final ResourceLocation OUTLINE_FONT_ID = Hudglassescc.id("hud_term_outline");

    private static final BakedGlyph[] GLYPHS = new BakedGlyph[256];
    /** Same glyphs pre-dilated by 1px, so OUTLINE costs one draw instead of four. */
    private static final BakedGlyph[] OUTLINE_GLYPHS = new BakedGlyph[256];

    /** The font set the cache was built from; identity change = resource reload. */
    @Nullable
    private static FontSet builtFrom;
    /** Shared render type of every glyph, or null when the cache isn't usable. */
    @Nullable
    private static RenderType renderType;
    @Nullable
    private static FontSet outlineBuiltFrom;
    @Nullable
    private static RenderType outlineRenderType;

    private HudGlyphCache() {}

    /**
     * Makes sure the cache matches the current font set.
     *
     * @return {@code true} if the fast path can be used; {@code false} means the
     *         caller should fall back to {@code Font#drawInBatch}.
     */
    public static boolean refresh(Font font, Font.DisplayMode mode) {
        if (!ClientConfig.FAST_GLYPH_RENDERING.get()) {
            // Escape hatch for conflicts with other text/HUD rendering mods. Drop the
            // cache so flipping it back on rebuilds from scratch.
            if (builtFrom != null) {
                builtFrom = null;
                renderType = null;
                outlineBuiltFrom = null;
                outlineRenderType = null;
                Arrays.fill(GLYPHS, null);
                Arrays.fill(OUTLINE_GLYPHS, null);
                Hudglassescc.LOGGER.debug("HUD glyph cache disabled by config; using Font#drawInBatch");
            }
            return false;
        }

        FontSet current = font.getFontSet(FONT_ID);
        if (current == builtFrom) return renderType != null;

        builtFrom = current;
        renderType = null;
        RenderType shared = null;
        boolean mixed = false;
        int baked = 0;

        for (int i = 0; i < 256; i++) {
            BakedGlyph glyph = current.getGlyph(CharMap.PUA_BASE | i);
            if (glyph instanceof EmptyGlyph) {
                GLYPHS[i] = null;
                continue;
            }
            GLYPHS[i] = glyph;
            baked++;

            RenderType type = glyph.renderType(mode);
            if (shared == null) {
                shared = type;
            } else if (shared != type) {
                // Glyphs split across atlas pages would need one buffer each; our
                // 256 6x9 glyphs fit a single page, so this shouldn't happen —
                // but bail to the safe path instead of rendering half the screen.
                mixed = true;
                break;
            }
        }

        renderType = mixed ? null : shared;
        if (renderType == null) {
            // Worth a warning: the HUD still draws, but via the slow per-glyph path.
            Hudglassescc.LOGGER.warn(
                    "HUD glyph cache unavailable ({}), falling back to Font#drawInBatch — "
                            + "large HUD sizes will be slower",
                    mixed ? "glyphs span multiple atlas pages" : "font " + FONT_ID + " has no glyphs");
        } else {
            Hudglassescc.LOGGER.debug("HUD glyph cache built: {}/256 glyphs, one render type", baked);
        }
        return renderType != null;
    }

    /**
     * Builds the outline glyph cache on demand — only OUTLINE mode needs it, so the
     * atlas stays unloaded for everyone else.
     *
     * @return {@code true} if the pre-dilated glyphs can be used; {@code false} means
     *         the caller should draw the four offset copies itself.
     */
    public static boolean refreshOutline(Font font, Font.DisplayMode mode) {
        FontSet current = font.getFontSet(OUTLINE_FONT_ID);
        if (current == outlineBuiltFrom) return outlineRenderType != null;

        outlineBuiltFrom = current;
        outlineRenderType = null;
        RenderType shared = null;
        boolean mixed = false;
        int baked = 0;

        for (int i = 0; i < 256; i++) {
            BakedGlyph glyph = current.getGlyph(CharMap.PUA_BASE | i);
            if (glyph instanceof EmptyGlyph) {
                OUTLINE_GLYPHS[i] = null;
                continue;
            }
            OUTLINE_GLYPHS[i] = glyph;
            baked++;

            RenderType type = glyph.renderType(mode);
            if (shared == null) {
                shared = type;
            } else if (shared != type) {
                mixed = true;
                break;
            }
        }

        outlineRenderType = mixed ? null : shared;
        if (outlineRenderType == null) {
            Hudglassescc.LOGGER.warn(
                    "HUD outline atlas unavailable ({}), OUTLINE falls back to 4 glyph copies",
                    mixed ? "glyphs span multiple atlas pages" : "font " + OUTLINE_FONT_ID + " has no glyphs");
        } else {
            Hudglassescc.LOGGER.debug("HUD outline cache built: {}/256 glyphs, one render type", baked);
        }
        return outlineRenderType != null;
    }

    /** Render type of the outline atlas. Only valid after {@link #refreshOutline} returned true. */
    public static RenderType outlineRenderType() {
        return outlineRenderType;
    }

    /** Pre-dilated glyph for a HUD character index, drawn at {@code (x - 1, y)}. */
    @Nullable
    public static BakedGlyph outlineGlyph(int index) {
        return OUTLINE_GLYPHS[index];
    }

    /** Shared render type for the HUD font. Only valid after {@link #refresh} returned true. */
    public static RenderType renderType() {
        return renderType;
    }

    /** Baked glyph for a HUD character index (0-255), or null if it draws nothing. */
    @Nullable
    public static BakedGlyph glyph(int index) {
        return GLYPHS[index];
    }
}
