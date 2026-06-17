package org.semakol.hudglassescc.peripheral;

import dan200.computercraft.api.lua.Coerced;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;
import org.semakol.hudglassescc.block.entity.HudModemBlockEntity;
import org.semakol.hudglassescc.hud.HudBuffer;
import org.semakol.hudglassescc.hud.HudDisplay;
import org.semakol.hudglassescc.network.HudSettingsPayload;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * CC peripheral exposing a CC:Tweaked-compatible terminal API on top of a
 * {@link HudBuffer}. Method names, return shapes and color-parsing semantics
 * mirror {@code dan200.computercraft.core.apis.TermMethods} so existing
 * monitor-style scripts work unchanged. Extras (binding info, opaque-bg = 0):
 *
 *   getId(), getOwner(), getOwners(), getOwnerCount()
 *   setBackgroundColour(0) — switches the pen to transparent (HUD-only feature)
 *   blit bg may contain '-' meaning transparent for that cell.
 */
public class HudPeripheral implements IPeripheral {
    private final HudModemBlockEntity be;

    public HudPeripheral(HudModemBlockEntity be) {
        this.be = be;
    }

    @Override
    public String getType() {
        return "hud_glasses";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof HudPeripheral hp && hp.be == this.be;
    }

    @Override
    public void attach(IComputerAccess computer) {}

    @Override
    public void detach(IComputerAccess computer) {}

    private HudBuffer buf() {
        return be.getBuffer();
    }

    /**
     * Mirrors {@code ColourUtils.parseColour}: argument must be a single-bit
     * positive integer ≤ 0x8000.
     */
    private static int parseColour(int v) throws LuaException {
        if (v <= 0 || v > 0x8000 || (v & (v - 1)) != 0) {
            throw new LuaException("Expected color");
        }
        return Integer.numberOfTrailingZeros(v);
    }

    // ---- TermMethods-compatible surface ----

    @LuaFunction
    public final void write(Coerced<String> text) {
        String s = text.value();
        if (s == null || s.isEmpty()) return;
        buf().write(s);
    }

    @LuaFunction
    public final void scroll(int y) {
        buf().scroll(y);
    }

    @LuaFunction
    public final Object[] getCursorPos() {
        HudBuffer b = buf();
        return new Object[]{ b.cursorX + 1, b.cursorY + 1 };
    }

    @LuaFunction
    public final void setCursorPos(int x, int y) {
        buf().setCursorPos(x - 1, y - 1);
    }

    @LuaFunction
    public final boolean getCursorBlink() {
        return buf().getCursorBlink();
    }

    @LuaFunction
    public final void setCursorBlink(boolean blink) {
        buf().setCursorBlink(blink);
    }

    @LuaFunction
    public final Object[] getSize() {
        HudBuffer b = buf();
        return new Object[]{ b.width, b.height };
    }

    @LuaFunction
    public final void setSize(int width, int height) throws LuaException {
        if (width < HudModemBlockEntity.MIN_W || width > HudModemBlockEntity.MAX_W) {
            throw new LuaException("Width must be between " + HudModemBlockEntity.MIN_W + " and " + HudModemBlockEntity.MAX_W);
        }
        if (height < HudModemBlockEntity.MIN_H || height > HudModemBlockEntity.MAX_H) {
            throw new LuaException("Height must be between " + HudModemBlockEntity.MIN_H + " and " + HudModemBlockEntity.MAX_H);
        }
        be.setBufferSize(width, height);
    }

    @LuaFunction
    public final void setWidth(int width) throws LuaException {
        setSize(width, buf().height);
    }

    @LuaFunction
    public final void setHeight(int height) throws LuaException {
        setSize(buf().width, height);
    }

    /** Drops the per-modem override and reverts to the config's default size. */
    @LuaFunction
    public final void resetSize() {
        be.resetBufferSize();
    }

    @LuaFunction
    public final void clear() { buf().clear(); }

    @LuaFunction
    public final void clearLine() { buf().clearLine(); }

    @LuaFunction({"getTextColour", "getTextColor"})
    public final int getTextColour() {
        return 1 << buf().getTextColor();
    }

    @LuaFunction({"setTextColour", "setTextColor"})
    public final void setTextColour(int colour) throws LuaException {
        buf().setTextColor(parseColour(colour));
    }

    @LuaFunction({"getBackgroundColour", "getBackgroundColor"})
    public final int getBackgroundColour() {
        int b = buf().getBackgroundColor();
        return b < 0 ? 0 : 1 << b;
    }

    @LuaFunction({"setBackgroundColour", "setBackgroundColor"})
    public final void setBackgroundColour(int colour) throws LuaException {
        if (colour == 0) {
            // HUD extension: 0 = transparent background pen
            buf().setBackgroundColor(-1);
        } else {
            buf().setBackgroundColor(parseColour(colour));
        }
    }

    @LuaFunction({"getIsColour", "getIsColor", "isColour", "isColor"})
    public final boolean getIsColour() {
        return true;
    }

    @LuaFunction
    public final void blit(ByteBuffer text, ByteBuffer textColour, ByteBuffer backgroundColour) throws LuaException {
        int n = text.remaining();
        if (textColour.remaining() != n || backgroundColour.remaining() != n) {
            throw new LuaException("Arguments must be the same length");
        }
        byte[] t = new byte[n]; text.get(t);
        byte[] f = new byte[n]; textColour.get(f);
        byte[] b = new byte[n]; backgroundColour.get(b);
        buf().blit(t, f, b);
    }

    @LuaFunction({"setPaletteColour", "setPaletteColor"})
    public final void setPaletteColour(IArguments args) throws LuaException {
        int colour = parseColour(args.getInt(0));
        int rgb;
        if (args.count() == 2) {
            rgb = args.getInt(1) & 0xFFFFFF;
        } else if (args.count() >= 4) {
            int r = (int) Math.round(args.getDouble(1) * 255) & 0xFF;
            int g = (int) Math.round(args.getDouble(2) * 255) & 0xFF;
            int bl = (int) Math.round(args.getDouble(3) * 255) & 0xFF;
            rgb = (r << 16) | (g << 8) | bl;
        } else {
            throw new LuaException("Expected (color, rgb) or (color, r, g, b)");
        }
        buf().setPaletteColor(colour, rgb);
    }

    @LuaFunction({"getPaletteColour", "getPaletteColor"})
    public final Object[] getPaletteColour(int colour) throws LuaException {
        int idx = parseColour(colour);
        int rgb = buf().getPaletteColor(idx);
        return new Object[]{
                ((rgb >> 16) & 0xFF) / 255.0,
                ((rgb >> 8) & 0xFF) / 255.0,
                (rgb & 0xFF) / 255.0
        };
    }

    // ---- HUD-glasses specific extensions ----

    @LuaFunction
    public final int getId() {
        return be.getModemId();
    }

    @LuaFunction
    public final Object[] getOwner() {
        java.util.List<String> viewers = be.getCurrentViewers();
        return new Object[]{ viewers.isEmpty() ? null : viewers.get(0) };
    }

    @LuaFunction
    public final Object[] getOwners() {
        return be.getCurrentViewers().toArray();
    }

    @LuaFunction
    public final int getOwnerCount() {
        return be.getCurrentViewers().size();
    }

    // ---- Display overrides ----
    //
    // Force a client display setting for everyone wearing glasses bound to this
    // modem, or "auto" to let each viewer's own client config decide. These never
    // touch the player's config file — they're a per-modem, runtime override.

    @LuaFunction
    public final void setTextShadow(String value) throws LuaException {
        be.setOverrideTextShadow(parseTextShadow(value));
    }

    @LuaFunction
    public final String getTextShadow() {
        return enumName(HudDisplay.TextShadowStyle.values(), be.getOverrideTextShadow());
    }

    @LuaFunction
    public final void setHudFit(String value) throws LuaException {
        be.setOverrideHudFit(parseHudFit(value));
    }

    @LuaFunction
    public final String getHudFit() {
        return enumName(HudDisplay.HudFit.values(), be.getOverrideHudFit());
    }

    @LuaFunction({"setShadowLayer", "setTextShadowLayer"})
    public final void setShadowLayer(String value) throws LuaException {
        be.setOverrideShadowLayer(parseShadowLayer(value));
    }

    @LuaFunction({"getShadowLayer", "getTextShadowLayer"})
    public final String getShadowLayer() {
        byte b = be.getOverrideShadowLayer();
        if (b == HudDisplay.ShadowLayer.OVER_BACKGROUND.ordinal()) return "over";
        if (b == HudDisplay.ShadowLayer.UNDER_BACKGROUND.ordinal()) return "under";
        return "auto";
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /** Friendly lowercase name for an override byte, or "auto" for AUTO / out of range. */
    private static <E extends Enum<E>> String enumName(E[] values, byte b) {
        return (b < 0 || b >= values.length) ? "auto" : values[b].name().toLowerCase(Locale.ROOT);
    }

    private static int parseTextShadow(String s) throws LuaException {
        String v = norm(s);
        if (v.equals("auto")) return HudSettingsPayload.AUTO;
        for (HudDisplay.TextShadowStyle t : HudDisplay.TextShadowStyle.values()) {
            if (t.name().toLowerCase(Locale.ROOT).equals(v)) return t.ordinal();
        }
        throw new LuaException("Expected one of: auto, none, shadow, outline");
    }

    private static int parseHudFit(String s) throws LuaException {
        String v = norm(s);
        if (v.equals("auto")) return HudSettingsPayload.AUTO;
        for (HudDisplay.HudFit t : HudDisplay.HudFit.values()) {
            if (t.name().toLowerCase(Locale.ROOT).equals(v)) return t.ordinal();
        }
        throw new LuaException("Expected one of: auto, fit, stretch, cover");
    }

    private static int parseShadowLayer(String s) throws LuaException {
        return switch (norm(s)) {
            case "auto" -> HudSettingsPayload.AUTO;
            case "over", "over_background" -> HudDisplay.ShadowLayer.OVER_BACKGROUND.ordinal();
            case "under", "under_background" -> HudDisplay.ShadowLayer.UNDER_BACKGROUND.ordinal();
            default -> throw new LuaException("Expected one of: auto, over, under");
        };
    }
}
