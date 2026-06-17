# Changelog

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
