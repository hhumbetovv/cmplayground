package iconfont

import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

class IconFontGenerator(private val config: FontConfig = FontConfig()) {
    private val parser = SvgParser()

    fun generate(
        fontName: String,
        fontStyle: String,
        svgFiles: List<File>,
        codePointStart: Int
    ): IconFontResult {
        val orderedFiles = svgFiles.filter { it.isFile && it.extension.equals("svg", ignoreCase = true) }
            .sortedBy { it.nameWithoutExtension }
        val glyphs = mutableListOf<GlyphDefinition>()
        val metadata = mutableListOf<GeneratedGlyph>()
        val usedGlyphNames = mutableSetOf<String>()
        var codePoint = codePointStart
        var maxPoints = 0
        var maxContours = 0

        for ((index, file) in orderedFiles.withIndex()) {
            val svgGlyph = parser.parse(file)
            val contours = convertContours(svgGlyph)
            if (contours.isEmpty()) {
                continue
            }
            val cleanedContours = orientContours(contours)
            val bounds = computeBounds(cleanedContours) ?: continue
            maxPoints = maxOf(maxPoints, cleanedContours.sumOf { it.points.size })
            maxContours = maxOf(maxContours, cleanedContours.size)
            val glyphName = uniqueGlyphName(
                sanitizeGlyphName(file.nameWithoutExtension, index),
                usedGlyphNames
            )
            glyphs += GlyphDefinition(
                name = glyphName,
                codePoint = codePoint,
                contours = cleanedContours,
                advanceWidth = config.advanceWidth,
                leftSideBearing = bounds.xMin,
                bounds = bounds
            )
            val width = bounds.xMax - bounds.xMin
            val center = (bounds.xMin + bounds.xMax) / 2.0
            metadata += GeneratedGlyph(
                name = glyphName,
                codePoint = codePoint,
                glyphIndex = glyphs.size,
                displayName = file.nameWithoutExtension
            )
            codePoint += 1
        }

        val notDef = GlyphDefinition.notDef(config.advanceWidth)
        val allGlyphs = mutableListOf(notDef)
        allGlyphs.addAll(glyphs)

        val writer = TrueTypeWriter(
            config = config,
            fontName = fontName,
            fontStyle = fontStyle,
            glyphs = allGlyphs,
            maxPoints = maxPoints,
            maxContours = maxContours
        )
        val bytes = writer.write()
        return IconFontResult(bytes, metadata)
    }

    private fun convertContours(svgGlyph: SvgGlyphData): List<GlyphContour> {
        val viewBox = svgGlyph.viewBox
        val verticalRange = config.ascender - config.descender
        val verticalMargin = verticalRange * config.verticalMarginRatio
        val horizontalMargin = config.advanceWidth * config.horizontalMarginRatio
        val availableHeight = verticalRange - 2 * verticalMargin
        val availableWidth = config.advanceWidth - 2 * horizontalMargin
        val scale = min(
            if (viewBox.height != 0.0) availableHeight / viewBox.height else 1.0,
            if (viewBox.width != 0.0) availableWidth / viewBox.width else 1.0
        )
        val rawContours = mutableListOf<List<FloatPoint>>()
        var rawMinX = Double.POSITIVE_INFINITY
        var rawMaxX = Double.NEGATIVE_INFINITY
        var rawMinY = Double.POSITIVE_INFINITY
        var rawMaxY = Double.NEGATIVE_INFINITY

        for (contour in svgGlyph.contours) {
            if (contour.isEmpty()) continue
            val points = contour.map { point ->
                val normalizedX = (point.x - viewBox.minX) * scale
                val normalizedY = (point.y - viewBox.minY) * scale
                val flippedY = (viewBox.height * scale) - normalizedY
                val px = normalizedX
                val py = flippedY
                if (px < rawMinX) rawMinX = px
                if (px > rawMaxX) rawMaxX = px
                if (py < rawMinY) rawMinY = py
                if (py > rawMaxY) rawMaxY = py
                FloatPoint(px, py)
            }
            if (points.size >= 3) {
                rawContours += points
            }
        }

        if (rawContours.isEmpty()) return emptyList()

        val targetCenterX = config.advanceWidth / 2.0
        val targetCenterY = (config.ascender + config.descender) / 2.0
        val shapeCenterX = (rawMinX + rawMaxX) / 2.0
        val shapeCenterY = (rawMinY + rawMaxY) / 2.0
        val xShift = targetCenterX - shapeCenterX
        val yShift = targetCenterY - shapeCenterY

        var contours = mutableListOf<GlyphContour>()
        for (rawContour in rawContours) {
            val points = rawContour.map { point ->
                val x = point.x + xShift
                val y = point.y + yShift
                IntPoint(x.roundToInt(), y.roundToInt())
            }.removeSequentialDuplicates()
            if (points.size >= 3) {
                contours += GlyphContour(points)
            }
        }
        if (contours.isNotEmpty()) {
            val allPoints = contours.flatMap { it.points }
            val bounds = GlyphBounds.fromPoints(allPoints)
            val actualCenter = (bounds.xMin + bounds.xMax) / 2.0
            val desiredCenter = config.advanceWidth / 2.0
            val correction = (desiredCenter - actualCenter).roundToInt()
            if (correction != 0) {
                contours = contours.map { contour ->
                    GlyphContour(contour.points.map { point ->
                        IntPoint(point.x + correction, point.y)
                    })
                }.toMutableList()
            }
        }
        return contours
    }

    private fun orientContours(contours: List<GlyphContour>): List<GlyphContour> {
        if (contours.isEmpty()) return contours
        val mutable = contours.map { it.points.toMutableList() }.toMutableList()
        val bounds = mutable.map { GlyphBounds.fromPoints(it) }
        val orientedContours = mutableListOf<GlyphContour>()
        for (i in mutable.indices) {
            val points = mutable[i]
            val testPoint = points.first()
            var containment = 0
            for (j in mutable.indices) {
                if (i == j) continue
                if (!bounds[j].contains(testPoint)) continue
                if (testPoint.isInside(mutable[j])) {
                    containment++
                }
            }
            val isHole = containment % 2 == 1
            val contour = if (isHole) {
                if (computeSignedArea(points) < 0) {
                    points.reverse()
                }
                GlyphContour(points.toList())
            } else {
                if (computeSignedArea(points) > 0) {
                    points.reverse()
                }
                GlyphContour(points.toList())
            }
            orientedContours += contour
        }
        return orientedContours
    }

    private fun computeSignedArea(points: List<IntPoint>): Double {
        var area = 0.0
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            area += (p1.x * p2.y - p2.x * p1.y)
        }
        return area / 2.0
    }

    private fun computeBounds(contours: List<GlyphContour>): GlyphBounds? {
        if (contours.isEmpty()) return null
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE
        for (contour in contours) {
            val bounds = contour.bounds()
            if (bounds.xMin < minX) minX = bounds.xMin
            if (bounds.xMax > maxX) maxX = bounds.xMax
            if (bounds.yMin < minY) minY = bounds.yMin
            if (bounds.yMax > maxY) maxY = bounds.yMax
        }
        return GlyphBounds(minX, minY, maxX, maxY)
    }

    private fun sanitizeGlyphName(name: String, index: Int): String {
        val base = name.lowercase().replace("[^a-z0-9_]+".toRegex(), "_").trim('_')
        val fallback = if (base.isEmpty()) "icon_$index" else base
        return if (fallback.length <= 31) fallback else fallback.take(31)
    }

    private fun uniqueGlyphName(candidate: String, used: MutableSet<String>): String {
        var result = candidate
        var suffix = 1
        while (!used.add(result)) {
            val suffixText = "_${suffix++}"
            val maxLength = (31 - suffixText.length).coerceAtLeast(1)
            val trimmed = if (candidate.length > maxLength) candidate.take(maxLength) else candidate
            result = trimmed + suffixText
        }
        return result
    }
}

data class FontConfig(
    val unitsPerEm: Int = 2048,
    val ascender: Int = 1640,
    val descender: Int = -408,
    val advanceWidth: Int = 2048,
    val horizontalMarginRatio: Double = 0.08,
    val verticalMarginRatio: Double = 0.08
)

data class IconFontResult(
    val bytes: ByteArray,
    val generatedGlyphs: List<GeneratedGlyph>
)

data class GeneratedGlyph(
    val name: String,
    val codePoint: Int,
    val glyphIndex: Int,
    val displayName: String
)

data class GlyphDefinition(
    val name: String,
    val codePoint: Int?,
    val contours: List<GlyphContour>,
    val advanceWidth: Int,
    val leftSideBearing: Int,
    val bounds: GlyphBounds? = null
) {
    companion object {
        fun notDef(advanceWidth: Int): GlyphDefinition = GlyphDefinition(
            name = ".notdef",
            codePoint = null,
            contours = emptyList(),
            advanceWidth = advanceWidth,
            leftSideBearing = 0,
            bounds = GlyphBounds(0, 0, 0, 0)
        )
    }
}
