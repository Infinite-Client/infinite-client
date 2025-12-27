# Infinite Client

![Infinite Client Icon](src/main/resources/assets/infinite/icon.svg)

Infinite Client is a Fabric-based Minecraft client focused on a clean UI and lightweight feature set.

## Highlights
- Clean, simple interface with intuitive controls.
- Core feature categories (movement, rendering, fighting, automatic, server, utils) kept lean.
- Built-in theme system with several presets and easy user customization.

## Getting Started
1) Prerequisites: JDK 21 (Adoptium or similar) and Git.
2) Clone: `git clone https://github.com/Infinite-Client/infinite-client.git`
3) Run the client in dev: `./gradlew runClient`
4) Build a mod jar: `./gradlew build` (output in `build/libs`).

## Custom Themes
- In-game: `Options` → `Infinite Client Settings` → `Themes` tab → enable **Theme** and pick from the list.
- Built-in presets: infinite, SME Clan, Hacker, Pastel, Minecraft, Cyber, plus bundled JSONs (ocean, sunset, modern, neon, forest).
- Add your own by placing a JSON in `<minecraft dir>/infinite/themes/`; sample files live in the repo `themes/` folder.
- JSON fields: `name`, `backgroundColor`, `foregroundColor`, `primaryColor`, `secondaryColor`, optional `icon` (`namespace:path`).
- Restart the client after adding a theme to load it.

## Links
- GitHub: https://github.com/Infinite-Client/
- Homepage: https://infinite-client.infinityon.com/

## Disclaimer
This client is intended for educational or personal use. Use it responsibly and respect the rules of any servers you join.
