# HUD Glasses — Lua API

🇷🇺 [Русская версия](API_ru.md)

Peripheral type: **`hud_glasses`**.

```lua
local hud = peripheral.find("hud_glasses")
if not hud then error("HUD modem not found on the network") end
```

Most methods are **drop-in compatible** with CC's `monitor` / `term`, so a monitor
script almost always runs on the HUD unchanged.

---

## Contents

| Section | Methods |
|---|---|
| [Writing text](#writing-text)          | `write`, `blit`, `clear`, `clearLine`, `scroll` |
| [Cursor](#cursor)                      | `getCursorPos`, `setCursorPos`, `getCursorBlink`, `setCursorBlink` |
| [Size](#size)                          | `getSize`, `setSize`, `setWidth`, `setHeight`, `resetSize` |
| [Display overrides](#display-overrides) | `getTextShadow`, `setTextShadow`, `getHudFit`, `setHudFit`, `getShadowLayer`, `setShadowLayer` |
| [Pen colours](#pen-colours)            | `getTextColour`, `setTextColour`, `getBackgroundColour`, `setBackgroundColour`, `isColour` |
| [Palette](#palette)                    | `getPaletteColour`, `setPaletteColour` |
| [Binding / identity](#binding--identity) | `getId`, `getOwner`, `getOwners`, `getOwnerCount` |
| [Behaviour & errors](#behaviour--errors) | summary |
| [Examples](#examples)                  | ready-to-run scripts |
| [Differences from monitor](#differences-from-monitor) | |

---

## Writing text

### `write(text)`

Writes `text` at the cursor position in the current pen colour. The cursor then
advances by `#text` to the right. Characters past the right edge are silently
dropped.

The argument is **coerced** — `42`, `true`, `nil` (→ `"nil"`) are all accepted.

```lua
hud.setCursorPos(1, 1)
hud.write("HP: ")
hud.write(20)
```

---

### `blit(text, fgColours, bgColours)`

Writes a run of characters with per-cell colours. All three arguments are strings
of **equal length** (CC passes them as `ByteBuffer`; you just pass plain strings).

- `text` — the characters (one byte per cell)
- `fgColours` — hex `0..9 a..f` (palette index) per cell
- `bgColours` — same, **plus** `-` meaning a **transparent** background cell

```lua
hud.setCursorPos(1, 3)
hud.blit("WARN",
         "eeee",     -- all text red (e = colors.red)
         "----")     -- transparent background
```

**Throws** `Arguments must be the same length` if the lengths differ.

---

### `clear()`

Fills the whole buffer with spaces in the current `setBackgroundColour`. If the
background is transparent (`0`) the screen becomes blank.

### `clearLine()`

Same, but only the line the cursor is on.

---

### `scroll(y)`

Shifts every line by `y`. `y > 0` moves content up (blank lines appear at the
bottom in the current pen background); `y < 0` moves it down. `|y| ≥ height` is
equivalent to `clear()`.

```lua
hud.scroll(1)   -- scroll up one line
```

---

## Cursor

### `getCursorPos() → x, y`

Returns the current cursor position. **1-based**: `(1, 1)` is the top-left.

### `setCursorPos(x, y)`

Moves the cursor. Out-of-range values are allowed — the next write simply draws
nothing outside the buffer.

### `getCursorBlink() → blink`

Returns the "cursor blinking" flag.

### `setCursorBlink(blink)`

Sets the flag. **Note:** the cursor is not currently drawn on the HUD — the flag
exists only for compatibility with libraries that read/write it.

---

## Size

The buffer is a character grid. How it maps onto the player's screen is a
*client* choice — the `hudFit` setting: `FIT` (keep aspect, letterbox, default),
`STRETCH` (fill the screen, may distort), or `COVER` (fill + crop). Your script
only controls the grid size; each viewer controls the fit. A grid near 16:9
(e.g. 80×28) fills a widescreen monitor cleanly under any mode.

### `getSize() → width, height`

Returns the current grid size **in characters** (not pixels). Fewer cells means a
bigger glyph per cell.

### `setSize(width, height)`

Resizes this specific modem. The buffer contents are **cleared**.

- `width` ∈ `[4, 256]`
- `height` ∈ `[1, 128]`
- otherwise → `LuaException`.

After this the modem is flagged "custom" — changes to `hudWidth`/`hudHeight` in
`hudglassescc-common.toml` no longer touch it. The size is stored in the block's
NBT and survives server restarts, pistons and Sable sub-levels.

```lua
hud.setSize(60, 20)
```

### `setWidth(width)` / `setHeight(height)`

Convenience: change one axis, leave the other as is. Same bounds.

### `resetSize()`

Drops the "custom" flag and reverts to the current `hudWidth`/`hudHeight` config
values. Contents are cleared.

---

## Display overrides

These let the **computer** force a viewer-side display setting for everyone wearing
glasses bound to this modem — handy for a control room that wants one consistent look.
Every setter also accepts `"auto"`, which hands control back to the viewer's own client
config. **`"auto"` is the default for all three.**

Overrides are **per-modem**, persist across restarts (like `setSize`), and **never
modify any player's config file** — they apply only while a player is watching this
modem. Unbinding the glasses (or binding a different modem) drops the override.

> The viewer's choice in **Mods → CC: HUD Glasses → Config** is the fallback; an
> override here takes precedence until set back to `"auto"`.

### `setTextShadow(style)`

Forces the text-shadow style. `style` (case-insensitive): `"auto"`, `"none"`,
`"shadow"`, `"outline"`. **Throws** `LuaException` on any other value.

### `getTextShadow() → style`

Returns the current override as a string — `"auto"` when not overridden.

### `setHudFit(fit)`

Forces how the grid is scaled to the screen. `fit`: `"auto"`, `"fit"`, `"stretch"`,
`"cover"` (see [Size](#size) for what each does). **Throws** otherwise.

### `getHudFit() → fit`

Returns the current fit override — `"auto"` when not overridden.

### `setShadowLayer(layer)`

Forces where the shadow/outline sits relative to the cell backgrounds. `layer`:
`"auto"`, `"over"` (background → shadow → text) or `"under"` (shadow → background →
text). Only visible when the shadow style is `shadow`/`outline`. **Aliases:**
`setTextShadowLayer`. **Throws** otherwise.

### `getShadowLayer() → layer`

Returns `"auto"`, `"over"`, or `"under"`. **Aliases:** `getTextShadowLayer`.

```lua
local hud = peripheral.find("hud_glasses")
hud.setHudFit("stretch")      -- everyone watching sees it stretched...
hud.setTextShadow("outline")  -- ...with an outline...
hud.setShadowLayer("under")   -- ...drawn behind the cell backgrounds.
hud.setHudFit("auto")         -- hand fit back to each viewer's own setting
```

---

## Pen colours

A `colour` argument is always a **CC bitmask**: a power of two in `[1, 0x8000]`.
Use the CC API constants: `colors.red`, `colors.lime`, etc. Any other number →
`LuaException("Expected color")`.

### `getTextColour() → c`

Returns the current text colour (used by the next `write`/`clear`).

**Aliases:** `getTextColor`.

### `setTextColour(c)`

Sets the text colour.

**Aliases:** `setTextColor`. **Throws** on an invalid colour.

### `getBackgroundColour() → c`

Returns the current background colour. **`0`** means transparent (HUD extension).

**Aliases:** `getBackgroundColor`.

### `setBackgroundColour(c)`

Sets the background colour. **HUD extension:** passing `0` makes the background
**transparent** — `clear()` then leaves the screen blank and `write` draws no
backdrop behind glyphs. Any other `c` is a normal CC colour.

**Aliases:** `setBackgroundColor`. **Throws** on a colour that's neither `0` nor a
valid CC bitmask.

### `isColour() → true`

Always returns `true` (the HUD supports all 16 colours).

**Aliases:** `isColor`, `getIsColour`, `getIsColor`.

---

## Palette

Each modem has its own 16-colour palette that can be re-mapped.

### `getPaletteColour(c) → r, g, b`

Returns three numbers in `[0, 1]` (the RGB components of the current palette for
colour `c`).

**Aliases:** `getPaletteColor`.

```lua
local r, g, b = hud.getPaletteColour(colors.red)
```

### `setPaletteColour(c, ...)`

Re-maps one of the 16 colours. Two call forms:

```lua
-- RGB int
hud.setPaletteColour(colors.red, 0xFF8800)

-- three floats in [0..1]
hud.setPaletteColour(colors.red, 1.0, 0.5, 0.0)
```

**Aliases:** `setPaletteColor`. **Throws** on an invalid colour or a bad argument
count.

A re-mapped palette lives **only in the buffer's RAM** — it is reset to the
defaults on `setSize` / `resetSize`.

---

## Binding / identity

> **Binding lives on the glasses.** Right-clicking a modem with glasses in hand
> writes the modem's id onto the item. Whoever wears those glasses (the Curios
> Glasses slot or the helmet) sees the HUD. Hand the glasses to another player and
> the HUD moves with them. Any number of players can watch one modem at once.

### `getId() → id`

Returns the modem's **stable** id (a number). It does not change when the modem is
moved, pushed by a piston, or carried into a Sable sub-level (Create Aeronautics).
The same id is shown in the glasses tooltip (`Bound to modem #N`).

```lua
print("This is modem #" .. hud.getId())
```

### `getOwner() → name`

Returns the name of the **first current viewer** (someone wearing bound glasses
right now), or `nil` if nobody is watching.

### `getOwners() → table`

Returns a Lua table of the names of **current viewers** — online players currently
wearing glasses bound to this modem.

```lua
for _, name in ipairs(hud.getOwners()) do
    print(name)
end
```

### `getOwnerCount() → n`

How many players are watching this modem right now.

---

## Behaviour & errors

### What throws `LuaException`

| Method | When |
|---|---|
| `setTextColour` / `setBackgroundColour` | colour isn't a valid CC bitmask (for bg, `0` is also valid) |
| `setPaletteColour` | invalid colour or odd argument count |
| `setSize` / `setWidth` / `setHeight` | outside `[4..256] × [1..128]` |
| `blit` | mismatched `text`/`fg`/`bg` lengths |

### What is handled **silently** (no-op, no error)

| Situation | Behaviour |
|---|---|
| Nobody wears bound glasses | Writes accumulate in the buffer. Whoever puts them on sees the current state. |
| A player takes the glasses off | Their HUD disappears. Put them back on and it returns. |
| Glasses handed to another player | The HUD moves with them: the new wearer sees the same thing. |
| Player past `maxRangeBlocks` | The frame "freezes". Back in range → fresh frame. |
| `write("")` or `write` past the edge | No-op. |

This is deliberate: the peripheral **never** errors because of the receiver's
state. The script writes to the buffer; delivery is the server's concern.

---

## Examples

### 1. Minimal: "Hello, HUD!"

```lua
local hud = peripheral.find("hud_glasses")
hud.setBackgroundColour(0)
hud.setTextColour(colors.lime)
hud.setCursorPos(1, 1)
hud.write("Hello, HUD!")
```

### 2. Centred clock

```lua
local hud = peripheral.find("hud_glasses")
hud.setBackgroundColour(0)
hud.setTextColour(colors.white)

while true do
    local text = textutils.formatTime(os.time(), true)
    local w, h = hud.getSize()
    local x = math.floor((w - #text) / 2) + 1
    local y = math.floor(h / 2)
    hud.clear()
    hud.setCursorPos(x, y)
    hud.write(text)
    sleep(1)
end
```

### 3. Coloured bar with `blit`

```lua
local hud = peripheral.find("hud_glasses")
local w, h = hud.getSize()
local label = " STATUS: OK "
local pad = math.floor((w - #label) / 2)

hud.setCursorPos(1, h)
hud.blit(string.rep(" ", pad) .. label .. string.rep(" ", w - pad - #label),
         string.rep("0", w),                      -- all text white
         string.rep("d", w))                      -- all background green
```

### 4. Adaptive layout: resize at runtime

```lua
local hud = peripheral.find("hud_glasses")

local function showLarge(text)
    hud.setSize(40, 10)             -- big
    hud.setBackgroundColour(0)
    hud.clear()
    hud.setTextColour(colors.red)
    local w, h = hud.getSize()
    hud.setCursorPos(math.floor((w - #text) / 2) + 1, math.floor(h / 2))
    hud.write(text)
end

local function showCompact(lines)
    hud.setSize(80, 28)             -- small, many lines
    hud.setBackgroundColour(0)
    hud.clear()
    hud.setTextColour(colors.white)
    for i, line in ipairs(lines) do
        hud.setCursorPos(1, i)
        hud.write(line)
    end
end

showLarge("ALERT")
sleep(2)
showCompact({"Power: 80%", "Fuel:  120", "Crew:  3/4"})
```

### 5. Who's watching this HUD

```lua
local hud = peripheral.find("hud_glasses")
print("Modem #" .. hud.getId() .. " is watched by:")
for _, name in ipairs(hud.getOwners()) do
    print("  " .. name)
end
```

---

## Differences from monitor

| Aspect | `monitor` (CC) | `hud_glasses` |
|---|---|---|
| Display | a physical block | overlay on the player's screen |
| Viewers | everyone who can see the block | everyone wearing bound glasses |
| Transparent background | no | `setBackgroundColour(0)` or `-` in blit |
| Scale | `setTextScale(s)` (pixels) | `setSize(w, h)` (cells) |
| Size | via `setTextScale` + block multiples | `setSize` + config defaults |
| Offline behaviour | n/a (the block always exists) | buffer accumulates, delivered when possible |
| Extra methods | — | `getId`, `getOwners`, `setSize`, `resetSize`, `setHudFit`, `setTextShadow`, `setShadowLayer` |

`setTextScale` / `getTextScale` are **not implemented** — the HUD has no pixel
step; size is set with `setSize`.

See [CHARS.md](CHARS.md) for the character-encoding reference (the CC font is a
CP437-style set, addressed by byte value).

![img.png](img.png)
