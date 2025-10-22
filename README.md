# CMP Icon Fonts

A Compose Multiplatform sample that turns your SVG assets into a ready-to-use OpenType font and Kotlin enum during every build. Drop icons in, run the Gradle task, and reference the generated `IconData` straight from your UI code.

## Quick Example

1. **Add an SVG**
   - Copy any monochrome SVG into `composeApp/icon-font/svg/` (subfolders allowed).
2. **Generate the font + enum**
   - Run `./gradlew generateIconFont`
   - Alternatively trigger any regular build (`./gradlew assemble`, Android Studio Build, etc.). The task is wired in automatically.
3. **Use the generated enum**
   - `IconData` is regenerated in `composeApp/build/generated/iconFont/IconData.kt`.
   - The helper composable in `composeApp/src/commonMain/kotlin/az/theternal/cmplayground/UiIcon.kt` expects an `IconData` value:

```kotlin
UiIcon(
    icon = IconData.Search,
    contentDescription = "Search",
)
```

That’s it—every icon uses the shared `icons.otf` font and remains perfectly centred.

## What the Generator Does

- Parses every SVG path with Batik, flattens Bézier curves at a tolerance of `0.1`, and scales onto a 2048 units/em grid for crisp outlines.
- Automatically centres each glyph horizontally and vertically, then emits:
  - `src/commonMain/composeResources/font/icons.otf`
  - `build/generated/iconFont/IconData.kt`
- Assigns sequential code points (starting at U+E001) sorted by file name.
- Logs the mapping when you run `./gradlew generateIconFont --info` for easy copy/paste into docs.

## Directory Cheat Sheet

```
composeApp/
├── icon-font/svg/              # Source SVG icons
├── src/commonMain/composeResources/font/icons.otf
├── build/generated/iconFont/IconData.kt
└── src/commonMain/kotlin/az/theternal/cmplayground/
    ├── App.kt
    └── UiIcon.kt               # Shared icon composable that consumes IconData
```

All Gradle logic lives in `buildSrc/iconfont/` (`SvgParser`, `IconFontGenerator`, `TrueTypeWriter`, `GenerateIconFontTask`). No external CLI needed—everything is Kotlin code plus the `org.apache.xmlgraphics:batik-parser` dependency.

## Tips & Troubleshooting

| Scenario | Fix |
| --- | --- |
| Icon doesn’t show up in `IconData` | Regenerate (`./gradlew generateIconFont`), then sync Gradle so the generated source root is visible to the IDE. |
| Icon looks off-centre | Check the SVG `viewBox` and surrounding whitespace; the generator centres the actual paths. |
| Large SVG feels slow | Lower the flattening accuracy in `SvgParser` if absolutely necessary, but the current settings prioritise high fidelity. |

## License

Choose one for your needs—none is bundled by default.
