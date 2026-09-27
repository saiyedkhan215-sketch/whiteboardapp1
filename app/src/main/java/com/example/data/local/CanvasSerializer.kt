package com.example.data.local

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

object CanvasSerializer {

    fun serializePages(pages: List<BoardPageState>): String {
        val rootArray = JSONArray()
        for (page in pages) {
            val pageObj = JSONObject()
            pageObj.put("pageIndex", page.pageIndex)

            // Layers
            val layersArray = JSONArray()
            for (layer in page.layers) {
                val lObj = JSONObject()
                lObj.put("id", layer.id)
                lObj.put("name", layer.name)
                lObj.put("isVisible", layer.isVisible)
                lObj.put("isLocked", layer.isLocked)
                layersArray.put(lObj)
            }
            pageObj.put("layers", layersArray)

            // Elements
            val elementsArray = JSONArray()
            for (el in page.elements) {
                val elObj = JSONObject()
                elObj.put("id", el.id)
                elObj.put("layerId", el.layerId)
                elObj.put("rotation", el.rotation.toDouble())
                elObj.put("scale", el.scale.toDouble())
                when (el) {
                    is CanvasElement.Stroke -> {
                        elObj.put("type", "stroke")
                        elObj.put("colorArgb", el.colorArgb)
                        elObj.put("strokeWidth", el.strokeWidth.toDouble())
                        elObj.put("brushType", el.brushType.name)
                        val ptsArray = JSONArray()
                        for (p in el.points) {
                            val ptObj = JSONObject()
                            ptObj.put("x", p.x.toDouble())
                            ptObj.put("y", p.y.toDouble())
                            ptObj.put("p", p.pressure.toDouble())
                            ptsArray.put(ptObj)
                        }
                        elObj.put("points", ptsArray)
                    }
                    is CanvasElement.Shape -> {
                        elObj.put("type", "shape")
                        elObj.put("shapeType", el.shapeType.name)
                        elObj.put("startX", el.startX.toDouble())
                        elObj.put("startY", el.startY.toDouble())
                        elObj.put("endX", el.endX.toDouble())
                        elObj.put("endY", el.endY.toDouble())
                        elObj.put("colorArgb", el.colorArgb)
                        elObj.put("strokeWidth", el.strokeWidth.toDouble())
                        elObj.put("isFilled", el.isFilled)
                    }
                    is CanvasElement.Text -> {
                        elObj.put("type", "text")
                        elObj.put("text", el.text)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("colorArgb", el.colorArgb)
                        elObj.put("fontSize", el.fontSize.toDouble())
                    }
                    is CanvasElement.Formula -> {
                        elObj.put("type", "formula")
                        elObj.put("formula", el.formula)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("colorArgb", el.colorArgb)
                        elObj.put("fontSize", el.fontSize.toDouble())
                    }
                    is CanvasElement.Table -> {
                        elObj.put("type", "table")
                        elObj.put("rows", el.rows)
                        elObj.put("cols", el.cols)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("cellWidth", el.cellWidth.toDouble())
                        elObj.put("cellHeight", el.cellHeight.toDouble())
                        elObj.put("colorArgb", el.colorArgb)
                    }
                    is CanvasElement.Sticker -> {
                        elObj.put("type", "sticker")
                        elObj.put("stickerId", el.stickerId)
                        elObj.put("title", el.title)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("size", el.size.toDouble())
                    }
                    is CanvasElement.ScienceDiagram -> {
                        elObj.put("type", "science")
                        elObj.put("category", el.category)
                        elObj.put("symbolKey", el.symbolKey)
                        elObj.put("label", el.label)
                        elObj.put("atomicNumber", el.atomicNumber)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("size", el.size.toDouble())
                    }
                    is CanvasElement.Image -> {
                        elObj.put("type", "image")
                        elObj.put("uriString", el.uriString)
                        elObj.put("x", el.x.toDouble())
                        elObj.put("y", el.y.toDouble())
                        elObj.put("width", el.width.toDouble())
                        elObj.put("height", el.height.toDouble())
                    }
                }
                elementsArray.put(elObj)
            }
            pageObj.put("elements", elementsArray)
            rootArray.put(pageObj)
        }
        return rootArray.toString()
    }

    fun deserializePages(jsonString: String): List<BoardPageState> {
        if (jsonString.isBlank()) {
            return listOf(BoardPageState())
        }
        val result = mutableListOf<BoardPageState>()
        try {
            val rootArray = JSONArray(jsonString)
            for (i in 0 until rootArray.length()) {
                val pageObj = rootArray.getJSONObject(i)
                val pageIndex = pageObj.optInt("pageIndex", i)

                val layers = mutableListOf<LayerInfo>()
                val layersArray = pageObj.optJSONArray("layers")
                if (layersArray != null && layersArray.length() > 0) {
                    for (l in 0 until layersArray.length()) {
                        val lObj = layersArray.getJSONObject(l)
                        layers.add(
                            LayerInfo(
                                id = lObj.optString("id", "layer_$l"),
                                name = lObj.optString("name", "Layer ${l + 1}"),
                                isVisible = lObj.optBoolean("isVisible", true),
                                isLocked = lObj.optBoolean("isLocked", false)
                            )
                        )
                    }
                } else {
                    layers.add(LayerInfo("layer_1", "Layer 1"))
                }

                val elements = mutableListOf<CanvasElement>()
                val elementsArray = pageObj.optJSONArray("elements")
                if (elementsArray != null) {
                    for (e in 0 until elementsArray.length()) {
                        val elObj = elementsArray.getJSONObject(e)
                        val id = elObj.optString("id", System.currentTimeMillis().toString())
                        val layerId = elObj.optString("layerId", "layer_1")
                        val rotation = elObj.optDouble("rotation", 0.0).toFloat()
                        val scale = elObj.optDouble("scale", 1.0).toFloat()
                        val type = elObj.optString("type")

                        when (type) {
                            "stroke" -> {
                                val ptsArray = elObj.optJSONArray("points")
                                val pts = mutableListOf<PointF>()
                                if (ptsArray != null) {
                                    for (p in 0 until ptsArray.length()) {
                                        val ptObj = ptsArray.getJSONObject(p)
                                        pts.add(
                                            PointF(
                                                x = ptObj.optDouble("x", 0.0).toFloat(),
                                                y = ptObj.optDouble("y", 0.0).toFloat(),
                                                pressure = ptObj.optDouble("p", 1.0).toFloat()
                                            )
                                        )
                                    }
                                }
                                val brushName = elObj.optString("brushType", "PEN")
                                val brushType = try { BrushType.valueOf(brushName) } catch (_: Exception) { BrushType.PEN }
                                elements.add(
                                    CanvasElement.Stroke(
                                        id = id,
                                        layerId = layerId,
                                        points = pts,
                                        colorArgb = elObj.optInt("colorArgb", 0xFF000000.toInt()),
                                        strokeWidth = elObj.optDouble("strokeWidth", 12.0).toFloat(),
                                        brushType = brushType,
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "shape" -> {
                                val shapeName = elObj.optString("shapeType", "RECT")
                                val shapeType = try { ShapeType.valueOf(shapeName) } catch (_: Exception) { ShapeType.RECT }
                                elements.add(
                                    CanvasElement.Shape(
                                        id = id,
                                        layerId = layerId,
                                        shapeType = shapeType,
                                        startX = elObj.optDouble("startX", 0.0).toFloat(),
                                        startY = elObj.optDouble("startY", 0.0).toFloat(),
                                        endX = elObj.optDouble("endX", 0.0).toFloat(),
                                        endY = elObj.optDouble("endY", 0.0).toFloat(),
                                        colorArgb = elObj.optInt("colorArgb", 0xFF000000.toInt()),
                                        strokeWidth = elObj.optDouble("strokeWidth", 8.0).toFloat(),
                                        isFilled = elObj.optBoolean("isFilled", false),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "text" -> {
                                elements.add(
                                    CanvasElement.Text(
                                        id = id,
                                        layerId = layerId,
                                        text = elObj.optString("text", ""),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        colorArgb = elObj.optInt("colorArgb", 0xFF000000.toInt()),
                                        fontSize = elObj.optDouble("fontSize", 48.0).toFloat(),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "formula" -> {
                                elements.add(
                                    CanvasElement.Formula(
                                        id = id,
                                        layerId = layerId,
                                        formula = elObj.optString("formula", ""),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        colorArgb = elObj.optInt("colorArgb", 0xFF000000.toInt()),
                                        fontSize = elObj.optDouble("fontSize", 40.0).toFloat(),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "table" -> {
                                elements.add(
                                    CanvasElement.Table(
                                        id = id,
                                        layerId = layerId,
                                        rows = elObj.optInt("rows", 2),
                                        cols = elObj.optInt("cols", 2),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        cellWidth = elObj.optDouble("cellWidth", 90.0).toFloat(),
                                        cellHeight = elObj.optDouble("cellHeight", 45.0).toFloat(),
                                        colorArgb = elObj.optInt("colorArgb", 0xFF000000.toInt()),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "sticker" -> {
                                elements.add(
                                    CanvasElement.Sticker(
                                        id = id,
                                        layerId = layerId,
                                        stickerId = elObj.optString("stickerId", "star"),
                                        title = elObj.optString("title", "Sticker"),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        size = elObj.optDouble("size", 120.0).toFloat(),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "science" -> {
                                elements.add(
                                    CanvasElement.ScienceDiagram(
                                        id = id,
                                        layerId = layerId,
                                        category = elObj.optString("category", "atomic"),
                                        symbolKey = elObj.optString("symbolKey", "H"),
                                        label = elObj.optString("label", "H"),
                                        atomicNumber = elObj.optInt("atomicNumber", 1),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        size = elObj.optDouble("size", 130.0).toFloat(),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                            "image" -> {
                                elements.add(
                                    CanvasElement.Image(
                                        id = id,
                                        layerId = layerId,
                                        uriString = elObj.optString("uriString", ""),
                                        x = elObj.optDouble("x", 0.0).toFloat(),
                                        y = elObj.optDouble("y", 0.0).toFloat(),
                                        width = elObj.optDouble("width", 300.0).toFloat(),
                                        height = elObj.optDouble("height", 300.0).toFloat(),
                                        rotation = rotation,
                                        scale = scale
                                    )
                                )
                            }
                        }
                    }
                }
                result.add(BoardPageState(pageIndex = pageIndex, elements = elements, layers = layers))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return listOf(BoardPageState())
        }
        return if (result.isEmpty()) listOf(BoardPageState()) else result
    }
}
