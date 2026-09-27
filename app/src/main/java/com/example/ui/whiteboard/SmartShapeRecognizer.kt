package com.example.ui.whiteboard

import com.example.data.model.CanvasElement
import com.example.data.model.PointF
import com.example.data.model.ShapeType
import kotlin.math.*

object SmartShapeRecognizer {

    fun recognize(points: List<PointF>, layerId: String, colorArgb: Int, strokeWidth: Float): CanvasElement? {
        if (points.size < 5) return null

        val first = points.first()
        val last = points.last()
        val totalDist = distance(first, last)

        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        var pathLength = 0f
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            pathLength += distance(p1, p2)
            minX = minOf(minX, p1.x, p2.x)
            maxX = maxOf(maxX, p1.x, p2.x)
            minY = minOf(minY, p1.y, p2.y)
            maxY = maxOf(maxY, p1.y, p2.y)
        }

        val width = maxX - minX
        val height = maxY - minY
        if (width < 20 && height < 20) return null

        val isClosed = totalDist < 0.25f * pathLength || totalDist < 60f

        // Check Line
        if (!isClosed) {
            val directDist = distance(first, last)
            if (directDist > 0 && pathLength / directDist < 1.15f) {
                return CanvasElement.Shape(
                    id = "shape_${System.currentTimeMillis()}",
                    layerId = layerId,
                    shapeType = ShapeType.LINE,
                    startX = first.x,
                    startY = first.y,
                    endX = last.x,
                    endY = last.y,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }
        }

        // Closed shapes
        if (isClosed) {
            val aspectRatio = if (height > 0) width / height else 1f
            val perimeter = 2 * (width + height)
            val circlePerimeter = (PI * ((width + height) / 2)).toFloat()

            // Circle / Oval check
            val centerX = (minX + maxX) / 2
            val centerY = (minY + maxY) / 2
            val radiusX = width / 2
            val radiusY = height / 2

            var circleVariance = 0f
            for (p in points) {
                if (radiusX > 0 && radiusY > 0) {
                    val normalizedDist = hypot((p.x - centerX) / radiusX, (p.y - centerY) / radiusY)
                    circleVariance += abs(normalizedDist - 1f)
                }
            }
            circleVariance /= points.size

            if (circleVariance < 0.22f) {
                val shapeType = if (aspectRatio in 0.85f..1.18f) ShapeType.CIRCLE else ShapeType.OVAL
                return CanvasElement.Shape(
                    id = "shape_${System.currentTimeMillis()}",
                    layerId = layerId,
                    shapeType = shapeType,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // Rectangle / Square check
            if (abs(pathLength - perimeter) < perimeter * 0.35f) {
                val shapeType = if (aspectRatio in 0.85f..1.18f) ShapeType.SQUARE else ShapeType.RECT
                return CanvasElement.Shape(
                    id = "shape_${System.currentTimeMillis()}",
                    layerId = layerId,
                    shapeType = shapeType,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // Triangle check
            return CanvasElement.Shape(
                id = "shape_${System.currentTimeMillis()}",
                layerId = layerId,
                shapeType = ShapeType.TRIANGLE,
                startX = minX,
                startY = minY,
                endX = maxX,
                endY = maxY,
                colorArgb = colorArgb,
                strokeWidth = strokeWidth
            )
        }

        return null
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return hypot(p2.x - p1.x, p2.y - p1.y)
    }
}
