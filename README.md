# HUD Glasses for ComputerCraft: Tweaked

🇷🇺 [Русская версия](README_ru.md)

A NeoForge 1.21.1 mod that adds **HUD Glasses** and a **HUD Modem**. The modem is
a ComputerCraft peripheral with a monitor-style terminal API; whatever a computer
writes to it is drawn on the screen of any player wearing glasses bound to that
modem.

Binding lives on the glasses item, so handing the glasses to another player hands
them the HUD. Works with Create Aeronautics through Sable Companion — range and
binding stay correct when the modem rides inside a flying sub-level.

---

## Features

- 📺 **CC-compatible peripheral** — `write`, `blit`, `clear`, `scroll`,
  `setCursorPos`, `setTextColour`, `setBackgroundColour`, `getSize`,
  `setPaletteColour`, … (see the [API](docs/API.md))
- 🕶️ **Renders on the head** — standard armor model; HUD fills the screen
- 🔗 **Binding follows the glasses** — transfer the item, transfer the HUD; any
  number of players can watch one modem
- 🆔 **Stable modem IDs** — binding survives the modem moving into a Create
  Aeronautics sub-level (bundled Sable Companion)
- 📡 **Optional range limit** — configurable, unlimited by default
- 🌫️ **Transparent background** — `setBackgroundColour(0)` or `-` in `blit`
- 🎨 **Pixel-perfect CC font** — reuses CC:Tweaked's own glyph set (all 256 chars)
- ⚡ **Low overhead** — server-side payload cache, batched background fills and a
  single-draw glyph batch on the client

---

## Dependencies

| Mod | Version | Type |
|---|---|---|
| NeoForge | 21.1.x (MC 1.21.1) | required |
| CC: Tweaked | 1.119.0+ | required |
| Sable Companion | 1.6.0+ | bundled (JiJ) |
| Create Aeronautics / Sable | any | optional — better cross-sub-level distance |
| Curios | 9.5+ | optional — adds a dedicated Glasses slot |

CC: Tweaked must be installed separately. Sable Companion is jar-in-jar'd into the
mod, so no separate install is needed.

---

## Install

1. Drop the mod jar into `mods/`.
2. Drop [CC: Tweaked for 1.21.1](https://modrinth.com/mod/cc-tweaked) in there too.
3. Launch the NeoForge client/server.

Both items live in the **ComputerCraft** creative tab.

---

## Quick start

1. Craft **HUD Glasses** and a **HUD Modem** (recipes below).
2. Place the modem.
3. **Holding the glasses**, right-click the modem → the glasses are bound (chat:
   `Glasses bound to HUD Modem #1`).
4. Wear the glasses in the helmet slot or the Curios **Glasses** slot.
5. Wire the modem to a CC computer (e.g. via a wired modem / cable).
6. On the computer:

```lua
local hud = peripheral.find("hud_glasses")
hud.setBackgroundColour(0)         -- transparent background
hud.setTextColour(colors.lime)
hud.setCursorPos(1, 1)
hud.write("Hello, HUD!")
```

Right-clicking the modem again with the same glasses unbinds them.

---

## Recipes

### HUD Glasses (shaped)

```
M L M     M = computercraft:monitor_advanced
          L = minecraft:leather
```
Two advanced monitors (the lenses) with leather between them.

### HUD Modem (shapeless)

- `computercraft:wireless_modem_advanced`
- `minecraft:glass_pane`
- `minecraft:ender_pearl`

---

## Config

File: `config/hudglassescc-common.toml`. Applied live — buffers are rebuilt within
one server tick of an edit.

| Key | Default | Meaning |
|---|---|---|
| `hudWidth` | 80 | Text grid width in characters. Fewer cells → bigger text. |
| `hudHeight` | 28 | Text grid height. |
| `maxRangeBlocks` | -1 | Max modem↔player distance in blocks. `-1` = unlimited. |

Client config (`config/hudglassescc-client.toml`, per-player):

| Key | Default | Meaning |
|---|---|---|
| `hudEnabled` | true | Whether the HUD renders. Toggle with the **H** key. |
| `hudFit` | FIT | How the HUD scales to the screen — see [Screen resolution](#screen-resolution). |
| `textShadow` | NONE | Text shadow style: `NONE` / `SHADOW` / `OUTLINE`. *(experimental)* |

Edit these from **Mods → CC: HUD Glasses → Config**, or bind a key to "Open
settings" to jump straight to the client section.

### Screen resolution

Two things control how the HUD looks on screen:

- **Buffer size** (server / Lua) — the character grid. Set the defaults with
  `hudWidth` / `hudHeight`, or per-modem at runtime with `hud.setSize(w, h)`.
  Fewer cells = bigger text. A grid near 16:9 (e.g. 80×28) fills a widescreen
  monitor cleanly.
- **Screen fit** (client `hudFit`) — how that grid is scaled onto your screen:
  - `FIT` — keep aspect ratio, centered; letterbox bars where it doesn't match.
  - `STRETCH` — fill the whole screen; distorts if the grid ratio differs.
  - `COVER` — keep aspect ratio, scaled up to cover the screen; crops the overflow.

When a player is past the distance limit they keep the **last delivered frame**;
they get a fresh one as soon as they're back in range.

---

## API

Full Lua API: [docs/API.md](docs/API.md).

Peripheral type: `"hud_glasses"`. `peripheral.find("hud_glasses")` finds the modem.

The API is mostly **compatible with `monitor` / `term`** from ComputerCraft, plus
HUD-specific extras (transparency, viewer info, modem ID, runtime resize).

Character encoding reference (CP437-ish CC font): [docs/CHARS.md](docs/CHARS.md).

---

## How binding behaves

Binding lives **on the glasses themselves** (a data component holding the modem
id). The modem does not track players — it just outputs its buffer to whoever
wears glasses bound to it.

- **Nobody wears bound glasses** → the modem keeps writing to its buffer; whoever
  puts them on sees the current state immediately.
- **A player takes the glasses off** → their HUD disappears; put them back on and
  it returns.
- **The glasses are handed to another player** → the HUD moves with the item; the
  new wearer sees the same thing. Make "duplicates" — several glasses bound to the
  same modem show one shared screen.
- **The modem rides into a Create Aeronautics sub-level** → binding holds (matched
  by stable ID, distance via Sable).
- **The glasses are destroyed / thrown in lava** → one viewer simply disappears;
  nothing lingers on the modem (there's no per-player bookkeeping).

---

## Compatibility

- ✅ **CC: Tweaked 1.119+** — core dependency.
- ✅ **Create Aeronautics + Sable** — via bundled Sable Companion (no-op fallback
  when Sable isn't installed).
- ✅ **Curios** — not required. When present, the mod adds a dedicated **Glasses**
  slot (glasses icon) so the glasses don't take the helmet slot. The vanilla
  helmet slot works too. The glasses render on the head in either case.

---

## License

All Rights Reserved. See `gradle.properties` → `mod_license`.
