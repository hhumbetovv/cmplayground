package iconfont

data class ViewBox(
    val minX: Double,
    val minY: Double,
    val width: Double,
    val height: Double
) {
    init {
        require(width > 0.0 && height > 0.0) { "viewBox width and height must be positive" }
    }
}

data class FloatPoint(val x: Double, val y: Double)

data class IntPoint(val x: Int, val y: Int)

data class GlyphContour(val points: List<IntPoint>) {
    init {
        require(points.size >= 3) { "A contour must contain at least three points" }
    }

    fun signedArea(): Double {
        var area = 0.0
        val size = points.size
        for (i in 0 until size) {
            val p1 = points[i]
            val p2 = points[(i + 1) % size]
            area += p1.x * p2.y - p2.x * p1.y
        }
        return area / 2.0
    }

    fun bounds(): GlyphBounds = GlyphBounds.fromPoints(points)
}

data class GlyphBounds(val xMin: Int, val yMin: Int, val xMax: Int, val yMax: Int) {
    val width: Int get() = xMax - xMin
    val height: Int get() = yMax - yMin

    fun contains(point: IntPoint): Boolean {
        return point.x >= xMin && point.x <= xMax && point.y >= yMin && point.y <= yMax
    }

    companion object {
        fun fromPoints(points: List<IntPoint>): GlyphBounds {
            require(points.isNotEmpty()) { "Cannot build bounds from empty point list" }
            var minX = points.first().x
            var maxX = points.first().x
            var minY = points.first().y
            var maxY = points.first().y
            for (point in points) {
                if (point.x < minX) minX = point.x
                if (point.x > maxX) maxX = point.x
                if (point.y < minY) minY = point.y
                if (point.y > maxY) maxY = point.y
            }
            return GlyphBounds(minX, minY, maxX, maxY)
        }
    }
}

fun List<IntPoint>.removeSequentialDuplicates(): List<IntPoint> {
    if (isEmpty()) return this
    val result = ArrayList<IntPoint>(size)
    var previous: IntPoint? = null
    for (point in this) {
        if (previous == null || previous.x != point.x || previous.y != point.y) {
            result.add(point)
            previous = point
        }
    }
    if (result.size > 1 && result.first() == result.last()) {
        result.removeAt(result.lastIndex)
    }
    return result
}

fun IntPoint.isInside(contour: List<IntPoint>): Boolean {
    var inside = false
    var j = contour.size - 1
    val px = x.toDouble()
    val py = y.toDouble()
    for (i in contour.indices) {
        val xi = contour[i].x.toDouble()
        val yi = contour[i].y.toDouble()
        val xj = contour[j].x.toDouble()
        val yj = contour[j].y.toDouble()
        val intersects = ((yi > py) != (yj > py)) &&
            (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
        if (intersects) inside = !inside
        j = i
    }
    return inside
}
