package com.example.ui.whiteboard

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.BoardEntity
import com.example.data.local.CanvasSerializer
import com.example.data.local.WhiteboardDatabase
import com.example.data.model.*
import com.example.data.repository.BoardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

data class WhiteboardUiState(
    val boardId: Long = 0L,
    val boardTitle: String = "Untitled Board",
    val activeTool: ToolType = ToolType.PEN,
    val brushType: BrushType = BrushType.PEN,
    val penColor: Color = Color.Black,
    val penSize: Float = 12f,
    val eraserSize: Float = 40f,
    val selectedShape: ShapeType = ShapeType.RECT,
    val isShapeFilled: Boolean = false,
    val boardColor: Color = Color.White,
    val boardTexture: BoardTexture = BoardTexture.PLAIN,
    val laserColor: Color = Color(0xFFFF1744),
    val laserSize: Float = 8f,
    val laserPoints: List<LaserPoint> = emptyList(),
    val pages: List<BoardPageState> = listOf(BoardPageState()),
    val currentPageIndex: Int = 0,
    val activeLayerId: String = "layer_1",
    val isRulerVisible: Boolean = false,
    val isToolbarCollapsed: Boolean = false,
    val selectedElementId: String? = null,
    val enabledTools: Set<ToolType> = ToolType.values().toSet(),
    val toolOrder: List<ToolType> = listOf(
        ToolType.SELECT, ToolType.PEN, ToolType.SMART, ToolType.SHAPES, ToolType.ERASER, ToolType.BOARD, ToolType.TEXT,
        ToolType.FORMULA, ToolType.TABLE, ToolType.LAYERS, ToolType.IMAGE, ToolType.WEB_IMAGE, ToolType.CAMERA,
        ToolType.SAVE, ToolType.SHARE, ToolType.PDF, ToolType.UNDO, ToolType.REDO, ToolType.CLEAR,
        ToolType.OCR, ToolType.IMPORT_PDF, ToolType.STICKERS, ToolType.SCIENCE, ToolType.LASER, ToolType.RULER,
        ToolType.LASSO, ToolType.HAND_PAN, ToolType.CUSTOMIZE, ToolType.HELP
    ),
    // Dialog visibility
    val showPenDialog: Boolean = false,
    val showEraserDialog: Boolean = false,
    val showShapesDialog: Boolean = false,
    val showBoardDialog: Boolean = false,
    val showTextDialog: Boolean = false,
    val showFormulaDialog: Boolean = false,
    val showTableDialog: Boolean = false,
    val showLayersSheet: Boolean = false,
    val showStickersDialog: Boolean = false,
    val showScienceDialog: Boolean = false,
    val showLaserDialog: Boolean = false,
    val showCustomizeDialog: Boolean = false,
    val showClearConfirmDialog: Boolean = false,
    val showPagesDialog: Boolean = false,
    val showHelpDialog: Boolean = false,
    val showSavedSuccess: Boolean = false,
    val showWebImageDialog: Boolean = false,
    val showOcrDialog: Boolean = false,
    val showElementEditDialog: Boolean = false,
    val isOcrProcessing: Boolean = false,
    val ocrExtractedText: String = "",
    // Pan & Zoom
    val zoomScale: Float = 1f,
    val panOffset: Offset = Offset.Zero
)

class WhiteboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BoardRepository

    private val _uiState = MutableStateFlow(WhiteboardUiState())
    val uiState: StateFlow<WhiteboardUiState> = _uiState.asStateFlow()

    // Temporary current drawing stroke / shape
    val currentDrawingPoints = mutableStateListOf<PointF>()
    val currentShapeDraft = MutableStateFlow<CanvasElement.Shape?>(null)

    init {
        val db = WhiteboardDatabase.getDatabase(application)
        repository = BoardRepository(db.boardDao())

        // Laser trail cleanup ticker
        viewModelScope.launch {
            while (true) {
                delay(100)
                if (_uiState.value.laserPoints.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val filtered = _uiState.value.laserPoints.filter { now - it.timestamp < 1200L }
                    _uiState.update { it.copy(laserPoints = filtered) }
                }
            }
        }
    }

    fun loadBoard(boardId: Long) {
        if (boardId == 0L) {
            _uiState.value = WhiteboardUiState()
            return
        }
        viewModelScope.launch {
            val board = repository.getBoardById(boardId)
            if (board != null) {
                val pages = CanvasSerializer.deserializePages(board.dataJson)
                val texture = try { BoardTexture.valueOf(board.boardTexture) } catch (_: Exception) { BoardTexture.PLAIN }
                _uiState.update {
                    it.copy(
                        boardId = board.id,
                        boardTitle = board.title,
                        boardColor = Color(board.boardColorArgb),
                        boardTexture = texture,
                        pages = pages,
                        currentPageIndex = 0
                    )
                }
            }
        }
    }

    fun selectTool(tool: ToolType) {
        val current = _uiState.value.activeTool
        if (current == tool) {
            // Re-tapping opens the dialog for configuration
            when (tool) {
                ToolType.PEN -> _uiState.update { it.copy(showPenDialog = true) }
                ToolType.ERASER -> _uiState.update { it.copy(showEraserDialog = true) }
                ToolType.SHAPES -> _uiState.update { it.copy(showShapesDialog = true) }
                ToolType.BOARD -> _uiState.update { it.copy(showBoardDialog = true) }
                ToolType.TEXT -> _uiState.update { it.copy(showTextDialog = true) }
                ToolType.FORMULA -> _uiState.update { it.copy(showFormulaDialog = true) }
                ToolType.TABLE -> _uiState.update { it.copy(showTableDialog = true) }
                ToolType.LAYERS -> _uiState.update { it.copy(showLayersSheet = true) }
                ToolType.STICKERS -> _uiState.update { it.copy(showStickersDialog = true) }
                ToolType.SCIENCE -> _uiState.update { it.copy(showScienceDialog = true) }
                ToolType.LASER -> _uiState.update { it.copy(showLaserDialog = true) }
                ToolType.CUSTOMIZE -> _uiState.update { it.copy(showCustomizeDialog = true) }
                ToolType.WEB_IMAGE -> _uiState.update { it.copy(showWebImageDialog = true) }
                ToolType.OCR -> _uiState.update { it.copy(showOcrDialog = true) }
                else -> {}
            }
        } else {
            // First tap selects tool and for some tools immediately opens configuration
            _uiState.update { it.copy(activeTool = tool) }
            when (tool) {
                ToolType.SHAPES -> _uiState.update { it.copy(showShapesDialog = true) }
                ToolType.BOARD -> _uiState.update { it.copy(showBoardDialog = true) }
                ToolType.TEXT -> _uiState.update { it.copy(showTextDialog = true) }
                ToolType.FORMULA -> _uiState.update { it.copy(showFormulaDialog = true) }
                ToolType.TABLE -> _uiState.update { it.copy(showTableDialog = true) }
                ToolType.LAYERS -> _uiState.update { it.copy(showLayersSheet = true) }
                ToolType.STICKERS -> _uiState.update { it.copy(showStickersDialog = true) }
                ToolType.SCIENCE -> _uiState.update { it.copy(showScienceDialog = true) }
                ToolType.CUSTOMIZE -> _uiState.update { it.copy(showCustomizeDialog = true) }
                ToolType.WEB_IMAGE -> _uiState.update { it.copy(showWebImageDialog = true) }
                ToolType.OCR -> _uiState.update { it.copy(showOcrDialog = true) }
                ToolType.SAVE -> saveCurrentBoard()
                ToolType.CLEAR -> _uiState.update { it.copy(showClearConfirmDialog = true) }
                ToolType.UNDO -> undo()
                ToolType.REDO -> redo()
                ToolType.RULER -> _uiState.update { it.copy(isRulerVisible = !it.isRulerVisible) }
                ToolType.HELP -> _uiState.update { it.copy(showHelpDialog = true) }
                else -> {}
            }
        }
    }

    fun dismissAllDialogs() {
        _uiState.update {
            it.copy(
                showPenDialog = false,
                showEraserDialog = false,
                showShapesDialog = false,
                showBoardDialog = false,
                showTextDialog = false,
                showFormulaDialog = false,
                showTableDialog = false,
                showLayersSheet = false,
                showStickersDialog = false,
                showScienceDialog = false,
                showLaserDialog = false,
                showCustomizeDialog = false,
                showClearConfirmDialog = false,
                showPagesDialog = false,
                showHelpDialog = false,
                showWebImageDialog = false,
                showOcrDialog = false,
                showElementEditDialog = false
            )
        }
    }

    fun setPenBrush(brush: BrushType) = _uiState.update { it.copy(brushType = brush) }
    fun setPenColor(color: Color) = _uiState.update { it.copy(penColor = color) }
    fun setPenSize(size: Float) = _uiState.update { it.copy(penSize = size) }
    fun setEraserSize(size: Float) = _uiState.update { it.copy(eraserSize = size) }
    fun setSelectedShape(shape: ShapeType) = _uiState.update { it.copy(selectedShape = shape) }
    fun setShapeFilled(filled: Boolean) = _uiState.update { it.copy(isShapeFilled = filled) }
    fun setBoardColor(color: Color) = _uiState.update { it.copy(boardColor = color) }
    fun setBoardTexture(texture: BoardTexture) = _uiState.update { it.copy(boardTexture = texture) }
    fun setLaserColor(color: Color) = _uiState.update { it.copy(laserColor = color) }
    fun setLaserSize(size: Float) = _uiState.update { it.copy(laserSize = size) }
    fun toggleToolbarCollapse() = _uiState.update { it.copy(isToolbarCollapsed = !it.isToolbarCollapsed) }

    // Page Management
    fun nextPage() {
        val curr = _uiState.value.currentPageIndex
        val max = _uiState.value.pages.size - 1
        if (curr < max) {
            _uiState.update { it.copy(currentPageIndex = curr + 1) }
        } else {
            // Add page
            addNewPage()
        }
    }

    fun previousPage() {
        val curr = _uiState.value.currentPageIndex
        if (curr > 0) {
            _uiState.update { it.copy(currentPageIndex = curr - 1) }
        }
    }

    fun addNewPage() {
        val newPage = BoardPageState(pageIndex = _uiState.value.pages.size)
        val updated = _uiState.value.pages + newPage
        _uiState.update { it.copy(pages = updated, currentPageIndex = updated.size - 1) }
    }

    fun selectPage(index: Int) {
        if (index in _uiState.value.pages.indices) {
            _uiState.update { it.copy(currentPageIndex = index) }
        }
    }

    fun deleteCurrentPage() {
        if (_uiState.value.pages.size <= 1) {
            clearCurrentPage()
            return
        }
        val curr = _uiState.value.currentPageIndex
        val updated = _uiState.value.pages.toMutableList()
        updated.removeAt(curr)
        val newIndex = (curr - 1).coerceAtLeast(0)
        _uiState.update { it.copy(pages = updated, currentPageIndex = newIndex) }
    }

    // Touch Event Handlers
    fun onDrawStart(point: PointF) {
        val state = _uiState.value
        when (state.activeTool) {
            ToolType.PEN, ToolType.SMART -> {
                currentDrawingPoints.clear()
                currentDrawingPoints.add(point)
            }
            ToolType.ERASER -> {
                eraseAt(point, state.eraserSize)
            }
            ToolType.SHAPES -> {
                currentShapeDraft.value = CanvasElement.Shape(
                    id = "shape_${System.currentTimeMillis()}",
                    layerId = state.activeLayerId,
                    shapeType = state.selectedShape,
                    startX = point.x,
                    startY = point.y,
                    endX = point.x,
                    endY = point.y,
                    colorArgb = state.penColor.toArgb(),
                    strokeWidth = state.penSize,
                    isFilled = state.isShapeFilled
                )
            }
            ToolType.LASER -> {
                addLaserPoint(point.x, point.y)
            }
            else -> {}
        }
    }

    fun onDrawMove(point: PointF) {
        val state = _uiState.value
        when (state.activeTool) {
            ToolType.PEN, ToolType.SMART -> {
                currentDrawingPoints.add(point)
            }
            ToolType.ERASER -> {
                eraseAt(point, state.eraserSize)
            }
            ToolType.SHAPES -> {
                currentShapeDraft.value?.let { draft ->
                    currentShapeDraft.value = draft.copy(endX = point.x, endY = point.y)
                }
            }
            ToolType.LASER -> {
                addLaserPoint(point.x, point.y)
            }
            else -> {}
        }
    }

    fun onDrawEnd() {
        val state = _uiState.value
        when (state.activeTool) {
            ToolType.PEN -> {
                if (currentDrawingPoints.size > 1) {
                    val stroke = CanvasElement.Stroke(
                        id = "stroke_${System.currentTimeMillis()}",
                        layerId = state.activeLayerId,
                        points = currentDrawingPoints.toList(),
                        colorArgb = state.penColor.toArgb(),
                        strokeWidth = state.penSize,
                        brushType = state.brushType
                    )
                    addElementToCurrentPage(stroke)
                }
                currentDrawingPoints.clear()
            }
            ToolType.SMART -> {
                if (currentDrawingPoints.size > 2) {
                    val pts = currentDrawingPoints.toList()
                    val layerId = state.activeLayerId
                    val colorArgb = state.penColor.toArgb()
                    val strokeWidth = state.penSize
                    val brushType = state.brushType

                    viewModelScope.launch {
                        var recognizedElement: CanvasElement? = null

                        // 1. Google ML Kit Digital Ink Recognition API
                        try {
                            val mlResult = com.example.ui.whiteboard.ml.MlKitManager.recognizeInk(pts)
                            if (mlResult != null) {
                                val candidate = mlResult.first
                                val shapeType = com.example.ui.whiteboard.ml.MlKitManager.mapCandidateToShapeType(candidate)
                                val minX = pts.minOf { it.x }
                                val maxX = pts.maxOf { it.x }
                                val minY = pts.minOf { it.y }
                                val maxY = pts.maxOf { it.y }
                                val w = maxX - minX
                                val h = maxY - minY

                                if (shapeType != null) {
                                    val sEndX = if (shapeType == ShapeType.SQUARE || shapeType == ShapeType.CIRCLE) minX + maxOf(w, h) else maxX
                                    val sEndY = if (shapeType == ShapeType.SQUARE || shapeType == ShapeType.CIRCLE) minY + maxOf(w, h) else maxY
                                    recognizedElement = CanvasElement.Shape(
                                        id = "smart_ml_${System.currentTimeMillis()}",
                                        layerId = layerId,
                                        shapeType = shapeType,
                                        startX = minX,
                                        startY = minY,
                                        endX = sEndX,
                                        endY = sEndY,
                                        colorArgb = colorArgb,
                                        strokeWidth = strokeWidth
                                    )
                                } else if (candidate.isNotBlank() && candidate.length <= 20) {
                                    val fontSize = maxOf(32f, minOf(96f, h * 0.9f))
                                    recognizedElement = CanvasElement.Text(
                                        id = "smart_txt_${System.currentTimeMillis()}",
                                        layerId = layerId,
                                        text = candidate,
                                        x = minX,
                                        y = maxY,
                                        colorArgb = colorArgb,
                                        fontSize = fontSize
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("WhiteboardViewModel", "ML Kit Ink recognition error", e)
                        }

                        // 2. Fallback to geometric shape & symbol recognizer
                        if (recognizedElement == null) {
                            recognizedElement = SmartShapeRecognizer.recognize(pts, layerId, colorArgb, strokeWidth)
                        }

                        // 3. Fallback to regular stroke
                        if (recognizedElement != null) {
                            addElementToCurrentPage(recognizedElement)
                        } else {
                            val stroke = CanvasElement.Stroke(
                                id = "stroke_${System.currentTimeMillis()}",
                                layerId = layerId,
                                points = pts,
                                colorArgb = colorArgb,
                                strokeWidth = strokeWidth,
                                brushType = brushType
                            )
                            addElementToCurrentPage(stroke)
                        }
                    }
                }
                currentDrawingPoints.clear()
            }
            ToolType.SHAPES -> {
                currentShapeDraft.value?.let { shape ->
                    addElementToCurrentPage(shape)
                }
                currentShapeDraft.value = null
            }
            else -> {}
        }
    }

    private fun addLaserPoint(x: Float, y: Float) {
        val lp = LaserPoint(x, y)
        _uiState.update { it.copy(laserPoints = it.laserPoints + lp) }
    }

    private fun eraseAt(point: PointF, radius: Float) {
        val state = _uiState.value
        val currentPage = state.pages[state.currentPageIndex]
        val remaining = currentPage.elements.filterNot { element ->
            isElementIntersecting(element, point, radius)
        }
        if (remaining.size != currentPage.elements.size) {
            pushUndoState()
            updateCurrentPageElements(remaining)
        }
    }

    private fun isElementIntersecting(element: CanvasElement, point: PointF, radius: Float): Boolean {
        val screenOffset = androidx.compose.ui.geometry.Offset(point.x, point.y)
        val local = ElementTransformer.mapToLocal(screenOffset, element)
        val effectiveScale = maxOf(0.1f, element.scale)
        val localRadius = radius / effectiveScale

        return when (element) {
            is CanvasElement.Stroke -> {
                val pts = element.points
                if (pts.isEmpty()) return false
                if (pts.size == 1) {
                    val p = pts[0]
                    kotlin.math.hypot(p.x - local.x, p.y - local.y) < localRadius + element.strokeWidth / 2f
                } else {
                    for (i in 0 until pts.size - 1) {
                        val p1 = pts[i]
                        val p2 = pts[i + 1]
                        val d = distToSegment(local.x, local.y, p1.x, p1.y, p2.x, p2.y)
                        if (d < localRadius + element.strokeWidth / 2f) {
                            return true
                        }
                    }
                    false
                }
            }
            else -> {
                val b = element.getBounds()
                local.x in (b.left - localRadius)..(b.right + localRadius) &&
                        local.y in (b.top - localRadius)..(b.bottom + localRadius)
            }
        }
    }

    private fun distToSegment(px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return kotlin.math.hypot(px - x1, py - y1)
        val t = maxOf(0f, minOf(1f, ((px - x1) * dx + (py - y1) * dy) / lenSq))
        val projX = x1 + t * dx
        val projY = y1 + t * dy
        return kotlin.math.hypot(px - projX, py - projY)
    }

    fun addElementToCurrentPage(element: CanvasElement) {
        pushUndoState()
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        val currentPage = updatedPages[currIdx]
        updatedPages[currIdx] = currentPage.copy(
            elements = currentPage.elements + element,
            redoStack = emptyList()
        )
        _uiState.update { it.copy(pages = updatedPages, selectedElementId = element.id) }
    }

    fun selectElement(elementId: String?) {
        _uiState.update { it.copy(selectedElementId = elementId) }
    }

    fun deleteSelectedElement() {
        val selectedId = _uiState.value.selectedElementId ?: return
        pushUndoState()
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        val page = updatedPages[currIdx]
        updatedPages[currIdx] = page.copy(
            elements = page.elements.filterNot { it.id == selectedId }
        )
        _uiState.update { it.copy(pages = updatedPages, selectedElementId = null) }
    }

    fun duplicateSelectedElement() {
        val selectedId = _uiState.value.selectedElementId ?: return
        val currentPage = _uiState.value.pages[_uiState.value.currentPageIndex]
        val element = currentPage.elements.find { it.id == selectedId } ?: return
        pushUndoState()
        val duplicated = element.duplicate("dup_${System.currentTimeMillis()}")
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        val page = updatedPages[currIdx]
        updatedPages[currIdx] = page.copy(
            elements = page.elements + duplicated
        )
        _uiState.update { it.copy(pages = updatedPages, selectedElementId = duplicated.id) }
    }

    fun resetSelectedElementRotation() {
        val selectedId = _uiState.value.selectedElementId ?: return
        pushUndoState()
        updateElement(selectedId) { it.withRotation(0f) }
    }

    fun bringSelectedToFront() {
        val selectedId = _uiState.value.selectedElementId ?: return
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        val page = updatedPages[currIdx]
        val el = page.elements.find { it.id == selectedId } ?: return
        val remaining = page.elements.filterNot { it.id == selectedId }
        updatedPages[currIdx] = page.copy(elements = remaining + el)
        _uiState.update { it.copy(pages = updatedPages) }
    }

    fun openEditDialogForSelected() {
        if (_uiState.value.selectedElementId != null) {
            _uiState.update { it.copy(showElementEditDialog = true) }
        }
    }

    fun updateSelectedElementColor(newColor: Color) {
        val selectedId = _uiState.value.selectedElementId ?: return
        pushUndoState()
        updateElement(selectedId) { element ->
            val colorArgb = newColor.toArgb()
            when (element) {
                is CanvasElement.Stroke -> element.copy(colorArgb = colorArgb)
                is CanvasElement.Shape -> element.copy(colorArgb = colorArgb)
                is CanvasElement.Text -> element.copy(colorArgb = colorArgb)
                is CanvasElement.Formula -> element.copy(colorArgb = colorArgb)
                is CanvasElement.Table -> element.copy(colorArgb = colorArgb)
                else -> element
            }
        }
    }

    fun toggleSelectedShapeFill() {
        val selectedId = _uiState.value.selectedElementId ?: return
        pushUndoState()
        updateElement(selectedId) { element ->
            if (element is CanvasElement.Shape) {
                element.copy(isFilled = !element.isFilled)
            } else element
        }
    }

    fun updateElement(elementId: String, transform: (CanvasElement) -> CanvasElement) {
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        val page = updatedPages[currIdx]
        val updatedElements = page.elements.map {
            if (it.id == elementId) transform(it) else it
        }
        updatedPages[currIdx] = page.copy(elements = updatedElements)
        _uiState.update { it.copy(pages = updatedPages) }
    }

    private fun updateCurrentPageElements(elements: List<CanvasElement>) {
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = updatedPages[currIdx].copy(elements = elements)
        _uiState.update { it.copy(pages = updatedPages) }
    }

    private fun getViewportCenterOffset(): Offset {
        val state = _uiState.value
        val z = if (state.zoomScale != 0f) state.zoomScale else 1f
        val cx = (-state.panOffset.x + 200f) / z
        val cy = (-state.panOffset.y + 350f) / z
        return Offset(cx, cy)
    }

    // Add Text Element
    fun addTextElement(text: String, color: Color, size: Float) {
        val center = getViewportCenterOffset()
        val element = CanvasElement.Text(
            id = "text_${System.currentTimeMillis()}",
            layerId = _uiState.value.activeLayerId,
            text = text,
            x = center.x - 100f,
            y = center.y,
            colorArgb = color.toArgb(),
            fontSize = size
        )
        addElementToCurrentPage(element)
    }

    // Add Math Formula Element
    fun addFormulaElement(formula: String, color: Color, size: Float) {
        val center = getViewportCenterOffset()
        val element = CanvasElement.Formula(
            id = "formula_${System.currentTimeMillis()}",
            layerId = _uiState.value.activeLayerId,
            formula = formula,
            x = center.x - 100f,
            y = center.y,
            colorArgb = color.toArgb(),
            fontSize = size
        )
        addElementToCurrentPage(element)
    }

    // Add Table Element
    fun addTableElement(rows: Int, cols: Int) {
        val center = getViewportCenterOffset()
        val w = cols * 100f
        val h = rows * 50f
        val element = CanvasElement.Table(
            id = "table_${System.currentTimeMillis()}",
            layerId = _uiState.value.activeLayerId,
            rows = rows,
            cols = cols,
            x = center.x - w / 2f,
            y = center.y - h / 2f,
            colorArgb = _uiState.value.penColor.toArgb()
        )
        addElementToCurrentPage(element)
    }

    // Add Sticker Element
    fun addStickerElement(stickerId: String, title: String) {
        val center = getViewportCenterOffset()
        val element = CanvasElement.Sticker(
            id = "sticker_${System.currentTimeMillis()}",
            layerId = _uiState.value.activeLayerId,
            stickerId = stickerId,
            title = title,
            x = center.x - 60f,
            y = center.y - 60f
        )
        addElementToCurrentPage(element)
    }

    // Add Science Diagram Element
    fun addScienceElement(category: String, symbolKey: String, label: String, atomicNum: Int) {
        val center = getViewportCenterOffset()
        val element = CanvasElement.ScienceDiagram(
            id = "sci_${System.currentTimeMillis()}",
            layerId = _uiState.value.activeLayerId,
            category = category,
            symbolKey = symbolKey,
            label = label,
            atomicNumber = atomicNum,
            x = center.x - 65f,
            y = center.y - 65f
        )
        addElementToCurrentPage(element)
    }

    // Add Image Element
    fun addImageElement(uriString: String) {
        val state = _uiState.value
        val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 100f
        val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 220f
        val element = CanvasElement.Image(
            id = "img_${System.currentTimeMillis()}",
            layerId = state.activeLayerId,
            uriString = uriString,
            x = x,
            y = y
        )
        addElementToCurrentPage(element)
    }

    fun importUriImage(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "gallery_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                val imgW = if (boundsOptions.outWidth > 0) boundsOptions.outWidth.toFloat() else 300f
                val imgH = if (boundsOptions.outHeight > 0) boundsOptions.outHeight.toFloat() else 300f
                val aspect = imgH / imgW.coerceAtLeast(1f)

                withContext(Dispatchers.Main) {
                    val state = _uiState.value
                    val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 100f
                    val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 200f
                    val element = CanvasElement.Image(
                        id = "img_${System.currentTimeMillis()}",
                        layerId = state.activeLayerId,
                        uriString = Uri.fromFile(file).toString(),
                        x = x,
                        y = y,
                        width = 320f,
                        height = (320f * aspect).coerceIn(100f, 600f)
                    )
                    addElementToCurrentPage(element)
                }
            } catch (e: Exception) {
                Log.e("WhiteboardViewModel", "Failed to import gallery image", e)
                withContext(Dispatchers.Main) {
                    addImageElement(uri.toString())
                }
            }
        }
    }

    // Pan & Zoom Controls
    fun panCanvas(delta: Offset) {
        _uiState.update { it.copy(panOffset = Offset(it.panOffset.x + delta.x, it.panOffset.y + delta.y)) }
    }

    fun zoomCanvas(scaleMultiplier: Float) {
        _uiState.update {
            val newScale = (it.zoomScale * scaleMultiplier).coerceIn(0.2f, 5.0f)
            it.copy(zoomScale = newScale)
        }
    }

    fun resetPanZoom() {
        _uiState.update { it.copy(panOffset = Offset.Zero, zoomScale = 1f) }
    }

    fun addWebImage(url: String) {
        val state = _uiState.value
        val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 120f
        val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 200f
        val element = CanvasElement.Image(
            id = "web_img_${System.currentTimeMillis()}",
            layerId = state.activeLayerId,
            uriString = url,
            x = x,
            y = y,
            width = 320f,
            height = 320f
        )
        addElementToCurrentPage(element)
    }

    fun addImageBitmap(context: Context, bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(context.filesDir, "cam_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                withContext(Dispatchers.Main) {
                    val state = _uiState.value
                    val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 120f
                    val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 200f
                    val aspect = bitmap.height.toFloat() / bitmap.width.toFloat().coerceAtLeast(1f)
                    val element = CanvasElement.Image(
                        id = "cam_img_${System.currentTimeMillis()}",
                        layerId = state.activeLayerId,
                        uriString = Uri.fromFile(file).toString(),
                        x = x,
                        y = y,
                        width = 320f,
                        height = 320f * aspect
                    )
                    addElementToCurrentPage(element)
                }
            } catch (e: Exception) {
                Log.e("WhiteboardViewModel", "Error saving camera bitmap", e)
            }
        }
    }

    fun importPdf(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                if (pfd != null) {
                    val renderer = android.graphics.pdf.PdfRenderer(pfd)
                    val pageCount = renderer.pageCount
                    val page = renderer.openPage(0)
                    val w = page.width * 2
                    val h = page.height * 2
                    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    renderer.close()
                    pfd.close()

                    val file = File(context.filesDir, "pdf_page_${System.currentTimeMillis()}.png")
                    FileOutputStream(file).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                    withContext(Dispatchers.Main) {
                        val state = _uiState.value
                        val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 80f
                        val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 140f
                        val el = CanvasElement.Image(
                            id = "pdf_${System.currentTimeMillis()}",
                            layerId = state.activeLayerId,
                            uriString = Uri.fromFile(file).toString(),
                            x = x,
                            y = y,
                            width = 400f,
                            height = 400f * (h.toFloat() / w.toFloat().coerceAtLeast(1f))
                        )
                        addElementToCurrentPage(el)
                        Toast.makeText(context, "Imported PDF Page 1 ($pageCount pages total)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("WhiteboardViewModel", "Failed to import PDF", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Could not import PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun runOcrOnBitmap(bitmap: Bitmap) {
        _uiState.update { it.copy(isOcrProcessing = true, showOcrDialog = true) }
        viewModelScope.launch {
            val text = com.example.ui.whiteboard.ml.MlKitManager.recognizeTextFromBitmap(bitmap)
            _uiState.update { it.copy(isOcrProcessing = false, ocrExtractedText = text) }
        }
    }

    fun addOcrTextToCanvas(text: String) {
        val state = _uiState.value
        val x = -state.panOffset.x / maxOf(0.1f, state.zoomScale) + 120f
        val y = -state.panOffset.y / maxOf(0.1f, state.zoomScale) + 240f
        val el = CanvasElement.Text(
            id = "ocr_txt_${System.currentTimeMillis()}",
            layerId = state.activeLayerId,
            text = text,
            x = x,
            y = y,
            colorArgb = state.penColor.toArgb(),
            fontSize = 36f
        )
        addElementToCurrentPage(el)
        _uiState.update { it.copy(showOcrDialog = false, ocrExtractedText = "") }
    }

    // Layers Management
    fun selectLayer(id: String) = _uiState.update { it.copy(activeLayerId = id) }

    fun addLayer() {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        val newId = "layer_${page.layers.size + 1}"
        val newLayer = LayerInfo(id = newId, name = "Layer ${page.layers.size + 1}")
        val updatedLayers = page.layers + newLayer
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = page.copy(layers = updatedLayers)
        _uiState.update { it.copy(pages = updatedPages, activeLayerId = newId) }
    }

    fun toggleLayerVisibility(layerId: String) {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        val updatedLayers = page.layers.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = page.copy(layers = updatedLayers)
        _uiState.update { it.copy(pages = updatedPages) }
    }

    fun deleteLayer(layerId: String) {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        if (page.layers.size <= 1) return
        val updatedLayers = page.layers.filterNot { it.id == layerId }
        val updatedElements = page.elements.filterNot { it.layerId == layerId }
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = page.copy(layers = updatedLayers, elements = updatedElements)
        val nextActive = updatedLayers.first().id
        _uiState.update { it.copy(pages = updatedPages, activeLayerId = nextActive) }
    }

    // Undo / Redo
    private fun pushUndoState() {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        val updatedUndo = (page.undoStack + listOf(page.elements)).takeLast(25)
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = page.copy(undoStack = updatedUndo)
        _uiState.update { it.copy(pages = updatedPages) }
    }

    fun undo() {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        if (page.undoStack.isNotEmpty()) {
            val previousElements = page.undoStack.last()
            val newUndo = page.undoStack.dropLast(1)
            val newRedo = page.redoStack + listOf(page.elements)
            val updatedPages = _uiState.value.pages.toMutableList()
            updatedPages[currIdx] = page.copy(
                elements = previousElements,
                undoStack = newUndo,
                redoStack = newRedo
            )
            _uiState.update { it.copy(pages = updatedPages) }
        }
    }

    fun redo() {
        val currIdx = _uiState.value.currentPageIndex
        val page = _uiState.value.pages[currIdx]
        if (page.redoStack.isNotEmpty()) {
            val nextElements = page.redoStack.last()
            val newRedo = page.redoStack.dropLast(1)
            val newUndo = page.undoStack + listOf(page.elements)
            val updatedPages = _uiState.value.pages.toMutableList()
            updatedPages[currIdx] = page.copy(
                elements = nextElements,
                undoStack = newUndo,
                redoStack = newRedo
            )
            _uiState.update { it.copy(pages = updatedPages) }
        }
    }

    fun clearCurrentPage() {
        pushUndoState()
        val currIdx = _uiState.value.currentPageIndex
        val updatedPages = _uiState.value.pages.toMutableList()
        updatedPages[currIdx] = updatedPages[currIdx].copy(elements = emptyList(), redoStack = emptyList())
        _uiState.update { it.copy(pages = updatedPages, showClearConfirmDialog = false) }
    }

    // Toolbar customization
    fun toggleToolEnabled(tool: ToolType) {
        val current = _uiState.value.enabledTools.toMutableSet()
        if (current.contains(tool)) current.remove(tool) else current.add(tool)
        _uiState.update { it.copy(enabledTools = current) }
    }

    fun resetToolbar() {
        _uiState.update {
            it.copy(
                enabledTools = ToolType.values().toSet(),
                toolOrder = ToolType.values().toList()
            )
        }
    }

    // Save & Export
    fun saveCurrentBoard(onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            val state = _uiState.value
            val json = CanvasSerializer.serializePages(state.pages)
            val entity = BoardEntity(
                id = state.boardId,
                title = state.boardTitle.ifBlank { "Board ${System.currentTimeMillis() % 10000}" },
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                pageCount = state.pages.size,
                boardColorArgb = state.boardColor.toArgb(),
                boardTexture = state.boardTexture.name,
                dataJson = json
            )
            val newId = repository.saveBoard(entity)
            _uiState.update { it.copy(boardId = newId, showSavedSuccess = true) }
            Toast.makeText(getApplication(), "Board saved successfully!", Toast.LENGTH_SHORT).show()
            onSaved?.invoke()
            delay(1500)
            _uiState.update { it.copy(showSavedSuccess = false) }
        }
    }

    fun exportAndShareImage(context: Context) {
        viewModelScope.launch {
            try {
                val bitmap = renderCurrentPageToBitmap(1080, 1920)
                val cacheDir = File(context.cacheDir, "shared")
                cacheDir.mkdirs()
                val imageFile = File(cacheDir, "whiteboard_${System.currentTimeMillis()}.png")
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Whiteboard"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportAndSharePdf(context: Context) {
        viewModelScope.launch {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 1080
                val pageHeight = 1920

                _uiState.value.pages.forEachIndexed { idx, pageState ->
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                    val pdfPage = pdfDocument.startPage(pageInfo)
                    val canvas = pdfPage.canvas

                    // Draw background
                    val bgPaint = Paint().apply { color = _uiState.value.boardColor.toArgb() }
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

                    // Draw elements of visible layers
                    val visibleLayerIds = pageState.layers.filter { it.isVisible }.map { it.id }.toSet()
                    val visibleElements = pageState.elements.filter { visibleLayerIds.contains(it.layerId) }

                    // Render elements using Android Canvas
                    for (el in visibleElements) {
                        when (el) {
                            is CanvasElement.Stroke -> {
                                val paint = Paint().apply {
                                    color = el.colorArgb
                                    strokeWidth = el.strokeWidth
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    isAntiAlias = true
                                }
                                for (i in 0 until el.points.size - 1) {
                                    val p1 = el.points[i]
                                    val p2 = el.points[i + 1]
                                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                                }
                            }
                            is CanvasElement.Shape -> {
                                val paint = Paint().apply {
                                    color = el.colorArgb
                                    strokeWidth = el.strokeWidth
                                    style = if (el.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                                    isAntiAlias = true
                                }
                                val minX = minOf(el.startX, el.endX)
                                val maxX = maxOf(el.startX, el.endX)
                                val minY = minOf(el.startY, el.endY)
                                val maxY = maxOf(el.startY, el.endY)
                                when (el.shapeType) {
                                    ShapeType.RECT, ShapeType.SQUARE -> canvas.drawRect(minX, minY, maxX, maxY, paint)
                                    ShapeType.CIRCLE, ShapeType.OVAL -> canvas.drawOval(minX, minY, maxX, maxY, paint)
                                    ShapeType.LINE, ShapeType.ARROW -> canvas.drawLine(el.startX, el.startY, el.endX, el.endY, paint)
                                    else -> canvas.drawRect(minX, minY, maxX, maxY, paint)
                                }
                            }
                            is CanvasElement.Text -> {
                                val paint = Paint().apply {
                                    color = el.colorArgb
                                    textSize = el.fontSize
                                    isAntiAlias = true
                                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                                }
                                canvas.drawText(el.text, el.x, el.y, paint)
                            }
                            is CanvasElement.Formula -> {
                                val paint = Paint().apply {
                                    color = el.colorArgb
                                    textSize = el.fontSize
                                    isAntiAlias = true
                                }
                                canvas.drawText(el.formula, el.x, el.y, paint)
                            }
                            else -> {}
                        }
                    }

                    pdfDocument.finishPage(pdfPage)
                }

                val cacheDir = File(context.cacheDir, "exports")
                cacheDir.mkdirs()
                val pdfFile = File(cacheDir, "whiteboard_${System.currentTimeMillis()}.pdf")
                FileOutputStream(pdfFile).use { out ->
                    pdfDocument.writeTo(out)
                }
                pdfDocument.close()

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Whiteboard PDF"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "PDF export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun renderCurrentPageToBitmap(width: Int, height: Int): Bitmap = withContext(Dispatchers.Default) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background
        val bgPaint = Paint().apply { color = _uiState.value.boardColor.toArgb() }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val page = _uiState.value.pages[_uiState.value.currentPageIndex]
        val visibleLayerIds = page.layers.filter { it.isVisible }.map { it.id }.toSet()
        val visibleElements = page.elements.filter { visibleLayerIds.contains(it.layerId) }

        for (el in visibleElements) {
            when (el) {
                is CanvasElement.Stroke -> {
                    val paint = Paint().apply {
                        color = el.colorArgb
                        strokeWidth = el.strokeWidth
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        isAntiAlias = true
                    }
                    for (i in 0 until el.points.size - 1) {
                        val p1 = el.points[i]
                        val p2 = el.points[i + 1]
                        canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)
                    }
                }
                is CanvasElement.Shape -> {
                    val paint = Paint().apply {
                        color = el.colorArgb
                        strokeWidth = el.strokeWidth
                        style = if (el.isFilled) Paint.Style.FILL else Paint.Style.STROKE
                        isAntiAlias = true
                    }
                    val minX = minOf(el.startX, el.endX)
                    val maxX = maxOf(el.startX, el.endX)
                    val minY = minOf(el.startY, el.endY)
                    val maxY = maxOf(el.startY, el.endY)
                    when (el.shapeType) {
                        ShapeType.RECT, ShapeType.SQUARE -> canvas.drawRect(minX, minY, maxX, maxY, paint)
                        ShapeType.CIRCLE, ShapeType.OVAL -> canvas.drawOval(minX, minY, maxX, maxY, paint)
                        ShapeType.LINE, ShapeType.ARROW -> canvas.drawLine(el.startX, el.startY, el.endX, el.endY, paint)
                        else -> canvas.drawRect(minX, minY, maxX, maxY, paint)
                    }
                }
                is CanvasElement.Text -> {
                    val paint = Paint().apply {
                        color = el.colorArgb
                        textSize = el.fontSize
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    canvas.drawText(el.text, el.x, el.y, paint)
                }
                is CanvasElement.Formula -> {
                    val paint = Paint().apply {
                        color = el.colorArgb
                        textSize = el.fontSize
                        isAntiAlias = true
                    }
                    canvas.drawText(el.formula, el.x, el.y, paint)
                }
                else -> {}
            }
        }
        bitmap
    }
}
