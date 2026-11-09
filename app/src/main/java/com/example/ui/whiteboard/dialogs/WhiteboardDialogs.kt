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
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.data.model.*
import com.example.ui.ads.BannerAdView
import com.example.ui.whiteboard.CanvasRenderer

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
        ShapeType.CUBE, ShapeType.CUBOID, ShapeType.CYLINDER, ShapeType.CONE,
        ShapeType.SPHERE, ShapeType.HEMISPHERE, ShapeType.PYRAMID, ShapeType.TRIANGULAR_PRISM,
        ShapeType.TORUS, ShapeType.HEXAGONAL_PRISM, ShapeType.CAPSULE
    )

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

                Text("3D Shapes (Detailed)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF9C27B0))
                Spacer(Modifier.height(8.dp))

                // 3D Grid in rows of 4
                Column {
                    shapes3D.chunked(4).forEach { rowShapes ->
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
            ShapeType.CUBE, ShapeType.CUBOID -> Icons.Default.ViewInAr
            ShapeType.CYLINDER -> Icons.Default.Category
            ShapeType.CONE -> Icons.Default.ChangeHistory
            ShapeType.SPHERE -> Icons.Default.Circle
            ShapeType.HEMISPHERE -> Icons.Default.Brightness3
            ShapeType.PYRAMID -> Icons.Default.Terrain
            ShapeType.TRIANGULAR_PRISM -> Icons.Default.ChangeHistory
            ShapeType.TORUS -> Icons.Default.DonutLarge
            ShapeType.HEXAGONAL_PRISM -> Icons.Default.Hexagon
            ShapeType.CAPSULE -> Icons.Default.Lens
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
    onAddText: (String, Color, Float, Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedSize by remember { mutableStateOf(48f) }
    var isHandwritingAnimated by remember { mutableStateOf(true) }
    var selectedHandStyle by remember { mutableStateOf("pen") }
    val sizePresets = listOf(36f, 48f, 72f, 100f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 620.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
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
                                    onAddText(text, selectedColor, selectedSize, isHandwritingAnimated, selectedHandStyle)
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(14.dp))

                // Handwriting Animation Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHandwritingAnimated) Color(0xFFF3E5F5) else Color(0xFFF8F9FA)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (isHandwritingAnimated) Color(0xFFBA68C8) else Color(0xFFEEEEEE),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✍️", fontSize = 22.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Hand Animation",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isHandwritingAnimated) Color(0xFF6A1B9A) else Color.Black
                                    )
                                    Text(
                                        "Draws text with animated hand & pen",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            Switch(
                                checked = isHandwritingAnimated,
                                onCheckedChange = { isHandwritingAnimated = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF9C27B0)
                                )
                            )
                        }

                        if (isHandwritingAnimated) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Select Hand & Pen Tool:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4A148C)
                            )
                            Spacer(Modifier.height(6.dp))
                            val styles = listOf(
                                "pen" to "✍️ Pen",
                                "fountain" to "🖋️ Fountain",
                                "marker" to "🖍️ Marker",
                                "pencil" to "✏️ Pencil"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                styles.forEach { (styleKey, label) ->
                                    val isSel = selectedHandStyle == styleKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Color(0xFF9C27B0) else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSel) Color(0xFF9C27B0) else Color(0xFFD1C4E9),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedHandStyle = styleKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else Color.Black,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            // Live Hand & Pen Preview
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sample = if (text.isNotBlank()) text.take(14) else "Whiteboard"
                                    val p = android.graphics.Paint().apply {
                                        color = selectedColor.toArgb()
                                        textSize = 28f
                                        isAntiAlias = true
                                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                                    }
                                    val startX = 20f
                                    val baseline = size.height * 0.65f
                                    drawIntoCanvas { c ->
                                        c.nativeCanvas.drawText(sample, startX, baseline, p)
                                    }
                                    val textW = p.measureText(sample)
                                    CanvasRenderer.drawHandHoldingPen(
                                        drawScope = this,
                                        tipX = startX + textW + 2f,
                                        tipY = baseline - 8f,
                                        baselineY = baseline,
                                        scale = 0.52f,
                                        handStyle = selectedHandStyle,
                                        isPenLifted = false
                                    )
                                }
                            }
                        }
                    }
                }

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
    var selectedCategoryTab by remember { mutableStateOf(0) }

    val categoryTabs = listOf("Algebra & Real", "Geometry & Trig", "Mensuration", "Physics & Chem")

    val algebraFormulas = listOf(
        "Euclid: a = bq + r (0 ≤ r < b)",
        "Quadratic: x = (-b ± √(b² - 4ac)) / (2a)",
        "Discriminant: D = b² - 4ac",
        "Polynomial: α + β = -b/a, αβ = c/a",
        "Cubic: α + β + γ = -b/a, αβ + βγ + γα = c/a",
        "AP n-th Term: aₙ = a + (n - 1)d",
        "AP Sum: Sₙ = (n/2)[2a + (n - 1)d]",
        "AP Sum Alt: Sₙ = (n/2)(a + l)",
        "(a + b)² = a² + 2ab + b²",
        "(a - b)² = a² - 2ab + b²",
        "a² - b² = (a - b)(a + b)",
        "(a + b + c)² = a² + b² + c² + 2ab + 2bc + 2ca"
    )

    val geometryTrigFormulas = listOf(
        "Pythagoras: AB² + BC² = AC²",
        "Distance: d = √[(x₂ - x₁)² + (y₂ - y₁)²]",
        "Section: ((m₁x₂ + m₂x₁)/(m₁+m₂), (m₁y₂ + m₂y₁)/(m₁+m₂))",
        "Midpoint: ((x₁ + x₂)/2, (y₁ + y₂)/2)",
        "Triangle Area: ½|x₁(y₂ - y₃) + x₂(y₃ - y₁) + x₃(y₁ - y₂)|",
        "sin θ = Opp/Hyp, cos θ = Adj/Hyp",
        "tan θ = Opp/Adj = sin θ / cos θ",
        "sin² θ + cos² θ = 1",
        "1 + tan² θ = sec² θ",
        "1 + cot² θ = cosec² θ",
        "sin(90° - θ) = cos θ, tan(90° - θ) = cot θ",
        "sec(90° - θ) = cosec θ"
    )

    val mensurationFormulas = listOf(
        "Circle Area: A = πr², Circumference = 2πr",
        "Sector Area: A = (θ / 360°) × πr²",
        "Arc Length: l = (θ / 360°) × 2πr",
        "Cylinder Vol: V = πr²h, CSA = 2πrh, TSA = 2πr(r + h)",
        "Cone Vol: V = ⅓πr²h, Slant Height l = √(r² + h²)",
        "Cone CSA = πrl, TSA = πr(r + l)",
        "Sphere Vol: V = ⁴⁄₃πr³, Surface Area = 4πr²",
        "Hemisphere Vol: V = ⅔πr³, CSA = 2πr², TSA = 3πr²",
        "Frustum Vol: V = ⅓πh(r₁² + r₂² + r₁r₂)",
        "Frustum CSA: πl(r₁ + r₂), l = √[h² + (r₁ - r₂)²]",
        "Mean: x̄ = Σfᵢxᵢ / Σfᵢ",
        "Mode = l + [(f₁ - f₀) / (2f₁ - f₀ - f₂)] × h"
    )

    val physicsChemFormulas = listOf(
        "Ohm's Law: V = I × R",
        "Resistance: R = ρ(L / A)",
        "Series R: Rₑ = R₁ + R₂ + R₃",
        "Parallel R: 1/Rₑ = 1/R₁ + 1/R₂ + 1/R₃",
        "Joule's Heating: H = I²Rt = VIt = (V²/R)t",
        "Electric Power: P = VI = I²R = V²/R",
        "Mirror Formula: 1/f = 1/v + 1/u",
        "Lens Formula: 1/f = 1/v - 1/u",
        "Magnification: m = -v/u = hᵢ/hₒ",
        "Power of Lens: P = 1 / f(in meters)",
        "Snell's Law: n = sin i / sin r",
        "Refractive Index: n₂₁ = v₁ / v₂"
    )

    val currentFormulas = when (selectedCategoryTab) {
        0 -> algebraFormulas
        1 -> geometryTrigFormulas
        2 -> mensurationFormulas
        else -> physicsChemFormulas
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 580.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                    Text("Class 10 Formulas", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = {
                            if (formula.isNotBlank()) {
                                onAddFormula(formula, selectedColor, fontSize)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Insert")
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (formula.isBlank()) {
                        Text("Tap any formula below or type custom formula", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        Text(
                            text = formula,
                            color = selectedColor,
                            fontSize = (fontSize * 0.55f).sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = formula,
                    onValueChange = { formula = it },
                    label = { Text("Formula Text") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(Modifier.height(10.dp))

                // Formula Categories
                ScrollableTabRow(
                    selectedTabIndex = selectedCategoryTab,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    indicator = {},
                    divider = {}
                ) {
                    categoryTabs.forEachIndexed { idx, title ->
                        val isSel = selectedCategoryTab == idx
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                                .clickable { selectedCategoryTab = idx }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                title,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color(0xFF1976D2) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Formula List Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentFormulas) { item ->
                        Surface(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                                .fillMaxWidth()
                                .clickable { formula = item },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = item,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row {
                        DEFAULT_COLORS.take(6).forEach { color ->
                            val isSelected = selectedColor.toArgb() == color.toArgb()
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(24.dp)
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
    val tabs = listOf("Teaching", "Stamps", "Reactions", "Math & Sci", "Office")

    val teachingStickers = listOf(
        Triple("star", "Star", "⭐"),
        Triple("trophy", "Trophy", "🏆"),
        Triple("medal", "Gold Medal", "🥇"),
        Triple("silver_medal", "Silver Medal", "🥈"),
        Triple("bronze_medal", "Bronze Medal", "🥉"),
        Triple("crown", "Crown", "👑"),
        Triple("a_plus", "A+ Grade", "🅰️"),
        Triple("hundred", "100%", "💯"),
        Triple("thumbs_up", "Thumbs Up", "👍"),
        Triple("excellent", "Excellent!", "🌟"),
        Triple("good_job", "Good Job!", "👏"),
        Triple("target", "Bullseye", "🎯"),
        Triple("grad_cap", "Graduate", "🎓"),
        Triple("books", "Books", "📚"),
        Triple("microscope", "Microscope", "🔬"),
        Triple("test_tube", "Science", "🧪")
    )

    val feedbackStamps = listOf(
        Triple("verified", "Verified", "✅"),
        Triple("needs_work", "Needs Work", "⚠️"),
        Triple("pin", "Important", "📌"),
        Triple("priority", "Urgent", "🚨"),
        Triple("question", "Question?", "❓"),
        Triple("idea", "Idea", "💡"),
        Triple("perfect", "Perfect", "💎"),
        Triple("well_done", "Well Done", "🎉"),
        Triple("star_student", "Top Effort", "🏅"),
        Triple("keep_going", "Keep Going", "🚀"),
        Triple("great_effort", "High Five", "🙌"),
        Triple("reviewed", "Inspected", "🔍"),
        Triple("rejected", "Needs Revision", "❌"),
        Triple("favorite", "Favorite", "❤️"),
        Triple("alarm", "Deadline", "⏰"),
        Triple("award", "Recognition", "🎖️")
    )

    val reactionStickers = listOf(
        Triple("smile", "Grinning", "😀"),
        Triple("heart_eyes", "In Love", "😍"),
        Triple("party", "Party", "🥳"),
        Triple("thinking", "Thinking", "🤔"),
        Triple("cool", "Cool", "😎"),
        Triple("mind_blown", "Shocked", "🤯"),
        Triple("rocket", "Rocket", "🚀"),
        Triple("fire", "Fire", "🔥"),
        Triple("sparkles", "Sparkles", "✨"),
        Triple("rainbow", "Rainbow", "🌈"),
        Triple("lightning", "Energy", "⚡"),
        Triple("coffee", "Coffee", "☕"),
        Triple("clap", "Applause", "👏"),
        Triple("bulb", "Inspiration", "💡"),
        Triple("heart", "Love", "❤️"),
        Triple("gem", "Gem", "💎")
    )

    val mathSciStickers = listOf(
        Triple("pi", "Pi", "π"),
        Triple("sum", "Sigma", "∑"),
        Triple("integral", "Integral", "∫"),
        Triple("sqrt", "Square Root", "√"),
        Triple("infinity", "Infinity", "∞"),
        Triple("not_equal", "Not Equal", "≠"),
        Triple("approx", "Approx", "≈"),
        Triple("plus_minus", "Plus Minus", "±"),
        Triple("delta", "Delta", "△"),
        Triple("omega", "Omega", "Ω"),
        Triple("atom", "Atom", "⚛️"),
        Triple("magnet", "Magnet", "🧲"),
        Triple("dna", "Genetics", "🧬"),
        Triple("compass", "Geometry", "🧭"),
        Triple("ruler", "Measurement", "📏"),
        Triple("gear", "Mechanics", "⚙️")
    )

    val officeStickers = listOf(
        Triple("clipboard", "Tasks", "📋"),
        Triple("calendar", "Schedule", "📅"),
        Triple("bar_chart", "Analytics", "📊"),
        Triple("trend_up", "Growth", "📈"),
        Triple("trend_down", "Decline", "📉"),
        Triple("lock", "Private", "🔒"),
        Triple("key", "Solution", "🔑"),
        Triple("mail", "Notice", "✉️"),
        Triple("megaphone", "Broadcast", "📢"),
        Triple("memo", "Notes", "📝"),
        Triple("folder", "Archive", "📁"),
        Triple("timer", "Stopwatch", "⏱️"),
        Triple("flag", "Milestone", "🚩"),
        Triple("shield", "Protected", "🛡️"),
        Triple("globe", "Global", "🌐"),
        Triple("chat", "Discussion", "💬")
    )

    val currentStickers = when (selectedTab) {
        0 -> teachingStickers
        1 -> feedbackStamps
        2 -> reactionStickers
        3 -> mathSciStickers
        else -> officeStickers
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp).padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sticker Library", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
                                .padding(horizontal = 3.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF5F5F5))
                                .clickable { selectedTab = index }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF1976D2) else Color.DarkGray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.weight(1f)
                ) {
                    items(currentStickers) { (id, title, icon) ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable {
                                    onStickerSelected(id, title)
                                    onDismiss()
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(icon, fontSize = 28.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    title,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray,
                                    maxLines = 1
                                )
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

// 18. WEB IMAGE DIALOG
@Composable
fun WebImageDialog(
    onUrlConfirmed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    val presets = listOf(
        "World Map" to "https://images.unsplash.com/photo-1524661135-423995f22d0b?w=600&auto=format&fit=crop&q=80",
        "Graph Paper" to "https://images.unsplash.com/photo-1606326608606-aa0b62935f2b?w=600&auto=format&fit=crop&q=80",
        "Solar System" to "https://images.unsplash.com/photo-1614728894747-a83421e2b9c9?w=600&auto=format&fit=crop&q=80",
        "Notebook Lines" to "https://images.unsplash.com/photo-1586075010923-2dd4570fb338?w=600&auto=format&fit=crop&q=80"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Import Web Image", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    placeholder = { Text("https://example.com/image.png") },
                    label = { Text("Image URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(14.dp))
                Text("Quick Educational Presets:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(110.dp)
                ) {
                    items(presets) { (title, url) ->
                        Button(
                            onClick = {
                                onUrlConfirmed(url)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.padding(4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(title, fontSize = 12.sp, color = Color(0xFF1E293B), maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (urlText.isNotBlank()) {
                                onUrlConfirmed(urlText.trim())
                                onDismiss()
                            }
                        },
                        enabled = urlText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                    ) {
                        Text("Insert Image")
                    }
                }
            }
        }
    }
}

// 19. OCR DIALOG
@Composable
fun OcrDialog(
    extractedText: String,
    isProcessing: Boolean,
    onScanGallery: () -> Unit,
    onScanCamera: () -> Unit,
    onInsertText: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var editableText by remember(extractedText) { mutableStateOf(extractedText) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).padding(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color(0xFF2563EB))
                        Spacer(Modifier.width(8.dp))
                        Text("Optical Recognition (OCR)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "🎁 Rewarded OCR: Watch test ad (ca-app-pub-3940256099942544/5224354917) to scan text",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onScanGallery,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Image", color = Color(0xFF0284C7), fontSize = 12.sp)
                    }
                    Button(
                        onClick = onScanCamera,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2FE)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Take Photo", color = Color(0xFF0284C7), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                if (isProcessing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                            Spacer(Modifier.height(10.dp))
                            Text("Recognizing text via ML Kit...", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = editableText,
                        onValueChange = { editableText = it },
                        placeholder = { Text("Extracted text will appear here. You can also edit it before inserting into the canvas.") },
                        label = { Text("Recognized Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(Modifier.height(10.dp))
                BannerAdView()
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (editableText.isNotBlank()) {
                                onInsertText(editableText.trim())
                                onDismiss()
                            }
                        },
                        enabled = editableText.isNotBlank() && !isProcessing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Add to Whiteboard")
                    }
                }
            }
        }
    }
}

// 20. ELEMENT EDIT DIALOG
@Composable
fun ElementEditDialog(
    selectedElement: CanvasElement?,
    onColorSelected: (Color) -> Unit,
    onFillToggle: () -> Unit,
    onShapePartColorSelected: ((String, Color) -> Unit)? = null,
    onUpdateTextAnimation: ((Boolean, String) -> Unit)? = null,
    onPlayTextAnimation: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (selectedElement == null) return

    val isShape = selectedElement is CanvasElement.Shape
    val isText = selectedElement is CanvasElement.Text
    val isFilled = if (selectedElement is CanvasElement.Shape) selectedElement.isFilled else false

    // For shape part coloring
    val shapeParts = remember(selectedElement) {
        if (selectedElement is CanvasElement.Shape) {
            getShapeDistinctParts(selectedElement.shapeType)
        } else emptyList()
    }
    var selectedPartKey by remember(selectedElement.id) {
        mutableStateOf(shapeParts.firstOrNull()?.key ?: "stroke")
    }

    // For text animation
    var isTextAnimated by remember(selectedElement.id) {
        mutableStateOf(if (selectedElement is CanvasElement.Text) selectedElement.isHandwritingAnimated else false)
    }
    var textHandStyle by remember(selectedElement.id) {
        mutableStateOf(if (selectedElement is CanvasElement.Text) selectedElement.handStyle else "pen")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Edit Style & Colors", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                if (selectedElement is CanvasElement.Shape) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FormatColorFill, contentDescription = null, tint = Color.DarkGray)
                            Spacer(Modifier.width(8.dp))
                            Text("Fill Shape Color", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                        }
                        Switch(
                            checked = isFilled,
                            onCheckedChange = { onFillToggle() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF00E676)
                            )
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFEEEEEE))

                    // Shape distinct parts section
                    Text(
                        text = "Color Distinct Parts (${selectedElement.shapeType.label})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Text(
                        text = "Select a face, cap, or part below, then pick its color:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))

                    // Part selection chips
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shapeParts.chunked(2).forEach { rowParts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowParts.forEach { part ->
                                    val isSelected = selectedPartKey == part.key
                                    val currColorArgb = selectedElement.partColors[part.key] ?: part.defaultColor
                                    val partColor = Color(currColorArgb)

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFFE3F2FD) else Color(0xFFF8F9FA))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color(0xFF2196F3) else Color(0xFFE0E0E0),
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedPartKey = part.key }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(partColor)
                                                .border(1.dp, Color.Gray, CircleShape)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = part.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color(0xFF1565C0) else Color.DarkGray,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (rowParts.size < 2) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    val currentPartName = shapeParts.find { it.key == selectedPartKey }?.name ?: "Selected Part"
                    Text(
                        text = "Apply Color to: $currentPartName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1976D2)
                    )
                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(EXTENDED_PALETTE) { color ->
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color(0xFFCCCCCC), CircleShape)
                                    .clickable {
                                        onShapePartColorSelected?.invoke(selectedPartKey, color)
                                    }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val activeColor = Color(selectedElement.partColors[selectedPartKey] ?: Color.Black.toArgb())
                            onColorSelected(activeColor)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply to Entire Shape", fontSize = 12.sp)
                    }
                } else if (selectedElement is CanvasElement.Text) {
                    Spacer(Modifier.height(12.dp))

                    // Text Animation Controls
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTextAnimated) Color(0xFFF3E5F5) else Color(0xFFF8F9FA)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isTextAnimated) Color(0xFFBA68C8) else Color(0xFFEEEEEE),
                                shape = RoundedCornerShape(14.dp)
                            )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("✍️", fontSize = 20.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            "Hand Drawing Animation",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isTextAnimated) Color(0xFF6A1B9A) else Color.Black
                                        )
                                        Text(
                                            "Animate letter-by-letter writing",
                                            fontSize = 10.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                Switch(
                                    checked = isTextAnimated,
                                    onCheckedChange = {
                                        isTextAnimated = it
                                        onUpdateTextAnimation?.invoke(it, textHandStyle)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF9C27B0)
                                    )
                                )
                            }

                            if (isTextAnimated) {
                                Spacer(Modifier.height(10.dp))
                                Text("Pen Tool Style:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A148C))
                                Spacer(Modifier.height(6.dp))
                                val styles = listOf(
                                    "pen" to "✍️ Pen",
                                    "fountain" to "🖋️ Fountain",
                                    "marker" to "🖍️ Marker",
                                    "pencil" to "✏️ Pencil"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    styles.forEach { (styleKey, label) ->
                                        val isSel = textHandStyle == styleKey
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSel) Color(0xFF9C27B0) else Color.White)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSel) Color(0xFF9C27B0) else Color(0xFFD1C4E9),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    textHandStyle = styleKey
                                                    onUpdateTextAnimation?.invoke(true, styleKey)
                                                }
                                                .padding(vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSel) Color.White else Color.Black,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = { onPlayTextAnimation?.invoke() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9C27B0)),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Replay Handwriting Animation", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Text("Text Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(150.dp)
                    ) {
                        items(EXTENDED_PALETTE) { color ->
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color(0xFFCCCCCC), CircleShape)
                                    .clickable {
                                        onColorSelected(color)
                                    }
                            )
                        }
                    }
                } else {
                    // For other elements (e.g. Strokes, Formulas, etc.)
                    Spacer(Modifier.height(12.dp))
                    Text("Select Color", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(Modifier.height(10.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.height(180.dp)
                    ) {
                        items(EXTENDED_PALETTE) { color ->
                            Box(
                                modifier = Modifier
                                    .padding(3.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(1.dp, Color(0xFFCCCCCC), CircleShape)
                                    .clickable {
                                        onColorSelected(color)
                                        onDismiss()
                                    }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

