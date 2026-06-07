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

        float scale = Math.min((float) screenW / bufferPxW, (float) screenH / bufferPxH);
        float renderedW = bufferPxW * scale;
        float renderedH = bufferPxH * scale;
        float xOff = (screenW - renderedW) * 0.5f;
        float yOff = (screenH - renderedH) * 0.5f;

        Font font = mc.font;
        int[] palette = data.palette();
        String[] lines = data.lines();
        String[] fgColors = data.fgColors();
        String[] bgColors = data.bgColors();

        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(xOff, yOff, 0);
        pose.scale(scale, scale, 1f);

        // Pass 1: bg fills, runs of same colour per row.
        // g.fill writes to the gui RenderType buffer with flushIfUnmanaged() —
        // inside a managed gui-layer rendering pass this never forces a flush,
        // so all rectangles batch into a single draw call.
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

        // Pass 2: fg glyphs via direct Font.drawInBatch.
        //
        // Crucial: GuiGraphics.drawString calls flushIfManaged() after EVERY
        // call, which turns each glyph into a synchronous GPU sync point.
        // For an 80x28 buffer in OUTLINE mode that's ~11k sync points/frame.
        // Bypassing GuiGraphics and writing straight to the shared BufferSource
        // batches all 2240 (or 5x in outline) glyphs into a single GPU draw
        // call when the gui layer flushes at the end.
        ClientConfig.TextShadowStyle shadowStyle = ClientConfig.TEXT_SHADOW.get();
        boolean drawShadow = shadowStyle == ClientConfig.TextShadowStyle.SHADOW;
        boolean drawOutline = shadowStyle == ClientConfig.TextShadowStyle.OUTLINE;
        Matrix4f matrix = pose.last().pose();
        MultiBufferSource.BufferSource buffers = g.bufferSource();
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
                if (drawOutline) {
                    font.drawInBatch(seq, px - 1, py,     OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    font.drawInBatch(seq, px + 1, py,     OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    font.drawInBatch(seq, px,     py - 1, OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    font.drawInBatch(seq, px,     py + 1, OUTLINE_RGB, false, matrix, buffers, mode, 0, PACKED_LIGHT);
                    font.drawInBatch(seq, px,     py,     rgb,         false, matrix, buffers, mode, 0, PACKED_LIGHT);
                } else {
                    font.drawInBatch(seq, px, py, rgb, drawShadow, matrix, buffers, mode, 0, PACKED_LIGHT);
                }
            }
        }

        // Single flush at the end of the layer. Without this the queued text
        // vertices never reach the GPU because we bypassed GuiGraphics.drawString
        // (which would normally call flushIfManaged() after each glyph).
        // One flush per frame keeps the perf win from batching.
        buffers.endBatch();

        pose.popPose();
    }

    private static FormattedCharSequence glyphSequence(char c) {
        int puaIdx = c - CharMap.PUA_BASE;
        if (puaIdx >= 0 && puaIdx < 256) return GLYPH_SEQUENCES[puaIdx];
        // Non-PUA codepoint (e.g. UTF-8 literal in Lua source) — falls through
        // to the vanilla font reference declared inside our hud_term JSON.
        return Component.literal(Character.toString(c)).withStyle(HUD_STYLE).getVisualOrderText();
    }
}
