package chat.stoat.sheets

import android.graphics.RuntimeShader
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import chat.stoat.R
import chat.stoat.internals.TailwindColourScheme
import org.intellij.lang.annotations.Language

enum class ColourPickerMode {
    Sliders,
    Palette,
    Hex
}

private fun Color.asHsv(): Triple<Float, Float, Float> {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val delta = max - min

    val hue: Float = if (max == min) {
        0f
    } else {
        when (max) {
            red -> (60 * ((green - blue) / delta) + 360) % 360
            green -> (60 * ((blue - red) / delta) + 120) % 360
            blue -> (60 * ((red - green) / delta) + 240) % 360
            else -> throw IllegalStateException("Unexpected max value: $max")
        }
    }
    val saturation: Float = if (max == 0f) 0f else delta / max

    return Triple(hue, saturation, max)
}

@OptIn(ExperimentalMaterial3Api::class)
private fun DrawScope.drawThumbMargin(
    color: Color,
    sliderState: SliderState
) {
    // Area around the thumb to mimic material3 track
    val thumbOutlineXBase =
        (sliderState.value / sliderState.valueRange.endInclusive) * size.width
    drawRect(
        color,
        topLeft = Offset(thumbOutlineXBase - 8.dp.toPx(), 0f),
        size = androidx.compose.ui.geometry.Size(16.dp.toPx(), size.height)
    )
}

private fun Color.asHexString(): String {
    val alpha = (alpha * 255).toInt()
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}

// 11x11 palette of Tailwind colours
val palette = listOf(
    TailwindColourScheme.neutral[0],
    TailwindColourScheme.red[0],
    TailwindColourScheme.red[1],
    TailwindColourScheme.red[2],
    TailwindColourScheme.red[3],
    TailwindColourScheme.red[4],
    TailwindColourScheme.red[5],
    TailwindColourScheme.red[6],
    TailwindColourScheme.red[7],
    TailwindColourScheme.red[9],
    TailwindColourScheme.red[10],

    TailwindColourScheme.neutral[1],
    TailwindColourScheme.orange[0],
    TailwindColourScheme.orange[1],
    TailwindColourScheme.orange[2],
    TailwindColourScheme.orange[3],
    TailwindColourScheme.orange[4],
    TailwindColourScheme.orange[5],
    TailwindColourScheme.orange[6],
    TailwindColourScheme.orange[7],
    TailwindColourScheme.orange[9],
    TailwindColourScheme.orange[10],

    TailwindColourScheme.neutral[2],
    TailwindColourScheme.yellow[0],
    TailwindColourScheme.yellow[1],
    TailwindColourScheme.yellow[2],
    TailwindColourScheme.yellow[3],
    TailwindColourScheme.yellow[4],
    TailwindColourScheme.yellow[5],
    TailwindColourScheme.yellow[6],
    TailwindColourScheme.yellow[7],
    TailwindColourScheme.yellow[9],
    TailwindColourScheme.yellow[10],

    TailwindColourScheme.neutral[3],
    TailwindColourScheme.green[0],
    TailwindColourScheme.green[1],
    TailwindColourScheme.green[2],
    TailwindColourScheme.green[3],
    TailwindColourScheme.green[4],
    TailwindColourScheme.green[5],
    TailwindColourScheme.green[6],
    TailwindColourScheme.green[7],
    TailwindColourScheme.green[9],
    TailwindColourScheme.green[10],

    TailwindColourScheme.neutral[4],
    TailwindColourScheme.teal[0],
    TailwindColourScheme.teal[1],
    TailwindColourScheme.teal[2],
    TailwindColourScheme.teal[3],
    TailwindColourScheme.teal[4],
    TailwindColourScheme.teal[5],
    TailwindColourScheme.teal[6],
    TailwindColourScheme.teal[7],
    TailwindColourScheme.teal[9],
    TailwindColourScheme.teal[10],

    TailwindColourScheme.neutral[5],
    TailwindColourScheme.sky[0],
    TailwindColourScheme.sky[1],
    TailwindColourScheme.sky[2],
    TailwindColourScheme.sky[3],
    TailwindColourScheme.sky[4],
    TailwindColourScheme.sky[5],
    TailwindColourScheme.sky[6],
    TailwindColourScheme.sky[7],
    TailwindColourScheme.sky[9],
    TailwindColourScheme.sky[10],

    TailwindColourScheme.neutral[6],
    TailwindColourScheme.blue[0],
    TailwindColourScheme.blue[1],
    TailwindColourScheme.blue[2],
    TailwindColourScheme.blue[3],
    TailwindColourScheme.blue[4],
    TailwindColourScheme.blue[5],
    TailwindColourScheme.blue[6],
    TailwindColourScheme.blue[7],
    TailwindColourScheme.blue[9],
    TailwindColourScheme.blue[10],

    TailwindColourScheme.neutral[7],
    TailwindColourScheme.violet[0],
    TailwindColourScheme.violet[1],
    TailwindColourScheme.violet[2],
    TailwindColourScheme.violet[3],
    TailwindColourScheme.violet[4],
    TailwindColourScheme.violet[5],
    TailwindColourScheme.violet[6],
    TailwindColourScheme.violet[7],
    TailwindColourScheme.violet[9],
    TailwindColourScheme.violet[10],

    TailwindColourScheme.neutral[8],
    TailwindColourScheme.purple[0],
    TailwindColourScheme.purple[1],
    TailwindColourScheme.purple[2],
    TailwindColourScheme.purple[3],
    TailwindColourScheme.purple[4],
    TailwindColourScheme.purple[5],
    TailwindColourScheme.purple[6],
    TailwindColourScheme.purple[7],
    TailwindColourScheme.purple[9],
    TailwindColourScheme.purple[10],

    TailwindColourScheme.neutral[9],
    TailwindColourScheme.pink[0],
    TailwindColourScheme.pink[1],
    TailwindColourScheme.pink[2],
    TailwindColourScheme.pink[3],
    TailwindColourScheme.pink[4],
    TailwindColourScheme.pink[5],
    TailwindColourScheme.pink[6],
    TailwindColourScheme.pink[7],
    TailwindColourScheme.pink[9],
    TailwindColourScheme.pink[10],

    TailwindColourScheme.neutral[10],
    TailwindColourScheme.rose[0],
    TailwindColourScheme.rose[1],
    TailwindColourScheme.rose[2],
    TailwindColourScheme.rose[3],
    TailwindColourScheme.rose[4],
    TailwindColourScheme.rose[5],
    TailwindColourScheme.rose[6],
    TailwindColourScheme.rose[7],
    TailwindColourScheme.rose[9],
    TailwindColourScheme.rose[10],
)

@Language("AGSL")
const val CheckerboardShader = """
layout(color) uniform vec4 color1;
layout(color) uniform vec4 color2;
uniform float squareSize;

vec4 main(vec2 fragCoord) {
    // add 4 to make it less boring
    float x = floor((fragCoord.x + 4) / squareSize);
    float y = floor((fragCoord.y + 4) / squareSize);
    
    return mod(x + y, 2.0) == 0.0 ? color1 : color2;
}
"""

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ColumnScope.ColourPickerSheet(
    initialValue: Int,
    onColourSelected: (Int) -> Unit,
    onUseDefaultColour: () -> Unit,
    onDismiss: () -> Unit
) {
    var color by remember { mutableStateOf(Color(initialValue)) }
    var mode by remember { mutableStateOf(ColourPickerMode.Sliders) }

    val hueTrackColours = remember {
        (0..359).map {
            Color.hsv(it.toFloat(), 1f, 1f, 1f)
        }
    }

    val colorHsv by remember(color) { derivedStateOf { color.asHsv() } }

    val checkerboardShader = with(MaterialTheme.colorScheme) {
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RuntimeShader(CheckerboardShader).apply {
                    setFloatUniform("squareSize", 8f)
                    setColorUniform("color1", surface.toArgb())
                    setColorUniform("color2", surfaceVariant.toArgb())
                }
            } else {
                null
            }
        }
    }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val slidersLabel = stringResource(R.string.colour_picker_mode_sliders)
        val paletteLabel = stringResource(R.string.colour_picker_mode_palette)
        val hexLabel = stringResource(R.string.colour_picker_hex)

        ButtonGroup(
            modifier = Modifier.fillMaxWidth(),
            overflowIndicator = {}
        ) {
            toggleableItem(
                weight = 1f,
                checked = mode == ColourPickerMode.Sliders,
                onCheckedChange = { mode = ColourPickerMode.Sliders },
                label = slidersLabel
            )
            toggleableItem(
                weight = 1f,
                checked = mode == ColourPickerMode.Palette,
                onCheckedChange = { mode = ColourPickerMode.Palette },
                label = paletteLabel
            )
            toggleableItem(
                weight = 1f,
                checked = mode == ColourPickerMode.Hex,
                onCheckedChange = { mode = ColourPickerMode.Hex },
                label = hexLabel
            )
        }

        Spacer(Modifier.height(16.dp))

        AnimatedContent(targetState = mode, label = "picker mode") { pickerMode ->
            when (pickerMode) {
                ColourPickerMode.Sliders -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_hue),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                stringResource(
                                    R.string.colour_picker_hue_value_fmt,
                                    colorHsv.first.toInt()
                                ),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = colorHsv.first,
                            onValueChange = {
                                val (_, saturation, value) = colorHsv
                                color = Color.hsv(it, saturation, value)
                            },
                            valueRange = 0f..359f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = Color.hsv(
                                    colorHsv.first,
                                    1f,
                                    1f
                                )
                            ),
                            track = { state ->
                                with(MaterialTheme.colorScheme) {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                    ) {
                                        drawRoundRect(
                                            Brush.horizontalGradient(
                                                hueTrackColours,
                                                endX = size.width
                                            ),
                                            cornerRadius = CornerRadius(999f, 999f)
                                        )
                                        drawThumbMargin(surfaceContainerLow, state)
                                    }
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_saturation),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                (colorHsv.second * 100).toInt().toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = colorHsv.second,
                            onValueChange = {
                                val (hue, _, value) = colorHsv
                                color = Color.hsv(hue, it, value)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = Color.hsv(
                                    colorHsv.first,
                                    colorHsv.second,
                                    1f
                                )
                            ),
                            track = { state ->
                                with(MaterialTheme.colorScheme) {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                    ) {
                                        drawRoundRect(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.hsv(
                                                        colorHsv.first,
                                                        0f,
                                                        1f
                                                    ),
                                                    Color.hsv(
                                                        colorHsv.first,
                                                        1f,
                                                        1f
                                                    )
                                                ),
                                                endX = size.width
                                            ),
                                            cornerRadius = CornerRadius(999f, 999f)
                                        )
                                        drawThumbMargin(surfaceContainerLow, state)
                                    }
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_value),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                (colorHsv.third * 100).toInt().toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = colorHsv.third,
                            onValueChange = {
                                val (hue, saturation, _) = colorHsv
                                color = Color.hsv(hue, saturation, it)
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = Color.hsv(
                                    colorHsv.first,
                                    colorHsv.second,
                                    colorHsv.third
                                )
                            ),
                            track = { state ->
                                with(MaterialTheme.colorScheme) {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                    ) {
                                        drawRoundRect(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color.hsv(
                                                        colorHsv.first,
                                                        colorHsv.second,
                                                        0f
                                                    ),
                                                    Color.hsv(
                                                        colorHsv.first,
                                                        colorHsv.second,
                                                        1f
                                                    )
                                                ),
                                                endX = size.width
                                            ),
                                            cornerRadius = CornerRadius(999f, 999f)
                                        )
                                        drawThumbMargin(surfaceContainerLow, state)
                                    }
                                }
                            }
                        )

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                stringResource(R.string.colour_picker_alpha),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                (color.alpha * 100).toInt().toString(),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                        alpha = 0.6f
                                    )
                                )
                            )
                        }

                        Slider(
                            value = color.alpha,
                            onValueChange = { color = color.copy(alpha = it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors().copy(
                                thumbColor = color
                            ),
                            track = { state ->
                                with(MaterialTheme.colorScheme) {
                                    Canvas(
                                        Modifier
                                            .fillMaxWidth()
                                            .height(16.dp)
                                    ) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            checkerboardShader?.let { shader ->
                                                drawRoundRect(
                                                    ShaderBrush(shader),
                                                    cornerRadius = CornerRadius(999f, 999f)
                                                )
                                            }
                                        }
                                        drawRoundRect(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    color.copy(alpha = 0f),
                                                    color.copy(alpha = 1f)
                                                ),
                                                endX = size.width
                                            ),
                                            cornerRadius = CornerRadius(999f, 999f)
                                        )
                                        drawThumbMargin(surfaceContainerLow, state)
                                    }
                                }
                            }
                        )
                    }
                }

                ColourPickerMode.Palette -> {
                    BoxWithConstraints {
                        val boxMaxWidth = this.maxWidth

                        FlowRow(
                            maxItemsInEachRow = 11,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (colour in palette) {
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .clickable { color = colour }
                                        .size((boxMaxWidth - 80.dp) / 11)
                                        .background(colour)
                                )
                            }
                        }
                    }
                }

                ColourPickerMode.Hex -> {
                    var hex by remember(color) {
                        mutableStateOf(color.asHexString())
                    }
                    var isFocused by remember { mutableStateOf(false) }
                    val focusManager = LocalFocusManager.current

                    BackHandler(enabled = isFocused) {
                        focusManager.clearFocus()
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hex,
                            onValueChange = {
                                hex = if (it.isNotEmpty() && it[0] == '#') {
                                    it
                                } else {
                                    "#$it"
                                }
                            },
                            label = { Text(stringResource(R.string.colour_picker_hex_template)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                },
                        )

                        TextButton(
                            onClick = {
                                try {
                                    color = Color(android.graphics.Color.parseColor(hex))
                                } catch (e: IllegalArgumentException) {
                                    // Ignore
                                }
                            },
                            enabled = (hex.length == 9 || hex.length == 7) && hex[0] == '#' && hex.substring(
                                1
                            ).all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.colour_picker_hex_use))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.colour_picker_preview),
            style = MaterialTheme.typography.labelLarge
        )

        Canvas(
            Modifier
                .clip(MaterialTheme.shapes.large)
                .fillMaxWidth()
                .height(64.dp)
        ) {
            drawRect(color)
        }

        Spacer(Modifier.height(8.dp))

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextButton(onClick = onUseDefaultColour, Modifier.fillMaxWidth()) {
                Icon(painter = painterResource(R.drawable.ic_undo_24dp), null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.colour_picker_use_default))
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.colour_picker_cancel))
                }
                Button(
                    onClick = { onColourSelected(color.toArgb()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.colour_picker_apply))
                }
            }
        }
    }


}