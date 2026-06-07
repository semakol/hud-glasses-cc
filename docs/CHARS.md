# Character encoding cheat-sheet — for AI assistants

> **Purpose.** This file exists so an LLM writing Lua for HUD Glasses knows
> exactly which byte values produce which on-screen glyphs. The HUD uses
> CC:Tweaked's font texture verbatim — **only the 256 glyphs in that font
> exist**. Box-drawing characters from CP437 (`╔═══╗ ║ ╚╝ ─│┌`) are **NOT in
> CC's font** and will render as wrong letters from Latin-1.

---

## TL;DR — three rules

1. **For ASCII (`0x20`–`0x7E`) — just type the character.**
   ```lua
   hud.write("Hello world!")
   ```

2. **For special glyphs — use `string.char(N)`** with `N` from the table below.
   **Don't** paste UTF-8 box-drawing characters into Lua source — they encode as
   multi-byte sequences and don't index this font.
   ```lua
   hud.write(string.char(0x10))            -- ► (right triangle)
   hud.write(string.char(0x18))            -- ↑ (up arrow)
   hud.write(string.char(0xB1))            -- ± (plus-minus, from Latin-1)
   ```

3. **For boxes use ASCII** (`+---+`, `|`) because CC's font has no
   single/double-line box-drawing glyphs. For solid bars and fills use the
   sextant blocks `0x81..0x9F`.

---

## What's actually in this font

The 256 cells are filled as follows. Bytes marked **empty** render a blank cell.

| Range        | Content                                                     |
|--------------|-------------------------------------------------------------|
| `0x00`       | empty (NULL)                                                |
| `0x01..0x08` | CP437 glyphs: `☺ ☻ ♥ ♦ ♣ ♠ • ◘`                             |
| `0x09, 0x0A` | **empty**                                                   |
| `0x0B, 0x0C` | `♂ ♀`                                                       |
| `0x0D`       | **empty**                                                   |
| `0x0E, 0x0F` | `♫ ☼`                                                       |
| `0x10..0x1F` | CP437 arrows + symbols: `► ◀ ↕ ‼ ¶ § ▬ ↨ ↑ ↓ → ← ∟ ↔ ▲ ▼`   |
| `0x20..0x7E` | standard ASCII                                              |
| `0x7F`       | 50% hatched / dither pattern (used as filler glyph)         |
| `0x80..0x9F` | **2×3 sextant blocks** for sub-cell pixel art (32 patterns) |
| `0xA0..0xFF` | **Latin-1 (ISO-8859-1)**: accents, currency, fractions, etc.|

**Things that are NOT here:**
- ❌ Box drawing (`─│┌┐└┘├┤┬┴┼ ═║╔╗╚╝...`) — use ASCII `+---+ |` instead
- ❌ Greek letters (`α β γ ...`) — only `ß µ` exist via Latin-1
- ❌ Math like `≈ ≡ ≥ ≤ ∞ √` — except `± ÷` from Latin-1

---

## `0x00..0x1F` — CP437-style control glyphs

| Hex | Glyph | Description | Notes |
|----:|:-----:|-------------|-------|
| `0x00` |       | NULL                 | empty cell |
| `0x01` | ☺     | smiley               | |
| `0x02` | ☻     | inverse smiley       | |
| `0x03` | ♥     | heart                | |
| `0x04` | ♦     | diamond              | |
| `0x05` | ♣     | club                 | |
| `0x06` | ♠     | spade                | |
| `0x07` | •     | bullet               | best status dot |
| `0x08` | ◘     | inverse bullet       | |
| `0x09` |       | **empty**            | (CP437 has `○` here, CC doesn't) |
| `0x0A` |       | **empty**            | (CP437 has `◙` here, CC doesn't) |
| `0x0B` | ♂     | male                 | |
| `0x0C` | ♀     | female               | |
| `0x0D` |       | **empty**            | (CP437 has `♪` here, CC doesn't) |
| `0x0E` | ♫     | double musical note  | |
| `0x0F` | ☼     | sun                  | |
| `0x10` | ►     | right triangle arrow | |
| `0x11` | ◀     | left triangle arrow  | |
| `0x12` | ↕     | up-down arrow        | |
| `0x13` | ‼     | double exclamation   | |
| `0x14` | ¶     | pilcrow              | |
| `0x15` | §     | section              | |
| `0x16` | ▬     | thick horizontal bar | |
| `0x17` | ↨     | up-down with bar     | |
| `0x18` | ↑     | up arrow             | |
| `0x19` | ↓     | down arrow           | |
| `0x1A` | →     | right arrow          | |
| `0x1B` | ←     | left arrow           | |
| `0x1C` | ∟     | right-angle          | |
| `0x1D` | ↔     | left-right arrow     | |
| `0x1E` | ▲     | up triangle (solid)  | |
| `0x1F` | ▼     | down triangle (solid)| |

---

## `0x20..0x7F` — ASCII + dither

`0x20..0x7E` is standard ASCII. Type characters directly.

`0x7F` is a **50% hatched / dither pattern**, not the CP437 house `⌂`. Useful as
a placeholder, "no signal" indicator, or pseudo-shading.

---

## `0x80..0x9F` — 2×3 sextant blocks (pixel art)

Each glyph divides its cell into a `2 wide × 3 tall` grid of sub-pixels.
The five bits `b0..b4` of the byte select which sub-pixels are ON. The
bottom-right sub-pixel is **always OFF** in this range (only 5 of 6 positions
are encoded).

```
Subpixel layout in a cell:
  TL TR
  ML MR
  BL .    (BR always blank in 0x80..0x9F)

bit 0 = TL    (0x01)
bit 1 = TR    (0x02)
bit 2 = ML    (0x04)
bit 3 = MR    (0x08)
bit 4 = BL    (0x10)
```

Build a sextant glyph by ORing the bits:

```lua
-- All five corners ON except bottom-right:
hud.write(string.char(0x80 + 0x01 + 0x02 + 0x04 + 0x08 + 0x10))   -- glyph 0x9F

-- Top half filled (TL + TR):
hud.write(string.char(0x80 + 0x01 + 0x02))                         -- glyph 0x83

-- Helper for readability:
local function sextant(tl, tr, ml, mr, bl)
    local b = 0x80
    if tl then b = b + 0x01 end
    if tr then b = b + 0x02 end
    if ml then b = b + 0x04 end
    if mr then b = b + 0x08 end
    if bl then b = b + 0x10 end
    return string.char(b)
end
```

The full enumeration (visually arranged 4 cols × 8 rows, columns = bits 0–1,
rows = bits 2–4):

```
0x80 [..]   0x81 [▘.]   0x82 [.▝]   0x83 [▘▝]
0x84 [.. ]  0x85 [▘. ]  0x86 [.▝ ]  0x87 [▘▝ ]
                                    
[continues — 32 patterns total]
```

(Approximate Unicode equivalents shown with ▘ ▝ ▖ ▗ ▌ ▐ ▀ ▄ ▙ ▟ ▚ ▞ █ etc. but
the real glyphs come from CC's font.)

**Common patterns** worth memorising:

| Byte   | Subpixels ON      | Visual approximation |
|-------:|-------------------|---|
| `0x80` | nothing           | (empty)                            |
| `0x83` | TL + TR           | top half (≈ `▀`)                   |
| `0x8F` | TL + TR + ML + MR | top 2/3 block                      |
| `0x90` | BL only           | bottom-left dot (≈ `▖`)            |
| `0x95` | TL + ML + BL      | left column full (≈ `▌`)           |
| `0x9F` | all 5 ON          | almost-full block (BR still empty) |

---

## `0xA0..0xFF` — Latin-1 (ISO-8859-1)

Standard ISO-8859-1. Each byte = the same Unicode codepoint. You can also paste
the literal character in Lua source (it'll be UTF-8, which our font's vanilla
fallback handles).

### `0xA0..0xAF` — punctuation + symbols

| Hex   | Glyph | Hex   | Glyph | Hex   | Glyph | Hex   | Glyph |
|------:|:-----:|------:|:-----:|------:|:-----:|------:|:-----:|
| `0xA0`|       | `0xA4`| ¤     | `0xA8`| ¨     | `0xAC`| ¬     |
| `0xA1`| ¡     | `0xA5`| ¥     | `0xA9`| ©     | `0xAD`| ­     |
| `0xA2`| ¢     | `0xA6`| ¦     | `0xAA`| ª     | `0xAE`| ®     |
| `0xA3`| £     | `0xA7`| §     | `0xAB`| «     | `0xAF`| ¯     |

(`0xA0` is non-breaking space — invisible.)

### `0xB0..0xBF` — symbols + ordinals (HIGH USE FOR HUD)

| Hex   | Glyph | Description     |
|------:|:-----:|-----------------|
| `0xB0`| °     | degree          |
| `0xB1`| ±     | plus-minus      |
| `0xB2`| ²     | superscript 2   |
| `0xB3`| ³     | superscript 3   |
| `0xB4`| ´     | acute accent    |
| `0xB5`| µ     | micro / mu      |
| `0xB6`| ¶     | pilcrow         |
| `0xB7`| ·     | middle dot      |
| `0xB8`| ¸     | cedilla         |
| `0xB9`| ¹     | superscript 1   |
| `0xBA`| º     | masc. ordinal   |
| `0xBB`| »     | right guillemet |
| `0xBC`| ¼     | one quarter     |
| `0xBD`| ½     | one half        |
| `0xBE`| ¾     | three quarters  |
| `0xBF`| ¿     | inverted ?      |

### `0xC0..0xFF` — accented Latin letters

| Block          | Letters                                  |
|----------------|------------------------------------------|
| `0xC0..0xCF`   | À Á Â Ã Ä Å Æ Ç È É Ê Ë Ì Í Î Ï         |
| `0xD0..0xDF`   | Ð Ñ Ò Ó Ô Õ Ö × Ø Ù Ú Û Ü Ý Þ ß         |
| `0xE0..0xEF`   | à á â ã ä å æ ç è é ê ë ì í î ï         |
| `0xF0..0xFF`   | ð ñ ò ó ô õ ö ÷ ø ù ú û ü ý þ ÿ         |

`0xD7 ×` is multiplication, `0xF7 ÷` is division — both useful for math
indicators.

---

## Filled cells — use the background colour, not a glyph

The largest sextant glyph `0x9F` covers only **5 of 6** sub-pixels — the
bottom-right corner stays empty (CC's font has no full-block glyph). For a
genuinely solid filled cell, set the **background colour** and write a space
(or any glyph; the background covers the whole 6×9 cell regardless):

```lua
-- A 10-cell solid lime bar:
hud.setBackgroundColour(colors.lime)
hud.write(string.rep(" ", 10))            -- spaces are 6x9 of pure bg colour
hud.setBackgroundColour(0)                -- back to transparent

-- Filled progress bar with a transparent gap:
local function fillCells(x, y, n, bg)
    hud.setCursorPos(x, y)
    hud.setBackgroundColour(bg)
    hud.write(string.rep(" ", n))
    hud.setBackgroundColour(0)             -- reset to transparent
end

fillCells(1, 5, 8, colors.lime)            -- 8 truly-solid lime cells
```

Rule of thumb:

| Goal                                  | Technique                                          |
|---------------------------------------|----------------------------------------------------|
| Single bright pixel-art glyph         | sextant byte `0x80..0x9F`                          |
| Solid filled cell (or cells) of color | `setBackgroundColour(c)` + `write(" ")`            |
| Solid filled cell with text on top    | `setBackgroundColour(c) setTextColour(fg) write("X")` or use `blit` |

For `blit` the same rule applies: the bg-colour byte you pass per cell fills
the entire cell; the glyph drawn on top is only the foreground pixels.

---

## Recipes

### ASCII box

```lua
local w = 30
local hbar = "+" .. string.rep("-", w - 2) .. "+"
hud.setCursorPos(1, 1);  hud.write(hbar)
hud.setCursorPos(1, 2);  hud.write("|" .. string.rep(" ", w - 2) .. "|")
hud.setCursorPos(1, 3);  hud.write("|  Title             |")
hud.setCursorPos(1, 4);  hud.write("|" .. string.rep(" ", w - 2) .. "|")
hud.setCursorPos(1, 5);  hud.write(hbar)
```

### Progress bar — solid via background colour (preferred)

```lua
-- True full cells via background colour. No glyph involved.
local function bar(x, y, value, max, width, fg, bg)
    local n = math.floor(value / max * width)
    hud.setCursorPos(x, y)
    hud.setBackgroundColour(fg)
    hud.write(string.rep(" ", n))
    hud.setBackgroundColour(bg)              -- bg can be 0 (transparent)
    hud.write(string.rep(" ", width - n))
    hud.setBackgroundColour(0)
end

bar(1, 5, 15, 20, 30, colors.lime, 0)        -- 30-cell bar, 75% solid lime
```

### Progress bar — sextant blocks (5/6 fill, partial)

```lua
-- Pixel-art look. NOT a full cell — bottom-right of every "filled" cell stays
-- empty because 0x9F covers 5/6 subpixels. Fine for retro/dotted bars.
local function pixelBar(value, max, width)
    local n = math.floor(value / max * width)
    return string.rep(string.char(0x9F), n) .. string.rep(string.char(0x80), width - n)
end
```

### Progress bar — ASCII fallback

```lua
local function bar(value, max, width)
    local n = math.floor(value / max * width)
    return "[" .. string.rep("#", n) .. string.rep(".", width - n) .. "]"
end

hud.write(bar(15, 20, 30))    -- [############################......]
```

### Status icons

```lua
local OK    = string.char(0x07)   -- •
local WARN  = string.char(0x10)   -- ►
local ERROR = string.char(0x13)   -- ‼
local IDLE  = string.char(0x16)   -- ▬
local UP    = string.char(0x18)   -- ↑
local DOWN  = string.char(0x19)   -- ↓
```

### Temperature reading

```lua
hud.write("CPU: 85" .. string.char(0xB0) .. "C")   -- CPU: 85°C
hud.write("Tol: " .. string.char(0xB1) .. "0.5")    -- Tol: ±0.5
```

### Sub-pixel art

```lua
-- 2x3 sub-pixel grid; draw a checkerboard within one cell:
local checker = string.char(0x80 + 0x01 + 0x08 + 0x10)   -- TL + MR + BL ON
hud.write(checker)

-- A "filled-from-top" gradient across 3 cells using progressive sextants:
hud.write(string.char(0x83))                                -- top row only
hud.write(string.char(0x83 + 0x04 + 0x08))                  -- top + middle
hud.write(string.char(0x9F))                                -- all 5 ON
```

---

## Common pitfalls (for AI)

| Mistake                                | Why it fails | Fix |
|----------------------------------------|--------------|-----|
| Pasting `═` in Lua source              | UTF-8 → 3 bytes of garbage on HUD | Use ASCII `=` or `-` |
| `string.char(0xCD)` expecting `═`      | Byte `0xCD` is `Í` (Latin-1), not `═` | ASCII fallback `=` |
| Using `colors.white` in `blit` cols    | `blit` colour args are **hex chars `'0'..'9' 'a'..'f'`** | `"0"` for white, `"e"` for red, or `colors.toBlit(c)` |
| `\n` to advance lines                  | HUD has no newline | `hud.setCursorPos(1, y + 1)` |
| Looking for Greek (α β γ)              | Not in CC font (only Latin-1) | Use ASCII letters |
| Expecting `0x09` to be `○`             | CC's font is **empty** there | Use `0x07` (•) instead |

---

## Quick reference — high-use glyphs

```
0x07 •     0x10 ►     0x11 ◀     0x18 ↑     0x19 ↓     0x1A →     0x1B ←
0x16 ▬     0x1E ▲     0x1F ▼     0x13 ‼     0x07 •     0x0F ☼     0x03 ♥
0xB0 °     0xB1 ±     0xB5 µ     0xBC ¼     0xBD ½     0xBF ¿     0xF7 ÷
0x9F █-ish 0x83 ▀-ish 0x95 ▌-ish 0x80 empty
```

For boxes: use ASCII `+ - | =`. There are no line-drawing glyphs in this font.
