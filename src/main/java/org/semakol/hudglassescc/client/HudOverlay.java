package org.semakol.hudglassescc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.semakol.hudglassescc.ClientConfig;
import org.semakol.hudglassescc.Hudglassescc;
import org.semakol.hudglassescc.compat.CuriosCompat;
import org.semakol.hudglassescc.hud.CharMap;
import org.semakol.hudglassescc.hud.HudDisplay;
import org.semakol.hudglassescc.hud.HudPalette;
import org.semakol.hudglassescc.network.HudUpdatePayload;

public final class HudOverlay {
    private static final int CELL_W = 6;
    private static final int CELL_H = 9;
    private static final int OUTLINE_RGB = 0xFF000000;
    private static final int PACKED_LIGHT = 0x00F000F0;
    private static final char PUA_SPACE = (char) (CharMap.PUA_BASE | 0x20);

    private static final ResourceLocation FONT_ID = Hudglassescc.id("hud_term");
    private static final Style HUD_STYLE = Style.EMPTY.withFont(FONT_ID);

    /**
     * Pre-baked FormattedCharSequence per PUA codepoint. drawInBatch wants this
     * shape directly — caching it skips per-frame Component.getVisualOrderText()
     * allocations.
     */
    private static final FormattedCharSequence[] GLYPH_SEQUENCES = new FormattedCharSequence[256];
    static {
        for (int i = 0; i < 256; i++) {
            char c = (char) (CharMap.PUA_BASE | i);
            GLYPH_SEQUENCES[i] = Component.literal(String.valueOf(c)).withStyle(HUD_STYLE).getVisualOrderText();
        }
    }

    private HudOverlay() {}

    public static void render(GuiGraphics g, DeltaTracker dt) {
        if (!ClientHudState.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (!CuriosCompat.getWornHudStack(p).is(Hudglassescc.HUD_GLASSES.get())) return;

        HudUpdatePayload data = ClientHudState.get();
        if (data == null) return;

        final int width = data.width();
        final int height = data.height();
        if (width <= 0 || height <= 0) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int bufferPxW = width * CELL_W;
        int bufferPxH = height * CELL_H;

        float ratioX = (float) screenW / bufferPxW;
        float ratioY = (float) screenH / bufferPxH;
        float scaleX, scaleY, xOff, yOff;
        // A modem override (if any) wins; otherwise use the viewer's own config.
        HudDisplay.HudFit fitOverride = ClientHudState.fitOverride();
        HudDisplay.HudFit fit = fitOverride != null ? fitOverride : ClientConfig.HUD_FIT.get();
        switch (fit) {
            case STRETCH -> {
                // Fill the whole screen; X and Y scale independently (may distort).
                scaleX = ratioX;
                scaleY = ratioY;
                xOff = 0;
                yOff = 0;
            }
            case COVER -> {
                // Keep aspect ratio, scale up to cover the screen; overflow is
                // drawn off-screen and naturally cropped by the framebuffer.
                float s = Math.max(ratioX, ratioY);
                scaleX = s;
                scaleY = s;
                xOff = (screenW - bufferPxW * s) * 0.5f;
                yOff = (screenH - bufferPxH * s) * 0.5f;
            }
            default -> { // FIT
                // Keep aspect ratio, centered (letterboxed).
                float s = Math.min(ratioX, ratioY);
                scaleX = s;
                scaleY = s;
                xOff = (screenW - bufferPxW * s) * 0.5f;
                yOff = (screenH - bufferPxH * s) * 0.5f;
            }
        }

        Font font = mc.font;
        int[] palette = data.palette();
        String[] lines = data.lines();
        String[] fgColors = data.fgColors();
        String[] bgColors = data.bgColors();

        // Modem overrides win over the viewer's config for shadow style and layer too.
        HudDisplay.TextShadowStyle shadowOverride = ClientHudState.shadowOverride();
        HudDisplay.TextShadowStyle shadowStyle = shadowOverride != null
                ? shadowOverride : ClientConfig.TEXT_SHADOW.get();
        boolean drawShadow = shadowStyle == HudDisplay.TextShadowStyle.SHADOW;
        boolean drawOutline = shadowStyle == HudDisplay.TextShadowStyle.OUTLINE;
        boolean hasDecoration = drawShadow || drawOutline;
        HudDisplay.ShadowLayer layerOverride = ClientHudState.layerOverride();
        HudDisplay.ShadowLayer shadowLayer = layerOverride != null
                ? layerOverride : ClientConfig.TEXT_SHADOW_LAYER.get();
        boolean shadowUnder = hasDecoration
                && shadowLayer == HudDisplay.ShadowLayer.UNDER_BACKGROUND;

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(xOff, yOff, 0);
        pose.scale(scaleX, scaleY, 1f);

        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = g.bufferSource();

        if (shadowUnder) {
            // shadow -> background -> text. The shadow/outline lives in the text
            // render type, which always flushes *after* the gui-fill render type
            // within one batch — so to put it *under* the backgrounds it has to
            // reach the framebuffer in its own earlier flush.
            drawGlyphs(font, matrix, buffers, lines, fgColors, palette, width, height,
                    true, false, drawOutline);
            buffers.endBatch();

            drawBackgrounds(g, palette, bgColors, width, height);
            drawGlyphs(font, matrix, buffers, lines, fgColors, palette, width, height,
                    false, true, drawOutline);
            buffers.endBatch();
        } else {
            // background -> shadow -> text (default). One batch: fills flush first
            // (gui render type), then decoration + main glyphs (text render type).
            drawBackgrounds(g, palette, bgColors, width, height);
            drawGlyphs(font, matrix, buffers, lines, fgColors, palette, width, height,
                    hasDecoration, true, drawOutline);
            buffers.endBatch();
        }

        pose.popPose();
    }

    /**
     * Background fills, runs of same colour per row.
     *
     * <p>{@code g.fill} writes to the gui RenderType buffer with
     * {@code flushIfUnmanaged()} — inside a managed gui-layer rendering pass this
     * never forces a flush, so all rectangles batch into a single draw call.
     */
    private static void drawBackgrounds(GuiGraphics g, int[] palette, String[] bgColors,
                                        int width, int height) {
        for (int y = 0; y < height; y++) {
            String bgLine = bgColors[y];
            int py = y * CELL_H;
            int runStart = 0;
            int runColor = HudPalette.parseHex(bgLine.charAt(0));
            for (int x = 1; x < width; x++) {
                int idx = HudPalette.parseHex(bgLine.charAt(x));
                if (idx == runColor) continue;
                if (runColor >= 0) {
                    int rgb = 0xFF000000 | (palette[runColor] & 0xFFFFFF);
                    g.fill(runStart * CELL_W, py, x * CELL_W, py + CELL_H, rgb);
                }
                runStart = x;
                runColor = idx;
            }
            if (runColor >= 0) {
                int rgb = 0xFF000000 | (palette[runColor] & 0xFFFFFF);
                g.fill(runStart * CELL_W, py, width * CELL_W, py + CELL_H, rgb);
            }
        }
    }

    /**
     * fg glyphs via direct {@link Font#drawInBatch}.
     *
     * <p>Crucial: {@code GuiGraphics.drawString} calls {@code flushIfManaged()}
     * after EVERY call, which turns each glyph into a synchronous GPU sync point.
     * For an 80x28 buffer in OUTLINE mode that's ~11k sync points/frame. Bypassing
     * GuiGraphics and writing straight to the shared BufferSource batches all the
     * glyphs into a single GPU draw call when the batch flushes.
     *
     * @param decoration draw the shadow/outline copies (the {@code outline} flag
     *                   picks which: a 4-direction black outline, else a single
     *                   darkened drop-shadow copy)
     * @param main       draw the main glyph in its fg colour on top
     */
    private static void drawGlyphs(Font font, Matrix4f matrix, MultiBufferSource.BufferSource buffers,
                                   String[] lines, String[] fgColors, int[] palette,
                                   int width, int height,
                                   boolean decoration, boolean main, boolean outline) {
        Font.DisplayMode mode = Font.DisplayMode.NORMAL;
        for (int y = 0; y < height; y++) {
            String line = lines[y];
            String fgLine = fgColors[y];
            int py = y * CELL_H;
            for (int x = 0; x < width; x++) {
                char c = line.charAt(x);
                if (c == PUA_SPACE) continue;
                int fgIdx = HudPalette.parseHex(fgLine.charAt(x));
                if (fgIdx < 0) continue;
                FormattedCharSequence seq = glyphSequence(c);
                int rgb = 0xFF000000 | (palette[fgIdx] & 0xFFFFFF);
                int px = x * CELL_W;
                if (decoration) {
                    if (outline) {
                        font.drawInBatch(seq, px - 1, py,     OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                        font.drawInBatch(seq, px + 1, py,     OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                        font.drawInBatch(seq, px,     py - 1, OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                        font.drawInBatch(seq, px,     py + 1, OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    } else {
                        // Vanilla drop-shadow colour: each component * 0.25, alpha kept.
                        int shadowRgb = (rgb & 0xFCFCFC) >> 2 | (rgb & 0xFF000000);
                        font.drawInBatch(seq, px + 1, py + 1, shadowRgb, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    }
                }
                if (main) {
                    font.drawInBatch(seq, px, py, rgb, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                }
            }
        }
    }

    private static FormattedCharSequence glyphSequence(char c) {
        int puaIdx = c - CharMap.PUA_BASE;
        if (puaIdx >= 0 && puaIdx < 256) return GLYPH_SEQUENCES[puaIdx];
        // Non-PUA codepoint (e.g. UTF-8 literal in Lua source) — falls through
        // to the vanilla font reference declared inside our hud_term JSON.
        return Component.literal(Character.toString(c)).withStyle(HUD_STYLE).getVisualOrderText();
    }
}
