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
    CLOUD("Cloud"),
    // 3D Shapes (Enhanced Detailed Collection)
    CUBE("Cube", true),
    CUBOID("Cuboid", true),
    CYLINDER("Cylinder", true),
    CONE("Cone", true),
    SPHERE("Sphere", true),
    HEMISPHERE("Hemisphere", true),
    PYRAMID("Pyramid", true),
    TRIANGULAR_PRISM("Tri-Prism", true),
    TORUS("Torus 3D", true),
    HEXAGONAL_PRISM("Hex-Prism", true),
    CAPSULE("Capsule 3D", true)
}

data class ShapePartDef(
    val key: String,
    val name: String,
    val defaultColor: Int
)

fun getShapeDistinctParts(shapeType: ShapeType): List<ShapePartDef> {
    return when (shapeType) {
        ShapeType.CUBE, ShapeType.CUBOID -> listOf(
            ShapePartDef("front", "Front Face", 0xFF42A5F5.toInt()),
            ShapePartDef("top", "Top Face", 0xFF90CAF9.toInt()),
            ShapePartDef("side", "Side Face", 0xFF1E88E5.toInt()),
            ShapePartDef("stroke", "Outline / Edges", 0xFF0D47A1.toInt())
        )
        ShapeType.CYLINDER -> listOf(
            ShapePartDef("top", "Top Cap", 0xFF81C784.toInt()),
            ShapePartDef("body", "Curved Body", 0xFF4CAF50.toInt()),
            ShapePartDef("base", "Bottom Base", 0xFF2E7D32.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF1B5E20.toInt())
        )
        ShapeType.CONE -> listOf(
            ShapePartDef("body", "Conical Body", 0xFFFFB74D.toInt()),
            ShapePartDef("base", "Circular Base", 0xFFF57C00.toInt()),
            ShapePartDef("stroke", "Outline", 0xFFE65100.toInt())
        )
        ShapeType.SPHERE -> listOf(
            ShapePartDef("top", "Upper Light Zone", 0xFFBA68C8.toInt()),
            ShapePartDef("bottom", "Lower Shadow", 0xFF7B1FA2.toInt()),
            ShapePartDef("ring", "Equator Ring", 0xFFE1BEE7.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF4A148C.toInt())
        )
        ShapeType.HEMISPHERE -> listOf(
            ShapePartDef("dome", "Curved Dome", 0xFF4DD0E1.toInt()),
            ShapePartDef("base", "Flat Base", 0xFF0097A7.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF006064.toInt())
        )
        ShapeType.PYRAMID -> listOf(
            ShapePartDef("face1", "Front-Left Face", 0xFFFF8A65.toInt()),
            ShapePartDef("face2", "Front-Right Face", 0xFFE64A19.toInt()),
            ShapePartDef("base", "Base / Floor", 0xFFBF360C.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF3E2723.toInt())
        )
        ShapeType.TRIANGULAR_PRISM -> listOf(
            ShapePartDef("front", "Front Triangle", 0xFFFFD54F.toInt()),
            ShapePartDef("top", "Top Slanted Face", 0xFFFFCA28.toInt()),
            ShapePartDef("side", "Side Face", 0xFFFFA000.toInt()),
            ShapePartDef("stroke", "Outline", 0xFFE65100.toInt())
        )
        ShapeType.TORUS -> listOf(
            ShapePartDef("outer", "Outer Ring", 0xFFF06292.toInt()),
            ShapePartDef("inner", "Core / Hole", 0xFFAD1457.toInt()),
            ShapePartDef("ring", "Highlight Band", 0xFFF8BBD0.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF880E4F.toInt())
        )
        ShapeType.HEXAGONAL_PRISM -> listOf(
            ShapePartDef("top", "Top Hexagon", 0xFFAED581.toInt()),
            ShapePartDef("front", "Front Panel", 0xFF7CB342.toInt()),
            ShapePartDef("side", "Side Panel", 0xFF558B2F.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF33691E.toInt())
        )
        ShapeType.CAPSULE -> listOf(
            ShapePartDef("top_cap", "Top Cap", 0xFF4FC3F7.toInt()),
            ShapePartDef("body", "Middle Body", 0xFF0288D1.toInt()),
            ShapePartDef("bottom_cap", "Bottom Cap", 0xFF01579B.toInt()),
            ShapePartDef("stroke", "Outline", 0xFF0D47A1.toInt())
        )
        else -> listOf(
            ShapePartDef("stroke", "Border / Stroke", 0xFF000000.toInt()),
            ShapePartDef("fill", "Primary Fill", 0xFF2196F3.toInt()),
            ShapePartDef("fill_secondary", "Distinct Accent Part", 0xFFFF9800.toInt())
        )
    }
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
        val fillColorArgb: Int? = null,
        val partColors: Map<String, Int> = emptyMap(),
        override val rotation: Float = 0f,
        override val scale: Float = 1f
    ) : CanvasElement() {
        override fun getBounds(): Rect {
            val left = min(startX, endX)
            val right = max(startX, endX)
            val top = min(startY, endY)
            val bottom = max(startY, endY)
            val pad = strokeWidth * scale / 2f + 4f
            if (shapeType == ShapeType.SQUARE) {
                val side = max(right - left, bottom - top)
                return Rect(left - pad, top - pad, left + side + pad, top + side + pad)
            }
            if (shapeType == ShapeType.CIRCLE) {
                val diam = max(right - left, bottom - top)
                return Rect(left - pad, top - pad, left + diam + pad, top + diam + pad)
            }
            return Rect(left - pad, top - pad, right + pad, bottom + pad)
        }

        override fun getCenter(): PointF {
            val left = min(startX, endX)
            val right = max(startX, endX)
            val top = min(startY, endY)
            val bottom = max(startY, endY)
            if (shapeType == ShapeType.SQUARE || shapeType == ShapeType.CIRCLE) {
                val side = max(right - left, bottom - top)
                return PointF(left + side / 2f, top + side / 2f)
            }
            return PointF((startX + endX) / 2f, (startY + endY) / 2f)
        }

        override fun withTranslation(dx: Float, dy: Float): CanvasElement =
            copy(startX = startX + dx, startY = startY + dy, endX = endX + dx, endY = endY + dy)

        fun withBoundsAdjustment(dLeft: Float, dTop: Float, dRight: Float, dBottom: Float): Shape {
            val minX = min(startX, endX)
            val maxX = max(startX, endX)
            val minY = min(startY, endY)
            val maxY = max(startY, endY)
            val newLeft = minOf(minX + dLeft, maxX + dRight - 20f)
            val newRight = maxOf(maxX + dRight, minX + dLeft + 20f)
            val newTop = minOf(minY + dTop, maxY + dBottom - 20f)
            val newBottom = maxOf(maxY + dBottom, minY + dTop + 20f)
            return copy(
                startX = newLeft,
                startY = newTop,
                endX = newRight,
                endY = newBottom
            )
        }

        fun withPartColor(partKey: String, colorArgb: Int): Shape =
            copy(partColors = partColors + (partKey to colorArgb), isFilled = true)

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
        val isHandwritingAnimated: Boolean = false,
        val handStyle: String = "pen",
        val animationProgress: Float = 1f,
        val isPlayingAnimation: Boolean = false,
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

        fun withSizeAdjustment(dWidth: Float, dHeight: Float): Image =
            copy(width = max(50f, width + dWidth), height = max(50f, height + dHeight))

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
