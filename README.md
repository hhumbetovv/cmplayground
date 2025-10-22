# CMPlayground

A Compose Multiplatform playground that also showcases an SVG-to-icon-font build pipeline. The project targets Android and iOS with shared UI code while a custom Gradle task converts SVG assets into an OpenType font and a Kotlin enum at build time—completely in Kotlin and without external CLIs.

## Project Layout

- `composeApp/`
  - `icon-font/svg/` – Drop your source SVG icons here (subdirectories allowed).
  - `src/commonMain/composeResources/font/icons.otf` – The generated icon font, overwritten on every build.
  - `build/generated/iconFont/IconData.kt` – Generated enum that maps SVG names to Unicode code points for Compose.
  - `src/commonMain/kotlin/` – Shared Compose UI and common business logic.
- `iosApp/` – iOS entry point plus any Swift/SwiftUI glue you might need.

## Icon Font Generation

The `generateIconFont` Gradle task handles conversion from SVG to font.

```bash
./gradlew generateIconFont
```

When executed, the task:

1. Scans `composeApp/icon-font/svg` for `.svg` files.
2. Normalises every shape, flattens paths at high resolution (2048 units/em) and centres the contours.
3. Writes `icons.otf` into the Compose resources directory.
4. Generates the `IconData` enum alongside the project’s Kotlin sources.

The task is wired as a dependency of `prepareComposeResourcesTaskFor*`, `copyNonXmlValueResourcesFor*`, and every Kotlin compile task. Any regular build therefore keeps the font and enum up to date—no manual steps after editing SVGs.

## Using `IconData`

The generated enum exposes a `value` property with the icon’s Unicode character.

```kotlin
Text(
    text = IconData.Search.value,
    fontFamily = FontFamily(Font(R.font.icons)),
    textAlign = TextAlign.Center
)
```

Because the enum lives under `build/generated/iconFont/`, remember to sync the project in your IDE after running Gradle so the source root is recognised.

## Preparing SVGs

- **ViewBox matters:** the converter normalises to the biggest dimension, recentres, and scales into the 2048-unit grid.
- **Fill support:** focus on filled shapes; strokes are flattened into outlines.
- **Detail retention:** flattening tolerance is `0.1`, so Bézier curves keep their detail. Icons stay sharp even at large sizes.

## Handy Commands

- `./gradlew generateIconFont` – Rebuild only the font and enum.
- `./gradlew composeApp:compileKotlinMetadata` – Compiles shared sources and triggers the font task.
- `./gradlew build` – Full build for all targets.

## Troubleshooting

| Issue | Fix |
| --- | --- |
| New SVG isn’t listed in the enum | Run `generateIconFont` or any build; then sync the IDE. |
| Icon looks offset | SVG viewBox or whitespace may be off; the generator centres each glyph, so inspect the original file. |
| Generation feels slow | Extremely detailed paths increase runtime; you can tweak `SvgParser`’s flattening tolerance if needed. |

## Development Notes

- All font logic lives in `buildSrc` (`SvgParser`, `IconFontGenerator`, `TrueTypeWriter`, `GenerateIconFontTask`).
- The only external dependency is `org.apache.xmlgraphics:batik-parser` (for SVG path parsing).
- Run `./gradlew generateIconFont --info` to see glyph names and code points emitted to the log.

## License

No explicit license yet—add one if you plan to distribute.
