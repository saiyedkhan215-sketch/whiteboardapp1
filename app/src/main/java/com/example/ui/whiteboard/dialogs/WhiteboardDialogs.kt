package com.example.ui.whiteboard.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*

val DEFAULT_COLORS = listOf(
    Color(0xFF000000), // Black
    Color(0xFFFF0000), // Red
    Color(0xFFFF9900), // Orange
    Color(0xFFFFFF00), // Yellow
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF), // Blue
    Color(0xFFFF00FF), // Magenta
    Color(0xFFFFFFFF)  // White
)

val EXTENDED_PALETTE = listOf(
    Color(0xFF000000), Color(0xFF424242), Color(0xFF757575), Color(0xFFBDBDBD), Color(0xFFFFFFFF),
    Color(0xFFE53935), Color(0xFFD81B60), Color(0xFF8E24AA), Color(0xFF5E35B1), Color(0xFF3949AB),
    Color(0xFF1E88E5), Color(0xFF039BE5), Color(0xFF00ACC1), Color(0xFF00897B), Color(0xFF43A047),
    Color(0xFF7CB342), Color(0xFFC0CA33), Color(0xFFFDD835), Color(0xFFFFB300), Color(0xFFFB8C00),
    Color(0xFFF4511E), Color(0xFF6D4C41), Color(0xFF546E7A), Color(0xFF00E676), Color(0xFF00E5FF)
)

// 1. PEN & BRUSH DIALOG
@Composable
fun PenDialog(
    currentBrush: BrushType,
    currentColor: Color,
    currentSize: Float,
    onBrushChange: (BrushType) -> Unit,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brush Types Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val brushes = listOf(
                        BrushType.PEN to Icons.Default.Edit,
                        BrushType.BRUSH to Icons.Default.Brush,
                        BrushType.MARKER to Icons.Default.BorderColor,
                        BrushType.HIGHLIGHT to Icons.Default.FlashlightOn,
                        BrushType.CALLIGRAPHY to Icons.Default.FormatPaint
                    )
                    brushes.forEach { (brush, icon) ->
                        val isSelected = currentBrush == brush
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF2196F3) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onBrushChange(brush) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = brush.label,
                                tint = if (isSelected) Color(0xFF2196F3) else Color.DarkGray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = brush.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE0E0E0))

                Text("Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(10.dp))

                // Colors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DEFAULT_COLORS.forEach { color ->
                        val isSelected = currentColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFFDDDDDD),
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(color) }
                        )
                    }

                    // Rainbow picker button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                                )
                            )
                            .clickable { showColorPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "More colors", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFE0E0E0))

                Text("Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Text(
                    text = "${currentSize.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(vertical = 2.dp)
                )

                Slider(
                    value = currentSize,
                    onValueChange = onSizeChange,
                    valueRange = 2f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showColorPicker) {
        CustomColorPickerDialog(
            onColorSelected = {
                onColorChange(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

// 2. ERASER DIALOG
@Composable
fun EraserDialog(
    currentSize: Float,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(10f, 20f, 40f, 60f, 80f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Eraser Size",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presets.forEach { preset ->
                        val isSelected = currentSize.toInt() == preset.toInt()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onSizeChange(preset) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Custom: ${currentSize.toInt()}",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Slider(
                    value = currentSize,
                    onValueChange = onSizeChange,
                    valueRange = 5f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3),
                        inactiveTrackColor = Color(0xFFE0E0E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// 3. SHAPES DIALOG
@Composable
fun ShapesDialog(
    selectedShape: ShapeType,
    isFilled: Boolean,
    onShapeSelect: (ShapeType) -> Unit,
    onFillToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val shapes2D = listOf(
        ShapeType.LINE, ShapeType.ARROW, ShapeType.RECT, ShapeType.SQUARE,
        ShapeType.CIRCLE, ShapeType.OVAL, ShapeType.TRIANGLE, ShapeType.RIGHT_TRIANGLE,
        ShapeType.STAR, ShapeType.PENTAGON, ShapeType.HEXAGON, ShapeType.DIAMOND,
        ShapeType.PARALLEL, ShapeType.TRAPEZOID, ShapeType.HEART, ShapeType.CROSS,
        ShapeType.SEMICIRCLE, ShapeType.SECTOR, ShapeType.RIGHT_ANGLE
    )

    val shapes3D = listOf(
        ShapeType.CUBE, ShapeType.CYLINDER, ShapeType.CONE, ShapeType.SPHERE, ShapeType.PYRAMID
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Shapes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Fill Shape Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatColorFill, contentDescription = null, tint = Color.DarkGray)
                        Spacer(Modifier.width(8.dp))
                        Text("Fill Shape", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    }
                    Switch(
                        checked = isFilled,
                        onCheckedChange = onFillToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF9C27B0)
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                Text("2D Shapes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                // 2D Grid
                Column {
                    shapes2D.chunked(4).forEach { rowShapes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowShapes.forEach { shape ->
                                ShapeItemButton(
                                    shape = shape,
                                    isSelected = selectedShape == shape,
                                    onClick = {
                                        onShapeSelect(shape)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowShapes.size < 4) {
                                repeat(4 - rowShapes.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                Text("3D Shapes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                // 3D Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    shapes3D.forEach { shape ->
                        ShapeItemButton(
                            shape = shape,
                            isSelected = selectedShape == shape,
                            onClick = {
                                onShapeSelect(shape)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShapeItemButton(
    shape: ShapeType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(0xFFE3F2FD) else Color.Transparent)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF2196F3) else Color(0xFFEEEEEE),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (shape) {
            ShapeType.LINE -> Icons.Default.HorizontalRule
            ShapeType.ARROW -> Icons.AutoMirrored.Filled.ArrowForward
            ShapeType.RECT, ShapeType.SQUARE -> Icons.Default.CropSquare
            ShapeType.CIRCLE, ShapeType.OVAL -> Icons.Default.Circle
            ShapeType.TRIANGLE, ShapeType.RIGHT_TRIANGLE -> Icons.Default.ChangeHistory
            ShapeType.STAR -> Icons.Default.StarOutline
            ShapeType.PENTAGON, ShapeType.HEXAGON, ShapeType.DIAMOND -> Icons.Default.Hexagon
            ShapeType.HEART -> Icons.Default.FavoriteBorder
            ShapeType.CROSS -> Icons.Default.Add
            ShapeType.CUBE -> Icons.Default.ViewInAr
            ShapeType.CYLINDER -> Icons.Default.Category
            else -> Icons.Default.Interests
        }

        Icon(
            imageVector = icon,
            contentDescription = shape.label,
            tint = if (isSelected) Color(0xFF2196F3) else Color.DarkGray,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = shape.label,
            fontSize = 11.sp,
            color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

// 4. BOARD COLOR & TEXTURE DIALOG
@Composable
fun BoardColorDialog(
    currentColor: Color,
    currentTexture: BoardTexture,
    onColorChange: (Color) -> Unit,
    onTextureChange: (BoardTexture) -> Unit,
    onDismiss: () -> Unit
) {
    var showCustomPicker by remember { mutableStateOf(false) }

    val darkColors = listOf(
        Color(0xFFFFFFFF), Color(0xFF1E1E1E), Color(0xFF2C3E50), Color(0xFF34495E), Color(0xFF1B4332),
        Color(0xFF3E2723), Color(0xFF004D40), Color(0xFF1A237E), Color(0xFF263238), Color(0xFF121212)
    )

    val pastelColors = listOf(
        Color(0xFFFFF9C4), Color(0xFFFFEBEE), Color(0xFFE8F5E9), Color(0xFFE3F2FD), Color(0xFFF3E5F5), Color(0xFFFFF3E0),
        Color(0xFFFCE4EC), Color(0xFFE0F7FA), Color(0xFFF1F8E9), Color(0xFFFFFDE7), Color(0xFFEDE7F6), Color(0xFFE0F2F1)
    )

    val mutedColors = listOf(
        Color(0xFFD7CCC8), Color(0xFFCFD8DC), Color(0xFFC5CAE9), Color(0xFFB0BEC5), Color(0xFFD1C4E9), Color(0xFFFFCCBC),
        Color(0xFFBCAAA4), Color(0xFFB2DFDB), Color(0xFFC8E6C9), Color(0xFFD7CCC8), Color(0xFF90A4AE), Color(0xFFB0BEC5)
    )

    val textures = BoardTexture.values().toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Board Color", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Text("Dark", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))

                // Dark Palette
                Column {
                    darkColors.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { color ->
                                val isSelected = currentColor.toArgb() == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFCCCCCC),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(color) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Pastel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))

                // Pastel Palette
                Column {
                    pastelColors.chunked(6).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { color ->
                                val isSelected = currentColor.toArgb() == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(color) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Muted", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(6.dp))

                // Muted Palette
                Column {
                    mutedColors.chunked(6).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { color ->
                                val isSelected = currentColor.toArgb() == color.toArgb()
                                Box(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(color) }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                // Rainbow custom color button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                            )
                        )
                        .clickable { showCustomPicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Custom Color", tint = Color.White)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                Text("Texture", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray, modifier = Modifier.align(Alignment.Start))
                Spacer(Modifier.height(8.dp))

                // Texture Options
                Column {
                    textures.chunked(5).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            row.forEach { tex ->
                                val isSelected = currentTexture == tex
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF9F9F9))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onTextureChange(tex) }
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (tex) {
                                            BoardTexture.PLAIN -> Icons.Default.Square
                                            BoardTexture.GRID, BoardTexture.GRAPH -> Icons.Default.GridOn
                                            BoardTexture.LINES, BoardTexture.NOTEBOOK -> Icons.Default.FormatAlignJustify
                                            BoardTexture.DOTS -> Icons.Default.Grain
                                            BoardTexture.CHALK -> Icons.Default.BlurOn
                                            BoardTexture.ISOMETRIC, BoardTexture.DIAMOND -> Icons.Default.Diamond
                                            BoardTexture.MUSIC -> Icons.Default.MusicNote
                                        },
                                        contentDescription = tex.label,
                                        tint = if (isSelected) Color(0xFF2196F3) else Color.DarkGray,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = tex.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomPicker) {
        CustomColorPickerDialog(
            onColorSelected = {
                onColorChange(it)
                showCustomPicker = false
            },
            onDismiss = { showCustomPicker = false }
        )
    }
}

// 5. TEXT DIALOG
@Composable
fun TextDialog(
    initialText: String = "",
    onAddText: (String, Color, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedSize by remember { mutableStateOf(48f) }
    val sizePresets = listOf(36f, 48f, 72f, 100f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add Text", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Button(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onAddText(text, selectedColor, selectedSize)
                                }
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Add")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Type here...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(14.dp))
                Text("Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DEFAULT_COLORS.forEach { color ->
                        val isSelected = selectedColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFFDDDDDD),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text("Size: ${selectedSize.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2196F3))
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sizePresets.forEach { size ->
                        val isSelected = selectedSize.toInt() == size.toInt()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFF2196F3) else Color(0xFFF0F0F0))
                                .clickable { selectedSize = size }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${size.toInt()}",
                                color = if (isSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Slider(
                    value = selectedSize,
                    onValueChange = { selectedSize = it },
                    valueRange = 24f..140f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3)
                    )
                )
            }
        }
    }
}

// 6. MATH FORMULA DIALOG
@Composable
fun FormulaDialog(
    onAddFormula: (String, Color, Float) -> Unit,
    onDismiss: () -> Unit
) {
    var formula by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var fontSize by remember { mutableStateOf(40f) }

    val quickInserts = listOf("a/b", "√x", "x²", "xₙ", "∫", "∑", "()", "=")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                    Text("Math Formula", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            if (formula.isNotBlank()) {
                                onAddFormula(formula, selectedColor, fontSize)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Add")
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (formula.isBlank()) {
                        Text("Preview will appear here", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        Text(
                            text = formula,
                            color = selectedColor,
                            fontSize = (fontSize * 0.6f).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("Formula") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(12.dp))
                Text("Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DEFAULT_COLORS.forEach { color ->
                        val isSelected = selectedColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFFDDDDDD),
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Font Size: ${fontSize.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2196F3))
                Slider(
                    value = fontSize,
                    onValueChange = { fontSize = it },
                    valueRange = 24f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF2196F3),
                        activeTrackColor = Color(0xFF2196F3)
                    )
                )

                Spacer(Modifier.height(8.dp))
                Text("Quick Insert", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickInserts.take(4).forEach { sym ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F3F4))
                                .clickable {
                                    val insertion = when (sym) {
                                        "a/b" -> "(a/b)"
                                        "√x" -> "√(x)"
                                        "x²" -> "x²"
                                        "xₙ" -> "xₙ"
                                        else -> sym
                                    }
                                    formula += insertion
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sym, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickInserts.drop(4).forEach { sym ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF1F3F4))
                                .clickable {
                                    formula += sym
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sym, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// 7. TABLE SELECTOR DIALOG
@Composable
fun TableDialog(
    onTableSelected: (rows: Int, cols: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedRows by remember { mutableStateOf(2) }
    var selectedCols by remember { mutableStateOf(2) }
    val maxRows = 8
    val maxCols = 8

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222222)),
            modifier = Modifier.padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$selectedRows rows × $selectedCols columns",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = {
                            onTableSelected(selectedRows, selectedCols)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Grid of interactive cells
                for (r in 1..maxRows) {
                    Row {
                        for (c in 1..maxCols) {
                            val isHighlighted = r <= selectedRows && c <= selectedCols
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isHighlighted) Color(0xFF2196F3) else Color(0xFF383838))
                                    .clickable {
                                        selectedRows = r
                                        selectedCols = c
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 8. LAYERS SHEET
@Composable
fun LayersSheet(
    layers: List<LayerInfo>,
    activeLayerId: String,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Layers", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Row {
                        IconButton(onClick = onAddLayer) {
                            Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = Color(0xFF2196F3))
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    layers.forEach { layer ->
                        val isSelected = layer.id == activeLayerId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFFEDE7F6) else Color(0xFFF9F9F9))
                                .clickable { onSelectLayer(layer.id) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleVisibility(layer.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Visibility",
                                        tint = if (layer.isVisible) Color(0xFF673AB7) else Color.Gray
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF673AB7) else Color.Gray)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = layer.name,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF673AB7) else Color.Black
                                )
                            }

                            if (layers.size > 1) {
                                IconButton(
                                    onClick = { onDeleteLayer(layer.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Layer", tint = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 9. STICKERS DIALOG
@Composable
fun StickersDialog(
    onStickerSelected: (stickerId: String, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Teaching Rewards", "Feedback Stamps", "Math Symbols")

    val teachingStickers = listOf(
        "star" to "Star",
        "trophy" to "Trophy",
        "medal" to "Medal",
        "crown" to "Crown",
        "a_plus" to "A+ Grade",
        "hundred" to "100%",
        "thumbs_up" to "Thumbs Up",
        "excellent" to "Excellent!",
        "good_job" to "Good Job!"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Stickers", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f)
                ) {
                    items(teachingStickers) { (id, title) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                            modifier = Modifier
                                .padding(6.dp)
                                .clickable {
                                    onStickerSelected(id, title)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val icon = when (id) {
                                    "star" -> "⭐"
                                    "trophy" -> "🏆"
                                    "medal" -> "🥇"
                                    "crown" -> "👑"
                                    "a_plus" -> "A+"
                                    "hundred" -> "💯"
                                    "thumbs_up" -> "👍"
                                    "excellent" -> "🌟"
                                    "good_job" -> "👏"
                                    else -> "🏷️"
                                }
                                Text(icon, fontSize = 32.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 10. SCIENCE SYMBOLS DIALOG
@Composable
fun ScienceDialog(
    onScienceSelected: (category: String, symbolKey: String, label: String, atomicNum: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Atomic Structure", "Flasks & Beakers", "Measuring")

    val atomicElements = listOf(
        Triple("H", "1", 1),
        Triple("He", "2", 2),
        Triple("Li", "3", 3),
        Triple("Be", "4", 4),
        Triple("B", "5", 5),
        Triple("C", "6", 6),
        Triple("N", "7", 7),
        Triple("O", "8", 8),
        Triple("F", "9", 9),
        Triple("Ne", "10", 10),
        Triple("Na", "11", 11),
        Triple("Mg", "12", 12)
    )

    val labEquip = listOf(
        Pair("flask", "Erlenmeyer Flask"),
        Pair("beaker", "Glass Beaker"),
        Pair("tube", "Test Tube"),
        Pair("burner", "Bunsen Burner")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Science", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(10.dp))

                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                                .clickable { selectedTab = index }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF2196F3) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (selectedTab == 0) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(atomicElements) { (sym, numStr, num) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clickable {
                                        onScienceSelected("atomic", sym, sym, num)
                                        onDismiss()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE74C3C)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(sym, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(numStr, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(labEquip) { (key, label) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .clickable {
                                        onScienceSelected("lab", key, label, 0)
                                        onDismiss()
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Science, contentDescription = null, tint = Color(0xFF00897B), modifier = Modifier.size(36.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// 11. LASER DIALOG
@Composable
fun LaserDialog(
    laserColor: Color,
    laserSize: Float,
    onColorChange: (Color) -> Unit,
    onSizeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val laserColors = listOf(
        Color(0xFFFF1744), // Bright Red
        Color(0xFF00E676), // Bright Green
        Color(0xFF2979FF), // Bright Blue
        Color(0xFFFFEA00), // Yellow
        Color(0xFFF50057), // Pink
        Color(0xFF00E5FF), // Cyan
        Color(0xFFFFFFFF)  // White
    )

    val sizePresets = listOf(4f, 8f, 16f, 24f, 32f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Laser Color", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    laserColors.forEach { color ->
                        val isSelected = laserColor.toArgb() == color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF673AB7) else Color(0xFFDDDDDD),
                                    shape = CircleShape
                                )
                                .clickable { onColorChange(color) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 14.dp), color = Color(0xFFEEEEEE))

                Text("Laser Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sizePresets.forEach { preset ->
                        val isSelected = laserSize.toInt() == preset.toInt()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Color(0xFFEDE7F6) else Color(0xFFF5F5F5))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF673AB7) else Color(0xFFE0E0E0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSizeChange(preset) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${preset.toInt()}",
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF673AB7) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Custom: ${laserSize.toInt()}", fontSize = 13.sp, color = Color.Gray)
                Slider(
                    value = laserSize,
                    onValueChange = onSizeChange,
                    valueRange = 2f..48f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF673AB7),
                        activeTrackColor = Color(0xFF673AB7)
                    )
                )
            }
        }
    }
}

// 12. CUSTOMIZE TOOLBAR DIALOG
@Composable
fun CustomizeToolbarDialog(
    toolOrder: List<ToolType>,
    enabledTools: Set<ToolType>,
    onToggleTool: (ToolType) -> Unit,
    onResetToolbar: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Customize Toolbar", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onResetToolbar) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                }

                Text("Drag to reorder tools\nEnable/disable tools in toolbar", fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.height(12.dp))

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    toolOrder.forEach { tool ->
                        val isEnabled = enabledTools.contains(tool)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF282828))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DragHandle, contentDescription = "Drag", tint = Color.Gray)
                                Spacer(Modifier.width(10.dp))
                                Text(tool.label, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (tool.isEssential) {
                                    Text("Essential", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { if (!tool.isEssential) onToggleTool(tool) },
                                    enabled = !tool.isEssential,
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50),
                                        disabledCheckedTrackColor = Color(0xFF555555),
                                        disabledCheckedThumbColor = Color(0xFF888888)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom Color Picker Dialog
@Composable
fun CustomColorPickerDialog(
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Select Color", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(EXTENDED_PALETTE) { color ->
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(1.dp, Color(0xFFCCCCCC), CircleShape)
                                .clickable {
                                    onColorSelected(color)
                                }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
