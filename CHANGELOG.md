# Changelog

## 0.1.4-beta

- **OUTLINE text shadow is much cheaper.** It used to draw every glyph five times (four
  black copies plus the glyph). The mod now ships a pre-dilated copy of the font atlas,
  so the outline is a single black draw and OUTLINE costs two draws per glyph instead of
  five. It looks identical: dilating a glyph by 1px in four directions is exactly what the
  four offset copies produced, and the font is 1-bit pixel art so nothing blurs. If the
  outline atlas ever fails to load, the old four-copy path still runs.
- **The F3 debug screen, chat and the player list now draw on top of the HUD.** The
  overlay used to sit above every other GUI layer, so a full-screen HUD buried them with
  no way to read them short of switching the HUD off. It now renders just below the debug
  overlay instead.
- **Much faster rendering at large HUD sizes.** The glyph pass no longer goes through
  `Font#drawInBatch` per cell, which allocated twice and re-resolved the font set, glyph
  info, baked glyph and style for every character, every frame. Glyphs are now baked
  once and their vertices written straight into the batch, so the pass is allocation-free
  and the palette is unpacked once per frame instead of once per cell. This scaled with
  cell count, so the bigger the HUD the bigger the win (`setSize(256, 96)` was hit hardest).
  New client setting `fastGlyphRendering` (default `true`) turns this off again if another
  mod's text/HUD rendering conflicts with it.

## 0.1.3-beta

- **HUD on a pocket computer.** Craft `pocket computer + HUD Modem` to give it the HUD
  upgrade — the same `hud_glasses` peripheral, but portable. Bind your **worn** glasses
  to it with **sneak + right-click** (a plain right-click still opens the terminal).
  Modem blocks and pocket computers share one id space, so switching sources is just
  right-clicking whichever you want, and the glasses never come off.
  The pocket's buffer size is per-session; its id persists.
- **HUD + Ender Modem in one upgrade slot.** A pocket computer has only one slot, so a
  HUD-only upgrade would leave it with no networking. Craft a **HUD Ender Modem**
  (`HUD Modem + Ender Modem`) and fit it to a pocket computer for a peripheral that is both:
  its primary type is `modem` (so `rednet.open("back")` works) with `hud_glasses` as an
  additional type (so `peripheral.find("hud_glasses")` finds the same object). The modem
  half is interdimensional and supports `open`/`isOpen`/`close`/`closeAll`/`transmit`/
  `isWireless` plus `modem_message` events. Fitting it uses CC's own upgrade recipe, so
  the computer's id and files are preserved; removing it returns the item.
- New server config `allowPocketHudEnderCraft` (default `true`) — set `false` and the
  HUD Ender Modem recipe is removed outright (gone from JEI, item unlisted). Pockets that
  already have the upgrade keep working. Read at recipe load, so it needs a `/reload`.
- Fixed `getBackgroundColour()` returning `0` when the background was transparent. `0`
  isn't a valid CC colour, which broke term-compatible libraries (e.g. More Fonts) that
  map the result through a colour table and got `nil`. It now reports `colors.black`;
  `setBackgroundColour(0)` still sets transparency.

## 0.1.2-beta

- Computers can now override a viewer's display settings per modem from Lua:
  `setTextShadow`, `setHudFit`, `setShadowLayer` (plus getters). Each accepts
  `"auto"` (the default), which defers to the player's own config. Overrides apply
  to everyone wearing glasses bound to the modem, persist with the modem, and
  never modify any player's config file.
- New client setting "Shadow layer" (`textShadowLayer`): choose whether the text
  shadow/outline is drawn `OVER_BACKGROUND` (default) or `UNDER_BACKGROUND` — under
  the cell backgrounds, so it only shows where it bleeds past an opaque cell.

## 0.1.1-beta

- Modem can now be placed on any block face (floor, wall, ceiling) — it orients
  to the surface, screen facing out.
- Modem has its own flat model and custom textures (no longer the iron-block look).
- Empty-hand right-click on a modem now binds/unbinds the glasses you're already
  wearing (helmet or Curios slot) — no need to take them off.
- New "Screen fit" setting: `FIT` (default, letterbox), `STRETCH` (fill screen),
  `COVER` (fill + crop).
- Mod config is now editable from the Mods list "Config" button.
- New keybind "Open settings" (unbound by default) opens the client config directly.
- Toggle HUD default key changed from **G** to **H**.
- Added a mod icon.

## 0.1.0-beta

- Initial beta release.
- HUD Glasses + HUD Modem: a ComputerCraft peripheral that renders a
  monitor-style terminal onto a player's screen.
- Binding lives on the glasses item; transferring the glasses transfers the HUD.
- Monitor/term-compatible Lua API, transparent background, runtime resize,
  pixel-perfect CC font.
- Optional Curios (dedicated Glasses slot) and Create Aeronautics / Sable support.
