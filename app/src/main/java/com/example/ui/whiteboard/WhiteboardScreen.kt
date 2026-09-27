package com.example.ui.whiteboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.ads.RewardedAdManager
import com.example.ui.whiteboard.dialogs.*

@Composable
fun WhiteboardScreen(
    viewModel: WhiteboardViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentShapeDraft by viewModel.currentShapeDraft.collectAsState()
    val context = LocalContext.current

    val isAdLoaded by RewardedAdManager.isAdLoaded.collectAsState()
    val isAdLoading by RewardedAdManager.isLoading.collectAsState()
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var rewardEarnedMessage by remember { mutableStateOf<String?>(null) }

    // Handle back button
    BackHandler {
        viewModel.saveCurrentBoard {
            onNavigateBack()
        }
    }

    // Photo picker for images
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImageElement(it.toString()) }
    }

    // Ruler drag state
    var rulerPosition by remember { mutableStateOf(Offset(100f, 400f)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiState.boardColor)
    ) {
        val currentPage = uiState.pages.getOrElse(uiState.currentPageIndex) { BoardPageState() }
        val visibleLayers = remember(currentPage.layers) {
            currentPage.layers.filter { it.isVisible }.map { it.id }.toSet()
        }
        val isDarkBg = remember(uiState.boardColor) {
            val c = uiState.boardColor
            (c.red * 0.299f + c.green * 0.587f + c.blue * 0.114f) < 0.5f
        }

        val currentElementsState = rememberUpdatedState(currentPage.elements)
        val currentSelectedIdState = rememberUpdatedState(uiState.selectedElementId)
        val currentVisibleLayersState = rememberUpdatedState(visibleLayers)
        val currentToolState = rememberUpdatedState(uiState.activeTool)

        var activeHandle by remember { mutableStateOf(TransformHandle.NONE) }
        var initialTouchPoint by remember { mutableStateOf(Offset.Zero) }
        var initialElement by remember { mutableStateOf<CanvasElement?>(null) }

        val selectedElement = remember(uiState.selectedElementId, currentPage.elements) {
            currentPage.elements.find { it.id == uiState.selectedElementId }
        }

        // 1. MAIN DRAWING CANVAS
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val elements = currentElementsState.value
                            val selectedId = currentSelectedIdState.value
                            val selected = elements.find { it.id == selectedId }
                            var handle = TransformHandle.NONE
                            if (selected != null) {
                                handle = ElementTransformer.hitTestHandle(selected, offset)
                            }

                            if (handle != TransformHandle.NONE && selected != null) {
                                activeHandle = handle
                                initialTouchPoint = offset
                                initialElement = selected
                            } else {
                                // Check if touching any other visible element (top-most first)
                                val touchedElement = elements.asReversed().find { el ->
                                    currentVisibleLayersState.value.contains(el.layerId) && ElementTransformer.hitTestElement(el, offset)
                                }

                                if (touchedElement != null && (currentToolState.value == ToolType.SELECT || currentToolState.value == ToolType.LASSO || selectedId != null)) {
                                    viewModel.selectElement(touchedElement.id)
                                    activeHandle = TransformHandle.BODY
                                    initialTouchPoint = offset
                                    initialElement = touchedElement
                                } else {
                                    if (currentToolState.value == ToolType.SELECT || currentToolState.value == ToolType.LASSO) {
                                        viewModel.selectElement(null)
                                        activeHandle = TransformHandle.NONE
                                        initialElement = null
                                    } else {
                                        activeHandle = TransformHandle.NONE
                                        initialElement = null
                                        viewModel.onDrawStart(PointF(offset.x, offset.y))
                                    }
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (activeHandle != TransformHandle.NONE && initialElement != null) {
                                val currentPt = change.position
                                val dx = currentPt.x - initialTouchPoint.x
                                val dy = currentPt.y - initialTouchPoint.y
                                val currentInitial = initialElement!!

                                when (activeHandle) {
                                    TransformHandle.BODY -> {
                                        viewModel.updateElement(currentInitial.id) {
                                            currentInitial.withTranslation(dx, dy)
                                        }
                                    }
                                    TransformHandle.ROTATE -> {
                                        val center = currentInitial.getCenter()
                                        val initialAngle = kotlin.math.atan2((initialTouchPoint.y - center.y).toDouble(), (initialTouchPoint.x - center.x).toDouble())
                                        val currentAngle = kotlin.math.atan2((currentPt.y - center.y).toDouble(), (currentPt.x - center.x).toDouble())
                                        val deltaDeg = Math.toDegrees(currentAngle - initialAngle).toFloat()
                                        val newRot = (currentInitial.rotation + deltaDeg) % 360f
                                        viewModel.updateElement(currentInitial.id) {
                                            currentInitial.withRotation(newRot)
                                        }
                                    }
                                    TransformHandle.TOP_LEFT, TransformHandle.TOP_RIGHT, TransformHandle.BOTTOM_LEFT, TransformHandle.BOTTOM_RIGHT -> {
                                        val center = currentInitial.getCenter()
                                        val initialDist = kotlin.math.hypot(initialTouchPoint.x - center.x, initialTouchPoint.y - center.y)
                                        val currentDist = kotlin.math.hypot(currentPt.x - center.x, currentPt.y - center.y)
                                        if (initialDist > 8f) {
                                            val factor = currentDist / initialDist
                                            val newScale = (currentInitial.scale * factor).coerceIn(0.15f, 10f)
                                            viewModel.updateElement(currentInitial.id) {
                                                currentInitial.withScale(newScale)
                                            }
                                        }
                                    }
                                    else -> {}
                                }
                            } else {
                                viewModel.onDrawMove(PointF(change.position.x, change.position.y))
                            }
                        },
                        onDragEnd = {
                            if (activeHandle != TransformHandle.NONE) {
                                activeHandle = TransformHandle.NONE
                                initialElement = null
                            } else {
                                viewModel.onDrawEnd()
                            }
                        },
                        onDragCancel = {
                            if (activeHandle != TransformHandle.NONE) {
                                activeHandle = TransformHandle.NONE
                                initialElement = null
                            } else {
                                viewModel.onDrawEnd()
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val elements = currentElementsState.value
                        val touched = elements.asReversed().find { el ->
                            currentVisibleLayersState.value.contains(el.layerId) && ElementTransformer.hitTestElement(el, offset)
                        }
                        if (touched != null) {
                            viewModel.selectElement(touched.id)
                        } else {
                            if (currentToolState.value == ToolType.SELECT || currentToolState.value == ToolType.LASSO) {
                                viewModel.selectElement(null)
                            } else if (currentToolState.value == ToolType.PEN || currentToolState.value == ToolType.SMART) {
                                viewModel.onDrawStart(PointF(offset.x, offset.y))
                                viewModel.onDrawEnd()
                            } else if (currentToolState.value == ToolType.LASER) {
                                viewModel.onDrawStart(PointF(offset.x, offset.y))
                            }
                        }
                    }
                }
        ) {
            // Draw board texture
            CanvasRenderer.drawBoardTexture(this, uiState.boardTexture, isDarkBg)

            // Draw elements on visible layers
            currentPage.elements.forEach { element ->
                if (visibleLayers.contains(element.layerId)) {
                    CanvasRenderer.drawElement(this, element)
                }
            }

            // Draw selection box and transformation handles if an element is selected
            selectedElement?.let { sel ->
                if (visibleLayers.contains(sel.layerId)) {
                    CanvasRenderer.drawSelectionBox(this, sel)
                }
            }

            // Draw in-progress stroke
            if (viewModel.currentDrawingPoints.size > 1) {
                val tempStroke = CanvasElement.Stroke(
                    id = "temp",
                    layerId = uiState.activeLayerId,
                    points = viewModel.currentDrawingPoints.toList(),
                    colorArgb = uiState.penColor.toArgb(),
                    strokeWidth = uiState.penSize,
                    brushType = uiState.brushType
                )
                CanvasRenderer.drawElement(this, tempStroke)
            }

            // Draw in-progress shape
            currentShapeDraft?.let { shapeDraft ->
                CanvasRenderer.drawElement(this, shapeDraft)
            }

            // Draw Laser Pointer Trail
            if (uiState.laserPoints.isNotEmpty()) {
                CanvasRenderer.drawLaserTrail(this, uiState.laserPoints, uiState.laserColor, uiState.laserSize)
            }
        }

        // Floating Action Bar for Selected Element
        selectedElement?.let { sel ->
            val center = sel.getCenter()
            val bounds = sel.getBounds()
            val pillX = (center.x - 110f).coerceIn(20f, 650f)
            val pillY = (bounds.top - 64f).coerceIn(70f, 1600f)

            Surface(
                modifier = Modifier
                    .offset(pillX.dp, pillY.dp)
                    .shadow(10.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF222222)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.deleteSelectedElement() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.duplicateSelectedElement() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.resetSelectedElementRotation() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset Angle", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.bringSelectedToFront() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FlipToFront, contentDescription = "To Front", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.selectElement(null) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Deselect", tint = Color(0xFF2196F3), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // 2. RULER OVERLAY (when enabled)
        if (uiState.isRulerVisible) {
            Box(
                modifier = Modifier
                    .offset(rulerPosition.x.dp, rulerPosition.y.dp)
                    .width(320.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xD9FFFFFF))
                    .border(1.5.dp, Color(0xFF64B5F6), RoundedCornerShape(8.dp))
                    .shadow(8.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            rulerPosition = Offset(
                                rulerPosition.x + dragAmount.x / density,
                                rulerPosition.y + dragAmount.y / density
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = size.width / 16f
                    for (i in 0..16) {
                        val x = i * step
                        val isMajor = i % 2 == 0
                        val tickH = if (isMajor) 22f else 12f
                        drawLine(
                            color = Color(0xFF1E88E5),
                            start = Offset(x, 0f),
                            end = Offset(x, tickH),
                            strokeWidth = if (isMajor) 2f else 1.2f
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ruler (cm)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
                    IconButton(
                        onClick = { viewModel.selectTool(ToolType.RULER) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Ruler", tint = Color.Gray)
                    }
                }
            }
        }

        // 3. TOP ACTION BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Group: Back Button + Page Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Circular Back Button (matches screenshot 2)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE7F6))
                        .clickable {
                            viewModel.saveCurrentBoard {
                                onNavigateBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF4A148C),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Page Indicator Badge: e.g. "1/1" (matches screenshot 2)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFB0BEC5))
                        .clickable { viewModel.selectPage(uiState.currentPageIndex) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${uiState.currentPageIndex + 1}/${uiState.pages.size}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // Quick Next/Prev Page mini buttons
                IconButton(
                    onClick = { viewModel.previousPage() },
                    enabled = uiState.currentPageIndex > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous Page",
                        tint = if (uiState.currentPageIndex > 0) Color.DarkGray else Color.LightGray
                    )
                }
                IconButton(
                    onClick = { viewModel.nextPage() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (uiState.currentPageIndex == uiState.pages.size - 1) Icons.Default.Add else Icons.Default.ChevronRight,
                        contentDescription = "Next Page",
                        tint = Color.DarkGray
                    )
                }
            }

            // Right Group: Rewarded Ad + Pen shortcut + 9-dot Grid/Board View
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 🎁 Rewarded Ad / VIP Perks Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF3E0))
                        .clickable { showRewardedAdDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CardGiftcard,
                        contentDescription = "Watch Rewarded Ad",
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Pen / Mode shortcut (matches screenshot 2)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE7F6))
                        .clickable { viewModel.selectTool(ToolType.PEN) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Pen",
                        tint = Color(0xFF4A148C),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // 9-dot Grid icon for pages/overview (matches screenshot 2)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDE7F6))
                        .clickable {
                            // Show pages dialog
                            viewModel.selectTool(ToolType.CUSTOMIZE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Board Overview",
                        tint = Color(0xFF4A148C),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 4. BOTTOM TOOLBAR
        AnimatedVisibility(
            visible = !uiState.isToolbarCollapsed,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = Color(0xFFF9F9F9),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scrollable list of active tools
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        uiState.toolOrder.forEach { tool ->
                            if (uiState.enabledTools.contains(tool)) {
                                ToolbarItem(
                                    tool = tool,
                                    isSelected = uiState.activeTool == tool,
                                    activeColor = uiState.penColor,
                                    onClick = {
                                        when (tool) {
                                            ToolType.IMAGE -> {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolType.SHARE -> {
                                                viewModel.exportAndShareImage(context)
                                            }
                                            ToolType.PDF -> {
                                                viewModel.exportAndSharePdf(context)
                                            }
                                            ToolType.SAVE -> {
                                                viewModel.saveCurrentBoard()
                                            }
                                            ToolType.WEB_IMAGE -> {
                                                Toast.makeText(context, "Enter image URL or select from gallery", Toast.LENGTH_SHORT).show()
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolType.CAMERA -> {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            }
                                            ToolType.OCR -> {
                                                Toast.makeText(context, "OCR: Recognizing handwriting...", Toast.LENGTH_SHORT).show()
                                            }
                                            ToolType.IMPORT_PDF -> {
                                                Toast.makeText(context, "Select PDF document to import", Toast.LENGTH_SHORT).show()
                                            }
                                            else -> {
                                                viewModel.selectTool(tool)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Sticky Collapse Button on right (matches screenshot 2, 3, 4, 5, 7)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                            .background(Color(0xFFEEEEEE))
                            .clickable { viewModel.toggleToolbarCollapse() }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Collapse",
                                tint = Color.DarkGray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Collapse",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }
        }

        // Floating Expand button when toolbar is collapsed
        if (uiState.isToolbarCollapsed) {
            FloatingActionButton(
                onClick = { viewModel.toggleToolbarCollapse() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(16.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF2196F3)
            ) {
                Icon(Icons.Default.ExpandLess, contentDescription = "Expand Toolbar")
            }
        }
    }

    // DIALOG HOSTS
    if (uiState.showPenDialog) {
        PenDialog(
            currentBrush = uiState.brushType,
            currentColor = uiState.penColor,
            currentSize = uiState.penSize,
            onBrushChange = { viewModel.setPenBrush(it) },
            onColorChange = { viewModel.setPenColor(it) },
            onSizeChange = { viewModel.setPenSize(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showEraserDialog) {
        EraserDialog(
            currentSize = uiState.eraserSize,
            onSizeChange = { viewModel.setEraserSize(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showShapesDialog) {
        ShapesDialog(
            selectedShape = uiState.selectedShape,
            isFilled = uiState.isShapeFilled,
            onShapeSelect = { viewModel.setSelectedShape(it) },
            onFillToggle = { viewModel.setShapeFilled(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showBoardDialog) {
        BoardColorDialog(
            currentColor = uiState.boardColor,
            currentTexture = uiState.boardTexture,
            onColorChange = { viewModel.setBoardColor(it) },
            onTextureChange = { viewModel.setBoardTexture(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showTextDialog) {
        TextDialog(
            onAddText = { text, color, size ->
                viewModel.addTextElement(text, color, size)
            },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showFormulaDialog) {
        FormulaDialog(
            onAddFormula = { formula, color, size ->
                viewModel.addFormulaElement(formula, color, size)
            },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showTableDialog) {
        TableDialog(
            onTableSelected = { rows, cols ->
                viewModel.addTableElement(rows, cols)
            },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showLayersSheet) {
        val currentPage = uiState.pages[uiState.currentPageIndex]
        LayersSheet(
            layers = currentPage.layers,
            activeLayerId = uiState.activeLayerId,
            onSelectLayer = { viewModel.selectLayer(it) },
            onAddLayer = { viewModel.addLayer() },
            onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
            onDeleteLayer = { viewModel.deleteLayer(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showStickersDialog) {
        StickersDialog(
            onStickerSelected = { id, title ->
                viewModel.addStickerElement(id, title)
            },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showScienceDialog) {
        ScienceDialog(
            onScienceSelected = { cat, sym, label, num ->
                viewModel.addScienceElement(cat, sym, label, num)
            },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showLaserDialog) {
        LaserDialog(
            laserColor = uiState.laserColor,
            laserSize = uiState.laserSize,
            onColorChange = { viewModel.setLaserColor(it) },
            onSizeChange = { viewModel.setLaserSize(it) },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showCustomizeDialog) {
        CustomizeToolbarDialog(
            toolOrder = uiState.toolOrder,
            enabledTools = uiState.enabledTools,
            onToggleTool = { viewModel.toggleToolEnabled(it) },
            onResetToolbar = { viewModel.resetToolbar() },
            onDismiss = { viewModel.dismissAllDialogs() }
        )
    }

    if (uiState.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAllDialogs() },
            title = { Text("Clear Canvas") },
            text = { Text("Are you sure you want to clear the entire page? This can be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearCurrentPage() }) {
                    Text("Clear", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAllDialogs() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showHelpDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissAllDialogs() },
            title = { Text("WhiteBoard Tools Guide") },
            text = {
                Column {
                    Text("• Pen: Freehand drawing with Pen, Brush, Marker, Highlighter, and Calligraphy.", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Smart: Shape recognition snaps your sketches into neat geometric shapes!", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Board: Switch between Plain, Grid, Lines, Dots, Chalk, Graph, and Notebook textures.", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Formula & Science: Insert math formulas and Bohr atomic structure diagrams.", fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("• Laser: Real-time fading presentation laser pointer.", fontSize = 13.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissAllDialogs() }) {
                    Text("Got it")
                }
            }
        )
    }

    // Rewarded Ad Dialog (ca-app-pub-3940256099942544/5224354917)
    if (showRewardedAdDialog) {
        Dialog(onDismissRequest = { showRewardedAdDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF262626)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3E2723)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "VIP WhiteBoard Rewards",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Watch a video ad to earn rewards and unlock:\n\n• Golden & Neon Stylus\n• Blueprint & Isometric Textures\n• VIP Science Diagrams & Stickers\n• High-Definition PDF Export",
                        color = Color(0xFFCCCCCC),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Test Ad Unit: ca-app-pub-3940256099942544/5224354917", color = Color.Gray, fontSize = 10.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isAdLoaded) "Status: Ready to Play ✅" else "Status: Ready (will load test ad)",
                                color = if (isAdLoaded) Color(0xFF81C784) else Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val activity = RewardedAdManager.findActivity(context)
                            if (activity != null) {
                                RewardedAdManager.showAd(
                                    activity = activity,
                                    onUserEarnedReward = { rewardItem ->
                                        showRewardedAdDialog = false
                                        rewardEarnedMessage = "🎉 Awesome! You earned ${rewardItem.amount} ${rewardItem.type}! Golden Stylus & Blueprint Textures are now active!"
                                    }
                                )
                            } else {
                                Toast.makeText(context, "Activity context not found", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Watch Video Ad", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    TextButton(
                        onClick = { showRewardedAdDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Maybe Later", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Celebration Reward Dialog
    if (rewardEarnedMessage != null) {
        Dialog(onDismissRequest = { rewardEarnedMessage = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF242B1E)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF81C784)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🎉", fontSize = 42.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "VIP Reward Unlocked!",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = rewardEarnedMessage ?: "",
                        color = Color(0xFFE8F5E9),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { rewardEarnedMessage = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Drawing!", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarItem(
    tool: ToolType,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFE8E8E8) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = when (tool) {
            ToolType.SELECT -> Icons.Default.NearMe
            ToolType.PEN -> Icons.Default.Edit
            ToolType.SMART -> Icons.Default.AutoFixHigh
            ToolType.SHAPES -> Icons.Default.Category
            ToolType.ERASER -> Icons.Default.AutoFixNormal
            ToolType.BOARD -> Icons.Default.FormatColorFill
            ToolType.TEXT -> Icons.Default.TextFields
            ToolType.FORMULA -> Icons.Default.Functions
            ToolType.TABLE -> Icons.Default.GridOn
            ToolType.LAYERS -> Icons.Default.Layers
            ToolType.IMAGE -> Icons.Default.Image
            ToolType.WEB_IMAGE -> Icons.Default.AddPhotoAlternate
            ToolType.CAMERA -> Icons.Default.PhotoCamera
            ToolType.SAVE -> Icons.Default.Save
            ToolType.SHARE -> Icons.Default.Share
            ToolType.PDF -> Icons.Default.PictureAsPdf
            ToolType.UNDO -> Icons.AutoMirrored.Filled.Undo
            ToolType.REDO -> Icons.AutoMirrored.Filled.Redo
            ToolType.CLEAR -> Icons.Default.DeleteOutline
            ToolType.OCR -> Icons.Default.DocumentScanner
            ToolType.IMPORT_PDF -> Icons.Default.UploadFile
            ToolType.STICKERS -> Icons.Default.EmojiEmotions
            ToolType.SCIENCE -> Icons.Default.Science
            ToolType.LASER -> Icons.Default.Highlight
            ToolType.RULER -> Icons.Default.Straighten
            ToolType.LASSO -> Icons.Default.Gesture
            ToolType.HAND_PAN -> Icons.Default.PanTool
            ToolType.CUSTOMIZE -> Icons.Default.Settings
            ToolType.HELP -> Icons.Default.HelpOutline
        }

        Icon(
            imageVector = icon,
            contentDescription = tool.label,
            tint = if (isSelected) Color.Black else Color(0xFF555555),
            modifier = Modifier.size(24.dp)
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = tool.label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.Black else Color(0xFF555555)
        )

        // Dot indicator below selected tool (matches screenshot 2)
        if (isSelected) {
            Spacer(Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}
