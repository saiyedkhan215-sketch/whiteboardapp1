package com.example.ui.whiteboard

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.model.CanvasElement
import kotlin.math.*

enum class TransformHandle {
    NONE,
    BODY,
    ROTATE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

object ElementTransformer {

    fun mapToLocal(point: Offset, element: CanvasElement): Offset {
        val center = element.getCenter()
        val rad = Math.toRadians(-element.rotation.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        val dx = point.x - center.x
        val dy = point.y - center.y

        val rx = dx * cosA - dy * sinA
        val ry = dx * sinA + dy * cosA

        val invScale = if (element.scale != 0f) 1f / element.scale else 1f
        return Offset(center.x + rx * invScale, center.y + ry * invScale)
    }

    fun hitTestHandle(element: CanvasElement, screenPoint: Offset): TransformHandle {
        val local = mapToLocal(screenPoint, element)
        val b = element.getBounds()
        val center = element.getCenter()

        // Generous touch radius for handles on touchscreens (scaled for high DPI)
        val touchRadius = 55f / element.scale.coerceAtLeast(0.5f)
        val stemLen = 48f / element.scale
        val rotateY = b.top - stemLen

        // 1. Check Rotate handle first
        if (hypot(local.x - center.x, local.y - rotateY) < touchRadius + 12f) {
            return TransformHandle.ROTATE
        }

        // 2. Check 4 Corner handles
        if (hypot(local.x - b.left, local.y - b.top) < touchRadius) return TransformHandle.TOP_LEFT
        if (hypot(local.x - b.right, local.y - b.top) < touchRadius) return TransformHandle.TOP_RIGHT
        if (hypot(local.x - b.left, local.y - b.bottom) < touchRadius) return TransformHandle.BOTTOM_LEFT
        if (hypot(local.x - b.right, local.y - b.bottom) < touchRadius) return TransformHandle.BOTTOM_RIGHT

        // 3. Check Body for dragging
        val margin = 28f / element.scale
        if (local.x in (b.left - margin)..(b.right + margin) && local.y in (b.top - margin)..(b.bottom + margin)) {
            return TransformHandle.BODY
        }

        return TransformHandle.NONE
    }

    fun hitTestElement(element: CanvasElement, screenPoint: Offset): Boolean {
        val local = mapToLocal(screenPoint, element)
        val b = element.getBounds()
        val margin = 24f / element.scale
        return local.x in (b.left - margin)..(b.right + margin) && local.y in (b.top - margin)..(b.bottom + margin)
    }
}
