package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CanvasSerializer
import com.example.data.model.*
import com.example.ui.whiteboard.ElementTransformer
import com.example.ui.whiteboard.SmartShapeRecognizer
import com.example.ui.whiteboard.TransformHandle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WhiteBoard", appName)
    }

    @Test
    fun `serialize and deserialize board pages`() {
        val testPages = listOf(
            BoardPageState(
                pageIndex = 0,
                elements = listOf(
                    CanvasElement.Stroke(
                        id = "stroke_1",
                        layerId = "layer_1",
                        points = listOf(PointF(10f, 10f), PointF(20f, 20f)),
                        colorArgb = 0xFF000000.toInt(),
                        strokeWidth = 12f,
                        rotation = 15f,
                        scale = 1.2f
                    ),
                    CanvasElement.Text(
                        id = "text_1",
                        layerId = "layer_1",
                        text = "Hello WhiteBoard",
                        x = 100f,
                        y = 100f,
                        colorArgb = 0xFF2196F3.toInt(),
                        fontSize = 48f,
                        rotation = 45f,
                        scale = 1.5f
                    )
                ),
                layers = listOf(LayerInfo("layer_1", "Layer 1"))
            )
        )

        val json = CanvasSerializer.serializePages(testPages)
        assertTrue(json.isNotBlank())

        val deserialized = CanvasSerializer.deserializePages(json)
        assertEquals(1, deserialized.size)
        assertEquals(2, deserialized[0].elements.size)
        assertEquals(45f, deserialized[0].elements[1].rotation, 0.01f)
        assertEquals(1.5f, deserialized[0].elements[1].scale, 0.01f)
    }

    @Test
    fun `smart shape recognizer identifies straight line`() {
        val linePoints = listOf(
            PointF(10f, 10f),
            PointF(30f, 30f),
            PointF(60f, 60f),
            PointF(90f, 90f),
            PointF(120f, 120f),
            PointF(150f, 150f)
        )
        val recognized = SmartShapeRecognizer.recognize(linePoints, "layer_1", 0xFF000000.toInt(), 10f)
        assertNotNull(recognized)
        assertTrue(recognized is CanvasElement.Shape)
        val shape = recognized as CanvasElement.Shape
        assertEquals(ShapeType.LINE, shape.shapeType)
    }

    @Test
    fun `element transformation move rotate scale`() {
        val shape = CanvasElement.Shape(
            id = "shape_1",
            layerId = "layer_1",
            shapeType = ShapeType.RECT,
            startX = 100f,
            startY = 100f,
            endX = 200f,
            endY = 200f,
            colorArgb = 0xFF000000.toInt(),
            strokeWidth = 4f
        )

        // Translation
        val moved = shape.withTranslation(50f, 30f) as CanvasElement.Shape
        assertEquals(150f, moved.startX, 0.01f)
        assertEquals(130f, moved.startY, 0.01f)

        // Rotation
        val rotated = moved.withRotation(45f)
        assertEquals(45f, rotated.rotation, 0.01f)

        // Scale
        val scaled = rotated.withScale(2f)
        assertEquals(2f, scaled.scale, 0.01f)

        // Hit testing
        val hit = ElementTransformer.hitTestElement(scaled, Offset(150f, 150f))
        assertTrue(hit)
    }
}
