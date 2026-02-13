package az.theternal.cmplayground.ui.components.wheelpicker.internal

/**
 * Aligns virtual index to the center of Int.MAX_VALUE range,
 * ensuring proper modulo alignment for infinite scroll.
 */
internal fun infiniteCenterIndex(itemCount: Int, selectedIndex: Int): Int {
    val center = Int.MAX_VALUE / 2
    return center - (center % itemCount) + selectedIndex
}
