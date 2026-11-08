package com.example.ui.whiteboard

import com.example.data.model.CanvasElement
import com.example.data.model.PointF
import com.example.data.model.ShapeType
import kotlin.math.*

object SmartShapeRecognizer {

    fun recognize(
        rawPoints: List<PointF>,
        layerId: String,
        colorArgb: Int,
        strokeWidth: Float
    ): CanvasElement? {
        if (rawPoints.size < 4) return null

        // 1. Resample to standard count (48 equidistant points) for reliable feature extraction
        val points = resample(rawPoints, 48)
        if (points.size < 48) return null

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

        val width = max(maxX - minX, 10f)
        val height = max(maxY - minY, 10f)
        if (width < 12f && height < 12f) return null

        val start = points.first()
        val end = points.last()
        val directDist = distance(start, end)
        val closureRatio = if (pathLength > 0) directDist / pathLength else 1f
        val isClosed = closureRatio < 0.22f || (directDist < 35f && closureRatio < 0.35f)
        val aspectRatio = width / height

        // Detect corners / inflection points
        val corners = detectCorners(points)
        val cornerCount = corners.size

        // Calculate max deviation from straight line connecting start and end
        var maxDevFromLine = 0f
        for (p in points) {
            val dev = perpendicularDistance(p, start, end)
            if (dev > maxDevFromLine) maxDevFromLine = dev
        }
        val straightness = if (directDist > 0) maxDevFromLine / directDist else 1f

        // Count self-intersections
        val intersections = countIntersections(points)

        val now = System.currentTimeMillis()

        // -------------------------------------------------------------
        // CATEGORY 1: OPEN STROKES (Straight lines, Arrows, Symbols, Letters, Digits)
        // -------------------------------------------------------------
        if (!isClosed) {

            // 1.1 Straight Lines & Single-stroke dashes
            if (straightness < 0.14f && pathLength / max(directDist, 1f) < 1.18f) {
                // Horizontal line:
                if (height < width * 0.18f) {
                    if (width < 65f) {
                        return createTextElement("smart_minus_$now", layerId, "-", minX, maxY, colorArgb, height * 1.5f)
                    }
                    return CanvasElement.Shape(
                        id = "shape_line_$now",
                        layerId = layerId,
                        shapeType = ShapeType.LINE,
                        startX = start.x,
                        startY = (start.y + end.y) / 2f,
                        endX = end.x,
                        endY = (start.y + end.y) / 2f,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }
                // Vertical line:
                if (width < height * 0.18f) {
                    if (height < 75f) {
                        return createTextElement("smart_one_$now", layerId, "1", minX, maxY, colorArgb, height * 0.95f)
                    }
                    return CanvasElement.Shape(
                        id = "shape_line_$now",
                        layerId = layerId,
                        shapeType = ShapeType.LINE,
                        startX = (start.x + end.x) / 2f,
                        startY = start.y,
                        endX = (start.x + end.x) / 2f,
                        endY = end.y,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }
                // Diagonal line
                return CanvasElement.Shape(
                    id = "shape_line_$now",
                    layerId = layerId,
                    shapeType = ShapeType.LINE,
                    startX = start.x,
                    startY = start.y,
                    endX = end.x,
                    endY = end.y,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 1.2 Arrow recognition (Line with V-shape return at the end)
            if (isArrowGesture(points)) {
                return CanvasElement.Shape(
                    id = "shape_arrow_$now",
                    layerId = layerId,
                    shapeType = ShapeType.ARROW,
                    startX = start.x,
                    startY = start.y,
                    endX = end.x,
                    endY = end.y,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 1.3 Checkmark (✓)
            if (isCheckmark(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_check_$now", layerId, "✓", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.4 Plus sign (+) or Cross (×)
            if (isCrossOrPlus(points, minX, maxX, minY, maxY, aspectRatio)) {
                val symbol = if (abs(start.x - end.x) < width * 0.4f || abs(start.y - end.y) < height * 0.4f) "+" else "×"
                return createTextElement("smart_op_$now", layerId, symbol, minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.5 Letter C (Open arc on right)
            if (isLetterC(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_c_$now", layerId, "C", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.6 Letter L (Vertical down + horizontal right with 1 sharp corner)
            if (isLetterL(points, corners, minX, maxX, minY, maxY)) {
                return createTextElement("smart_l_$now", layerId, "L", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.7 Letter V (Diagonal down + diagonal up with 1 sharp corner at bottom)
            if (isLetterV(points, corners, minX, maxX, minY, maxY)) {
                return createTextElement("smart_v_$now", layerId, "V", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.8 Letter U (Vertical down, smooth curve at bottom, vertical up)
            if (isLetterU(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_u_$now", layerId, "U", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.9 Letter S or Digit 5 (Smooth double curve)
            if (isLetterS(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_s_$now", layerId, "S", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.10 Letter Z or Digit 7
            if (isLetterZorSeven(points, corners, minX, maxX, minY, maxY)) {
                val char = if (corners.size >= 2 || end.x > minX + width * 0.4f) "Z" else "7"
                return createTextElement("smart_z7_$now", layerId, char, minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.11 Letter M or W
            if (isLetterM(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_m_$now", layerId, "M", minX, maxY, colorArgb, height * 0.95f)
            }
            if (isLetterW(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_w_$now", layerId, "W", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.12 Digit 2 or 3
            if (isDigitThree(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_3_$now", layerId, "3", minX, maxY, colorArgb, height * 0.95f)
            }
            if (isDigitTwo(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_2_$now", layerId, "2", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.13 Figure-8 (8) or Infinity (∞)
            if (intersections >= 1) {
                val symbol = if (aspectRatio > 1.3f) "∞" else "8"
                return createTextElement("smart_8_$now", layerId, symbol, minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.14 Question mark (?)
            if (isQuestionMark(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_q_$now", layerId, "?", minX, maxY, colorArgb, height * 0.95f)
            }

            // 1.15 Square root (√)
            if (isSquareRoot(points, minX, maxX, minY, maxY)) {
                return createTextElement("smart_sqrt_$now", layerId, "√", minX, maxY, colorArgb, height * 0.95f)
            }
        }

        // -------------------------------------------------------------
        // CATEGORY 2: CLOSED STROKES (Triangles, Rectangles, Squares, Stars, Hearts, Circles, Ovals)
        // -------------------------------------------------------------
        if (isClosed) {

            // 2.1 Heart Shape (Top central dip, bottom sharp vertex)
            if (isHeartShape(points, minX, maxX, minY, maxY)) {
                return CanvasElement.Shape(
                    id = "shape_heart_$now",
                    layerId = layerId,
                    shapeType = ShapeType.HEART,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 2.2 Star Shape (5 or 4 outer sharp peaks)
            if (isStarShape(points, corners, minX, maxX, minY, maxY)) {
                return CanvasElement.Shape(
                    id = "shape_star_$now",
                    layerId = layerId,
                    shapeType = ShapeType.STAR,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 2.3 Cloud Shape (Multiple rounded bumps along boundary)
            if (isCloudShape(points, minX, maxX, minY, maxY)) {
                return CanvasElement.Shape(
                    id = "shape_cloud_$now",
                    layerId = layerId,
                    shapeType = ShapeType.CLOUD,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 2.4 Triangle (Exactly 3 sharp corners)
            if (cornerCount == 3) {
                // Check if one angle is approximately 90° (Right Triangle)
                val isRight = isRightTriangleCorners(corners)
                val shapeType = if (isRight) ShapeType.RIGHT_TRIANGLE else ShapeType.TRIANGLE
                return CanvasElement.Shape(
                    id = "shape_tri_$now",
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

            // 2.5 Rectangle, Square, or Diamond (4 dominant corners)
            if (cornerCount == 4) {
                // Check Diamond: corners near midpoints of bounding box
                val isDiamond = corners.any { abs(it.x - (minX + maxX) / 2f) < width * 0.2f && it.y < minY + height * 0.25f } &&
                        corners.any { abs(it.y - (minY + maxY) / 2f) < height * 0.2f && it.x > maxX - width * 0.25f }

                if (isDiamond) {
                    return CanvasElement.Shape(
                        id = "shape_diamond_$now",
                        layerId = layerId,
                        shapeType = ShapeType.DIAMOND,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }

                // Square vs Rectangle:
                if (aspectRatio in 0.82f..1.22f) {
                    val side = max(width, height)
                    return CanvasElement.Shape(
                        id = "shape_square_$now",
                        layerId = layerId,
                        shapeType = ShapeType.SQUARE,
                        startX = minX,
                        startY = minY,
                        endX = minX + side,
                        endY = minY + side,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                } else {
                    return CanvasElement.Shape(
                        id = "shape_rect_$now",
                        layerId = layerId,
                        shapeType = ShapeType.RECT,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }
            }

            // 2.6 Hexagon (6 corners)
            if (cornerCount in 5..7 && !isStarShape(points, corners, minX, maxX, minY, maxY)) {
                return CanvasElement.Shape(
                    id = "shape_hex_$now",
                    layerId = layerId,
                    shapeType = ShapeType.HEXAGON,
                    startX = minX,
                    startY = minY,
                    endX = maxX,
                    endY = maxY,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }

            // 2.7 Smooth closed curves: Circle vs Oval vs Digit 0 / Letter O
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val radiusX = width / 2f
            val radiusY = height / 2f

            var varianceSum = 0f
            for (p in points) {
                val normDist = hypot((p.x - centerX) / radiusX, (p.y - centerY) / radiusY)
                varianceSum += abs(normDist - 1f)
            }
            val avgVariance = varianceSum / points.size

            // Low variance curve indicates a smooth geometric circle/oval
            if (avgVariance < 0.18f && cornerCount <= 2) {
                if (aspectRatio in 0.84f..1.18f) {
                    // Small closed circles in writing can be 0 or O, but standard shape is CIRCLE
                    val diam = max(width, height)
                    return CanvasElement.Shape(
                        id = "shape_circle_$now",
                        layerId = layerId,
                        shapeType = ShapeType.CIRCLE,
                        startX = minX,
                        startY = minY,
                        endX = minX + diam,
                        endY = minY + diam,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                } else {
                    return CanvasElement.Shape(
                        id = "shape_oval_$now",
                        layerId = layerId,
                        shapeType = ShapeType.OVAL,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }
            }

            // 2.8 Fallback for closed shapes with high straight line perimeter (Rect/Square)
            val perimeter = 2 * (width + height)
            if (abs(pathLength - perimeter) < perimeter * 0.28f) {
                if (aspectRatio in 0.82f..1.22f) {
                    val side = max(width, height)
                    return CanvasElement.Shape(
                        id = "shape_square_$now",
                        layerId = layerId,
                        shapeType = ShapeType.SQUARE,
                        startX = minX,
                        startY = minY,
                        endX = minX + side,
                        endY = minY + side,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                } else {
                    return CanvasElement.Shape(
                        id = "shape_rect_$now",
                        layerId = layerId,
                        shapeType = ShapeType.RECT,
                        startX = minX,
                        startY = minY,
                        endX = maxX,
                        endY = maxY,
                        colorArgb = colorArgb,
                        strokeWidth = strokeWidth
                    )
                }
            }

            // Default smooth circle fallback if reasonably circular
            if (aspectRatio in 0.80f..1.25f && avgVariance < 0.25f) {
                val diam = max(width, height)
                return CanvasElement.Shape(
                    id = "shape_circle_$now",
                    layerId = layerId,
                    shapeType = ShapeType.CIRCLE,
                    startX = minX,
                    startY = minY,
                    endX = minX + diam,
                    endY = minY + diam,
                    colorArgb = colorArgb,
                    strokeWidth = strokeWidth
                )
            }
        }

        // Return null if not confidently recognizable so it preserves user's organic drawing
        return null
    }

    // -------------------------------------------------------------
    // HELPER FUNCTIONS & FEATURE DETECTORS
    // -------------------------------------------------------------

    private fun createTextElement(
        id: String,
        layerId: String,
        text: String,
        minX: Float,
        maxY: Float,
        colorArgb: Int,
        fontSize: Float
    ): CanvasElement.Text {
        return CanvasElement.Text(
            id = id,
            layerId = layerId,
            text = text,
            x = minX + 4f,
            y = maxY - 4f,
            colorArgb = colorArgb,
            fontSize = fontSize.coerceIn(24f, 180f)
        )
    }

    private fun resample(points: List<PointF>, targetCount: Int): List<PointF> {
        if (points.size < 2) return points
        var totalLength = 0f
        for (i in 0 until points.size - 1) {
            totalLength += distance(points[i], points[i + 1])
        }
        if (totalLength <= 0f) return points

        val step = totalLength / (targetCount - 1)
        val result = mutableListOf<PointF>()
        result.add(points.first())

        var accumulated = 0f
        var currentIdx = 0

        for (i in 1 until targetCount - 1) {
            val targetDist = i * step
            while (currentIdx < points.size - 1) {
                val segLen = distance(points[currentIdx], points[currentIdx + 1])
                if (accumulated + segLen >= targetDist) {
                    val remain = targetDist - accumulated
                    val t = if (segLen > 0) remain / segLen else 0f
                    val interpX = points[currentIdx].x + t * (points[currentIdx + 1].x - points[currentIdx].x)
                    val interpY = points[currentIdx].y + t * (points[currentIdx + 1].y - points[currentIdx].y)
                    result.add(PointF(interpX, interpY))
                    break
                } else {
                    accumulated += segLen
                    currentIdx++
                }
            }
        }
        result.add(points.last())
        return result
    }

    private fun detectCorners(points: List<PointF>): List<PointF> {
        val corners = mutableListOf<PointF>()
        val n = points.size
        val k = 4 // Lookahead distance
        for (i in k until n - k) {
            val pPrev = points[i - k]
            val pCurr = points[i]
            val pNext = points[i + k]

            val angle1 = atan2(pCurr.y - pPrev.y, pCurr.x - pPrev.x)
            val angle2 = atan2(pNext.y - pCurr.y, pNext.x - pCurr.x)
            var deltaAngle = abs(Math.toDegrees((angle2 - angle1).toDouble())).toFloat()
            if (deltaAngle > 180f) deltaAngle = 360f - deltaAngle

            if (deltaAngle > 50f) {
                // Ensure not too close to previously detected corner
                if (corners.isEmpty() || distance(corners.last(), pCurr) > 32f) {
                    corners.add(pCurr)
                }
            }
        }
        return corners
    }

    private fun isRightTriangleCorners(corners: List<PointF>): Boolean {
        if (corners.size < 3) return false
        val p1 = corners[0]
        val p2 = corners[1]
        val p3 = corners[2]

        val angles = listOf(
            angleBetween(p1, p2, p3),
            angleBetween(p2, p3, p1),
            angleBetween(p3, p1, p2)
        )
        return angles.any { abs(it - 90f) < 22f }
    }

    private fun angleBetween(a: PointF, b: PointF, c: PointF): Float {
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v2x = c.x - b.x
        val v2y = c.y - b.y
        val dot = v1x * v2x + v1y * v2y
        val mag1 = hypot(v1x, v1y)
        val mag2 = hypot(v2x, v2y)
        if (mag1 * mag2 == 0f) return 0f
        val cosVal = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return Math.toDegrees(acos(cosVal.toDouble())).toFloat()
    }

    private fun countIntersections(points: List<PointF>): Int {
        var count = 0
        val n = points.size
        for (i in 0 until n - 3 step 2) {
            for (j in i + 3 until n - 1 step 2) {
                if (segmentsIntersect(points[i], points[i + 1], points[j], points[j + 1])) {
                    count++
                    if (count >= 3) return count
                }
            }
        }
        return count
    }

    private fun segmentsIntersect(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
        val d1 = ccw(p3, p4, p1)
        val d2 = ccw(p3, p4, p2)
        val d3 = ccw(p1, p2, p3)
        val d4 = ccw(p1, p2, p4)
        return ((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))
    }

    private fun ccw(a: PointF, b: PointF, c: PointF): Float {
        return (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)
    }

    private fun perpendicularDistance(p: PointF, a: PointF, b: PointF): Float {
        val l = distance(a, b)
        if (l == 0f) return distance(p, a)
        return abs((b.y - a.y) * p.x - (b.x - a.x) * p.y + b.x * a.y - b.y * a.x) / l
    }

    private fun distance(p1: PointF, p2: PointF): Float {
        return hypot(p2.x - p1.x, p2.y - p1.y)
    }

    // Gesture specific tests:

    private fun isArrowGesture(points: List<PointF>): Boolean {
        val n = points.size
        val mainLineEndIdx = (n * 0.78f).toInt()
        val start = points.first()
        val mid = points[mainLineEndIdx]
        val mainDist = distance(start, mid)
        if (mainDist < 40f) return false

        // Last 20% should fold back sharply forming arrowhead
        val tip = points[mainLineEndIdx]
        val end = points.last()
        val tailDist = distance(tip, end)
        return tailDist > 15f && tailDist < mainDist * 0.65f
    }

    private fun isCheckmark(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val n = points.size
        val start = points.first()
        val end = points.last()
        // Starts higher, dips down, ends much higher on the right
        val lowestPt = points.minByOrNull { it.y } ?: return false
        val lowestIdx = points.indexOf(lowestPt)
        return start.x < end.x &&
                end.x > minX + (maxX - minX) * 0.7f &&
                end.y < lowestPt.y - (maxY - minY) * 0.5f &&
                lowestIdx in (n * 0.2f).toInt()..(n * 0.55f).toInt()
    }

    private fun isCrossOrPlus(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float, aspectRatio: Float): Boolean {
        return aspectRatio in 0.75f..1.35f &&
                points.any { it.x < minX + (maxX - minX) * 0.15f } &&
                points.any { it.x > maxX - (maxX - minX) * 0.15f } &&
                points.any { it.y < minY + (maxY - minY) * 0.15f } &&
                points.any { it.y > maxY - (maxY - minY) * 0.15f }
    }

    private fun isLetterC(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val mid = points[points.size / 2]
        val width = maxX - minX
        return start.x > minX + width * 0.5f &&
                end.x > minX + width * 0.5f &&
                mid.x < minX + width * 0.25f &&
                start.y < minY + (maxY - minY) * 0.35f &&
                end.y > maxY - (maxY - minY) * 0.35f
    }

    private fun isLetterL(points: List<PointF>, corners: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        return start.y < minY + (maxY - minY) * 0.3f &&
                start.x < minX + (maxX - minX) * 0.4f &&
                end.x > maxX - (maxX - minX) * 0.3f &&
                end.y > maxY - (maxY - minY) * 0.3f &&
                corners.size in 1..2
    }

    private fun isLetterV(points: List<PointF>, corners: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val lowestPt = points.maxByOrNull { it.y } ?: return false
        val width = maxX - minX
        val height = maxY - minY
        return start.y < minY + height * 0.35f &&
                end.y < minY + height * 0.35f &&
                start.x < minX + width * 0.35f &&
                end.x > maxX - width * 0.35f &&
                lowestPt.y > maxY - height * 0.15f &&
                abs(lowestPt.x - (minX + maxX) / 2f) < width * 0.35f
    }

    private fun isLetterU(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val mid = points[points.size / 2]
        val height = maxY - minY
        val width = maxX - minX
        return start.y < minY + height * 0.35f &&
                end.y < minY + height * 0.35f &&
                mid.y > maxY - height * 0.2f &&
                start.x < minX + width * 0.35f &&
                end.x > maxX - width * 0.35f
    }

    private fun isLetterS(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val p25 = points[(points.size * 0.25f).toInt()]
        val p75 = points[(points.size * 0.75f).toInt()]
        val width = maxX - minX
        return start.x > minX + width * 0.4f &&
                p25.x > minX + width * 0.6f &&
                p75.x < minX + width * 0.4f &&
                end.x > minX + width * 0.3f &&
                start.y < minY + (maxY - minY) * 0.25f &&
                end.y > maxY - (maxY - minY) * 0.25f
    }

    private fun isLetterZorSeven(points: List<PointF>, corners: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val width = maxX - minX
        return start.x < minX + width * 0.35f &&
                start.y < minY + (maxY - minY) * 0.3f &&
                end.y > maxY - (maxY - minY) * 0.3f
    }

    private fun isLetterM(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val n = points.size
        val p25 = points[(n * 0.25f).toInt()]
        val p50 = points[(n * 0.5f).toInt()]
        val p75 = points[(n * 0.75f).toInt()]
        val height = maxY - minY
        return p25.y < minY + height * 0.3f &&
                p75.y < minY + height * 0.3f &&
                p50.y > minY + height * 0.45f
    }

    private fun isLetterW(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val n = points.size
        val p25 = points[(n * 0.25f).toInt()]
        val p50 = points[(n * 0.5f).toInt()]
        val p75 = points[(n * 0.75f).toInt()]
        val height = maxY - minY
        return p25.y > maxY - height * 0.3f &&
                p75.y > maxY - height * 0.3f &&
                p50.y < maxY - height * 0.45f
    }

    private fun isDigitThree(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val pMid = points[points.size / 2]
        val width = maxX - minX
        return start.x < minX + width * 0.5f &&
                end.x < minX + width * 0.5f &&
                pMid.x < minX + width * 0.6f &&
                points.any { it.x > maxX - width * 0.2f }
    }

    private fun isDigitTwo(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val width = maxX - minX
        return start.y < minY + (maxY - minY) * 0.3f &&
                end.y > maxY - (maxY - minY) * 0.2f &&
                end.x > maxX - width * 0.35f
    }

    private fun isQuestionMark(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val height = maxY - minY
        return start.y < minY + height * 0.4f &&
                end.y > maxY - height * 0.25f &&
                abs(end.x - (minX + maxX) / 2f) < (maxX - minX) * 0.3f
    }

    private fun isSquareRoot(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val start = points.first()
        val end = points.last()
        val lowestPt = points.minByOrNull { it.y } ?: return false
        val width = maxX - minX
        return start.x < minX + width * 0.3f &&
                end.x > maxX - width * 0.2f &&
                end.y < minY + (maxY - minY) * 0.3f
    }

    private fun isHeartShape(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val width = maxX - minX
        val height = maxY - minY
        val centerX = (minX + maxX) / 2f

        // Top center dip: points near center X at top have a valley
        val topCenterPt = points.filter { abs(it.x - centerX) < width * 0.15f }
            .minByOrNull { it.y } ?: return false

        val leftLobeMax = points.filter { it.x < centerX - width * 0.15f }
            .minByOrNull { it.y } ?: return false

        val rightLobeMax = points.filter { it.x > centerX + width * 0.15f }
            .minByOrNull { it.y } ?: return false

        val bottomPt = points.maxByOrNull { it.y } ?: return false

        return leftLobeMax.y < topCenterPt.y - height * 0.12f &&
                rightLobeMax.y < topCenterPt.y - height * 0.12f &&
                abs(bottomPt.x - centerX) < width * 0.25f &&
                bottomPt.y > maxY - height * 0.15f
    }

    private fun isStarShape(points: List<PointF>, corners: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        if (corners.size >= 5) return true
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val maxR = max(maxX - minX, maxY - minY) / 2f

        // Count radial oscillations (peaks and valleys)
        var peakCount = 0
        var wasIncreasing = false
        val radii = points.map { hypot(it.x - centerX, it.y - centerY) }

        for (i in 1 until radii.size - 1) {
            val isIncreasing = radii[i] > radii[i - 1]
            if (wasIncreasing && !isIncreasing && radii[i] > maxR * 0.65f) {
                peakCount++
            }
            wasIncreasing = isIncreasing
        }
        return peakCount in 4..6
    }

    private fun isCloudShape(points: List<PointF>, minX: Float, maxX: Float, minY: Float, maxY: Float): Boolean {
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val maxR = max(maxX - minX, maxY - minY) / 2f

        var bumps = 0
        val radii = points.map { hypot(it.x - centerX, it.y - centerY) }
        for (i in 2 until radii.size - 2) {
            if (radii[i] > radii[i - 1] && radii[i] > radii[i + 1] && radii[i] > maxR * 0.6f) {
                bumps++
            }
        }
        return bumps >= 4
    }
}
