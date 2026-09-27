package com.example.data.model

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min

enum class ToolType(val label: String, val isEssential: Boolean = false) {
    SELECT("Select", true),
    PEN("Pen", true),
    SMART("Smart", false),
    SHAPES("Shapes", false),
    ERASER("Eraser", true),
    BOARD("Board", false),
    TEXT("Text", false),
    FORMULA("Formula", false),
    TABLE("Table", false),
    LAYERS("Layers", false),
    IMAGE("Image", false),
    WEB_IMAGE("Web Image", false),
    CAMERA("Camera", false),
    SAVE("Save", true),
    SHARE("Share", true),
    PDF("PDF", true),
    UNDO("Undo", true),
    REDO("Redo", true),
    CLEAR("Clear", true),
    OCR("OCR", false),
    IMPORT_PDF("Import PDF", false),
    STICKERS("Stickers", false),
    SCIENCE("Science", false),
    LASER("Laser", false),
    RULER("Ruler", false),
    LASSO("Lasso", false),
    HAND_PAN("Hand/Pan", false),
    CUSTOMIZE("Customize", false),
    HELP("Help", false)
}

enum class BrushType(val label: String) {
    PEN("Pen"),
    BRUSH("Brush"),
    MARKER("Marker"),
    HIGHLIGHT("Highlight"),
    CALLIGRAPHY("Calligraphy")
}

enum class ShapeType(val label: String, val is3D: Boolean = false) {
    LINE("Line"),
    ARROW("Arrow"),
    RECT("Rect"),
    SQUARE("Square"),
    CIRCLE("Circle"),
    OVAL("Oval"),
    TRIANGLE("Triangle"),
    RIGHT_TRIANGLE("Right △"),
    STAR("Star"),
    PENTAGON("Pentagon"),
    HEXAGON("Hexagon"),
    DIAMOND("Diamond"),
    PARALLEL("Parallel"),
    TRAPEZOID("Trapezoid"),
    HEART("Heart"),
    CROSS("Cross"),
    SEMICIRCLE("Semicircle"),
    SECTOR("Sector"),
    RIGHT_ANGLE("Right ∠"),
    // 3D Shapes
    CUBE("Cube", true),
    CYLINDER("Cylinder", true),
    CONE("Cone", true),
    SPHERE("Sphere", true),
    PYRAMID("Pyramid", true)
}

enum class BoardTexture(val label: String) {
    PLAIN("Plain"),
    GRID("Grid"),
    LINES("Lines"),
    DOTS("Dots"),
    CHALK("Chalk"),
    ISOMETRIC("Isometric"),
    GRAPH("Graph"),
    MUSIC("Music"),
    DIAMOND("Diamond"),
    NOTEBOOK("Notebook")
}

data class LayerInfo(
    val id: String = "layer_1",
    val name: String = "Layer 1",
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
)

data class PointF(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

sealed class CanvasElement {
    abstract val id: String
    abstract val layerId: String
    abstract val rotation: Float
    abstract val scale: Float

    abstract fun getCenter(): PointF
    abstract fun getBounds(): Rect
    abstract fun withTranslation(dx: Float, dy: Float): CanvasElement
    abstract fun withRotation(newRotation: Float): CanvasElement
    abstract fun withScale(newScale: Float): CanvasElement
    abstract fun duplicate(newId: String = "dup_${System.currentTimeMillis()}"): CanvasElement

    data class Stroke(
        override val id: String,
        override val layerId: String,
        val points: List<PointF>,
        val colorArgb: Int,
        val strokeWidth: Float,
        val brushType: BrushType = BrushType.PEN,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            if (points.isEmpty()) return Rect(0f, 0f, 0f, 0f)
            var minX = Float.MAX_VALUE
            var maxX = -Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxY = -Float.MAX_VALUE
            for (p in points) {
                if (p.x < minX) minX = p.x
                if (p.x > maxX) maxX = p.x
                if (p.y < minY) minY = p.y
                if (p.y > maxY) maxY = p.y
            }
            val pad = strokeWidth * scale / 2f + 4f
            return Rect(minX - pad, minY - pad, maxX + pad, maxY + pad)
        }

        override fun getCenter(): PointF {
            val b = getBounds()
            return PointF((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
        }

        override fun withTranslation(dx: Float, dy: Float): CanvasElement {
            val moved = points.map { PointF(it.x + dx, it.y + dy, it.pressure) }
            return copy(points = moved)
        }

        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Shape(
        override val id: String,
        override val layerId: String,
        val shapeType: ShapeType,
        val startX: Float,
        val startY: Float,
        val endX: Float,
        val endY: Float,
        val colorArgb: Int,
        val strokeWidth: Float,
        val isFilled: Boolean = false,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val left = min(startX, endX)
            val right = max(startX, endX)
            val top = min(startY, endY)
            val bottom = max(startY, endY)
            val pad = strokeWidth * scale / 2f + 4f
            return Rect(left - pad, top - pad, right + pad, bottom + pad)
        }

        override fun getCenter(): PointF = PointF((startX + endX) / 2f, (startY + endY) / 2f)

        override fun withTranslation(dx: Float, dy: Float): CanvasElement =
            copy(startX = startX + dx, startY = startY + dy, endX = endX + dx, endY = endY + dy)

        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Text(
        override val id: String,
        override val layerId: String,
        val text: String,
        val x: Float,
        val y: Float,
        val colorArgb: Int,
        val fontSize: Float = 48f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val charWidth = fontSize * 0.6f
            val w = max(80f, text.length * charWidth)
            val h = fontSize * 1.25f
            return Rect(x - 8f, y - h, x + w + 8f, y + 8f)
        }

        override fun getCenter(): PointF {
            val b = getBounds()
            return PointF((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
        }

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Formula(
        override val id: String,
        override val layerId: String,
        val formula: String,
        val x: Float,
        val y: Float,
        val colorArgb: Int,
        val fontSize: Float = 40f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val charWidth = fontSize * 0.65f
            val w = max(100f, formula.length * charWidth + 32f)
            val h = fontSize * 1.35f + 20f
            return Rect(x - 16f, y - h, x + w + 16f, y + 16f)
        }

        override fun getCenter(): PointF {
            val b = getBounds()
            return PointF((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
        }

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Table(
        override val id: String,
        override val layerId: String,
        val rows: Int,
        val cols: Int,
        val x: Float,
        val y: Float,
        val cellWidth: Float = 90f,
        val cellHeight: Float = 45f,
        val colorArgb: Int,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val w = cols * cellWidth
            val h = rows * cellHeight
            return Rect(x - 4f, y - 4f, x + w + 4f, y + h + 4f)
        }

        override fun getCenter(): PointF = PointF(x + (cols * cellWidth) / 2f, y + (rows * cellHeight) / 2f)

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Sticker(
        override val id: String,
        override val layerId: String,
        val stickerId: String,
        val title: String,
        val x: Float,
        val y: Float,
        val size: Float = 120f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val s = if (stickerId in listOf("excellent", "good_job")) size * 1.4f else size
            return Rect(x - 4f, y - 4f, x + s + 4f, y + size + 4f)
        }

        override fun getCenter(): PointF {
            val b = getBounds()
            return PointF((b.left + b.right) / 2f, (b.top + b.bottom) / 2f)
        }

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class ScienceDiagram(
        override val id: String,
        override val layerId: String,
        val category: String, // "atomic", "lab", "measuring"
        val symbolKey: String, // e.g. "H", "He", "flask", "beaker"
        val label: String,
        val atomicNumber: Int = 1,
        val x: Float,
        val y: Float,
        val size: Float = 130f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect = Rect(x - 4f, y - 4f, x + size + 4f, y + size + 4f)

        override fun getCenter(): PointF = PointF(x + size / 2f, y + size / 2f)

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }

    data class Image(
        override val id: String,
        override val layerId: String,
        val uriString: String,
        val x: Float,
        val y: Float,
        val width: Float = 300f,
        val height: Float = 300f,
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect = Rect(x - 4f, y - 4f, x + width + 4f, y + height + 4f)

        override fun getCenter(): PointF = PointF(x + width / 2f, y + height / 2f)

        override fun withTranslation(dx: Float, dy: Float): CanvasElement = copy(x = x + dx, y = y + dy)
        override fun withRotation(newRotation: Float): CanvasElement = copy(rotation = newRotation)
        override fun withScale(newScale: Float): CanvasElement = copy(scale = newScale.coerceIn(0.2f, 8f))
        override fun duplicate(newId: String): CanvasElement = copy(id = newId).withTranslation(40f, 40f)
    }
}

data class LaserPoint(
    val x: Float,
    val y: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class BoardPageState(
    val pageIndex: Int = 0,
    val elements: List<CanvasElement> = emptyList(),
    val undoStack: List<List<CanvasElement>> = emptyList(),
    val redoStack: List<List<CanvasElement>> = emptyList(),
    val layers: List<LayerInfo> = listOf(LayerInfo("layer_1", "Layer 1"))
)
