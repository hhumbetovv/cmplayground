package iconfont

import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.roundToInt

class TrueTypeWriter(
    private val config: FontConfig,
    private val fontName: String,
    private val fontStyle: String,
    private val glyphs: List<GlyphDefinition>,
    private val maxPoints: Int,
    private val maxContours: Int
) {
    private data class Table(
        val tag: String,
        val data: ByteArray,
        var checksum: Int = 0,
        var offset: Int = 0
    ) {
        val length: Int get() = data.size
        val paddedLength: Int
            get() {
                val padding = (4 - (length % 4)) % 4
                return length + padding
            }
    }

    private data class GlyphMetrics(
        val advanceWidth: Int,
        val leftSideBearing: Int,
        val bounds: GlyphBounds
    )

    fun write(): ByteArray {
        require(glyphs.isNotEmpty()) { "Font must contain at least the .notdef glyph" }
        val cmapEntries = buildCmapEntries()
        val glyfData = buildGlyfTable()
        val hmtxData = buildHmtxTable(glyfData.metrics)
        val tables = mutableListOf<Table>()
        tables += Table("cmap", buildCmapTable(cmapEntries))
        tables += Table("glyf", glyfData.bytes)
        tables += Table("loca", buildLocaTable(glyfData.offsets))
        tables += Table("head", buildHeadTable(glyfData.fontBounds, glyfData.indexToLocFormat))
        tables += Table("hhea", buildHheaTable(glyfData.metrics))
        tables += Table("hmtx", hmtxData)
        tables += Table("maxp", buildMaxpTable())
        tables += Table("name", buildNameTable())
        tables += Table("OS/2", buildOs2Table(cmapEntries))
        tables += Table("post", buildPostTable())

        computeChecksums(tables)
        return assembleSfnt(tables)
    }

    private fun buildCmapEntries(): List<Pair<Int, Int>> {
        val entries = mutableListOf<Pair<Int, Int>>()
        glyphs.forEachIndexed { index, glyph ->
            val codePoint = glyph.codePoint ?: return@forEachIndexed
            require(codePoint in 0..0xFFFF) { "Only BMP code points are supported" }
            entries += codePoint to index
        }
        return entries.sortedBy { it.first }
    }

    private fun buildGlyfTable(): GlyfTableData {
        val output = ByteArrayOutputStream()
        val offsets = IntArray(glyphs.size + 1)
        val metrics = mutableListOf<GlyphMetrics>()
        var offset = 0
        var globalXMin = Int.MAX_VALUE
        var globalYMin = Int.MAX_VALUE
        var globalXMax = Int.MIN_VALUE
        var globalYMax = Int.MIN_VALUE

        glyphs.forEachIndexed { index, glyph ->
            offsets[index] = offset
            val glyphBytes = buildGlyphBytes(glyph)
            output.write(glyphBytes)
            offset += glyphBytes.size

            val bounds = glyph.bounds ?: GlyphBounds(0, 0, 0, 0)
            metrics += GlyphMetrics(
                advanceWidth = glyph.advanceWidth,
                leftSideBearing = glyph.leftSideBearing,
                bounds = bounds
            )
            if (glyph.contours.isNotEmpty()) {
                if (bounds.xMin < globalXMin) globalXMin = bounds.xMin
                if (bounds.yMin < globalYMin) globalYMin = bounds.yMin
                if (bounds.xMax > globalXMax) globalXMax = bounds.xMax
                if (bounds.yMax > globalYMax) globalYMax = bounds.yMax
            }
        }
        offsets[glyphs.size] = offset

        if (globalXMin == Int.MAX_VALUE) {
            globalXMin = 0
            globalYMin = 0
            globalXMax = 0
            globalYMax = 0
        }

        val indexToLocFormat = 1 // use long offsets
        return GlyfTableData(
            bytes = output.toByteArray(),
            offsets = offsets,
            metrics = metrics,
            fontBounds = GlyphBounds(globalXMin, globalYMin, globalXMax, globalYMax),
            indexToLocFormat = indexToLocFormat
        )
    }

    private fun buildGlyphBytes(glyph: GlyphDefinition): ByteArray {
        if (glyph.contours.isEmpty()) {
            val writer = DataWriter()
            writer.writeInt16(0)
            writer.writeInt16(0)
            writer.writeInt16(0)
            writer.writeInt16(0)
            writer.writeInt16(0)
            writer.writeUInt16(0)
            return writer.toByteArray()
        }
        val points = glyph.contours.flatMap { it.points }
        val endPts = mutableListOf<Int>()
        var pointCount = 0
        for (contour in glyph.contours) {
            pointCount += contour.points.size
            endPts += pointCount - 1
        }
        val flags = ArrayList<Int>(points.size)
        val xDeltas = ArrayList<Int>()
        val yDeltas = ArrayList<Int>()
        var prevX = 0
        var prevY = 0
        for (point in points) {
            var flag = ON_CURVE
            val dx = point.x - prevX
            val dy = point.y - prevY
            if (dx == 0) {
                flag = flag or X_SAME
            } else {
                xDeltas += dx
            }
            if (dy == 0) {
                flag = flag or Y_SAME
            } else {
                yDeltas += dy
            }
            flags += flag
            prevX = point.x
            prevY = point.y
        }

        val bounds = glyph.bounds ?: GlyphBounds(0, 0, 0, 0)
        val writer = DataWriter()
        writer.writeInt16(glyph.contours.size)
        writer.writeInt16(bounds.xMin)
        writer.writeInt16(bounds.yMin)
        writer.writeInt16(bounds.xMax)
        writer.writeInt16(bounds.yMax)
        endPts.forEach { writer.writeUInt16(it) }
        writer.writeUInt16(0) // instruction length
        flags.forEach { writer.writeUInt8(it) }

        prevX = 0
        var deltaIndex = 0
        for (flag in flags) {
            if ((flag and X_SAME) != 0 && (flag and X_SHORT) == 0) {
                continue
            }
            val delta = xDeltas[deltaIndex++]
            writer.writeInt16(delta)
        }

        prevY = 0
        deltaIndex = 0
        for (flag in flags) {
            if ((flag and Y_SAME) != 0 && (flag and Y_SHORT) == 0) {
                continue
            }
            val delta = yDeltas[deltaIndex++]
            writer.writeInt16(delta)
        }
        return writer.toByteArray()
    }

    private fun buildLocaTable(offsets: IntArray): ByteArray {
        val writer = DataWriter()
        offsets.forEach { offset -> writer.writeUInt32(offset) }
        return writer.toByteArray()
    }

    private fun buildHmtxTable(metrics: List<GlyphMetrics>): ByteArray {
        val writer = DataWriter()
        metrics.forEach { metric ->
            writer.writeUInt16(metric.advanceWidth)
            writer.writeInt16(metric.leftSideBearing)
        }
        return writer.toByteArray()
    }

    private fun buildCmapTable(entries: List<Pair<Int, Int>>): ByteArray {
        val writer = DataWriter()
        writer.writeUInt16(0) // version
        writer.writeUInt16(1) // numTables
        val encodingRecordOffset = 12
        writer.writeUInt16(3) // platform ID (Windows)
        writer.writeUInt16(1) // encoding ID (Unicode BMP)
        writer.writeUInt32(encodingRecordOffset)

        val segmentCount = entries.size + 1
        val segCountX2 = segmentCount * 2
        val maxPower = 1 shl (floorLog2(segmentCount))
        val searchRange = maxPower * 2
        val entrySelector = floorLog2(segmentCount)
        val rangeShift = segCountX2 - searchRange

        val formatWriter = DataWriter()
        formatWriter.writeUInt16(4) // format
        val length = 16 + segmentCount * 8
        formatWriter.writeUInt16(length)
        formatWriter.writeUInt16(0) // language
        formatWriter.writeUInt16(segCountX2)
        formatWriter.writeUInt16(searchRange)
        formatWriter.writeUInt16(entrySelector)
        formatWriter.writeUInt16(rangeShift)

        entries.forEach { (codePoint, _) -> formatWriter.writeUInt16(codePoint) }
        formatWriter.writeUInt16(0xFFFF) // sentinel endCode
        formatWriter.writeUInt16(0) // reservedPad
        entries.forEach { (codePoint, _) -> formatWriter.writeUInt16(codePoint) }
        formatWriter.writeUInt16(0xFFFF)
        entries.forEach { (codePoint, glyphIndex) ->
            val delta = (glyphIndex - codePoint).floorMod(0x10000)
            formatWriter.writeUInt16(delta)
        }
        formatWriter.writeUInt16(1)
        repeat(segmentCount) { formatWriter.writeUInt16(0) }

        writer.writeBytes(formatWriter.toByteArray())
        return writer.toByteArray()
    }

    private fun buildHeadTable(bounds: GlyphBounds, indexToLocFormat: Int): ByteArray {
        val writer = DataWriter()
        writer.writeUInt32(0x00010000)
        writer.writeUInt32(0x00010000)
        writer.writeUInt32(0) // checksum adjustment placeholder
        writer.writeUInt32(0x5F0F3CF5)
        writer.writeUInt16(0x0003)
        writer.writeUInt16(config.unitsPerEm)
        val timestamp = macTimestamp()
        writer.writeUInt64(timestamp)
        writer.writeUInt64(timestamp)
        writer.writeInt16(bounds.xMin)
        writer.writeInt16(bounds.yMin)
        writer.writeInt16(bounds.xMax)
        writer.writeInt16(bounds.yMax)
        writer.writeUInt16(0) // macStyle
        writer.writeUInt16(8) // lowestRecPPEM
        writer.writeInt16(2) // fontDirectionHint
        writer.writeInt16(indexToLocFormat)
        writer.writeInt16(0) // glyphDataFormat
        return writer.toByteArray()
    }

    private fun buildHheaTable(metrics: List<GlyphMetrics>): ByteArray {
        var advanceWidthMax = 0
        var minLeftSideBearing = Int.MAX_VALUE
        var minRightSideBearing = Int.MAX_VALUE
        var xMaxExtent = Int.MIN_VALUE
        metrics.forEach { metric ->
            val bounds = metric.bounds
            advanceWidthMax = max(advanceWidthMax, metric.advanceWidth)
            minLeftSideBearing = minOf(minLeftSideBearing, metric.leftSideBearing)
            val glyphWidth = bounds.xMax - bounds.xMin
            val rightSideBearing = metric.advanceWidth - metric.leftSideBearing - glyphWidth
            minRightSideBearing = minOf(minRightSideBearing, rightSideBearing)
            val extent = metric.leftSideBearing + glyphWidth
            xMaxExtent = max(xMaxExtent, extent)
        }
        if (minLeftSideBearing == Int.MAX_VALUE) minLeftSideBearing = 0
        if (minRightSideBearing == Int.MAX_VALUE) minRightSideBearing = 0
        if (xMaxExtent == Int.MIN_VALUE) xMaxExtent = 0

        val writer = DataWriter()
        writer.writeUInt32(0x00010000)
        writer.writeInt16(config.ascender)
        writer.writeInt16(config.descender)
        writer.writeInt16(0) // lineGap
        writer.writeUInt16(advanceWidthMax)
        writer.writeInt16(minLeftSideBearing)
        writer.writeInt16(minRightSideBearing)
        writer.writeInt16(xMaxExtent)
        writer.writeInt16(1) // caretSlopeRise
        writer.writeInt16(0) // caretSlopeRun
        writer.writeInt16(0) // caretOffset
        repeat(4) { writer.writeInt16(0) }
        writer.writeInt16(0) // metricDataFormat
        writer.writeUInt16(metrics.size)
        return writer.toByteArray()
    }

    private fun buildMaxpTable(): ByteArray {
        val writer = DataWriter()
        writer.writeUInt32(0x00010000)
        writer.writeUInt16(glyphs.size)
        writer.writeUInt16(maxPoints)
        writer.writeUInt16(maxContours)
        writer.writeUInt16(0) // maxCompositePoints
        writer.writeUInt16(0) // maxCompositeContours
        writer.writeUInt16(2) // maxZones
        writer.writeUInt16(0) // maxTwilightPoints
        writer.writeUInt16(0) // maxStorage
        writer.writeUInt16(0) // maxFunctionDefs
        writer.writeUInt16(0) // maxInstructionDefs
        writer.writeUInt16(0) // maxStackElements
        writer.writeUInt16(0) // maxSizeOfInstructions
        writer.writeUInt16(0) // maxComponentElements
        writer.writeUInt16(0) // maxComponentDepth
        return writer.toByteArray()
    }

    private fun buildNameTable(): ByteArray {
        val records = listOf(
            NameRecord(1, fontName),
            NameRecord(2, fontStyle),
            NameRecord(4, "$fontName $fontStyle".trim()),
            NameRecord(6, sanitizePostScriptName(fontName, fontStyle))
        )
        val writer = DataWriter()
        writer.writeUInt16(0)
        writer.writeUInt16(records.size)
        val stringOffset = 6 + records.size * 12
        writer.writeUInt16(stringOffset)

        var currentOffset = 0
        val stringData = ByteArrayOutputStream()
        for (record in records) {
            val bytes = record.value.toByteArray(Charsets.UTF_16BE)
            writer.writeUInt16(3)
            writer.writeUInt16(1)
            writer.writeUInt16(0x0409)
            writer.writeUInt16(record.nameId)
            writer.writeUInt16(bytes.size)
            writer.writeUInt16(currentOffset)
            stringData.write(bytes)
            currentOffset += bytes.size
        }
        writer.writeBytes(stringData.toByteArray())
        return writer.toByteArray()
    }

    private fun buildOs2Table(entries: List<Pair<Int, Int>>): ByteArray {
        val writer = DataWriter()
        val avgWidth = glyphs.drop(1).ifEmpty { listOf(glyphs.first()) }
            .map { it.advanceWidth }
            .average().roundToInt()
        val codePoints = entries.map { it.first }
        val firstChar = codePoints.minOrNull() ?: 0
        val lastChar = codePoints.maxOrNull() ?: 0
        val winAscent = config.ascender
        val winDescent = abs(config.descender)

        writer.writeUInt16(1)
        writer.writeInt16(avgWidth)
        writer.writeUInt16(400)
        writer.writeUInt16(5)
        writer.writeUInt16(0)
        writer.writeInt16((config.unitsPerEm * 0.6).toInt())
        writer.writeInt16((config.unitsPerEm * 0.6).toInt())
        writer.writeInt16(0)
        writer.writeInt16(0)
        writer.writeInt16((config.unitsPerEm * 0.6).toInt())
        writer.writeInt16((config.unitsPerEm * 0.6).toInt())
        writer.writeInt16(0)
        writer.writeInt16(0)
        writer.writeInt16((config.unitsPerEm * 0.05).toInt())
        writer.writeInt16((config.ascender * 0.7).toInt())
        writer.writeInt16(0)
        repeat(10) { writer.writeUInt8(0) }
        val unicodeRange1 = 0
        val unicodeRange2 = 1 shl 15 // Private Use Area
        writer.writeUInt32(unicodeRange1)
        writer.writeUInt32(unicodeRange2)
        writer.writeUInt32(0)
        writer.writeUInt32(0)
        writer.writeUInt8('C'.code)
        writer.writeUInt8('M'.code)
        writer.writeUInt8('P'.code)
        writer.writeUInt8('L'.code)
        writer.writeUInt16(0x0040)
        writer.writeUInt16(firstChar)
        writer.writeUInt16(lastChar)
        writer.writeInt16(config.ascender)
        writer.writeInt16(config.descender)
        writer.writeInt16(0)
        writer.writeUInt16(winAscent)
        writer.writeUInt16(winDescent)
        writer.writeUInt32(0)
        writer.writeUInt32(0)
        writer.writeInt16((config.ascender * 0.6).toInt())
        writer.writeInt16((config.ascender * 0.8).toInt())
        writer.writeUInt16(0)
        writer.writeUInt16(0)
        writer.writeUInt16(1)
        return writer.toByteArray()
    }

    private fun buildPostTable(): ByteArray {
        val writer = DataWriter()
        writer.writeUInt32(0x00030000)
        writer.writeUInt32(0)
        writer.writeInt32(0)
        writer.writeInt16(-75)
        writer.writeInt16(50)
        writer.writeUInt32(0)
        writer.writeUInt32(0)
        writer.writeUInt32(0)
        writer.writeUInt32(0)
        return writer.toByteArray()
    }

    private fun computeChecksums(tables: MutableList<Table>) {
        tables.forEach { table ->
            val padded = padToFour(table.data)
            table.checksum = calculateChecksum(padded)
        }
    }

    private fun assembleSfnt(tables: MutableList<Table>): ByteArray {
        val sortedTables = tables.sortedBy { it.tag }
        val initialData = buildSfnt(sortedTables)
        val adjustment = computeCheckSumAdjustment(initialData)
        updateHeadTable(sortedTables, adjustment)
        val headTable = sortedTables.first { it.tag == "head" }
        headTable.checksum = calculateChecksum(padToFour(headTable.data))
        return buildSfnt(sortedTables)
    }

    private fun buildSfnt(sortedTables: List<Table>): ByteArray {
        val numTables = sortedTables.size
        val searchRange = (1 shl floorLog2(numTables)) * 16
        val entrySelector = floorLog2(numTables)
        val rangeShift = numTables * 16 - searchRange

        var offset = 12 + numTables * 16
        sortedTables.forEach { table ->
            table.offset = offset
            offset += table.paddedLength
        }

        val writer = DataWriter()
        writer.writeUInt32(0x00010000)
        writer.writeUInt16(numTables)
        writer.writeUInt16(searchRange)
        writer.writeUInt16(entrySelector)
        writer.writeUInt16(rangeShift)
        sortedTables.forEach { table ->
            writer.writeTag(table.tag)
            writer.writeUInt32(table.checksum)
            writer.writeUInt32(table.offset)
            writer.writeUInt32(table.data.size)
        }
        sortedTables.forEach { table ->
            writer.writeBytes(table.data)
            val padding = table.paddedLength - table.length
            if (padding > 0) {
                writer.writeBytes(ByteArray(padding))
            }
        }
        return writer.toByteArray()
    }

    private fun computeCheckSumAdjustment(data: ByteArray): Int {
        val checksum = calculateChecksum(data)
        val adjustment = (0xB1B0AFBAL - checksum).floorMod(0x100000000L)
        return adjustment.toInt()
    }

    private fun updateHeadTable(tables: List<Table>, adjustment: Int) {
        val headTable = tables.first { it.tag == "head" }
        headTable.data[8] = (adjustment ushr 24).toByte()
        headTable.data[9] = (adjustment ushr 16).toByte()
        headTable.data[10] = (adjustment ushr 8).toByte()
        headTable.data[11] = adjustment.toByte()
        headTable.checksum = calculateChecksum(padToFour(headTable.data))
    }

    private fun calculateChecksum(data: ByteArray): Int {
        var sum = 0L
        var i = 0
        while (i < data.size) {
            val b0 = data.getOrNull(i)?.toInt()?.and(0xFF) ?: 0
            val b1 = data.getOrNull(i + 1)?.toInt()?.and(0xFF) ?: 0
            val b2 = data.getOrNull(i + 2)?.toInt()?.and(0xFF) ?: 0
            val b3 = data.getOrNull(i + 3)?.toInt()?.and(0xFF) ?: 0
            val value = (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            sum = (sum + value) and 0xFFFFFFFFL
            i += 4
        }
        return sum.toInt()
    }

    private fun macTimestamp(): Long {
        val secondsSinceEpoch = System.currentTimeMillis() / 1000L
        return secondsSinceEpoch + MAC_EPOCH_OFFSET
    }

    private fun sanitizePostScriptName(name: String, style: String): String {
        val sanitized = (name + "-" + style).replace("[^A-Za-z0-9]+".toRegex(), "")
        return if (sanitized.isEmpty()) "CMIcons" else sanitized.take(63)
    }

    private fun floorLog2(value: Int): Int {
        return if (value <= 0) 0 else log2(value.toDouble()).toInt()
    }

    private fun Int.floorMod(mod: Int): Int {
        val result = this % mod
        return if (result < 0) result + mod else result
    }

    private fun Long.floorMod(mod: Long): Long {
        val result = this % mod
        return if (result < 0) result + mod else result
    }

    private fun padToFour(data: ByteArray): ByteArray {
        val padding = (4 - (data.size % 4)) % 4
        if (padding == 0) return data
        return data + ByteArray(padding)
    }

    private data class GlyfTableData(
        val bytes: ByteArray,
        val offsets: IntArray,
        val metrics: List<GlyphMetrics>,
        val fontBounds: GlyphBounds,
        val indexToLocFormat: Int
    )

    private data class NameRecord(val nameId: Int, val value: String)

    private companion object {
        private const val ON_CURVE = 0x01
        private const val X_SHORT = 0x02
        private const val Y_SHORT = 0x04
        private const val REPEAT_FLAG = 0x08
        private const val X_SAME = 0x10
        private const val Y_SAME = 0x20
        private const val MAC_EPOCH_OFFSET = 2082844800L
    }
}

private class DataWriter {
    private val buffer = ByteArrayOutputStream()

    fun writeUInt8(value: Int) {
        buffer.write(value and 0xFF)
    }

    fun writeUInt16(value: Int) {
        buffer.write((value ushr 8) and 0xFF)
        buffer.write(value and 0xFF)
    }

    fun writeInt16(value: Int) {
        writeUInt16(value and 0xFFFF)
    }

    fun writeUInt32(value: Int) {
        buffer.write((value ushr 24) and 0xFF)
        buffer.write((value ushr 16) and 0xFF)
        buffer.write((value ushr 8) and 0xFF)
        buffer.write(value and 0xFF)
    }

    fun writeUInt32(value: Long) {
        buffer.write(((value ushr 24) and 0xFF).toInt())
        buffer.write(((value ushr 16) and 0xFF).toInt())
        buffer.write(((value ushr 8) and 0xFF).toInt())
        buffer.write((value and 0xFF).toInt())
    }

    fun writeInt32(value: Int) {
        writeUInt32(value)
    }

    fun writeUInt64(value: Long) {
        buffer.write(((value ushr 56) and 0xFF).toInt())
        buffer.write(((value ushr 48) and 0xFF).toInt())
        buffer.write(((value ushr 40) and 0xFF).toInt())
        buffer.write(((value ushr 32) and 0xFF).toInt())
        buffer.write(((value ushr 24) and 0xFF).toInt())
        buffer.write(((value ushr 16) and 0xFF).toInt())
        buffer.write(((value ushr 8) and 0xFF).toInt())
        buffer.write((value and 0xFF).toInt())
    }

    fun writeTag(tag: String) {
        require(tag.length == 4) { "Table tag must be four characters" }
        tag.forEach { buffer.write(it.code) }
    }

    fun writeBytes(bytes: ByteArray) {
        buffer.write(bytes)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
