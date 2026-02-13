# WheelPicker

A production-ready, highly customizable iOS-style wheel picker for Compose Multiplatform.

## Features

- ✅ **Dynamic Height** — Automatically wraps item content height
- ✅ **Smooth Scrolling** — Cupertino-style momentum with gentle snap
- ✅ **Cylindrical 3D Effect** — iOS picker-like depth transformation
- ✅ **Magnification** — Enlarge selected item (iOS magnifier)
- ✅ **Infinite Scroll** — Seamless circular wrapping with auto-recentering
- ✅ **Zero Dependencies** — No external libraries, pure Compose APIs
- ✅ **High Performance** — All effects use `graphicsLayer` (draw-only, no recomposition)

## Usage

### Basic Example

```kotlin
val items = (1..30).map { "Item $it" }
val state = rememberWheelPickerState(initialIndex = 5)

WheelPicker(
    itemCount = items.size,
    state = state,
    onSelectionChanged = { index ->
        println("Selected: ${items[index]}")
    }
) { index ->
    Text(items[index])
}
```

### iOS-Style Picker

```kotlin
WheelPicker(
    itemCount = items.size,
    cylindrical = true,          // 3D effect
    magnification = 1.1f,         // 10% enlargement
    infiniteScroll = true,        // Circular scrolling
    selectedBackground = {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Gray.copy(alpha = 0.2f))
        )
    }
) { index ->
    Text(items[index], fontSize = 18.sp)
}
```

## Parameters

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `itemCount` | `Int` | **required** | Total number of items |
| `modifier` | `Modifier` | `Modifier` | Container modifier |
| `state` | `WheelPickerState` | remembered | State controller |
| `extendCount` | `Int` | `2` | Visible items above/below center (total visible = `extendCount * 2 + 1`) |
| `infiniteScroll` | `Boolean` | `false` | Enable circular wrapping |
| `cylindrical` | `Boolean` | `false` | Enable 3D cylinder effect |
| `magnification` | `Float` | `1.0` | Center item scale (1.0 = no effect, 1.1 = 10% larger) |
| `onSelectionChanged` | `(Int) -> Unit?` | `null` | Selection change callback |
| `selectedBackground` | `@Composable` | `null` | Selection indicator background |
| `content` | `@Composable (Int)` | **required** | Item content renderer |

## Architecture

```
wheelpicker/
├── WheelPicker.kt              # Main API & layout
├── WheelPickerState.kt         # State management
├── internal/
│   ├── CupertinoFlingBehavior.kt   # iOS-style scroll physics
│   ├── ItemTransforms.kt           # 3D cylindrical & flat transforms
│   ├── MagnifierModifier.kt        # Center magnification effect
│   └── InfiniteScrollUtils.kt      # Infinite scroll math
└── samples/
    └── WheelPickerSamples.kt       # Demo implementations
```

## Implementation Details

### Dynamic Height
Uses `SubcomposeLayout` to measure first item, then sizes container to `itemHeight * visibleCount`.

### Smooth Scroll
Custom `FlingBehavior` with two phases:
1. **Exponential decay** — Low friction momentum (iOS-like glide)
2. **Spring snap** — Gentle centering to nearest item

### Cylindrical Effect
Each item receives `rotationX` and `translationY` transforms based on arc position on cylinder surface:
```
angle = distance / radius
rotationX = -angle (degrees)
translationY = radius * sin(angle) - distance
```

### Magnification
Draw-level effect: content renders twice — once normal (clipped outside band), once scaled (clipped inside band).

### Infinite Scroll
- Virtual item count: `Int.MAX_VALUE` (~2.1B items)
- Content wraps via modulo: `virtualIndex % itemCount`
- Auto re-centers when drifted >10% from middle (prevents overflow, imperceptible to user)

### Performance
- Item transforms: `graphicsLayer` only (draw phase, no composition)
- Selection tracking: `snapshotFlow` with `distinctUntilChanged`
- Magnifier: `drawWithContent` (draw phase)
- Zero allocations in scroll hot path

## Common Patterns

### Date Picker
```kotlin
Row {
    WheelPicker(12, cylindrical = true) { Text("Month ${it + 1}") }
    WheelPicker(31, cylindrical = true) { Text("Day ${it + 1}") }
    WheelPicker(100, cylindrical = true) { Text("${2000 + it}") }
}
```

### Settings Picker
```kotlin
val options = listOf("Off", "Low", "Medium", "High")
WheelPicker(
    itemCount = options.size,
    infiniteScroll = true,
    selectedBackground = { Divider() }
) { index ->
    Text(options[index], fontWeight = FontWeight.Bold)
}
```

## License

MIT
