package com.example.ui.whiteboard

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.model.*
import kotlin.math.*

import android.graphics.BitmapFactory
import java.io.InputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object CanvasRenderer {

    private val imageCache = ConcurrentHashMap<String, ImageBitmap?>()
    private val loadingUris = ConcurrentHashMap.newKeySet<String>()

    fun drawBoardTexture(drawScope: DrawScope, texture: BoardTexture, isDarkBackground: Boolean) {
        val w = drawScope.size.width
        val h = drawScope.size.height
        val gridColor = if (isDarkBackground) Color(0x28FFFFFF) else Color(0x1F000000)
        val lineColor = if (isDarkBackground) Color(0x33FFFFFF) else Color(0x24000000)

        when (texture) {
            BoardTexture.PLAIN -> {}
            BoardTexture.GRID -> {
                val step = 60f
                var x = 0f
                while (x <= w) {
                    drawScope.drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1.2f)
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawScope.drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.2f)
                    y += step
                }
            }
            BoardTexture.GRAPH -> {
                val smallStep = 24f
                var x = 0f
                var idx = 0
                while (x <= w) {
                    val sColor = if (idx % 5 == 0) (if (isDarkBackground) Color(0x40FFFFFF) else Color(0x35000000)) else gridColor
                    val sWidth = if (idx % 5 == 0) 2f else 1f
                    drawScope.drawLine(sColor, Offset(x, 0f), Offset(x, h), strokeWidth = sWidth)
                    x += smallStep
                    idx++
                }
                var y = 0f
                idx = 0
                while (y <= h) {
                    val sColor = if (idx % 5 == 0) (if (isDarkBackground) Color(0x40FFFFFF) else Color(0x35000000)) else gridColor
                    val sWidth = if (idx % 5 == 0) 2f else 1f
                    drawScope.drawLine(sColor, Offset(0f, y), Offset(w, y), strokeWidth = sWidth)
                    y += smallStep
                    idx++
                }
            }
            BoardTexture.LINES -> {
                val step = 70f
                var y = 80f
                while (y <= h) {
                    drawScope.drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                    y += step
                }
            }
            BoardTexture.NOTEBOOK -> {
                val step = 70f
                var y = 80f
                while (y <= h) {
                    drawScope.drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.5f)
                    y += step
                }
                // Left margin line in red/pink
                val marginX = 100f
                drawScope.drawLine(Color(0x88FF4444), Offset(marginX, 0f), Offset(marginX, h), strokeWidth = 2.5f)
            }
            BoardTexture.DOTS -> {
                val step = 50f
                var x = step
                while (x < w) {
                    var y = step
                    while (y < h) {
                        drawScope.drawCircle(gridColor, radius = 2.5f, center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
            }
            BoardTexture.ISOMETRIC -> {
                val step = 60f
                val hStep = (step * sqrt(3.0) / 2.0).toFloat()
                var y = 0f
                while (y <= h) {
                    var x = 0f
                    while (x <= w) {
                        drawScope.drawCircle(gridColor, radius = 2f, center = Offset(x, y))
                        drawScope.drawCircle(gridColor, radius = 2f, center = Offset(x + step / 2f, y + hStep / 2f))
                        x += step
                    }
                    y += hStep
                }
            }
            BoardTexture.MUSIC -> {
                val groupHeight = 80f
                val lineSpacing = 20f
                var startY = 100f
                while (startY < h - 100f) {
                    for (i in 0 until 5) {
                        val y = startY + i * lineSpacing
                        drawScope.drawLine(lineColor, Offset(40f, y), Offset(w - 40f, y), strokeWidth = 1.8f)
                    }
                    startY += groupHeight + 140f
                }
            }
            BoardTexture.DIAMOND -> {
                val step = 80f
                var x = -w
                while (x <= 2 * w) {
                    drawScope.drawLine(gridColor, Offset(x, 0f), Offset(x + h, h), strokeWidth = 1.2f)
                    drawScope.drawLine(gridColor, Offset(x, 0f), Offset(x - h, h), strokeWidth = 1.2f)
                    x += step
                }
            }
            BoardTexture.CHALK -> {
                // Subtle random-like dusty texture
                val step = 100f
                var x = 20f
                while (x < w) {
                    var y = 20f
                    while (y < h) {
                        val offsetSeed = (x * 37 + y * 91).toInt()
                        val dx = (offsetSeed % 30) - 15f
                        val dy = ((offsetSeed / 3) % 30) - 15f
                        drawScope.drawCircle(Color(0x18FFFFFF), radius = 3.5f, center = Offset(x + dx, y + dy))
                        y += step
                    }
                    x += step
                }
            }
        }
    }

    fun drawElement(drawScope: DrawScope, element: CanvasElement) {
        val center = element.getCenter()
        val hasTransform = element.rotation != 0f || element.scale != 1f

        if (hasTransform) {
            drawScope.drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(center.x, center.y)
                canvas.rotate(element.rotation)
                canvas.scale(element.scale, element.scale)
                canvas.translate(-center.x, -center.y)
                renderRawElement(drawScope, element)
                canvas.restore()
            }
        } else {
            renderRawElement(drawScope, element)
        }
    }

    private fun renderRawElement(drawScope: DrawScope, element: CanvasElement) {
        when (element) {
            is CanvasElement.Stroke -> drawStroke(drawScope, element)
            is CanvasElement.Shape -> drawShape(drawScope, element)
            is CanvasElement.Text -> drawText(drawScope, element)
            is CanvasElement.Formula -> drawFormula(drawScope, element)
            is CanvasElement.Table -> drawTable(drawScope, element)
            is CanvasElement.Sticker -> drawSticker(drawScope, element)
            is CanvasElement.ScienceDiagram -> drawScience(drawScope, element)
            is CanvasElement.Image -> drawImage(drawScope, element)
        }
    }

    fun drawSelectionBox(drawScope: DrawScope, element: CanvasElement) {
        val b = element.getBounds()
        val center = element.getCenter()

        drawScope.drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(center.x, center.y)
            canvas.rotate(element.rotation)
            canvas.scale(element.scale, element.scale)
            canvas.translate(-center.x, -center.y)

            val boxColor = Color(0xFF2196F3)
            val handleBg = Color.White
            val handleBorder = Color(0xFF1565C0)

            // Bounding box rect
            drawScope.drawRect(
                color = boxColor,
                topLeft = Offset(b.left, b.top),
                size = Size(b.width, b.height),
                style = Stroke(width = 2.8f / element.scale)
            )

            // Semi-transparent selection fill
            drawScope.drawRect(
                color = boxColor.copy(alpha = 0.08f),
                topLeft = Offset(b.left, b.top),
                size = Size(b.width, b.height),
                style = Fill
            )

            // Center Drag / Move Indicator
            val centerRadius = 14f / element.scale
            drawScope.drawCircle(
                color = boxColor.copy(alpha = 0.25f),
                radius = centerRadius,
                center = Offset(center.x, center.y),
                style = Fill
            )
            drawScope.drawCircle(
                color = boxColor,
                radius = centerRadius,
                center = Offset(center.x, center.y),
                style = Stroke(width = 2f / element.scale)
            )
            drawScope.drawCircle(
                color = boxColor,
                radius = 4f / element.scale,
                center = Offset(center.x, center.y),
                style = Fill
            )

            // Top stem to rotate handle
            val stemLen = 48f / element.scale
            val rotateY = b.top - stemLen
            drawScope.drawLine(
                color = boxColor,
                start = Offset(center.x, b.top),
                end = Offset(center.x, rotateY),
                strokeWidth = 2.5f / element.scale
            )

            // Top Rotate Handle (Large 44px circular knob)
            val rotateRadius = 22f / element.scale
            // Shadow / outer ring
            drawScope.drawCircle(
                color = Color(0x33000000),
                radius = rotateRadius + 2f / element.scale,
                center = Offset(center.x, rotateY + 1.5f / element.scale),
                style = Fill
            )
            drawScope.drawCircle(
                color = handleBg,
                radius = rotateRadius,
                center = Offset(center.x, rotateY),
                style = Fill
            )
            drawScope.drawCircle(
                color = Color(0xFF4CAF50),
                radius = rotateRadius,
                center = Offset(center.x, rotateY),
                style = Stroke(width = 3f / element.scale)
            )
            // Rotate circular arrow inside handle
            val arrowRadius = 11f / element.scale
            drawScope.drawArc(
                color = Color(0xFF2E7D32),
                startAngle = 40f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(center.x - arrowRadius, rotateY - arrowRadius),
                size = Size(arrowRadius * 2f, arrowRadius * 2f),
                style = Stroke(width = 2.5f / element.scale, cap = StrokeCap.Round)
            )
            // Arrow head
            drawScope.drawCircle(
                color = Color(0xFF2E7D32),
                radius = 3.5f / element.scale,
                center = Offset(center.x + arrowRadius * 0.75f, rotateY - arrowRadius * 0.65f),
                style = Fill
            )

            // Square Marking Points / Handles (Corner & Edge handles)
            val handleSize = 28f / element.scale
            val halfHandle = handleSize / 2f
            val corners = listOf(
                Offset(b.left, b.top),
                Offset(b.right, b.top),
                Offset(b.left, b.bottom),
                Offset(b.right, b.bottom)
            )
            corners.forEach { c ->
                // Shadow
                drawScope.drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(c.x - halfHandle, c.y - halfHandle + 1.5f / element.scale),
                    size = Size(handleSize, handleSize),
                    cornerRadius = CornerRadius(4f / element.scale)
                )
                // White background
                drawScope.drawRoundRect(
                    color = handleBg,
                    topLeft = Offset(c.x - halfHandle, c.y - halfHandle),
                    size = Size(handleSize, handleSize),
                    cornerRadius = CornerRadius(4f / element.scale)
                )
                // Border
                drawScope.drawRoundRect(
                    color = handleBorder,
                    topLeft = Offset(c.x - halfHandle, c.y - halfHandle),
                    size = Size(handleSize, handleSize),
                    cornerRadius = CornerRadius(4f / element.scale),
                    style = Stroke(width = 2.5f / element.scale)
                )
                // Inner square accent
                val accentSize = 8f / element.scale
                drawScope.drawRect(
                    color = Color(0xFF1976D2),
                    topLeft = Offset(c.x - accentSize / 2f, c.y - accentSize / 2f),
                    size = Size(accentSize, accentSize)
                )
            }

            // 4 Edge Midpoint Square Marking Handles
            val midpoints = listOf(
                Offset((b.left + b.right) / 2f, b.top),
                Offset((b.left + b.right) / 2f, b.bottom),
                Offset(b.left, (b.top + b.bottom) / 2f),
                Offset(b.right, (b.top + b.bottom) / 2f)
            )
            val midSize = 20f / element.scale
            val halfMid = midSize / 2f
            midpoints.forEach { m ->
                drawScope.drawRoundRect(
                    color = handleBg,
                    topLeft = Offset(m.x - halfMid, m.y - halfMid),
                    size = Size(midSize, midSize),
                    cornerRadius = CornerRadius(3f / element.scale)
                )
                drawScope.drawRoundRect(
                    color = handleBorder,
                    topLeft = Offset(m.x - halfMid, m.y - halfMid),
                    size = Size(midSize, midSize),
                    cornerRadius = CornerRadius(3f / element.scale),
                    style = Stroke(width = 2f / element.scale)
                )
            }

            canvas.restore()
        }
    }

    private fun drawStroke(drawScope: DrawScope, stroke: CanvasElement.Stroke) {
        if (stroke.points.size < 2) {
            if (stroke.points.isNotEmpty()) {
                val p = stroke.points.first()
                drawScope.drawCircle(
                    color = Color(stroke.colorArgb),
                    radius = stroke.strokeWidth / 2f,
                    center = Offset(p.x, p.y)
                )
            }
            return
        }

        val baseColor = Color(stroke.colorArgb)
        when (stroke.brushType) {
            BrushType.HIGHLIGHT -> {
                val highlightColor = baseColor.copy(alpha = 0.35f)
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        val prev = stroke.points[i - 1]
                        val curr = stroke.points[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    lineTo(stroke.points.last().x, stroke.points.last().y)
                }
                drawScope.drawPath(
                    path = path,
                    color = highlightColor,
                    style = Stroke(
                        width = stroke.strokeWidth * 2.2f,
                        cap = StrokeCap.Square,
                        join = StrokeJoin.Round
                    )
                )
            }
            BrushType.MARKER -> {
                val markerColor = baseColor.copy(alpha = 0.85f)
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        lineTo(stroke.points[i].x, stroke.points[i].y)
                    }
                }
                drawScope.drawPath(
                    path = path,
                    color = markerColor,
                    style = Stroke(
                        width = stroke.strokeWidth * 1.4f,
                        cap = StrokeCap.Square,
                        join = StrokeJoin.Bevel
                    )
                )
            }
            BrushType.CALLIGRAPHY -> {
                // Ribbony stroke with 45 degree angle
                val angleRad = Math.toRadians(45.0)
                val dx = (cos(angleRad) * stroke.strokeWidth * 0.7f).toFloat()
                val dy = (sin(angleRad) * stroke.strokeWidth * 0.7f).toFloat()

                for (i in 0 until stroke.points.size - 1) {
                    val p1 = stroke.points[i]
                    val p2 = stroke.points[i + 1]
                    val path = Path().apply {
                        moveTo(p1.x - dx, p1.y - dy)
                        lineTo(p1.x + dx, p1.y + dy)
                        lineTo(p2.x + dx, p2.y + dy)
                        lineTo(p2.x - dx, p2.y - dy)
                        close()
                    }
                    drawScope.drawPath(path, baseColor, style = Fill)
                }
            }
            BrushType.BRUSH -> {
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        val prev = stroke.points[i - 1]
                        val curr = stroke.points[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    lineTo(stroke.points.last().x, stroke.points.last().y)
                }
                drawScope.drawPath(
                    path = path,
                    color = baseColor.copy(alpha = 0.9f),
                    style = Stroke(
                        width = stroke.strokeWidth * 1.2f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            BrushType.PEN -> {
                val path = Path().apply {
                    moveTo(stroke.points[0].x, stroke.points[0].y)
                    for (i in 1 until stroke.points.size) {
                        val prev = stroke.points[i - 1]
                        val curr = stroke.points[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    lineTo(stroke.points.last().x, stroke.points.last().y)
                }
                drawScope.drawPath(
                    path = path,
                    color = baseColor,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }

    private fun drawShape(drawScope: DrawScope, shape: CanvasElement.Shape) {
        val color = Color(shape.colorArgb)
        val style = if (shape.isFilled) Fill else Stroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val minX = min(shape.startX, shape.endX)
        val maxX = max(shape.startX, shape.endX)
        val minY = min(shape.startY, shape.endY)
        val maxY = max(shape.startY, shape.endY)
        val width = max(maxX - minX, 10f)
        val height = max(maxY - minY, 10f)

        when (shape.shapeType) {
            ShapeType.LINE -> {
                drawScope.drawLine(
                    color = color,
                    start = Offset(shape.startX, shape.startY),
                    end = Offset(shape.endX, shape.endY),
                    strokeWidth = shape.strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            ShapeType.ARROW -> {
                val start = Offset(shape.startX, shape.startY)
                val end = Offset(shape.endX, shape.endY)
                drawScope.drawLine(color, start, end, strokeWidth = shape.strokeWidth, cap = StrokeCap.Round)
                val angle = atan2((end.y - start.y).toDouble(), (end.x - start.x).toDouble())
                val arrowLen = max(24f, shape.strokeWidth * 3f)
                val p1 = Offset(
                    (end.x - arrowLen * cos(angle - PI / 6)).toFloat(),
                    (end.y - arrowLen * sin(angle - PI / 6)).toFloat()
                )
                val p2 = Offset(
                    (end.x - arrowLen * cos(angle + PI / 6)).toFloat(),
                    (end.y - arrowLen * sin(angle + PI / 6)).toFloat()
                )
                val arrowPath = Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                }
                drawScope.drawPath(arrowPath, color, style = Fill)
            }
            ShapeType.RECT -> {
                drawScope.drawRect(color, topLeft = Offset(minX, minY), size = Size(width, height), style = style)
            }
            ShapeType.SQUARE -> {
                val side = max(width, height)
                drawScope.drawRect(color, topLeft = Offset(minX, minY), size = Size(side, side), style = style)
            }
            ShapeType.CIRCLE -> {
                val radius = max(width, height) / 2f
                val center = Offset(minX + radius, minY + radius)
                drawScope.drawCircle(color, radius = radius, center = center, style = style)
            }
            ShapeType.OVAL -> {
                drawScope.drawOval(color, topLeft = Offset(minX, minY), size = Size(width, height), style = style)
            }
            ShapeType.TRIANGLE -> {
                val path = Path().apply {
                    moveTo(minX + width / 2f, minY)
                    lineTo(maxX, maxY)
                    lineTo(minX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.RIGHT_TRIANGLE -> {
                val path = Path().apply {
                    moveTo(minX, minY)
                    lineTo(minX, maxY)
                    lineTo(maxX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.STAR -> {
                val cx = minX + width / 2f
                val cy = minY + height / 2f
                val outerR = min(width, height) / 2f
                val innerR = outerR * 0.42f
                val path = Path()
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val a = (i * PI / 5 - PI / 2).toFloat()
                    val px = cx + r * cos(a)
                    val py = cy + r * sin(a)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.PENTAGON -> {
                val cx = minX + width / 2f
                val cy = minY + height / 2f
                val r = min(width, height) / 2f
                val path = Path()
                for (i in 0 until 5) {
                    val a = (i * 2 * PI / 5 - PI / 2).toFloat()
                    val px = cx + r * cos(a)
                    val py = cy + r * sin(a)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.HEXAGON -> {
                val cx = minX + width / 2f
                val cy = minY + height / 2f
                val r = min(width, height) / 2f
                val path = Path()
                for (i in 0 until 6) {
                    val a = (i * 2 * PI / 6 - PI / 2).toFloat()
                    val px = cx + r * cos(a)
                    val py = cy + r * sin(a)
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.DIAMOND -> {
                val path = Path().apply {
                    moveTo(minX + width / 2f, minY)
                    lineTo(maxX, minY + height / 2f)
                    lineTo(minX + width / 2f, maxY)
                    lineTo(minX, minY + height / 2f)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.PARALLEL -> {
                val skew = width * 0.25f
                val path = Path().apply {
                    moveTo(minX + skew, minY)
                    lineTo(maxX, minY)
                    lineTo(maxX - skew, maxY)
                    lineTo(minX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.TRAPEZOID -> {
                val inset = width * 0.2f
                val path = Path().apply {
                    moveTo(minX + inset, minY)
                    lineTo(maxX - inset, minY)
                    lineTo(maxX, maxY)
                    lineTo(minX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.HEART -> {
                val path = Path().apply {
                    val cx = minX + width / 2f
                    moveTo(cx, minY + height * 0.35f)
                    cubicTo(
                        minX + width * 0.1f, minY,
                        minX, minY + height * 0.35f,
                        cx, maxY
                    )
                    cubicTo(
                        maxX, minY + height * 0.35f,
                        maxX - width * 0.1f, minY,
                        cx, minY + height * 0.35f
                    )
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.CROSS -> {
                val w3 = width / 3f
                val h3 = height / 3f
                val path = Path().apply {
                    moveTo(minX + w3, minY)
                    lineTo(minX + 2 * w3, minY)
                    lineTo(minX + 2 * w3, minY + h3)
                    lineTo(maxX, minY + h3)
                    lineTo(maxX, minY + 2 * h3)
                    lineTo(minX + 2 * w3, minY + 2 * h3)
                    lineTo(minX + 2 * w3, maxY)
                    lineTo(minX + w3, maxY)
                    lineTo(minX + w3, minY + 2 * h3)
                    lineTo(minX, minY + 2 * h3)
                    lineTo(minX, minY + h3)
                    lineTo(minX + w3, minY + h3)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.SEMICIRCLE -> {
                val path = Path().apply {
                    moveTo(minX, maxY)
                    quadraticTo(minX + width / 2f, minY - height * 0.2f, maxX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.SECTOR -> {
                val path = Path().apply {
                    moveTo(minX, maxY)
                    lineTo(maxX, maxY)
                    quadraticTo(maxX, minY, minX, maxY)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            ShapeType.RIGHT_ANGLE -> {
                val path = Path().apply {
                    moveTo(minX, minY)
                    lineTo(minX, maxY)
                    lineTo(maxX, maxY)
                }
                drawScope.drawPath(path, color, style = Stroke(width = shape.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Miter))
                val markerSize = min(width, height) * 0.2f
                val markerPath = Path().apply {
                    moveTo(minX, maxY - markerSize)
                    lineTo(minX + markerSize, maxY - markerSize)
                    lineTo(minX + markerSize, maxY)
                }
                drawScope.drawPath(markerPath, color, style = Stroke(width = shape.strokeWidth * 0.7f))
            }
            ShapeType.CLOUD -> {
                val r = height * 0.35f
                val path = Path().apply {
                    moveTo(minX + r, maxY - r * 0.3f)
                    lineTo(maxX - r, maxY - r * 0.3f)
                    arcTo(androidx.compose.ui.geometry.Rect(maxX - 2 * r, maxY - 2 * r, maxX, maxY), 90f, -180f, false)
                    arcTo(androidx.compose.ui.geometry.Rect(minX + width * 0.45f, minY, minX + width * 0.85f, minY + height * 0.7f), 0f, -180f, false)
                    arcTo(androidx.compose.ui.geometry.Rect(minX + width * 0.15f, minY + height * 0.08f, minX + width * 0.55f, minY + height * 0.75f), 10f, -180f, false)
                    arcTo(androidx.compose.ui.geometry.Rect(minX, maxY - 2 * r, minX + 2 * r, maxY), 270f, -180f, false)
                    close()
                }
                drawScope.drawPath(path, color, style = style)
            }
            // Detailed 3D Shapes with Distinct Part Coloring
            ShapeType.CUBE -> {
                val d = width * 0.3f
                val strokeColor = getPartColor(shape, "stroke", color)
                val frontColor = getPartColor(shape, "front", if (shape.isFilled) color.copy(alpha = 0.9f) else Color(0x3342A5F5))
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x3390CAF9))
                val sideColor = getPartColor(shape, "side", if (shape.isFilled) color.copy(alpha = 0.5f) else Color(0x331E88E5))

                // Front face
                drawScope.drawRect(frontColor, topLeft = Offset(minX, minY + d), size = Size(width - d, height - d), style = Fill)
                drawScope.drawRect(strokeColor, topLeft = Offset(minX, minY + d), size = Size(width - d, height - d), style = Stroke(width = shape.strokeWidth))

                // Top face
                val topPath = Path().apply {
                    moveTo(minX, minY + d)
                    lineTo(minX + d, minY)
                    lineTo(maxX, minY)
                    lineTo(maxX - d, minY + d)
                    close()
                }
                drawScope.drawPath(topPath, topColor, style = Fill)
                drawScope.drawPath(topPath, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Right face
                val rightPath = Path().apply {
                    moveTo(maxX - d, minY + d)
                    lineTo(maxX, minY)
                    lineTo(maxX, maxY - d)
                    lineTo(maxX - d, maxY)
                    close()
                }
                drawScope.drawPath(rightPath, sideColor, style = Fill)
                drawScope.drawPath(rightPath, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.CUBOID -> {
                val dx = width * 0.28f
                val dy = height * 0.22f
                val strokeColor = getPartColor(shape, "stroke", color)
                val frontColor = getPartColor(shape, "front", if (shape.isFilled) color.copy(alpha = 0.9f) else Color(0x3342A5F5))
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x3390CAF9))
                val sideColor = getPartColor(shape, "side", if (shape.isFilled) color.copy(alpha = 0.5f) else Color(0x331E88E5))

                // Front face
                drawScope.drawRect(frontColor, topLeft = Offset(minX, minY + dy), size = Size(width - dx, height - dy), style = Fill)
                drawScope.drawRect(strokeColor, topLeft = Offset(minX, minY + dy), size = Size(width - dx, height - dy), style = Stroke(width = shape.strokeWidth))

                // Top face
                val topPath = Path().apply {
                    moveTo(minX, minY + dy)
                    lineTo(minX + dx, minY)
                    lineTo(maxX, minY)
                    lineTo(maxX - dx, minY + dy)
                    close()
                }
                drawScope.drawPath(topPath, topColor, style = Fill)
                drawScope.drawPath(topPath, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Right face
                val rightPath = Path().apply {
                    moveTo(maxX - dx, minY + dy)
                    lineTo(maxX, minY)
                    lineTo(maxX, maxY - dy)
                    lineTo(maxX - dx, maxY)
                    close()
                }
                drawScope.drawPath(rightPath, sideColor, style = Fill)
                drawScope.drawPath(rightPath, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.CYLINDER -> {
                val rH = height * 0.22f
                val strokeColor = getPartColor(shape, "stroke", color)
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x3381C784))
                val bodyColor = getPartColor(shape, "body", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x334CAF50))
                val baseColor = getPartColor(shape, "base", if (shape.isFilled) color.copy(alpha = 0.55f) else Color(0x332E7D32))

                // Body surface quad
                val bodyPath = Path().apply {
                    moveTo(minX, minY + rH / 2f)
                    lineTo(maxX, minY + rH / 2f)
                    lineTo(maxX, maxY - rH / 2f)
                    lineTo(minX, maxY - rH / 2f)
                    close()
                }
                drawScope.drawPath(bodyPath, bodyColor, style = Fill)

                // Bottom oval
                drawScope.drawOval(baseColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Stroke(width = shape.strokeWidth))

                // Top oval
                drawScope.drawOval(topColor, topLeft = Offset(minX, minY), size = Size(width, rH), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(minX, minY), size = Size(width, rH), style = Stroke(width = shape.strokeWidth))

                // Side lines
                drawScope.drawLine(strokeColor, Offset(minX, minY + rH / 2f), Offset(minX, maxY - rH / 2f), strokeWidth = shape.strokeWidth)
                drawScope.drawLine(strokeColor, Offset(maxX, minY + rH / 2f), Offset(maxX, maxY - rH / 2f), strokeWidth = shape.strokeWidth)
            }
            ShapeType.CONE -> {
                val rH = height * 0.22f
                val tip = Offset(minX + width / 2f, minY)
                val strokeColor = getPartColor(shape, "stroke", color)
                val bodyColor = getPartColor(shape, "body", if (shape.isFilled) color.copy(alpha = 0.75f) else Color(0x33FFB74D))
                val baseColor = getPartColor(shape, "base", if (shape.isFilled) color.copy(alpha = 0.55f) else Color(0x33F57C00))

                // Conical body mantle
                val mantlePath = Path().apply {
                    moveTo(tip.x, tip.y)
                    lineTo(minX, maxY - rH / 2f)
                    lineTo(maxX, maxY - rH / 2f)
                    close()
                }
                drawScope.drawPath(mantlePath, bodyColor, style = Fill)

                // Bottom base oval
                drawScope.drawOval(baseColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Stroke(width = shape.strokeWidth))

                // Side lines to tip
                drawScope.drawLine(strokeColor, tip, Offset(minX, maxY - rH / 2f), strokeWidth = shape.strokeWidth)
                drawScope.drawLine(strokeColor, tip, Offset(maxX, maxY - rH / 2f), strokeWidth = shape.strokeWidth)
            }
            ShapeType.SPHERE -> {
                val radius = min(width, height) / 2f
                val center = Offset(minX + width / 2f, minY + height / 2f)
                val strokeColor = getPartColor(shape, "stroke", color)
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x33BA68C8))
                val bottomColor = getPartColor(shape, "bottom", if (shape.isFilled) color.copy(alpha = 0.6f) else Color(0x337B1FA2))
                val ringColor = getPartColor(shape, "ring", Color(0xFFE1BEE7))

                // Base sphere circle
                drawScope.drawCircle(bottomColor, radius = radius, center = center, style = Fill)
                // Upper hemisphere dome
                val topDome = Path().apply {
                    arcTo(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius), 180f, 180f, false)
                    close()
                }
                drawScope.drawPath(topDome, topColor, style = Fill)

                // Equator & meridian 3D rings
                drawScope.drawOval(
                    ringColor,
                    topLeft = Offset(center.x - radius, center.y - radius * 0.35f),
                    size = Size(radius * 2f, radius * 0.7f),
                    style = Stroke(width = shape.strokeWidth * 0.8f)
                )
                drawScope.drawCircle(strokeColor, radius = radius, center = center, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.HEMISPHERE -> {
                val rH = height * 0.28f
                val strokeColor = getPartColor(shape, "stroke", color)
                val domeColor = getPartColor(shape, "dome", if (shape.isFilled) color.copy(alpha = 0.8f) else Color(0x334DD0E1))
                val baseColor = getPartColor(shape, "base", if (shape.isFilled) color.copy(alpha = 0.6f) else Color(0x330097A7))

                // Dome
                val domePath = Path().apply {
                    moveTo(minX, maxY - rH / 2f)
                    cubicTo(minX, minY, maxX, minY, maxX, maxY - rH / 2f)
                    close()
                }
                drawScope.drawPath(domePath, domeColor, style = Fill)
                drawScope.drawPath(domePath, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Flat base oval
                drawScope.drawOval(baseColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(minX, maxY - rH), size = Size(width, rH), style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.PYRAMID -> {
                val apex = Offset(minX + width / 2f, minY)
                val strokeColor = getPartColor(shape, "stroke", color)
                val face1Color = getPartColor(shape, "face1", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x33FF8A65))
                val face2Color = getPartColor(shape, "face2", if (shape.isFilled) color.copy(alpha = 0.65f) else Color(0x33E64A19))
                val baseColor = getPartColor(shape, "base", Color(0x22BF360C))

                // Base floor
                val basePath = Path().apply {
                    moveTo(minX, maxY - height * 0.15f)
                    lineTo(minX + width * 0.42f, maxY)
                    lineTo(maxX, maxY - height * 0.1f)
                    lineTo(minX + width * 0.58f, maxY - height * 0.25f)
                    close()
                }
                drawScope.drawPath(basePath, baseColor, style = Fill)

                // Left face
                val path1 = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(minX, maxY - height * 0.15f)
                    lineTo(minX + width * 0.42f, maxY)
                    close()
                }
                drawScope.drawPath(path1, face1Color, style = Fill)
                drawScope.drawPath(path1, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Right face
                val path2 = Path().apply {
                    moveTo(apex.x, apex.y)
                    lineTo(minX + width * 0.42f, maxY)
                    lineTo(maxX, maxY - height * 0.1f)
                    close()
                }
                drawScope.drawPath(path2, face2Color, style = Fill)
                drawScope.drawPath(path2, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.TRIANGULAR_PRISM -> {
                val d = width * 0.28f
                val strokeColor = getPartColor(shape, "stroke", color)
                val frontColor = getPartColor(shape, "front", if (shape.isFilled) color.copy(alpha = 0.9f) else Color(0x33FFD54F))
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x33FFFF8D))
                val sideColor = getPartColor(shape, "side", if (shape.isFilled) color.copy(alpha = 0.5f) else Color(0x33FFA000))

                // Front triangle face
                val frontTri = Path().apply {
                    moveTo(minX + (width - d) / 2f, minY + d)
                    lineTo(minX, maxY)
                    lineTo(maxX - d, maxY)
                    close()
                }
                drawScope.drawPath(frontTri, frontColor, style = Fill)
                drawScope.drawPath(frontTri, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Top slope
                val topPath = Path().apply {
                    moveTo(minX + (width - d) / 2f, minY + d)
                    lineTo(minX + width / 2f, minY)
                    lineTo(maxX, maxY - d)
                    lineTo(maxX - d, maxY)
                    close()
                }
                drawScope.drawPath(topPath, topColor, style = Fill)
                drawScope.drawPath(topPath, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Side / Back edge
                val sidePath = Path().apply {
                    moveTo(minX + (width - d) / 2f, minY + d)
                    lineTo(minX + width / 2f, minY)
                    lineTo(minX + d, maxY - d)
                    lineTo(minX, maxY)
                    close()
                }
                drawScope.drawPath(sidePath, sideColor, style = Fill)
                drawScope.drawPath(sidePath, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.TORUS -> {
                val cx = minX + width / 2f
                val cy = minY + height / 2f
                val strokeColor = getPartColor(shape, "stroke", color)
                val outerColor = getPartColor(shape, "outer", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x33F06292))
                val innerColor = getPartColor(shape, "inner", if (shape.isFilled) Color.White.copy(alpha = 0.9f) else Color(0x33AD1457))
                val ringColor = getPartColor(shape, "ring", Color(0xFFF8BBD0))

                // Outer donut ellipse
                drawScope.drawOval(outerColor, topLeft = Offset(minX, minY + height * 0.1f), size = Size(width, height * 0.8f), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(minX, minY + height * 0.1f), size = Size(width, height * 0.8f), style = Stroke(width = shape.strokeWidth))

                // Highlight ring
                drawScope.drawOval(ringColor, topLeft = Offset(minX + width * 0.15f, minY + height * 0.22f), size = Size(width * 0.7f, height * 0.56f), style = Stroke(width = shape.strokeWidth * 0.8f))

                // Inner hole
                val holeW = width * 0.36f
                val holeH = height * 0.3f
                drawScope.drawOval(innerColor, topLeft = Offset(cx - holeW / 2f, cy - holeH / 2f), size = Size(holeW, holeH), style = Fill)
                drawScope.drawOval(strokeColor, topLeft = Offset(cx - holeW / 2f, cy - holeH / 2f), size = Size(holeW, holeH), style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.HEXAGONAL_PRISM -> {
                val d = height * 0.22f
                val strokeColor = getPartColor(shape, "stroke", color)
                val topColor = getPartColor(shape, "top", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x33AED581))
                val frontColor = getPartColor(shape, "front", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x337CB342))
                val sideColor = getPartColor(shape, "side", if (shape.isFilled) color.copy(alpha = 0.55f) else Color(0x33558B2F))

                // Front rectangular face
                val frontRect = Path().apply {
                    moveTo(minX + width * 0.25f, minY + d)
                    lineTo(minX + width * 0.75f, minY + d)
                    lineTo(minX + width * 0.75f, maxY)
                    lineTo(minX + width * 0.25f, maxY)
                    close()
                }
                drawScope.drawPath(frontRect, frontColor, style = Fill)
                drawScope.drawPath(frontRect, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Left & Right side faces
                val leftFace = Path().apply {
                    moveTo(minX, minY + d * 0.6f)
                    lineTo(minX + width * 0.25f, minY + d)
                    lineTo(minX + width * 0.25f, maxY)
                    lineTo(minX, maxY - d * 0.4f)
                    close()
                }
                drawScope.drawPath(leftFace, sideColor, style = Fill)
                drawScope.drawPath(leftFace, strokeColor, style = Stroke(width = shape.strokeWidth))

                val rightFace = Path().apply {
                    moveTo(maxX, minY + d * 0.6f)
                    lineTo(minX + width * 0.75f, minY + d)
                    lineTo(minX + width * 0.75f, maxY)
                    lineTo(maxX, maxY - d * 0.4f)
                    close()
                }
                drawScope.drawPath(rightFace, sideColor, style = Fill)
                drawScope.drawPath(rightFace, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Top hexagon face
                val topHex = Path().apply {
                    moveTo(minX + width * 0.5f, minY)
                    lineTo(maxX, minY + d * 0.6f)
                    lineTo(minX + width * 0.75f, minY + d)
                    lineTo(minX + width * 0.25f, minY + d)
                    lineTo(minX, minY + d * 0.6f)
                    close()
                }
                drawScope.drawPath(topHex, topColor, style = Fill)
                drawScope.drawPath(topHex, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
            ShapeType.CAPSULE -> {
                val strokeColor = getPartColor(shape, "stroke", color)
                val topCapColor = getPartColor(shape, "top_cap", if (shape.isFilled) color.copy(alpha = 0.85f) else Color(0x334FC3F7))
                val bodyColor = getPartColor(shape, "body", if (shape.isFilled) color.copy(alpha = 0.7f) else Color(0x330288D1))
                val bottomCapColor = getPartColor(shape, "bottom_cap", if (shape.isFilled) color.copy(alpha = 0.55f) else Color(0x3301579B))
                val r = width / 2f

                // Middle cylindrical body
                drawScope.drawRect(bodyColor, topLeft = Offset(minX, minY + r), size = Size(width, height - 2 * r), style = Fill)
                drawScope.drawLine(strokeColor, Offset(minX, minY + r), Offset(minX, maxY - r), strokeWidth = shape.strokeWidth)
                drawScope.drawLine(strokeColor, Offset(maxX, minY + r), Offset(maxX, maxY - r), strokeWidth = shape.strokeWidth)

                // Top cap
                val topCap = Path().apply {
                    arcTo(androidx.compose.ui.geometry.Rect(minX, minY, maxX, minY + 2 * r), 180f, 180f, false)
                    close()
                }
                drawScope.drawPath(topCap, topCapColor, style = Fill)
                drawScope.drawPath(topCap, strokeColor, style = Stroke(width = shape.strokeWidth))

                // Bottom cap
                val bottomCap = Path().apply {
                    arcTo(androidx.compose.ui.geometry.Rect(minX, maxY - 2 * r, maxX, maxY), 0f, 180f, false)
                    close()
                }
                drawScope.drawPath(bottomCap, bottomCapColor, style = Fill)
                drawScope.drawPath(bottomCap, strokeColor, style = Stroke(width = shape.strokeWidth))
            }
        }
    }

    private fun getPartColor(shape: CanvasElement.Shape, key: String, defaultColor: Color): Color {
        val argb = shape.partColors[key]
        return if (argb != null) Color(argb) else defaultColor
    }

    private fun drawText(drawScope: DrawScope, textEl: CanvasElement.Text) {
        drawScope.drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = textEl.colorArgb
                textSize = textEl.fontSize
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val text = textEl.text
            if (text.isEmpty()) return@drawIntoCanvas

            if (textEl.isHandwritingAnimated) {
                val totalChars = text.length
                val progress = textEl.animationProgress.coerceIn(0f, 1f)
                val totalWidth = paint.measureText(text)

                // 1. Text ink reveal synchronized smoothly with pen position
                if (progress >= 1f) {
                    canvas.nativeCanvas.drawText(text, textEl.x, textEl.y, paint)
                } else {
                    val currentProgressX = textEl.x + totalWidth * progress
                    canvas.save()
                    // Clip rect smoothly reveals letter strokes right at the pen tip
                    canvas.clipRect(
                        textEl.x - 20f,
                        textEl.y - textEl.fontSize * 1.5f,
                        currentProgressX,
                        textEl.y + textEl.fontSize * 0.8f
                    )
                    canvas.nativeCanvas.drawText(text, textEl.x, textEl.y, paint)
                    canvas.restore()
                }

                // 2. Animate the hand holding the pen while actively writing
                if (textEl.isPlayingAnimation || (progress > 0f && progress < 1f)) {
                    val currentProgressX = textEl.x + totalWidth * progress
                    val charFloat = (totalChars * progress).coerceIn(0f, (totalChars - 1).toFloat())
                    val charIndex = charFloat.toInt().coerceIn(0, totalChars - 1)
                    val currentChar = text[charIndex]
                    val isSpace = currentChar == ' '
                    val charFraction = charFloat - charIndex

                    // Natural handwriting strokes:
                    // Up-down vertical oscillations mimicking writing letters (cursive/print loops and vertical stems)
                    val strokePhase = charFraction * (Math.PI.toFloat() * 2f)
                    val h = textEl.fontSize
                    val strokeY = if (isSpace) {
                        -h * 0.40f // Lift pen off the whiteboard when moving between words
                    } else {
                        // Harmonic handwriting oscillation: rises on upstroke, dips on base
                        (-sin(strokePhase * 2f) * 0.22f + cos(strokePhase * 3f) * 0.08f) * h
                    }

                    val tipX = currentProgressX
                    val baselineY = textEl.y
                    val tipY = textEl.y - textEl.fontSize * 0.25f + strokeY

                    val handScale = (textEl.fontSize / 44f).coerceIn(0.7f, 1.9f)
                    drawHandHoldingPen(
                        drawScope = drawScope,
                        tipX = tipX,
                        tipY = tipY,
                        baselineY = baselineY,
                        scale = handScale,
                        handStyle = textEl.handStyle,
                        isPenLifted = isSpace
                    )
                }
            } else {
                canvas.nativeCanvas.drawText(text, textEl.x, textEl.y, paint)
            }
        }
    }

    fun drawHandHoldingPen(
        drawScope: DrawScope,
        tipX: Float,
        tipY: Float,
        baselineY: Float = tipY,
        scale: Float = 1f,
        handStyle: String = "pen",
        isPenLifted: Boolean = false
    ) {
        val s = scale.coerceIn(0.6f, 2.5f)
        val emojiSize = (44f * s * 1.35f).coerceIn(36f, 150f)

        drawScope.drawIntoCanvas { canvas ->
            // 1. Soft drop shadow on the whiteboard surface under the pen nib
            val shadowPaint = Paint().apply {
                color = android.graphics.Color.argb(40, 25, 25, 25)
                isAntiAlias = true
            }
            val shadowLiftOffset = if (isPenLifted) 12f * s else 3f * s
            canvas.nativeCanvas.drawOval(
                tipX - 3f * s,
                baselineY + shadowLiftOffset - 2f * s,
                tipX + 18f * s,
                baselineY + shadowLiftOffset + 7f * s,
                shadowPaint
            )

            // 2. Draw the iconic Hand with Pen (✍️)
            val iconPaint = Paint().apply {
                textSize = emojiSize
                isAntiAlias = true
            }

            // In Noto Color Emoji, the pen nib of ✍️ is located near (x + emojiSize * 0.12f, y - emojiSize * 0.10f)
            val drawX = tipX - emojiSize * 0.12f
            val drawY = tipY + emojiSize * 0.10f

            // Dynamic handwriting tilt while moving
            canvas.save()
            val tiltAngle = if (isPenLifted) -4f else (sin(tipX * 0.08) * 3f).toFloat()
            canvas.rotate(tiltAngle, tipX, tipY)
            canvas.nativeCanvas.drawText("\u270D\uFE0F", drawX, drawY, iconPaint)
            canvas.restore()
        }
    }

    private fun drawFormula(drawScope: DrawScope, formulaEl: CanvasElement.Formula) {
        drawScope.drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                color = formulaEl.colorArgb
                textSize = formulaEl.fontSize
                isAntiAlias = true
                typeface = android.graphics.Typeface.SERIF
            }
            // Draw neat math background card badge
            val bounds = Rect()
            paint.getTextBounds(formulaEl.formula, 0, formulaEl.formula.length, bounds)
            val padding = 16f
            val bgPaint = Paint().apply {
                color = 0x22888888
                style = Paint.Style.FILL
            }
            canvas.nativeCanvas.drawRoundRect(
                formulaEl.x - padding,
                formulaEl.y - bounds.height() - padding,
                formulaEl.x + bounds.width() + padding,
                formulaEl.y + padding,
                16f,
                16f,
                bgPaint
            )
            canvas.nativeCanvas.drawText(formulaEl.formula, formulaEl.x, formulaEl.y, paint)
        }
    }

    private fun drawTable(drawScope: DrawScope, table: CanvasElement.Table) {
        val color = Color(table.colorArgb)
        val strokeWidth = 2.5f

        val totalWidth = table.cols * table.cellWidth
        val totalHeight = table.rows * table.cellHeight

        // Outer border
        drawScope.drawRect(
            color = color,
            topLeft = Offset(table.x, table.y),
            size = Size(totalWidth, totalHeight),
            style = Stroke(width = strokeWidth * 1.5f)
        )

        // Header row fill
        drawScope.drawRect(
            color = color.copy(alpha = 0.12f),
            topLeft = Offset(table.x, table.y),
            size = Size(totalWidth, table.cellHeight),
            style = Fill
        )

        // Horizontal lines
        for (r in 1 until table.rows) {
            val y = table.y + r * table.cellHeight
            drawScope.drawLine(
                color = color,
                start = Offset(table.x, y),
                end = Offset(table.x + totalWidth, y),
                strokeWidth = strokeWidth
            )
        }

        // Vertical lines
        for (c in 1 until table.cols) {
            val x = table.x + c * table.cellWidth
            drawScope.drawLine(
                color = color,
                start = Offset(x, table.y),
                end = Offset(x, table.y + totalHeight),
                strokeWidth = strokeWidth
            )
        }
    }

    private fun drawSticker(drawScope: DrawScope, sticker: CanvasElement.Sticker) {
        val s = sticker.size
        val x = sticker.x
        val y = sticker.y

        when (sticker.stickerId) {
            "star" -> {
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.rgb(255, 204, 0)
                        style = Paint.Style.FILL
                        isAntiAlias = true
                    }
                    val strokePaint = Paint().apply {
                        color = android.graphics.Color.rgb(220, 150, 0)
                        style = Paint.Style.STROKE
                        strokeWidth = 3f
                        isAntiAlias = true
                    }
                    val starShape = ShapeElementDraft(ShapeType.STAR, x, y, x + s, y + s, android.graphics.Color.rgb(255, 204, 0), 3f, true)
                    drawShape(drawScope, CanvasElement.Shape("tmp", "", ShapeType.STAR, x, y, x + s, y + s, 0xFFFFCC00.toInt(), 3f, true))
                    drawShape(drawScope, CanvasElement.Shape("tmp2", "", ShapeType.STAR, x, y, x + s, y + s, 0xFFCC9900.toInt(), 3f, false))
                }
            }
            "trophy" -> {
                val gold = Color(0xFFFFB800)
                // Base
                drawScope.drawRect(Color(0xFF555555), topLeft = Offset(x + s * 0.25f, y + s * 0.75f), size = Size(s * 0.5f, s * 0.2f), style = Fill)
                // Stem
                drawScope.drawRect(gold, topLeft = Offset(x + s * 0.43f, y + s * 0.55f), size = Size(s * 0.14f, s * 0.2f), style = Fill)
                // Cup
                val cupPath = Path().apply {
                    moveTo(x + s * 0.25f, y + s * 0.2f)
                    lineTo(x + s * 0.75f, y + s * 0.2f)
                    cubicTo(
                        x + s * 0.75f, y + s * 0.55f,
                        x + s * 0.25f, y + s * 0.55f,
                        x + s * 0.25f, y + s * 0.2f
                    )
                    close()
                }
                drawScope.drawPath(cupPath, gold, style = Fill)
                // Handles
                drawScope.drawOval(gold, topLeft = Offset(x + s * 0.12f, y + s * 0.24f), size = Size(s * 0.2f, s * 0.25f), style = Stroke(4f))
                drawScope.drawOval(gold, topLeft = Offset(x + s * 0.68f, y + s * 0.24f), size = Size(s * 0.2f, s * 0.25f), style = Stroke(4f))
            }
            "a_plus" -> {
                drawScope.drawCircle(Color(0xFF2ECC71), radius = s * 0.45f, center = Offset(x + s / 2f, y + s / 2f), style = Fill)
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = s * 0.45f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("A+", x + s / 2f, y + s * 0.65f, paint)
                }
            }
            "hundred" -> {
                drawScope.drawCircle(Color(0xFFFF5722), radius = s * 0.45f, center = Offset(x + s / 2f, y + s / 2f), style = Fill)
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = s * 0.32f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("100%", x + s / 2f, y + s * 0.62f, paint)
                }
            }
            "medal" -> {
                // Ribbon
                val ribbonRed = Color(0xFFE74C3C)
                val rPath = Path().apply {
                    moveTo(x + s * 0.4f, y + s * 0.1f)
                    lineTo(x + s * 0.3f, y + s * 0.45f)
                    lineTo(x + s * 0.5f, y + s * 0.38f)
                    lineTo(x + s * 0.7f, y + s * 0.45f)
                    lineTo(x + s * 0.6f, y + s * 0.1f)
                    close()
                }
                drawScope.drawPath(rPath, ribbonRed, style = Fill)
                // Gold coin
                drawScope.drawCircle(Color(0xFFFFC107), radius = s * 0.32f, center = Offset(x + s / 2f, y + s * 0.62f), style = Fill)
                drawScope.drawCircle(Color(0xFFFFA000), radius = s * 0.32f, center = Offset(x + s / 2f, y + s * 0.62f), style = Stroke(3f))
                drawScope.drawCircle(Color(0xFFFFA000), radius = s * 0.24f, center = Offset(x + s / 2f, y + s * 0.62f), style = Stroke(2f))
            }
            "crown" -> {
                val gold = Color(0xFFFFB300)
                val path = Path().apply {
                    moveTo(x + s * 0.15f, y + s * 0.75f)
                    lineTo(x + s * 0.85f, y + s * 0.75f)
                    lineTo(x + s * 0.9f, y + s * 0.35f)
                    lineTo(x + s * 0.65f, y + s * 0.55f)
                    lineTo(x + s * 0.5f, y + s * 0.25f)
                    lineTo(x + s * 0.35f, y + s * 0.55f)
                    lineTo(x + s * 0.1f, y + s * 0.35f)
                    close()
                }
                drawScope.drawPath(path, gold, style = Fill)
                drawScope.drawCircle(Color(0xFFE91E63), radius = s * 0.04f, center = Offset(x + s * 0.5f, y + s * 0.25f), style = Fill)
                drawScope.drawCircle(Color(0xFF2196F3), radius = s * 0.04f, center = Offset(x + s * 0.1f, y + s * 0.35f), style = Fill)
                drawScope.drawCircle(Color(0xFF2196F3), radius = s * 0.04f, center = Offset(x + s * 0.9f, y + s * 0.35f), style = Fill)
            }
            "thumbs_up" -> {
                drawScope.drawCircle(Color(0xFF3498DB), radius = s * 0.45f, center = Offset(x + s / 2f, y + s / 2f), style = Fill)
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = s * 0.45f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("👍", x + s / 2f, y + s * 0.66f, paint)
                }
            }
            "excellent" -> {
                drawScope.drawRoundRect(
                    Color(0xFF27AE60),
                    topLeft = Offset(x, y + s * 0.25f),
                    size = Size(s * 1.4f, s * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
                    style = Stroke(3.5f)
                )
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.rgb(39, 174, 96)
                        textSize = s * 0.25f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("Excellent!", x + s * 0.7f, y + s * 0.58f, paint)
                }
            }
            "good_job" -> {
                drawScope.drawRoundRect(
                    Color(0xFF2980B9),
                    topLeft = Offset(x, y + s * 0.25f),
                    size = Size(s * 1.4f, s * 0.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f),
                    style = Stroke(3.5f)
                )
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        color = android.graphics.Color.rgb(41, 128, 185)
                        textSize = s * 0.25f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText("Good Job!", x + s * 0.7f, y + s * 0.58f, paint)
                }
            }
            else -> {
                val emoji = getStickerEmoji(sticker.stickerId)
                drawScope.drawCircle(
                    color = Color(0xFFF1F5F9),
                    radius = s * 0.46f,
                    center = Offset(x + s / 2f, y + s / 2f),
                    style = Fill
                )
                drawScope.drawCircle(
                    color = Color(0xFFCBD5E1),
                    radius = s * 0.46f,
                    center = Offset(x + s / 2f, y + s / 2f),
                    style = Stroke(3f)
                )
                drawScope.drawIntoCanvas { canvas ->
                    val paint = Paint().apply {
                        textSize = s * 0.48f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(emoji, x + s / 2f, y + s * 0.68f, paint)
                }
            }
        }
    }

    fun getStickerEmoji(id: String): String {
        return when (id) {
            "star" -> "⭐"
            "trophy" -> "🏆"
            "medal" -> "🥇"
            "silver_medal" -> "🥈"
            "bronze_medal" -> "🥉"
            "crown" -> "👑"
            "a_plus" -> "🅰️"
            "hundred" -> "💯"
            "thumbs_up" -> "👍"
            "excellent" -> "🌟"
            "good_job" -> "👏"
            "target" -> "🎯"
            "grad_cap" -> "🎓"
            "books" -> "📚"
            "microscope" -> "🔬"
            "test_tube" -> "🧪"
            "verified" -> "✅"
            "needs_work" -> "⚠️"
            "pin" -> "📌"
            "priority" -> "🚨"
            "question" -> "❓"
            "idea" -> "💡"
            "perfect" -> "💎"
            "well_done" -> "🎉"
            "star_student" -> "🏅"
            "keep_going" -> "🚀"
            "great_effort" -> "🙌"
            "reviewed" -> "🔍"
            "rejected" -> "❌"
            "favorite" -> "❤️"
            "alarm" -> "⏰"
            "award" -> "🎖️"
            "smile" -> "😀"
            "heart_eyes" -> "😍"
            "party" -> "🥳"
            "thinking" -> "🤔"
            "cool" -> "😎"
            "mind_blown" -> "🤯"
            "rocket" -> "🚀"
            "fire" -> "🔥"
            "sparkles" -> "✨"
            "rainbow" -> "🌈"
            "lightning" -> "⚡"
            "coffee" -> "☕"
            "clap" -> "👏"
            "bulb" -> "💡"
            "heart" -> "❤️"
            "gem" -> "💎"
            "pi" -> "π"
            "sum" -> "∑"
            "integral" -> "∫"
            "sqrt" -> "√"
            "infinity" -> "∞"
            "not_equal" -> "≠"
            "approx" -> "≈"
            "plus_minus" -> "±"
            "delta" -> "△"
            "omega" -> "Ω"
            "atom" -> "⚛️"
            "magnet" -> "🧲"
            "dna" -> "🧬"
            "compass" -> "🧭"
            "ruler" -> "📏"
            "gear" -> "⚙️"
            "clipboard" -> "📋"
            "calendar" -> "📅"
            "bar_chart" -> "📊"
            "trend_up" -> "📈"
            "trend_down" -> "📉"
            "lock" -> "🔒"
            "key" -> "🔑"
            "mail" -> "✉️"
            "megaphone" -> "📢"
            "memo" -> "📝"
            "folder" -> "📁"
            "timer" -> "⏱️"
            "flag" -> "🚩"
            "shield" -> "🛡️"
            "globe" -> "🌐"
            "chat" -> "💬"
            else -> "🏷️"
        }
    }

    private fun drawScience(drawScope: DrawScope, science: CanvasElement.ScienceDiagram) {
        val s = science.size
        val cx = science.x + s / 2f
        val cy = science.y + s / 2f

        if (science.category == "atomic") {
            // Draw Bohr Model (Nucleus + Orbits + Electrons)
            val orbitColor = Color(0xFF2980B9)
            val electronColor = Color(0xFF1E88E5)
            val nucleusColor = Color(0xFFE74C3C)

            val n = science.atomicNumber
            val shells = when {
                n <= 2 -> listOf(n)
                n <= 10 -> listOf(2, n - 2)
                else -> listOf(2, 8, n - 10)
            }

            // Orbits
            shells.forEachIndexed { idx, electronsInShell ->
                val orbitRadius = (s * 0.22f) * (idx + 1)
                drawScope.drawCircle(
                    color = orbitColor.copy(alpha = 0.6f),
                    radius = orbitRadius,
                    center = Offset(cx, cy),
                    style = Stroke(1.8f)
                )

                // Electrons
                for (e in 0 until electronsInShell) {
                    val angle = (2 * PI * e / electronsInShell).toFloat()
                    val ex = cx + orbitRadius * cos(angle)
                    val ey = cy + orbitRadius * sin(angle)
                    drawScope.drawCircle(electronColor, radius = 4.5f, center = Offset(ex, ey), style = Fill)
                }
            }

            // Central Nucleus with Element Symbol
            val nucleusRadius = s * 0.16f
            drawScope.drawCircle(nucleusColor, radius = nucleusRadius, center = Offset(cx, cy), style = Fill)
            drawScope.drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = nucleusRadius * 1.1f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(science.symbolKey, cx, cy + nucleusRadius * 0.38f, paint)

                val subPaint = Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = s * 0.14f
                    isAntiAlias = true
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(science.label, cx, cy + s * 0.48f, subPaint)
            }
        } else {
            // Flask / Lab apparatus
            val glassColor = Color(0xFF16A085)
            val path = Path().apply {
                moveTo(cx - s * 0.15f, cy - s * 0.4f)
                lineTo(cx + s * 0.15f, cy - s * 0.4f)
                lineTo(cx + s * 0.15f, cy - s * 0.15f)
                lineTo(cx + s * 0.42f, cy + s * 0.38f)
                lineTo(cx - s * 0.42f, cy + s * 0.38f)
                lineTo(cx - s * 0.15f, cy - s * 0.15f)
                close()
            }
            drawScope.drawPath(path, glassColor, style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            // Liquid inside
            val liquidPath = Path().apply {
                moveTo(cx - s * 0.32f, cy + s * 0.15f)
                lineTo(cx + s * 0.32f, cy + s * 0.15f)
                lineTo(cx + s * 0.4f, cy + s * 0.36f)
                lineTo(cx - s * 0.4f, cy + s * 0.36f)
                close()
            }
            drawScope.drawPath(liquidPath, Color(0xFF00E5FF).copy(alpha = 0.4f), style = Fill)
            drawScope.drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = android.graphics.Color.DKGRAY
                    textSize = s * 0.14f
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(science.label, cx, cy + s * 0.48f, paint)
            }
        }
    }

    private fun drawImage(drawScope: DrawScope, img: CanvasElement.Image) {
        val uriStr = img.uriString
        val cached = imageCache[uriStr]

        if (cached != null) {
            drawScope.drawImage(
                image = cached,
                dstOffset = androidx.compose.ui.unit.IntOffset(img.x.toInt(), img.y.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(img.width.toInt(), img.height.toInt())
            )
        } else {
            // Draw placeholder frame
            drawScope.drawRoundRect(
                color = Color(0x332196F3),
                topLeft = Offset(img.x, img.y),
                size = Size(img.width, img.height),
                cornerRadius = CornerRadius(12f, 12f),
                style = Fill
            )
            drawScope.drawRoundRect(
                color = Color(0xFF2196F3),
                topLeft = Offset(img.x, img.y),
                size = Size(img.width, img.height),
                cornerRadius = CornerRadius(12f, 12f),
                style = Stroke(width = 2f)
            )

            // Asynchronously load bitmap into cache
            if (!loadingUris.contains(uriStr)) {
                loadingUris.add(uriStr)
                Thread {
                    try {
                        var bitmap: android.graphics.Bitmap? = null
                        if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                            val conn = URL(uriStr).openConnection()
                            conn.connectTimeout = 8000
                            conn.readTimeout = 8000
                            val input = conn.getInputStream()
                            bitmap = BitmapFactory.decodeStream(input)
                            input.close()
                        } else {
                            val uri = android.net.Uri.parse(uriStr)
                            val path = uri.path
                            if (path != null && java.io.File(path).exists()) {
                                bitmap = BitmapFactory.decodeFile(path)
                            }
                        }
                        if (bitmap != null) {
                            imageCache[uriStr] = bitmap.asImageBitmap()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CanvasRenderer", "Failed to load image: $uriStr", e)
                    } finally {
                        loadingUris.remove(uriStr)
                    }
                }.start()
            }
        }
    }

    fun drawLaserTrail(drawScope: DrawScope, points: List<LaserPoint>, laserColor: Color, laserSize: Float) {
        if (points.isEmpty()) return
        val now = System.currentTimeMillis()
        val durationMs = 1200L

        points.forEachIndexed { index, p ->
            val age = now - p.timestamp
            if (age < durationMs) {
                val alpha = (1f - (age.toFloat() / durationMs.toFloat())).coerceIn(0f, 1f)
                // Outer glow
                drawScope.drawCircle(
                    color = laserColor.copy(alpha = alpha * 0.35f),
                    radius = laserSize * 1.8f,
                    center = Offset(p.x, p.y)
                )
                // Core
                drawScope.drawCircle(
                    color = laserColor.copy(alpha = alpha),
                    radius = laserSize * 0.8f,
                    center = Offset(p.x, p.y)
                )
                // Center bright spot
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.9f),
                    radius = laserSize * 0.35f,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }

    fun drawEraserCursor(drawScope: DrawScope, center: Offset, radius: Float, isDarkBg: Boolean) {
        // Soft outer shadow/glow
        drawScope.drawCircle(
            color = Color(0x33000000),
            radius = radius + 2f,
            center = center,
            style = Stroke(width = 3f)
        )
        // Translucent background fill
        drawScope.drawCircle(
            color = if (isDarkBg) Color.White.copy(alpha = 0.22f) else Color(0xFFFF5252).copy(alpha = 0.18f),
            radius = radius,
            center = center,
            style = Fill
        )
        // Outer crisp white/dark border
        drawScope.drawCircle(
            color = if (isDarkBg) Color.White else Color(0xFF222222),
            radius = radius,
            center = center,
            style = Stroke(width = 2.5f)
        )
        // Inner indicator ring
        drawScope.drawCircle(
            color = Color(0xFFFF1744),
            radius = (radius - 3f).coerceAtLeast(2f),
            center = center,
            style = Stroke(width = 1.5f)
        )
        // Center precision crosshair
        val cross = (radius * 0.35f).coerceIn(4f, 12f)
        drawScope.drawLine(
            color = if (isDarkBg) Color.White else Color(0xFFFF1744),
            start = Offset(center.x - cross, center.y),
            end = Offset(center.x + cross, center.y),
            strokeWidth = 2f
        )
        drawScope.drawLine(
            color = if (isDarkBg) Color.White else Color(0xFFFF1744),
            start = Offset(center.x, center.y - cross),
            end = Offset(center.x, center.y + cross),
            strokeWidth = 2f
        )
    }
}

private data class ShapeElementDraft(
    val shapeType: ShapeType,
    val x: Float,
    val y: Float,
    val endX: Float,
    val endY: Float,
    val color: Int,
    val strokeWidth: Float,
    val filled: Boolean
)
