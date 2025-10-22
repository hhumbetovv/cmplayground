package iconfont

import org.apache.batik.parser.AWTPathProducer
import org.apache.batik.parser.PathParser
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.Shape
import java.awt.geom.AffineTransform
import java.awt.geom.FlatteningPathIterator
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.min

class SvgParser(private val flatteningTolerance: Double = 0.1) {
    fun parse(file: File): SvgGlyphData {
        val document = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }.newDocumentBuilder().parse(file)
        val svgElement = document.documentElement
            ?: error("File ${file.path} does not contain an <svg> element")
        require(svgElement.tagName.equals("svg", ignoreCase = true)) {
            "Root element of ${file.path} must be <svg>"
        }

        val viewBox = extractViewBox(svgElement)
        val shape = collectShape(svgElement, AffineTransform())
        val contours = extractContours(shape)
        return SvgGlyphData(contours = contours, viewBox = viewBox)
    }

    private fun collectShape(node: Element, inheritedTransform: AffineTransform): Shape {
        val aggregate = GeneralPath(PathIterator.WIND_NON_ZERO)
        val transform = inheritedTransform.copy().apply {
            val raw = node.getAttribute("transform")
            if (!raw.isNullOrBlank()) concatenate(parseTransform(raw))
        }

        when (node.tagName.lowercase()) {
            "path" -> {
                val d = node.getAttribute("d")
                if (!d.isNullOrBlank()) {
                    val parser = PathParser()
                    val producer = AWTPathProducer()
                    producer.setWindingRule(PathIterator.WIND_NON_ZERO)
                    parser.pathHandler = producer
                    parser.parse(d)
                    aggregate.append(applyTransform(producer.shape, transform), false)
                }
            }
            "rect" -> buildRect(node)?.let { aggregate.append(applyTransform(it, transform), false) }
            "circle" -> buildCircle(node)?.let { aggregate.append(applyTransform(it, transform), false) }
            "ellipse" -> buildEllipse(node)?.let { aggregate.append(applyTransform(it, transform), false) }
            "polygon" -> buildPolygon(node, close = true)?.let { aggregate.append(applyTransform(it, transform), false) }
            "polyline" -> buildPolygon(node, close = false)?.let { aggregate.append(applyTransform(it, transform), false) }
            "g", "svg" -> {
                val list = node.childNodes
                for (i in 0 until list.length) {
                    val item = list.item(i)
                    if (item.nodeType == Node.ELEMENT_NODE) {
                        aggregate.append(collectShape(item as Element, transform), false)
                    }
                }
            }
            else -> {
                val list = node.childNodes
                for (i in 0 until list.length) {
                    val item = list.item(i)
                    if (item.nodeType == Node.ELEMENT_NODE) {
                        aggregate.append(collectShape(item as Element, transform), false)
                    }
                }
            }
        }
        return aggregate
    }

    private fun extractContours(shape: Shape): List<List<FloatPoint>> {
        val contours = mutableListOf<MutableList<FloatPoint>>()
        val iterator = FlatteningPathIterator(shape.getPathIterator(null), flatteningTolerance)
        val coords = DoubleArray(6)
        var current: MutableList<FloatPoint>? = null
        var startX = 0.0
        var startY = 0.0
        while (!iterator.isDone) {
            when (iterator.currentSegment(coords)) {
                PathIterator.SEG_MOVETO -> {
                    if (current != null && current.size >= 3) {
                        contours.add(current)
                    }
                    current = mutableListOf()
                    startX = coords[0]
                    startY = coords[1]
                    current.add(FloatPoint(coords[0], coords[1]))
                }
                PathIterator.SEG_LINETO -> {
                    current?.add(FloatPoint(coords[0], coords[1]))
                }
                PathIterator.SEG_CLOSE -> {
                    if (current != null) {
                        current.add(FloatPoint(startX, startY))
                        if (current.size >= 3) {
                            contours.add(current)
                        }
                        current = null
                    }
                }
            }
            iterator.next()
        }
        if (current != null && current.size >= 3) {
            contours.add(current)
        }
        return contours
    }

    private fun extractViewBox(element: Element): ViewBox {
        val viewBoxAttr = element.getAttribute("viewBox")
        if (!viewBoxAttr.isNullOrBlank()) {
            val values = parseNumberList(viewBoxAttr)
            require(values.size == 4) { "viewBox must contain four numbers" }
            val width = values[2]
            val height = values[3]
            require(width > 0.0 && height > 0.0) { "viewBox width and height must be positive" }
            return ViewBox(values[0], values[1], width, height)
        }
        val width = parseLength(element.getAttribute("width"), 24.0)
        val height = parseLength(element.getAttribute("height"), 24.0)
        return ViewBox(0.0, 0.0, width, height)
    }

    private fun buildRect(element: Element): Shape? {
        val width = parseLength(element.getAttribute("width"), 0.0)
        val height = parseLength(element.getAttribute("height"), 0.0)
        if (width <= 0.0 || height <= 0.0) return null
        val x = parseLength(element.getAttribute("x"), 0.0)
        val y = parseLength(element.getAttribute("y"), 0.0)
        val rx = parseLength(element.getAttribute("rx"), 0.0)
        val ry = parseLength(element.getAttribute("ry"), rx)
        val path = GeneralPath(PathIterator.WIND_NON_ZERO)
        if (rx <= 0.0 && ry <= 0.0) {
            path.moveTo(x, y)
            path.lineTo(x + width, y)
            path.lineTo(x + width, y + height)
            path.lineTo(x, y + height)
            path.closePath()
        } else {
            val radiusX = min(rx, width / 2.0)
            val radiusY = min(ry, height / 2.0)
            addRoundedRect(path, x, y, width, height, radiusX, radiusY)
        }
        return path
    }

    private fun buildCircle(element: Element): Shape? {
        val r = parseLength(element.getAttribute("r"), 0.0)
        if (r <= 0.0) return null
        val cx = parseLength(element.getAttribute("cx"), 0.0)
        val cy = parseLength(element.getAttribute("cy"), 0.0)
        return buildEllipse(cx, cy, r, r)
    }

    private fun buildEllipse(element: Element): Shape? {
        val rx = parseLength(element.getAttribute("rx"), 0.0)
        val ry = parseLength(element.getAttribute("ry"), 0.0)
        if (rx <= 0.0 || ry <= 0.0) return null
        val cx = parseLength(element.getAttribute("cx"), 0.0)
        val cy = parseLength(element.getAttribute("cy"), 0.0)
        return buildEllipse(cx, cy, rx, ry)
    }

    private fun buildEllipse(cx: Double, cy: Double, rx: Double, ry: Double): Shape {
        val path = GeneralPath(PathIterator.WIND_NON_ZERO)
        val magic = 0.5522847498307933
        val dx = rx * magic
        val dy = ry * magic
        path.moveTo(cx, cy - ry)
        path.curveTo(cx + dx, cy - ry, cx + rx, cy - dy, cx + rx, cy)
        path.curveTo(cx + rx, cy + dy, cx + dx, cy + ry, cx, cy + ry)
        path.curveTo(cx - dx, cy + ry, cx - rx, cy + dy, cx - rx, cy)
        path.curveTo(cx - rx, cy - dy, cx - dx, cy - ry, cx, cy - ry)
        path.closePath()
        return path
    }

    private fun buildPolygon(element: Element, close: Boolean): Shape? {
        val raw = element.getAttribute("points")
        if (raw.isNullOrBlank()) return null
        val numbers = parseNumberList(raw)
        if (numbers.size < 4) return null
        val path = GeneralPath(PathIterator.WIND_NON_ZERO)
        path.moveTo(numbers[0], numbers[1])
        var i = 2
        while (i + 1 < numbers.size) {
            path.lineTo(numbers[i], numbers[i + 1])
            i += 2
        }
        if (close) path.closePath()
        return path
    }

    private fun applyTransform(shape: Shape, transform: AffineTransform): Shape {
        return transform.createTransformedShape(shape)
    }

    private fun addRoundedRect(
        path: GeneralPath,
        x: Double,
        y: Double,
        width: Double,
        height: Double,
        rx: Double,
        ry: Double
    ) {
        val kappa = 0.5522847498307933
        val dx = rx * kappa
        val dy = ry * kappa
        val right = x + width
        val bottom = y + height

        path.moveTo(x + rx, y)
        path.lineTo(right - rx, y)
        path.curveTo(right - dx, y, right, y + ry - dy, right, y + ry)
        path.lineTo(right, bottom - ry)
        path.curveTo(right, bottom - dy, right - dx, bottom, right - rx, bottom)
        path.lineTo(x + rx, bottom)
        path.curveTo(x + dx, bottom, x, bottom - dy, x, bottom - ry)
        path.lineTo(x, y + ry)
        path.curveTo(x, y + dy, x + dx, y, x + rx, y)
        path.closePath()
    }

    private fun parseTransform(raw: String): AffineTransform {
        val transform = AffineTransform()
        val regex = Regex("([a-zA-Z]+)\\(([^)]*)\\)")
        for (match in regex.findAll(raw)) {
            val name = match.groupValues[1].lowercase()
            val values = parseNumberList(match.groupValues[2])
            when (name) {
                "translate" -> {
                    val tx = values.getOrNull(0) ?: 0.0
                    val ty = values.getOrNull(1) ?: 0.0
                    transform.concatenate(AffineTransform.getTranslateInstance(tx, ty))
                }
                "scale" -> {
                    val sx = values.getOrNull(0) ?: 1.0
                    val sy = values.getOrNull(1) ?: sx
                    transform.concatenate(AffineTransform.getScaleInstance(sx, sy))
                }
                "rotate" -> {
                    val angle = values.getOrNull(0) ?: 0.0
                    val theta = Math.toRadians(angle)
                    val rotate = if (values.size >= 3) {
                        AffineTransform.getRotateInstance(theta, values[1], values[2])
                    } else {
                        AffineTransform.getRotateInstance(theta)
                    }
                    transform.concatenate(rotate)
                }
                "skewx" -> {
                    val angle = values.getOrNull(0) ?: 0.0
                    transform.concatenate(AffineTransform(1.0, 0.0, kotlin.math.tan(Math.toRadians(angle)), 1.0, 0.0, 0.0))
                }
                "skewy" -> {
                    val angle = values.getOrNull(0) ?: 0.0
                    transform.concatenate(AffineTransform(1.0, kotlin.math.tan(Math.toRadians(angle)), 0.0, 1.0, 0.0, 0.0))
                }
                "matrix" -> {
                    require(values.size == 6) { "matrix transform expects six numbers" }
                    transform.concatenate(AffineTransform(values[0], values[1], values[2], values[3], values[4], values[5]))
                }
            }
        }
        return transform
    }

    private fun parseNumberList(raw: String): List<Double> {
        if (raw.isBlank()) return emptyList()
        val cleaned = raw.replace(",", " ")
        return cleaned.split(" ")
            .mapNotNull { token -> token.trim().takeIf { it.isNotEmpty() }?.toDouble() }
    }

    private fun parseLength(raw: String?, defaultValue: Double): Double {
        if (raw.isNullOrBlank()) return defaultValue
        val cleaned = raw.trim()
        val value = cleaned.takeWhile { it.isDigit() || it == '-' || it == '+' || it == '.' || it == 'e' || it == 'E' }
        return value.toDoubleOrNull() ?: defaultValue
    }
}

data class SvgGlyphData(
    val contours: List<List<FloatPoint>>,
    val viewBox: ViewBox
)

private fun AffineTransform.copy(): AffineTransform = AffineTransform(this)
